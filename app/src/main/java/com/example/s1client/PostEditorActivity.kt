package com.example.s1client

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import com.example.s1client.util.applyS1Theme
import com.example.s1client.util.setupS1EdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import com.example.s1client.ui.theme.S1Theme
import com.example.s1client.util.S1Notify
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.s1client.data.api.RetrofitClient
import com.example.s1client.data.cache.ForumDataCache
import com.example.s1client.util.BbCode
import com.example.s1client.data.store.DraftStore
import com.example.s1client.data.store.SettingStore
import com.example.s1client.util.ImageUploader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.s1client.ui.design.AccentPink
import com.example.s1client.ui.design.BgColor
import com.example.s1client.ui.design.BgColorBottom
import com.example.s1client.ui.design.ChipBg
import com.example.s1client.ui.design.GlassBar
import com.example.s1client.ui.design.GlassDialog
import com.example.s1client.ui.design.GlassDialogLine
import com.example.s1client.ui.design.GlassFloat
import com.example.s1client.ui.design.GlassFloatLine
import com.example.s1client.ui.design.OnAccent
import com.example.s1client.ui.design.TextMain
import com.example.s1client.ui.design.TextSub

private data class PostResult(val ok: Boolean, val message: String = "")

/** 发帖：版面/分类信息走移动版 API 的 module=newthread，真正的提交走 web 的 forum.php?mod=post */
class PostEditorActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        applyS1Theme()
        super.onCreate(savedInstanceState)
        setupS1EdgeToEdge()

        val fid = intent.getStringExtra("fid") ?: ""
        val forumName = intent.getStringExtra("forumName") ?: ""
        val editTid = intent.getStringExtra("editTid") ?: ""
        val editPid = intent.getStringExtra("editPid") ?: ""

        setContent {
            S1Theme {
                PostEditorScreen(
                    initialFid = fid,
                    initialForumName = forumName,
                    editTid = editTid,
                    editPid = editPid,
                    onBack = { finish() }
                )
            }
        }
    }
}

@Composable
fun PostEditorScreen(
    initialFid: String,
    initialForumName: String,
    editTid: String = "",
    editPid: String = "",
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val editMode = editTid.isNotBlank() && editPid.isNotBlank()
    // 编辑模式内容来自服务端，不参与草稿；发帖草稿仍按版面分 key
    val draftKey = remember { if (editMode) "" else DraftStore.keyForPost(initialFid) }

    var forums by remember { mutableStateOf<List<ForumTab>>(emptyList()) }
    var fid by remember { mutableStateOf(initialFid) }
    var forumName by remember { mutableStateOf(initialForumName) }
    var types by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var typeRequired by remember { mutableStateOf(false) }
    var typeId by remember { mutableStateOf("") }
    var formhash by remember { mutableStateOf(ForumDataCache.formhash) }
    var subject by remember { mutableStateOf("") }
    var message by remember { mutableStateOf(TextFieldValue("")) }
    var posting by remember { mutableStateOf(false) }
    var loadingTypes by remember { mutableStateOf(false) }
    var showForumPicker by remember { mutableStateOf(false) }
    var uploading by remember { mutableStateOf(false) }
    // 本次编辑上传成功的附件 aid，提交时要一起带上，服务端才会把图挂到帖子上
    val attachAids = remember { mutableStateListOf<String>() }
    var posttime by remember { mutableStateOf("") }
    var loadingEdit by remember { mutableStateOf(false) }

    fun toast(s: String, type: S1Notify.Type = S1Notify.Type.INFO) =
        S1Notify.show(context, s, type)

    // 本地图片：选图 → 传 S1 附件接口 → 正文插入 [attachimg]aid[/attachimg]
    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        uploading = true
        toast("正在上传图片…")
        scope.launch {
            // uid/hash 跟着「发帖权限」渲染：编辑走编辑页，再兜底回复页
            val tokenPages = buildList {
                if (editMode) {
                    add(ImageUploader.editPage(editTid, editPid, fid))
                    add(ImageUploader.replyPage(editTid, fid))
                } else {
                    add(ImageUploader.newthreadPage(fid))
                }
            }
            try {
                runCatching { ImageUploader.upload(context, uri, tokenPages) }
                    .onSuccess { aid ->
                        attachAids.add(aid)
                        val nv = BbCode.insert(message, "[attachimg]$aid[/attachimg]")
                        message = nv
                        DraftStore.save(context, draftKey, nv.text)
                        toast("图片已插入", S1Notify.Type.SUCCESS)
                    }
                    .onFailure {
                        android.util.Log.e("S1Upload", "pick/upload failed", it)
                        toast(
                            "图片上传失败：" + (it.message ?: it.javaClass.simpleName),
                            S1Notify.Type.ERROR
                        )
                    }
            } finally {
                // try/finally：即使用户中途返回导致协程被取消，uploading 也会复位
                // （否则选择图片按钮会永久禁用）。失败/成功同样复位。
                uploading = false
            }
        }
    }

    // 编辑模式：先拉原帖表单（formhash / 标题 / 正文 BBCode / posttime）
    LaunchedEffect(Unit) {
        if (!editMode) return@LaunchedEffect
        loadingEdit = true
        runCatching {
            withContext(Dispatchers.IO) {
                val url = "https://stage1st.com/2b/forum.php?mod=post&action=edit" +
                        "&tid=$editTid&pid=$editPid&fid=$initialFid"
                val req = okhttp3.Request.Builder().url(url).build()
                val html = RetrofitClient.httpClient.newCall(req).execute()
                    .use { it.body?.string().orEmpty() }
                val doc = org.jsoup.Jsoup.parse(html)
                val fh = doc.selectFirst("input[name=formhash]")?.attr("value").orEmpty()
                val subj = doc.selectFirst("input[name=subject]")?.attr("value").orEmpty()
                val msg = doc.selectFirst("textarea[name=message]")?.text().orEmpty()
                val pt = doc.selectFirst("input[name=posttime]")?.attr("value").orEmpty()
                Triple(fh, subj, msg) to pt
            }
        }.onSuccess { pair ->
            val (fh, subj, msg) = pair.first
            if (fh.isNotBlank()) formhash = fh
            subject = subj
            if (msg.isNotBlank()) {
                // 编辑回填剥离旧尾巴（对标 s1er PostSignature.stripTrailing）：
                // 输入框不显示旧落款，提交时按当前设置重新追加
                val clean = SettingStore.stripTail(msg)
                message = TextFieldValue(clean, selection = TextRange(clean.length))
            }
            posttime = pair.second
        }.onFailure { toast("读取原帖失败：" + (it.message ?: ""), S1Notify.Type.ERROR) }
        loadingEdit = false
    }

    // 恢复上次没发完的草稿（编辑模式不参与）
    LaunchedEffect(Unit) {
        if (draftKey.isBlank()) return@LaunchedEffect
        val d = DraftStore.load(context, draftKey)
        if (d.isNotBlank() && message.text.isBlank()) {
            message = TextFieldValue(d, TextRange(d.length))
        }
    }

    // 全部可选版面（含子版块），用于切换发表版面
    LaunchedEffect(Unit) {
        runCatching {
            flattenForumTabs(RetrofitClient.api.getForumIndex().variables?.forumList.orEmpty())
        }.onSuccess { forums = it }
        if (fid.isNotBlank() && forumName.isBlank()) {
            forumName = forums.firstOrNull { it.fid == fid }?.name ?: ""
        }
    }

    // 版面变了就重新取该版面的分类（编辑模式不需要）
    LaunchedEffect(fid) {
        if (editMode || fid.isBlank()) return@LaunchedEffect
        loadingTypes = true
        runCatching { RetrofitClient.api.getNewThreadForm(fid = fid).variables }
            .onSuccess { v ->
                if (v != null) {
                    formhash = v.formhash?.takeIf { it.isNotBlank() } ?: formhash
                    types = v.threadTypes?.types.orEmpty()
                    typeRequired = v.threadTypes?.required == "1"
                    // 只有唯一分类时默认选中，省一步
                    typeId = if (types.size == 1) types.keys.first() else ""
                }
            }
        loadingTypes = false
    }

    fun submit() {
        if (!editMode) {
            if (fid.isBlank()) { toast("请先选择版面"); return }
            if (typeRequired && typeId.isBlank()) { toast("这个版面必须选分类"); return }
            if (subject.isBlank()) { toast("请填写标题"); return }
        }
        if (message.text.isBlank()) { toast("请填写内容"); return }
        if (formhash.isBlank()) { toast("获取 formhash 失败，请先登录"); return }

        posting = true
        scope.launch {
            // 注意：移动版 API 的 module=newthread 只负责给「表单信息」，不带提交功能
            // （带 postsubmit 打过去它照样只回表单 JSON）。真正的提交是 web 端的
            // forum.php?mod=post&action=newthread，提交标志叫 topicsubmit。
            val result = withContext(Dispatchers.IO) {
                try {
                    val url = if (editMode) {
                        "https://stage1st.com/2b/forum.php?mod=post&action=edit" +
                                "&tid=$editTid&pid=$editPid&fid=$initialFid&extra=&editsubmit=yes"
                    } else {
                        "https://stage1st.com/2b/forum.php?mod=post&action=newthread" +
                                "&fid=$fid&extra=&topicsubmit=yes"
                    }
                    val fb = okhttp3.FormBody.Builder()
                        .add("formhash", formhash)
                        .add("subject", subject.trim())
                        // 发帖小尾巴（对标 s1er）：仅在提交时拼接，不进输入框
                        .add("message", SettingStore.appendPostTail(context, message.text))
                        .add("wysiwyg", "0")
                    if (editMode) {
                        // 编辑要带原帖的 posttime，Discuz 用它校验是不是旧版本
                        fb.add("posttime", posttime)
                        fb.add("delete", "0")
                        fb.add("reason", "")
                        fb.add("editsubmit", "yes")
                    } else {
                        fb.add("typeid", typeId)
                        fb.add("topicsubmit", "yes")
                    }
                    // 本次上传的附件要显式声明「使用」，Discuz 才会把它挂到帖子上，
                    // 否则 [attachimg] 引用不到，图片不会显示
                    for (aid in attachAids) {
                        fb.add("attachnew[$aid][readperm]", "0")
                        fb.add("attachnew[$aid][price]", "0")
                        fb.add("attachnew[$aid][description]", "")
                    }
                    val req = okhttp3.Request.Builder().url(url).post(fb.build()).build()

                    // use{}：Response 必须关闭，否则连接不归还连接池。
                    // 成功判定不靠 URL 字符串（含 "thread-" 的列表页/失败页会误判），
                    // 而是解析响应体里的 Discuz 提示框 —— 对应旧 BUG-12。
                    val outcome = RetrofitClient.httpClient.newCall(req).execute().use { resp ->
                        val body = resp.body?.string().orEmpty()
                        val httpOk = resp.code in 200..299
                        val doc = org.jsoup.Jsoup.parse(body)
                        val tip = doc.selectFirst("#messagetext")?.text()?.trim()
                            ?: doc.selectFirst(".alert_right")?.text()?.trim()
                            ?: doc.selectFirst(".alert_error")?.text()?.trim()
                            ?: doc.selectFirst(".alert_info")?.text()?.trim()
                        val success = httpOk && (
                                tip != null && tip.isNotBlank() &&
                                        tip.contains("成功") && !tip.contains("失败") && !tip.contains("抱歉")
                                ) || (httpOk && body.contains("alert_right"))
                        if (success) null else (tip?.takeIf { it.isNotBlank() } ?: ("HTTP " + resp.code))
                    }
                    if (outcome == null) PostResult(ok = true)
                    else PostResult(ok = false, message = outcome)
                } catch (e: Exception) {
                    PostResult(ok = false, message = e.message ?: "")
                }
            }
            posting = false
            if (result.ok) {
                if (draftKey.isNotBlank()) DraftStore.clear(context, draftKey)
                message = TextFieldValue("")
                subject = ""
                attachAids.clear()
                toast(if (editMode) "修改成功" else "发表成功", S1Notify.Type.SUCCESS)
                onBack()
            } else {
                toast((if (editMode) "修改失败：" else "发表失败：") + result.message, S1Notify.Type.ERROR)
            }
        }
    }

    if (showForumPicker) {
        ForumChooseDialog(
            forums = forums,
            currentFid = fid,
            onDismiss = { showForumPicker = false },
            onPick = { tab ->
                fid = tab.fid
                forumName = tab.name
                showForumPicker = false
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BgColor, BgColorBottom)))
            .statusBarsPadding()
            // 键盘弹起按键盘高度垫、收起按手势条高度垫，保证底部工具栏不被切掉
            .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
    ) {
        // ===== 顶栏：返回 + 标题 + 发表 =====
        // drawBehind 闭包不是组合上下文，颜色要在组合期先取出来
        val topBarLine = GlassFloatLine
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .background(GlassBar)
                .drawBehind {
                    drawLine(
                        color = topBarLine,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.ArrowBack, contentDescription = "返回",
                    tint = TextMain, modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = if (editMode) "编辑帖子" else "发表新主题",
                color = TextMain,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
        }

        // ===== 版面选择（编辑模式隐藏：版面不可改）=====
        if (!editMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showForumPicker = true }
                    .padding(horizontal = 21.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("版面", color = TextSub, fontSize = 14.sp)
                Spacer(Modifier.width(16.dp))
                Text(
                    text = forumName.ifBlank { "请选择" },
                    color = if (forumName.isBlank()) TextSub else TextMain,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = TextSub,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // ===== 分类 chips（该版面有分类才显示）=====
        if (loadingTypes) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 21.dp, vertical = 5.dp)
            ) {
                CircularProgressIndicator(
                    color = AccentPink,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(16.dp)
                )
            }
        } else if (types.isNotEmpty()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 21.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (typeRequired) "分类*" else "分类",
                        color = TextSub,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(end = 3.dp)
                    )
                    types.forEach { (id, name) ->
                        val selected = id == typeId
                        Text(
                            text = name,
                            color = if (selected) OnAccent else TextSub,
                            fontSize = 13.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            modifier = Modifier
                                .clip(RoundedCornerShape(99.dp))
                                .background(if (selected) AccentPink else ChipBg)
                                .border(
                                    1.dp,
                                    if (selected) AccentPink else GlassFloatLine,
                                    RoundedCornerShape(99.dp)
                                )
                                .clickable { typeId = if (selected) "" else id }
                                .padding(horizontal = 13.dp, vertical = 5.dp)
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
        }

        // ===== 标题 =====
        BasicTextField(
            value = subject,
            onValueChange = { subject = it },
            singleLine = true,
            textStyle = TextStyle(color = TextMain, fontSize = 16.sp, fontWeight = FontWeight.Bold),
            cursorBrush = SolidColor(AccentPink),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 21.dp)
        ) {
            if (subject.isEmpty()) {
                Text("标题", color = TextSub, fontSize = 16.sp)
            }
            it()
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 21.dp, vertical = 13.dp)
                .height(1.dp)
                .background(GlassFloatLine)
        )

        // ===== 正文（与回帖共用 RichEditor）=====
        RichEditor(
            value = message,
            onValueChange = {
                message = it
                if (draftKey.isNotBlank()) DraftStore.save(context, draftKey, it.text)
            },
            placeholder = "正文…",
            onPickLocalImage = {
                if (!uploading) {
                    pickImage.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            },
            actions = {
                Text(
                    text = "取消",
                    color = TextSub,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(99.dp))
                        .clickable(enabled = !posting) { onBack() }
                        .padding(horizontal = 13.dp, vertical = 8.dp)
                )
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(99.dp))
                        .background(if (posting) GlassFloat else AccentPink)
                        .clickable(enabled = !posting) { submit() }
                        .padding(horizontal = 13.dp, vertical = 7.dp)
                ) {
                    if (posting) {
                        CircularProgressIndicator(
                            color = TextSub,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Text(
                            "发表",
                            color = OnAccent,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 21.dp)
        )
    }
}

/** 选版面：竖向列表，玻璃弹窗 */
@Composable
private fun ForumChooseDialog(
    forums: List<ForumTab>,
    currentFid: String,
    onDismiss: () -> Unit,
    onPick: (ForumTab) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        androidx.compose.material3.Surface(
            shape = RoundedCornerShape(20.dp),
            color = GlassDialog,
            border = androidx.compose.foundation.BorderStroke(1.dp, GlassDialogLine),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(vertical = 13.dp)) {
                Text(
                    text = "选择版面",
                    color = TextMain,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 21.dp)
                )
                Spacer(Modifier.height(12.dp))
                if (forums.isEmpty()) {
                    Text(
                        text = "版面列表加载中…",
                        color = TextSub,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 21.dp, vertical = 8.dp)
                    )
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                        items(forums.size) { index ->
                            val tab = forums[index]
                            val selected = tab.fid == currentFid
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onPick(tab) }
                                    .padding(horizontal = 21.dp, vertical = 13.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = tab.name,
                                    color = if (selected) AccentPink else TextMain,
                                    fontSize = 14.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                if (selected) {
                                    Text("当前", color = AccentPink, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
