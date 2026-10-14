package com.example.s1client.data.store

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * 本地阅读进度库 —— **用 SQLiteOpenHelper 而不是 Room**。
 *
 * 为什么不用 Room：项目当前没有 kapt/KSP 注解处理器，引入 Room 要改构建系统
 * （加 plugin + 改 compileOptions），对一个"记一行进度"的需求来说风险远大于收益。
 * SQLiteOpenHelper 零新依赖，且这张表的操作只有"按 tid 读一条 / 写一条"，不需要 ORM。
 *
 * 表设计：一个帖子一行，**只留最近 N 条**（超出按 updated_at 淘汰），
 * 避免表无限增长。
 *
 * 字段口径 **照搬 s1er 的 ReadingRecord**：权威字段是**绝对楼层 `floor`**
 * （1-based，跨页累计），`page` 只是存储冗余。原因：服务端的 perPage(ppp) 会变，
 * 只存页码的话换页数后落点会漂；存楼层则始终能重算出"第几页第几楼"。
 * `per_page` 也一起存，因为进页面时还不知道服务端的 ppp，得先用上次的值算页。
 */
class ReadingProgressStore private constructor(ctx: Context) :
    SQLiteOpenHelper(ctx.applicationContext, DB_NAME, null, DB_VERSION) {

    data class Progress(
        val tid: String,
        /** 最后一次读到的**绝对楼层**（1-based），进度与落点以它为准 */
        val floor: Int,
        /** 存储冗余：当时的页码（由 floor 与 per_page 推得） */
        val page: Int,
        /** 当时服务端自报的每页帖数（API 的 ppp），换页数后仍能正确重算页 */
        val perPage: Int,
        /** 当时视口里第一条楼层的 pid，兜底定位用 */
        val floorPid: String?,
        val scrollOffset: Int,
        val updatedAt: Long
    ) {
        /** 绝对楼层 → 所在页（1-based） */
        fun pageFor(ppp: Int): Int {
            val p = if (ppp > 0) ppp else if (perPage > 0) perPage else 40
            return if (floor <= 0) 1 else ((floor - 1) / p) + 1
        }
    }

    companion object {
        private const val DB_NAME = "reading_progress.db"
        private const val DB_VERSION = 2
        private const val TABLE = "progress"
        /** 只保留最近 300 个帖子的进度，超出按时间淘汰 */
        private const val MAX_ROWS = 300

        @Volatile
        private var instance: ReadingProgressStore? = null

        fun get(ctx: Context): ReadingProgressStore =
            instance ?: synchronized(this) {
                instance ?: ReadingProgressStore(ctx).also { instance = it }
            }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE $TABLE(
                tid TEXT PRIMARY KEY,
                floor INTEGER NOT NULL DEFAULT 0,
                page INTEGER NOT NULL DEFAULT 1,
                per_page INTEGER NOT NULL DEFAULT 0,
                floor_pid TEXT,
                scroll_offset INTEGER NOT NULL DEFAULT 0,
                updated_at INTEGER NOT NULL DEFAULT 0
            )"""
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // v1 → v2：补 floor / per_page 两列（绝对楼层口径）。
        // 用 ALTER TABLE 而不是 DROP：老用户的进度不该因为一次升级就清零。
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE $TABLE ADD COLUMN floor INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE $TABLE ADD COLUMN per_page INTEGER NOT NULL DEFAULT 0")
        }
    }

    /** 写入/更新某个帖子的进度（重复 tid 覆盖） */
    fun save(
        tid: String,
        page: Int,
        floor: Int,
        perPage: Int,
        floorPid: String?,
        scrollOffset: Int
    ) {
        if (tid.isBlank()) return
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("tid", tid)
            put("page", page)
            put("floor", floor)
            put("per_page", perPage)
            put("floor_pid", floorPid)
            put("scroll_offset", scrollOffset)
            put("updated_at", System.currentTimeMillis())
        }
        db.insertWithOnConflict(TABLE, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        trimOld(db)
    }

    fun load(tid: String): Progress? {
        if (tid.isBlank()) return null
        val db = readableDatabase
        db.rawQuery("SELECT * FROM $TABLE WHERE tid = ?", arrayOf(tid)).use { c ->
            if (!c.moveToFirst()) return null
            return Progress(
                tid = c.getString(c.getColumnIndexOrThrow("tid")),
                floor = c.getInt(c.getColumnIndexOrThrow("floor")),
                page = c.getInt(c.getColumnIndexOrThrow("page")),
                perPage = c.getInt(c.getColumnIndexOrThrow("per_page")),
                floorPid = c.getString(c.getColumnIndexOrThrow("floor_pid"))
                    ?.takeIf { it.isNotBlank() },
                scrollOffset = c.getInt(c.getColumnIndexOrThrow("scroll_offset")),
                updatedAt = c.getLong(c.getColumnIndexOrThrow("updated_at"))
            )
        }
    }

    /** 帖子被读完 / 用户主动清除时删掉，下次从第一页开始 */
    fun clear(tid: String) {
        writableDatabase.delete(TABLE, "tid = ?", arrayOf(tid))
    }

    fun clearAll() {
        writableDatabase.delete(TABLE, null, null)
    }

    /** 只留最近 MAX_ROWS 条，避免表无限增长 */
    private fun trimOld(db: SQLiteDatabase) {
        db.execSQL(
            """DELETE FROM $TABLE WHERE tid NOT IN (
                SELECT tid FROM $TABLE ORDER BY updated_at DESC LIMIT $MAX_ROWS
            )"""
        )
    }
}
