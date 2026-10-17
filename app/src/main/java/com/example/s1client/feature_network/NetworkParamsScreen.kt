package com.example.s1client.feature_network

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.s1client.R
import com.example.s1client.data.api.RetrofitClient
import com.example.s1client.ui.theme.LocalS1Colors
import com.example.s1client.ui.theme.S1Theme
import com.example.s1client.util.applyS1Theme
import com.example.s1client.util.setupS1EdgeToEdge

/**
 * 网络参数（s1er 对标：网络参数页，PROBLEMS P8，2026-10-17）。
 *
 * 两条自救路径（默认全关 = 直连主域，零行为变化）：
 * - 强制替换基础网址：整站换到备用域名——Cookie 按域隔离，切换后需重新登录
 * - 强制使用 IP 列表：DNS 污染自救——OkHttp Dns 覆盖把论坛域钉到指定 IP，
 *   SNI/证书仍按原域名校验（比 s1er 的 Host 头方案更稳）
 *
 * 参数即时生效：任意改动经 RetrofitClient.applyNetworkOverride 下发快照，
 * 输入无效（解析不出 host / 无有效 IP）时自动回落主域并在输入框下方提示。
 */
class NetworkParamsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        applyS1Theme()
        super.onCreate(savedInstanceState)
        setupS1EdgeToEdge()
        setContent {
            S1Theme {
                NetworkParamsScreen(onFinish = { finish() })
            }
        }
    }

    companion object {
        fun intent(from: android.content.Context): Intent = Intent(from, NetworkParamsActivity::class.java)
    }
}

@Composable
fun NetworkParamsScreen(onFinish: () -> Unit) {
    val c = LocalS1Colors.current
    val context = androidx.compose.ui.platform.LocalContext.current

    var params by remember { mutableStateOf(com.example.s1client.feature_network.NetworkParamsStore.current(context)) }

    // 实时下发：写入 Store + 解析快照送 RetrofitClient（无效组合自动回落主域）
    fun commit(p: com.example.s1client.feature_network.NetworkParams) {
        params = p
        com.example.s1client.feature_network.NetworkParamsStore.save(context, p)
        RetrofitClient.applyNetworkOverride(
            RetrofitClient.parseNetworkOverride(p.forceBaseUrl, p.baseUrl, p.forceIp, p.ipList)
        )
    }

    val baseError = params.forceBaseUrl &&
            RetrofitClient.parseNetworkOverride(true, params.baseUrl, params.forceIp, params.ipList) == null
    val ipError = params.forceIp &&
            RetrofitClient.parseNetworkOverride(params.forceBaseUrl, params.baseUrl, true, params.ipList) == null

    S1Theme {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // —— 顶栏：返回 + 标题 ——
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_back),
                    contentDescription = "返回",
                    tint = c.textMain,
                    modifier = Modifier
                        .padding(8.dp)
                        .width(22.dp)
                        .height(22.dp)
                        .clickable(onClick = onFinish)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "网络参数",
                    color = c.textMain,
                    fontSize = 17.sp,
                    fontWeight = FontWeight(600)
                )
            }

            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 13.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "主域不可达时的自救开关。改动即时生效；换域名后需重新登录（Cookie 按域隔离）。",
                    color = c.textSub,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )

                // —— 强制替换基础网址 ——
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("强制替换基础网址", color = c.textMain, fontSize = 15.sp)
                    com.example.s1client.ui.S1Switch(
                        checked = params.forceBaseUrl,
                        onCheckedChange = { commit(params.copy(forceBaseUrl = it)) }
                    )
                }

                OutlinedTextField(
                    value = params.baseUrl,
                    onValueChange = { commit(params.copy(baseUrl = it)) },
                    label = { Text("基础网址") },
                    enabled = params.forceBaseUrl,
                    singleLine = true,
                    isError = baseError,
                    supportingText = {
                        if (baseError) Text("网址无效（需 https:// 开头且含域名）", color = Color(0xFFE5484D), fontSize = 11.sp)
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // —— 强制使用 IP 列表 ——
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("强制使用IP列表", color = c.textMain, fontSize = 15.sp)
                    com.example.s1client.ui.S1Switch(
                        checked = params.forceIp,
                        onCheckedChange = { commit(params.copy(forceIp = it)) }
                    )
                }

                OutlinedTextField(
                    value = params.ipList,
                    onValueChange = { commit(params.copy(ipList = it)) },
                    label = { Text("IP列表（多个用英文逗号分隔）") },
                    enabled = params.forceIp,
                    singleLine = true,
                    isError = ipError,
                    supportingText = {
                        Text(
                            if (ipError) "至少需要一个有效 IP" else "绕过 DNS 污染，直连服务器 IP",
                            color = if (ipError) Color(0xFFE5484D) else c.textSub,
                            fontSize = 11.sp
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
