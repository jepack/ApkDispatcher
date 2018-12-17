package com.jepack.service

import android.app.Notification
import android.content.Intent
import android.os.IBinder
import android.preference.PreferenceManager
import android.widget.Toast
import com.google.gson.Gson
import com.jepack.apkinstaller.R
import com.jepack.installer.*
import com.jepack.util.Constants
import com.jepack.util.HttpUtil
import com.jepack.util.NotificationUtil
import com.sunfusheng.daemon.AbsHeartBeatService
import io.reactivex.functions.Consumer
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.File
import java.io.IOException
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.SocketException




class AppService: AbsHeartBeatService() {

    private var lastTimed:Long = System.currentTimeMillis()
    private val heartBeat:Long = 30 * 1000
    private var interval:Long = 5 * 60
    private val NOTIFICATION_ID = 412
    override fun onStartService() {

    }

    override fun onStopService() {
    }

    override fun getDelayExecutedMillis(): Long {
        return 0
    }

    override fun getHeartBeatMillis(): Long {
        return heartBeat
    }

    override fun onHeartBeat() {
        interval = PreferenceManager
                .getDefaultSharedPreferences(baseContext).getString(TextUtil.getString(R.string.pref_heart_beat_interval_key),
                        "5").toLong() * 60
        if(System.currentTimeMillis() - lastTimed > interval){
            lastTimed = System.currentTimeMillis()
            val server = PreferenceManager
                    .getDefaultSharedPreferences(baseContext)
                    .getString(baseContext.getString(R.string.pref_app_push_server_key), null)
            HttpUtil.getResult(server?: Constants.APP_PUSH, object : Callback{
                override fun onFailure(call: Call, e: IOException) {
                    LogUtil.e(e)
                    Toast.makeText(this@AppService, "获取推送数据异常", Toast.LENGTH_SHORT).show()
                }

                override fun onResponse(call: Call, response: Response) {

                    dealPushResult(response)
                }


            })
        }
    }

    /**
     * 下载推送的文件
     */
    private fun downloadPush(url:String, pkg:String, md5:String?){
        AppUtil().let {
            it.registerReceiver(Consumer {
                if(it.state == AppUtil.STATE.STATE_COMPLETE){
                    AppUtil.installApkByIntent(this@AppService, it.targetFile)
                }
            })
            it.downloadFile(url, "${AppUtil.getApkDir()}${File.separator}$pkg.apk", md5, 0)
        }
    }

    private fun dealPushResult(response:Response){
        val pushApp:PushApp? = Gson().fromJson(response.body()?.string(), PushApp::class.java)
        pushApp?:return
        if(SharedPreferencesUtil.instance("app_push").getData(AIApplication.getAppCtx(), "flag", 0) < pushApp.flag) {
            SharedPreferencesUtil.instance("app_push").saveData(AIApplication.getAppCtx(), "flag", pushApp.flag)

            pushApp.url.takeIf { it.isEmpty() }?:return

            val actIntent = Intent(baseContext, MainActivity::class.java)
            actIntent.putExtra("app_push_url", pushApp.url)
            actIntent.putExtra("app_push_app_code", pushApp.appCode)
            actIntent.putExtra("app_push_app_md5", pushApp.md5)
            actIntent.putExtra("app_push_app_pkg", pushApp.pkg)
            actIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

            var notification:Notification? = null
            if (pushApp.force) {
                notification = NotificationUtil.createNotification(baseContext, pushApp.title,
                        pushApp.desc, actIntent)
            } else {
                val code = AppUtil.checkInstallState(pushApp.pkg, pushApp.appCode)
                if (code == 2) {
                    notification = NotificationUtil.createNotification(baseContext, pushApp.title,
                            pushApp.desc, actIntent)
                }
            }

            notification?.let{
                notification.flags = notification.flags or Notification.FLAG_AUTO_CANCEL
                startForeground(NOTIFICATION_ID, it)
            }

            //是否自动发起安装
            val autoInstall = PreferenceManager
                    .getDefaultSharedPreferences(baseContext)
                    .getBoolean(baseContext.getString(R.string.pref_app_push_install_key), false)
            if(autoInstall){
                downloadPush(pushApp.url, pushApp.pkg, pushApp.md5)
            }

        }
    }

    private val logServer = AppServer(8909)
    companion object {
        var serverDebugLogUrl:String? = null
        var serverDlDebugLogUrl:String? = null
    }
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }


    private fun getIP(): String? {

        try {
            val en = NetworkInterface.getNetworkInterfaces()
            while (en.hasMoreElements()) {
                val el = en.nextElement()
                val enumIp = el.inetAddresses
                while (enumIp.hasMoreElements()) {
                    val netAddress = enumIp.nextElement()
                    if (!netAddress.isLoopbackAddress && netAddress is Inet4Address) {
                        return netAddress.getHostAddress().toString()
                    }
                }
            }
        } catch (ex: SocketException) {
            ex.printStackTrace()
        }

        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        synchronized(this) {
            try {
                if(!logServer.isAlive)
                    logServer.start()
                serverDebugLogUrl = "http://${getIP()}:${logServer.listeningPort}/log/debug"
                serverDlDebugLogUrl = "http://${getIP()}:${logServer.listeningPort}/log/dl_debug"
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

}