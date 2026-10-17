package com.example.s1client.util

import android.graphics.Paint
import android.graphics.Typeface
import java.io.File

/**
 * 字体防破坏检查（2026-10-17 点5：导入预览 + 字形覆盖检测）。
 *
 * 目的：换字体最常见的破坏是「缺字」（生僻字/符号回落豆腐块）。
 * 导入前用抽样字符集跑 hasGlyph 给出覆盖率与缺字样本，设计者在
 * 预览对话框里先看渲染效果再决定是否应用——应用前不落盘。
 */
object FontInspector {

    /** 抽样字符集：常用汉字 + ASCII + 全半角标点（覆盖界面主要用字，约 600 字） */
    val SAMPLE: String =
        "一二三四五六七八九十百千万亿元角分天地人在有这中大为上个国我说生时到和也子过道对于身后主用己它回事所现分来去又如其才看明白出开关闭坐立行走飞入" +
        "的了吗呢吧啊呀哦嗯是在不没就都还也很被把让给向从按照根据通过由于所以但是而且或者虽然如果因为因此然后接着终于毕竟到底究竟简直几乎差不多可能必须应该需要能够可以正在进行已经曾经刚刚马上立刻现在过去将来" +
        "帖子主题回复楼层数量点赞收藏转发评论作者时间日期星期年月日时分秒用户名密码登录注册退出账号邮箱手机验证版块分类标签置顶精华热门最新旧刷新加载更多搜索筛选排序默认取消确认删除编辑修改保存应用设置选项偏好" +
        "主题颜色背景字体大小行高间距圆角边框阴影透明度动画过渡声音震动通知提醒消息私信聊天好友粉丝关注屏蔽拉黑举报投诉建议反馈帮助关于版本更新日志检查安装卸载缓存清理存储空间流量统计网络连接断开重连成功失败警告" +
        "错误异常崩溃修复优化性能体验流畅稳定安全隐私权限授权同意拒绝接受忽略跳过返回首页菜单栏工具箱新手引导常见问题解答客服支持社区论坛公告活动签到积分等级会员特权福利商城兑换充值提现余额钱包支付订单地址售后" +
        "退款换货评价分享邀请推荐奖励优惠券折扣秒杀拼团预售定金尾款分期免息审核驳回待定草稿发布上线定时撤回记录历史回滚恢复备份导入导出下载上传同步云端本地文件夹目录路径扩展名格式转换压缩解压加密签名验证令牌" +
        "会话黑名单屏蔽词过滤敏感词 Science technology stage1st 0123456789 ABCDEFGHIJKLMNOPQRSTUVWXYZ abcdefghijklmnopqrstuvwxyz" +
        "。，、；：？！「」『』（）《》〈〉【】…—·～＆＠＃％＾＊＋－＝｜＼／＄￥“”‘’"

    data class Coverage(val total: Int, val missingCount: Int, val missingSample: List<Char>) {
        val coveragePercent: Int get() = if (total == 0) 100 else (total - missingCount) * 100 / total
    }

    data class Preview(
        val name: String,
        val typeface: Typeface,
        val coverage: Coverage,
        val tmpFile: File,
        val uri: android.net.Uri
    )

    /** 对 [tf] 跑抽样覆盖检测（hasGlyph 逐字；抽样约 600 字，亚秒到秒级） */
    fun coverage(tf: Typeface): Coverage {
        val paint = Paint()
        paint.typeface = tf
        val chars = SAMPLE.toCharArray().distinct()
        val missing = chars.filter { !paint.hasGlyph(it.toString()) }
        return Coverage(chars.size, missing.size, missing.take(12))
    }
}
