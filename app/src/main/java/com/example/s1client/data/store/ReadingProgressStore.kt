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
 */
class ReadingProgressStore private constructor(ctx: Context) :
    SQLiteOpenHelper(ctx.applicationContext, DB_NAME, null, DB_VERSION) {

    data class Progress(
        val tid: String,
        val page: Int,
        /** 当时视口里第一条楼层的 pid，用于在同一页内精确定位 */
        val floorPid: String?,
        val scrollOffset: Int,
        val updatedAt: Long
    )

    companion object {
        private const val DB_NAME = "reading_progress.db"
        private const val DB_VERSION = 1
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
                page INTEGER NOT NULL DEFAULT 1,
                floor_pid TEXT,
                scroll_offset INTEGER NOT NULL DEFAULT 0,
                updated_at INTEGER NOT NULL DEFAULT 0
            )"""
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // 当前是 v1，暂无升级路径。将来加字段时在这里 ALTER TABLE，别直接删表丢用户进度。
    }

    /** 写入/更新某个帖子的进度（重复 tid 覆盖） */
    fun save(tid: String, page: Int, floorPid: String?, scrollOffset: Int) {
        if (tid.isBlank()) return
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("tid", tid)
            put("page", page)
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
                page = c.getInt(c.getColumnIndexOrThrow("page")),
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
