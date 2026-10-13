package com.example.s1client.util

import android.content.Context

/** 发帖 / 回帖草稿本地暂存（SharedPreferences），正文为空时自动清掉该草稿 */
object DraftStore {

    private const val PREF = "s1_editor_drafts"
    private const val TS_SUFFIX = "__ts"

    /** 发帖草稿按版面分开存，避免在 A 版写的草稿跑到 B 版去 */
    fun keyForPost(fid: String = ""): String = "draft_post_$fid"

    fun keyForReply(tid: String): String = "draft_reply_$tid"

    fun save(context: Context, key: String, text: String) {
        val sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val ed = sp.edit()
        if (text.isBlank()) {
            ed.remove(key).remove(key + TS_SUFFIX)
        } else {
            ed.putString(key, text)
            // 记时间戳：草稿列表要按「最近编辑在前」排序
            ed.putLong(key + TS_SUFFIX, System.currentTimeMillis())
        }
        ed.apply()
    }

    fun load(context: Context, key: String): String =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(key, "").orEmpty()

    fun clear(context: Context, key: String) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().remove(key).remove(key + TS_SUFFIX).apply()
    }

    /** 一条草稿：key 里带类型（发帖/回帖）与目标 id，正文与最后编辑时间 */
    data class Entry(
        val key: String,
        val text: String,
        val updatedAt: Long,
        /** true = 发帖草稿（按版面），false = 回帖草稿 */
        val isPost: Boolean,
        /** 版面 fid（发帖）或帖子 tid（回帖） */
        val targetId: String
    ) {
        val summary: String
            get() = text.replace(Regex("\\s+"), " ").trim().let {
                if (it.length > 60) it.take(60) + "…" else it
            }
    }

    /**
     * 列出全部草稿（最近编辑在前）。
     * key 命名：draft_post_<fid> / draft_reply_<tid>
     */
    fun listDrafts(context: Context): List<Entry> {
        val sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val out = ArrayList<Entry>()
        sp.all.forEach { (k, v) ->
            if (k.endsWith(TS_SUFFIX)) return@forEach
            if (v !is String) return@forEach
            val text = v
            if (text.isBlank()) return@forEach
            val isPost = k.startsWith("draft_post_")
            val targetId =
                if (isPost) k.removePrefix("draft_post_") else k.removePrefix("draft_reply_")
            out.add(
                Entry(
                    key = k,
                    text = text,
                    updatedAt = sp.getLong(k + TS_SUFFIX, 0L),
                    isPost = isPost,
                    targetId = targetId
                )
            )
        }
        return out.sortedByDescending { it.updatedAt }
    }
}
