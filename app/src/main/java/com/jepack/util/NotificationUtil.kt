package com.jepack.util

import android.app.Notification
import android.app.PendingIntent
import android.app.PendingIntent.*
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.support.v4.app.NotificationCompat
import com.jepack.apkinstaller.R

/**
 * 显示通知栏工具类
 * Created by Administrator on 2016-11-14.
 */

object NotificationUtil {
    /**
     * 显示一个普通的通知
     *
     * @param context 上下文
     */
    fun createNotification(context: Context, title: String, content: String, actIntent: Intent):Notification {
        actIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        val pendingIntent = PendingIntent.getActivity(context, 200, actIntent, FLAG_UPDATE_CURRENT)
        val notificationBuilder = NotificationCompat.Builder(context, "com.jepack.apkinstaller.CHANNEL")
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setChannelId("com.jepack.apkinstaller.CHANNEL")
                .setContentText(content).setContentTitle(title)
                .setAutoCancel(true)
                .setSmallIcon(R.mipmap.ic_launcher).setSound(Uri.parse("android.resource://" + context.packageName + "/" + R.raw.clean_short))
        return notificationBuilder.build()
    }

}