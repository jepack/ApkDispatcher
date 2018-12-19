package com.jepack.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.jepack.dispatcher.SharedPreferencesUtil
import com.jepack.service.AppService

/**
 *
 */
class BootReceiver:BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        intent?.takeIf { it.action == Intent.ACTION_BOOT_COMPLETED }?.let{
            context?.startService(Intent(context, AppService::class.java))
            Toast.makeText(context, "已启动心跳服务", Toast.LENGTH_SHORT).show()
            SharedPreferencesUtil.instance("BOOT").saveData(context, "LAST_BOOT_RECEIVE_TIME", System.currentTimeMillis())
        }
    }
}