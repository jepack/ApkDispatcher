package com.jepack.dispatcher.util

import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.io.InterruptedIOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

class HttpUtil {

    companion object {
        fun getResult(url:String, callback: Callback, vararg headers: Pair<String, String>){
            val httpClient = OkHttpClient()
            val requestBuilder = Request.Builder()
            val request = requestBuilder.url(url).let{builder ->
                headers.forEach {
                    builder.addHeader(it.first, it.second)
                }
                builder
            }.get().build()

            val call = httpClient.newBuilder().readTimeout(8, TimeUnit.SECONDS)
                    .connectTimeout(8, TimeUnit.SECONDS)
                    .build().newCall(request)

            try {
                call.enqueue(callback)
            }catch (e:UnknownHostException){

            }catch (e:SocketTimeoutException){

            }catch (e: InterruptedIOException){

            }catch (e: IOException){

            }catch (e:Exception){

            }


        }
    }


}