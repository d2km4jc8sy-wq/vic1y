package com.example.s1client.data.api

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Failure(val error: ApiError) : ApiResult<Nothing>()
}

sealed class ApiError(val userMessage: String, val cause: Throwable? = null) {
    class Network(cause: Throwable) :
        ApiError("网络连接失败，请检查网络后重试", cause)
    class Timeout(cause: Throwable) :
        ApiError("请求超时，请稍后重试", cause)
    class Server(val code: Int, msg: String?, cause: Throwable? = null) :
        ApiError("服务器错误(" + code + ")" + (msg?.let { "：" + it } ?: ""), cause)
    class Parse(cause: Throwable) :
        ApiError("数据解析失败", cause)
    class Unknown(cause: Throwable) :
        ApiError("出错了：" + (cause.message ?: cause.javaClass.simpleName), cause)
}
