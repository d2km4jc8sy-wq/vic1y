package com.example.s1client.data.api

import com.google.gson.JsonParseException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

suspend fun <T> safeApiCall(
    block: suspend () -> T
): ApiResult<T> = withContext(Dispatchers.IO) {
    try {
        ApiResult.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: SocketTimeoutException) {
        ApiResult.Failure(ApiError.Timeout(e))
    } catch (e: UnknownHostException) {
        ApiResult.Failure(ApiError.Network(e))
    } catch (e: HttpException) {
        ApiResult.Failure(ApiError.Server(e.code(), e.message(), e))
    } catch (e: JsonParseException) {
        ApiResult.Failure(ApiError.Parse(e))
    } catch (e: IOException) {
        ApiResult.Failure(ApiError.Network(e))
    } catch (e: Exception) {
        ApiResult.Failure(ApiError.Unknown(e))
    }
}
