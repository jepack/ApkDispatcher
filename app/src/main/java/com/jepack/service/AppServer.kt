package com.jepack.service

import android.net.Uri
import com.jepack.installer.AIApplication
import com.jepack.installer.MainActivity
import com.jepack.installer.MainActivity.Companion.actionSubject
import com.jepack.installer.SharedPreferencesUtil
import fi.iki.elonen.NanoHTTPD
import java.net.URLDecoder

class AppServer(port:Int): NanoHTTPD(port) {

    override fun serve(session: IHTTPSession?): Response {
        return session?.uri?.let{url->
            val uri = Uri.parse(url)
            handleUri(uri, session.parameters)
        }?: super.serve(session)
    }

    private fun handleUri(uri: Uri?, params:Map<String, List<String>>): Response {
        return uri?.pathSegments?.takeIf { it.size >= 2 }?.let{
            val func = it[0]
            val act = it[1]
            when(func){
                "api" ->{
                    return handleApi(act, params)

                }else ->{
                    NanoHTTPD.newFixedLengthResponse(Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "Not Found")
                }
            }
        }?:NanoHTTPD.newFixedLengthResponse(Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "Not Found")
    }

    private fun handleApi(act:String, params:Map<String, List<String>>): Response {
       return when(act) {
            "add_url" -> {
                val url = params["url"]?.get(0) ?: return NanoHTTPD.newFixedLengthResponse(Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "Url is needed!")
                actionSubject.onNext(MainActivity.Action(0, URLDecoder.decode(url, "utf-8")))
                NanoHTTPD.newFixedLengthResponse(Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "Successfully added: $url")
            }
            "rm_url" ->{
                val url = params["url"]?.get(0) ?: return NanoHTTPD.newFixedLengthResponse(Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "Url is needed!")
                actionSubject.onNext(MainActivity.Action(1, URLDecoder.decode(url, "utf-8")))
                NanoHTTPD.newFixedLengthResponse(Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "Successfully rm: $url")
            }
           "push_flag" ->{
               NanoHTTPD.newFixedLengthResponse(Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "APP PUSH FLAG NOW: " + SharedPreferencesUtil.instance("app_push").getData(AIApplication.getAppCtx(), "flag", 0))
           }
           else ->{
               NanoHTTPD.newFixedLengthResponse(Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "Not Found")
           }
        }
    }

}