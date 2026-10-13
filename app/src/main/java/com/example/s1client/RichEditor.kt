package com.example.s1client

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.s1client.util.BbCode
import com.example.s1client.util.EmojiCode
import com.example.s1client.util.EmojiLib
import com.example.s1client.util.ImageUploader
import com.example.s1client.util.PostContentParser
import com.example.s1client.ui.design.AccentPink
import com.example.s1client.ui.design.ChipBg
import com.example.s1client.ui.design.GlassDialog
import com.example.s1client.ui.design.GlassDialogLine
import com.example.s1client.ui.design.GlassFloatLine
import com.example.s1client.ui.design.OnAccent
import com.example.s1client.ui.design.TextMain
import com.example.s1client.ui.design.TextSub

/** 本地附件（发出去后由服务端渲染成图）：[attachimg]aid[/attachimg] */
private val ATTACH_IMG_RE = Regex("""\[attachimg](\d+)\[/attachimg]""")

/**
 * 发帖 / 回帖共用的富文本编辑器。
 * - 输入与输出的都是 BBCode 纯文本
 * - 工具栏固定在输入框下方（贴着键盘），右侧一个「预览」
 * - 粘贴裸 URL 会自动包成 [url]…[/url]
 *
 * 调用方需给 [modifier] 一个**有界高度**（weight / heightIn 均可），
 * 组件内部正文区用 weight 撑满后自行滚动。
 *
 * [fillToBottom] = true 时正文区撑满剩余高度，工具栏贴着容器底部（发帖页用）；
 * = false 时工具栏紧贴正文内容（回帖面板用，配合 heightIn(min,max) 自适应高度）。
 */
@Composable
fun RichEditor(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "说点什么…",
    fillToBottom: Boolean = true,
    /** 传入时工具栏会出现「本地图片」按钮（由调用方负责选图+上传+插入） */
    onPickLocalImage: (() -> Unit)? = null,
    /** 底部行右侧插槽：调用方在这里放「取消 / 发送 / 发表」按钮 */
    actions: @Composable RowScope.() -> Unit = {}
) {
    var showLinkDialog by remember { mutableStateOf(false) }
    var showImageDialog by remember { mutableStateOf(false) }
    var showPreview by remember { mutableStateOf(false) }
    var showEmoji by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }

    // 始终以受控参数 value 为基准做变换，避免快照滞后导致光标/删除错乱
    fun edit(transform: (TextFieldValue) -> TextFieldValue) {
        onValueChange(transform(value))
    }

    Column(modifier = modifier) {
        // R4：实时表情预览条——输入框里每有一个表情短码/[img]，这里立刻以图片呈现。
        // （BasicTextField 公开 API 不支持文本内嵌图片，做了光标会乱跳；预览条是等价可见方案，宪法 11）
        // 点预览图 = 从正文撤销那张。
        val smileyCodes = remember(value.text) {
            Regex("""\[[a-g]:\d{1,4}]|\[img\][^\[]+\[/img\]""").findAll(value.text).map { it.value }.toList()
        }
        if (smileyCodes.isNotEmpty()) {
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp)
            ) {
                item {
                    Text(
                        "表情 ${smileyCodes.size}",
                        color = TextSub, fontSize = 11.sp,
                        modifier = Modifier.padding(end = 2.dp)
                    )
                }
                itemsIndexed(smileyCodes) { idx, code ->
                    val model = EmojiCode.resolve(context, code)
                        ?: if (code.startsWith("[img]")) code.removePrefix("[img]").removeSuffix("[/img]") else null
                    if (model != null) {
                        coil.compose.AsyncImage(
                            model = model,
                            contentDescription = "撤销该表情",
                            modifier = Modifier
                                .size(26.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(ChipBg)
                                .clickable {
                                    // 撤销第 idx 次出现的该表情
                                    val sb = StringBuilder()
                                    var from = 0
                                    var seen = 0
                                    var cutStart = -1
                                    var cutEnd = -1
                                    val re = Regex("""\[[a-g]:\d{1,4}]|\[img\][^\[]+\[/img\]""")
                                    for (m in re.findAll(value.text)) {
                                        if (m.value == code && seen == idx) {
                                            cutStart = m.range.first; cutEnd = m.range.last + 1
                                            break
                                        }
                                        if (m.value == code) seen++
                                    }
                                    if (cutStart >= 0) {
                                        sb.append(value.text.substring(0, cutStart))
                                        sb.append(value.text.substring(cutEnd))
                                        val nv = TextFieldValue(
                                            text = sb.toString(),
                                            selection = androidx.compose.ui.text.TextRange(cutStart.coerceAtMost(sb.length))
                                        )
                                        onValueChange(nv)
                                    }
                                }
                        )
                    }
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                // 撑满模式（发帖页）用 weight；自适应模式（回帖面板）直接把高度给输入框本身，
                // 否则 weight(fill=false) 不会把框撑高，多出来的高度会变成框下方的空隙
                .then(
                    // 回帖面板要「比页面小一圈」，输入框压矮一点，别把功能键挤没
                    if (fillToBottom) Modifier.weight(1f)
                    else Modifier.heightIn(min = 100.dp, max = 160.dp)
                )
                .clip(RoundedCornerShape(16.dp))
                .background(ChipBg)
                .border(1.dp, GlassFloatLine, RoundedCornerShape(16.dp))
                .verticalScroll(rememberScrollState())
                // 点输入区也能开始输入。
                // 注意：这里不能用 Modifier.clickable —— 它会监听并吞掉回车键，
                // 而多行文本框对回车是「不消费、交给上层」的，于是回车永远换不了行。
                // detectTapGestures 只认点击、不碰按键，正文里点哪光标就落哪。
                .pointerInput(Unit) {
                    detectTapGestures { focusRequester.requestFocus() }
                }
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = { onValueChange(BbCode.autoLink(value, it)) },
                textStyle = TextStyle(color = TextMain, fontSize = 15.sp, lineHeight = 22.sp),
                cursorBrush = SolidColor(AccentPink),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    // 回车换行：多行文本框自己不吃回车键，有的输入法又是以按键事件发过来的，
                    // 这里显式在光标处插入换行（preview 里吃掉，避免和输入法重复插入）
                    .onPreviewKeyEvent { e ->
                        if (e.type == KeyEventType.KeyDown &&
                            (e.key == Key.Enter || e.key == Key.NumPadEnter)
                        ) {
                            onValueChange(BbCode.insert(value, "\n"))
                            true
                        } else {
                            false
                        }
                    }
            ) {
                if (value.text.isEmpty()) {
                    Text(placeholder, color = TextSub, fontSize = 15.sp)
                }
                it()
            }
        }

        Spacer(Modifier.height(8.dp))

        // ===== 底部一行：功能图标（可横滑） + 调用方的取消/发送 =====
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ToolButton(Icons.Default.EmojiEmotions, "表情") { showEmoji = true }
                ToolButton(Icons.Default.FormatBold, "加粗") {
                    edit { BbCode.wrap(it, "[b]", "[/b]") }
                }
                ToolButton(Icons.Default.FormatItalic, "斜体") {
                    edit { BbCode.wrap(it, "[i]", "[/i]") }
                }
                ToolButton(Icons.Default.Link, "插入链接") { showLinkDialog = true }
                ToolButton(Icons.Default.Image, "网络图片") { showImageDialog = true }
                if (onPickLocalImage != null) {
                    ToolButton(Icons.Default.AddPhotoAlternate, "本地图片", onPickLocalImage)
                }
                ToolButton(Icons.Default.FormatQuote, "引用") {
                    edit { BbCode.wrap(it, "[quote]", "[/quote]") }
                }
                ToolButton(Icons.Default.Code, "代码") {
                    edit { BbCode.wrap(it, "[code]", "[/code]") }
                }
                ToolButton(Icons.Default.Visibility, "预览") { showPreview = true }
            }
            Spacer(Modifier.width(6.dp))
            actions()
        }
    }

    if (showLinkDialog) {
        LinkInputDialog(
            onDismiss = { showLinkDialog = false },
            onConfirm = { url, label ->
                edit {
                    if (label.isBlank()) BbCode.insert(it, "[url]$url[/url]")
                    else BbCode.insert(it, "[url=$url]$label[/url]")
                }
                showLinkDialog = false
            }
        )
    }

    if (showImageDialog) {
        UrlInputDialog(
            title = "插入网络图片",
            hint = "https://… 图片地址",
            onDismiss = { showImageDialog = false },
            onConfirm = { url ->
                edit { BbCode.insert(it, "[img]$url[/img]") }
                showImageDialog = false
            }
        )
    }

    if (showPreview) {
        val parsed = remember(value.text) { PostContentParser.parse(value.text) }
        // 预览正文：表情短码 [f:044] 与从 [img] 解析出的表情都渲染成内联小图
        val previewBody = parsed.text.toString().ifBlank { "（空）" }
        // 本地附件在正文里是 [attachimg]aid[/attachimg]，直接显示标签太难看：
        // 正文里换成占位文字，真图在下方用缩略图呈现
        val displayBody = remember(previewBody) {
            ATTACH_IMG_RE.replace(previewBody, "［图片］")
        }
        val attachAids = remember(previewBody) {
            ATTACH_IMG_RE.findAll(previewBody).map { it.groupValues[1] }.distinct().toList()
        }
        // 附件图必须带 key，key 要从「未使用附件」接口现取
        var attachUrls by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
        LaunchedEffect(attachAids) {
            attachUrls = if (attachAids.isEmpty()) emptyMap() else ImageUploader.attachmentImageUrls()
        }
        val emojiSp = 20.sp
        val emojiDp = with(LocalDensity.current) { emojiSp.toDp() }
        // 文本 + 「表情占位 id -> 图片地址」；内联内容在组合期再组装
        val previewPlan = remember(displayBody, parsed.emojiUrls) {
            val entries = mutableListOf<Pair<String, String>>()
            val re = Regex("""\[[a-g]:\d{1,4}]|\uFFFC""")
            var emojiIndex = 0
            val annotated = buildAnnotatedString {
                var last = 0
                for (m in re.findAll(displayBody)) {
                    append(displayBody.substring(last, m.range.first))
                    val token = m.value
                    val url = if (token == "\uFFFC") parsed.emojiUrls.getOrNull(emojiIndex++)
                    else EmojiCode.resolve(context, token)
                    if (url != null) {
                        val id = "e" + m.range.first
                        entries.add(id to url)
                        appendInlineContent(id, "\uFFFC")
                    } else {
                        append(token)
                    }
                    last = m.range.last + 1
                }
                append(displayBody.substring(last))
            }
            annotated to entries
        }
        val previewInline = previewPlan.second.associate { (id, url) ->
            id to InlineTextContent(
                Placeholder(emojiSp, emojiSp, PlaceholderVerticalAlign.TextCenter)
            ) {
                AsyncImage(model = url, contentDescription = null, modifier = Modifier.size(emojiDp))
            }
        }
        Dialog(onDismissRequest = { showPreview = false }) {
            EditorSurface {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("预览", color = TextMain, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = previewPlan.first,
                            inlineContent = previewInline,
                            color = TextMain,
                            fontSize = 14.sp,
                            lineHeight = 22.sp
                        )
                        // 图片缩略图：网络图片 + 刚插入的本地附件（按正文顺序排）
                        val thumbs = parsed.imageUrls + attachAids.mapNotNull { attachUrls[it] }
                        if (thumbs.isNotEmpty()) {
                            Spacer(Modifier.height(10.dp))
                            Text("图片 ${thumbs.size} 张", color = TextSub, fontSize = 12.sp)
                            thumbs.forEach { u ->
                                Spacer(Modifier.height(6.dp))
                                AsyncImage(
                                    model = u,
                                    contentDescription = null,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 220.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            "关闭",
                            color = AccentPink,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(99.dp))
                                .clickable { showPreview = false }
                                .padding(horizontal = 20.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }

    if (showEmoji) {
        EmojiPickerDialog(
            context = context,
            onDismiss = { showEmoji = false },
            onPick = { cat, file ->
                // *2017 系列用官方短码（[f:044]），服务端直接认；老表情退回 [img]
                val code = EmojiCode.shortCode(cat, file) ?: EmojiLib.bbcode(cat, file)
                edit { BbCode.insert(it, code) }
                // R3：插入后焦点立刻回输入框（用户拍板：不然不知道点了几张）
                focusRequester.requestFocus()
            }
        )
    }
}

/** 内置表情面板：顶部分类 Tab + 网格，点选插入短码（老表情回退 [img] 长链） */
@Composable
private fun EmojiPickerDialog(
    context: android.content.Context,
    onDismiss: () -> Unit,
    onPick: (cat: String, file: String) -> Unit
) {
    // R3 中文合并分类（用户拍板：英文文件夹名太杂）——
    // *2017 系列有官方短码各占一页；老表情合并成两页"经典/怀旧"，短码不可用回退 [img]
    val tabs = remember {
        val byCat = EmojiLib.categories(context).toMap()
        listOf(
            "常用" to listOf("face2017"),
            "萌宠" to listOf("animal2017"),
            "卡通" to listOf("carton2017"),
            "高达" to listOf("bundam2017"),
            "装置" to listOf("device2017"),
            "大鹅" to listOf("goose2017"),
            "经典" to listOf("face", "normal"),
            "怀旧" to listOf("animal", "carton", "dym", "flash", "nq", "zdl")
        ).map { (name, cats) ->
            name to cats.flatMap { c -> (byCat[c] ?: emptyList()).map { f -> c to f } }
        }.filter { it.second.isNotEmpty() }
    }
    var current by remember { mutableStateOf(0) }

    Dialog(onDismissRequest = onDismiss) {
        EditorSurface {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("表情", color = TextMain, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "关闭",
                        color = AccentPink,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(99.dp))
                            .clickable(onClick = onDismiss)
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }

                Spacer(Modifier.height(10.dp))

                if (tabs.isEmpty()) {
                    Text("表情库为空", color = TextSub, fontSize = 13.sp)
                } else {
                    val idx = current.coerceIn(0, tabs.size - 1)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        tabs.forEachIndexed { i, entry ->
                            val on = i == idx
                            Text(
                                text = entry.first,
                                color = if (on) OnAccent else TextSub,
                                fontSize = 12.sp,
                                fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(99.dp))
                                    .background(if (on) AccentPink else ChipBg)
                                    .border(
                                        1.dp,
                                        if (on) AccentPink else GlassFloatLine,
                                        RoundedCornerShape(99.dp)
                                    )
                                    .clickable { current = i }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    val cat = tabs[idx].second.firstOrNull()?.first ?: ""
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(8),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(tabs[idx].second) { (cat, file) ->
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ChipBg)
                                    .clickable { onPick(cat, file) },
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = EmojiLib.assetPath(cat, file),
                                    contentDescription = null,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolButton(icon: ImageVector, desc: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(ChipBg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = desc, tint = TextSub, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun EditorSurface(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = GlassDialog,
        border = BorderStroke(1.dp, GlassDialogLine),
        modifier = Modifier.fillMaxWidth()
    ) {
        content()
    }
}

@Composable
private fun EditorInput(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ChipBg)
            .border(1.dp, GlassFloatLine, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(color = TextMain, fontSize = 14.sp),
            cursorBrush = SolidColor(AccentPink),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (value.isEmpty()) Text(hint, color = TextSub, fontSize = 14.sp)
            it()
        }
    }
}

@Composable
private fun LinkInputDialog(
    onDismiss: () -> Unit,
    onConfirm: (url: String, label: String) -> Unit
) {
    var url by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        EditorSurface {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("插入链接", color = TextMain, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(14.dp))
                EditorInput(url, { url = it }, "https://… 链接地址")
                Spacer(Modifier.height(10.dp))
                EditorInput(label, { label = it }, "显示文字（可留空）")
                Spacer(Modifier.height(18.dp))
                DialogButtons(
                    onCancel = onDismiss,
                    onConfirm = { if (url.isNotBlank()) onConfirm(url.trim(), label.trim()) }
                )
            }
        }
    }
}

@Composable
private fun UrlInputDialog(
    title: String,
    hint: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var url by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        EditorSurface {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(title, color = TextMain, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(14.dp))
                EditorInput(url, { url = it }, hint)
                Spacer(Modifier.height(18.dp))
                DialogButtons(
                    onCancel = onDismiss,
                    onConfirm = { if (url.isNotBlank()) onConfirm(url.trim()) }
                )
            }
        }
    }
}

@Composable
private fun DialogButtons(onCancel: () -> Unit, onConfirm: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "取消",
            color = TextSub,
            fontSize = 15.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(99.dp))
                .clickable(onClick = onCancel)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "确定",
            color = OnAccent,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clip(RoundedCornerShape(99.dp))
                .background(AccentPink)
                .clickable(onClick = onConfirm)
                .padding(horizontal = 20.dp, vertical = 8.dp)
        )
    }
}
