package com.example.s1client.data.store

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * 瀑布流封面图缓存 —— **tid → 封面 URL 的本地持久化**。
 *
 * ## 为什么需要它
 * Discuz 的 `forumdisplay` 接口只对约 1/4 的帖子返回 `attachmentImagePreviewList`
 * （实测冷启动 89 条：21 有条、68 条没有，且不少是 `attachment=2` 却给 null）。
 * 开源 s1er 直接放弃列表封面图；我们的方案是：
 *
 * **用户进过详情页 → 详情页有图 → 顺手把封面 URL 存下来 → 下次瀑布流就有图。**
 *
 * 这样：
 * - 不额外发请求（零流量成本，不打扰服务端）；
 * - 渐进式生效 —— 看得越多、封面越全；
 * - 服务端不给的，第一次不显示也不强求（用户原话：「第一次不展示就算了」）。
 *
 * 与 [ReadingProgressStore] 同款用 SQLiteOpenHelper：项目没有 kapt/KSP，
 * 为一张 key-value 表引入 Room 要改构建系统，不值得。
 */
class CoverCacheStore private constructor(ctx: Context) :
    SQLiteOpenHelper(ctx.applicationContext, DB_NAME, null, DB_VERSION) {

    companion object {
        private const val DB_NAME = "cover_cache.db"
        private const val DB_VERSION = 1
        private const val TABLE = "cover"
        /** 只留最近 800 条，超出按时间淘汰 */
        private const val MAX_ROWS = 800

        @Volatile
        private var instance: CoverCacheStore? = null

        fun get(ctx: Context): CoverCacheStore =
            instance ?: synchronized(this) {
                instance ?: CoverCacheStore(ctx).also { instance = it }
            }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE $TABLE(
                tid TEXT PRIMARY KEY,
                url TEXT NOT NULL,
                updated_at INTEGER NOT NULL DEFAULT 0
            )"""
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // v1 暂无升级路径；加字段时 ALTER TABLE，别删表丢缓存
    }

    /** 记下某个帖子的封面图（用户进详情页看到图后调用） */
    fun put(tid: String, url: String) {
        if (tid.isBlank() || url.isBlank()) return
        val cv = ContentValues().apply {
            put("tid", tid)
            put("url", url)
            put("updated_at", System.currentTimeMillis())
        }
        writableDatabase.insertWithOnConflict(TABLE, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        trimOld(writableDatabase)
    }

    fun get(tid: String): String? {
        if (tid.isBlank()) return null
        readableDatabase.rawQuery("SELECT url FROM $TABLE WHERE tid = ?", arrayOf(tid)).use { c ->
            return if (c.moveToFirst()) c.getString(0) else null
        }
    }

    /** 批量取（瀑布流一次渲染 6~10 张卡，逐条查库不如一次查完） */
    fun getAll(tids: List<String>): Map<String, String> {
        if (tids.isEmpty()) return emptyMap()
        val out = HashMap<String, String>()
        val q = tids.filter { it.isNotBlank() }.chunked(400)
        for (batch in q) {
            val ph = batch.joinToString(",") { "?" }
            readableDatabase.rawQuery("SELECT tid, url FROM $TABLE WHERE tid IN ($ph)",
                batch.toTypedArray()).use { c ->
                while (c.moveToNext()) out[c.getString(0)] = c.getString(1)
            }
        }
        return out
    }

    fun clear(tid: String) {
        writableDatabase.delete(TABLE, "tid = ?", arrayOf(tid))
    }

    fun clearAll() {
        writableDatabase.delete(TABLE, null, null)
    }

    private fun trimOld(db: SQLiteDatabase) {
        db.execSQL(
            """DELETE FROM $TABLE WHERE tid NOT IN (
                SELECT tid FROM $TABLE ORDER BY updated_at DESC LIMIT $MAX_ROWS
            )"""
        )
    }
}
