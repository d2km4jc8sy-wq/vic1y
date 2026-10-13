package com.example.s1client.util

import com.example.s1client.data.api.RetrofitClient
import com.example.s1client.data.model.ProfileData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * 战斗力（Stage1st 的「鹅」点数）按 uid 缓存，来源是移动版 Profile API。
 *
 * 网页版 viewthread HTML 的楼层作者区只输出「战斗力   鹅」标签，数值由前端 JS 注入，
 * 静态 HTML 里拿不到；唯一稳定来源是 `getProfile(uid)` 的 creditItems 里标题含「战斗力」的条目
 * （UserCardDialog 也是这么取的）。所以这里按 uid 缓存，跨页复用，避免每楼一次请求。
 *
 * 用法：页面拿到一页楼层后调 [ensure]，传入本页所有 uid；任意一条新数据到达就回调 [onArrival]
 * （调用方用它触发列表原地刷新）。已缓存 / 正在抓的 uid 自动跳过。
 */
object PowerRepository {

    /** uid -> 战斗力展示串（如 "12345 鹅" 或纯数字） */
    private val cache = ConcurrentHashMap<String, String>()
    /** 正在请求中的 uid，避免并发重复拉取 */
    private val pending = ConcurrentHashMap<String, Unit>()

    /** 同步取缓存；没有就返回 null（调用方隐藏 pill） */
    fun get(uid: String?): String? =
        if (uid.isNullOrBlank()) null else cache[uid]

    /**
     * 确保一批 uid 的战斗力已就绪。
     * @param onArrival 每当有一条新数据写入缓存就调用一次（用于原地刷新列表，调用方自行防抖）
     */
    suspend fun ensure(uids: List<String>, onArrival: () -> Unit) {
        val need = uids.filter {
            it.isNotBlank() && !cache.containsKey(it) && !pending.containsKey(it)
        }
        if (need.isEmpty()) return

        // 限制并发，避免一页几十个作者同时打 Profile 接口把请求打爆
        val sem = Semaphore(6)
        need.map { uid ->
            kotlinx.coroutines.coroutineScope {
                async {
                    sem.acquire()
                    try {
                        if (cache.containsKey(uid)) return@async
                        pending[uid] = Unit
                        val resp = withContext(Dispatchers.IO) {
                            RetrofitClient.api.getProfile(uid = uid)
                        }
                        val p: ProfileData = withContext(Dispatchers.IO) {
                            JsonParser.parseProfile(
                                resp.variables?.space,
                                resp.variables?.extCredits,
                                uid
                            )
                        }
                        val power = p.creditItems.firstOrNull { it.title.contains("战斗力") }
                        val s = power?.let {
                            if (it.unit.isBlank()) it.value else "${it.value} ${it.unit}"
                        }
                        if (!s.isNullOrBlank()) {
                            cache[uid] = s
                            onArrival()
                        }
                    } catch (_: Exception) {
                        // 单条失败不阻塞整体；留待下次进入页面再试
                    } finally {
                        pending.remove(uid)
                        sem.release()
                    }
                }
            }
        }.awaitAll()
    }
}
