package com.jepack.dispatcher

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.SystemClock
import android.preference.PreferenceManager
import android.support.v7.recyclerview.extensions.ListAdapter
import android.support.v7.util.DiffUtil
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import com.jepack.dispatcher.R

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jepack.dispatcher.settings.SettingsActivity
import com.jepack.service.AppService
import com.jepack.util.Constants
import com.jepack.util.HttpUtil
import com.umeng.analytics.MobclickAgent
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable

import java.io.File
import java.util.ArrayList

import io.reactivex.functions.Consumer
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import widget.XItemDecoration
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private var url: EditText? = null
    private var progressBar: ProgressBar? = null

    private fun loadHistory(): ArrayList<String> {
        val historyStr = SharedPreferencesUtil.instance("default").getData(this@MainActivity, "APK_URL_HISTORY", "")
        return if (TextUtil.isEmpty(historyStr)) ArrayList() else Gson().fromJson<ArrayList<String>>(historyStr, ArrayList::class.java)

    }

    private val consumer = Consumer<AppUtil.DownloadMsg> { downloadMsg ->
        if (downloadMsg.state === AppUtil.STATE.STATE_COMPLETE) {
            if (downloadMsg.targetFile != null && File(downloadMsg.targetFile).exists()) {
                progressBar!!.progress = 100
                AppUtil.installApkByIntent(this@MainActivity, downloadMsg.targetFile)
            } else {
                Toast.makeText(this@MainActivity, "安装包不存在！", Toast.LENGTH_SHORT).show()
            }
        } else if (downloadMsg.state === AppUtil.STATE.STATE_DOWNLOADING) {
            LogUtil.d(downloadMsg.action.toString() + " " + downloadMsg.state + " " + downloadMsg.total + " " + downloadMsg.contentLength + " ")
            if (downloadMsg.contentLength > 0) {
                progressBar!!.progress = (downloadMsg.total * 1f / downloadMsg.contentLength * 100).toInt()
            }
        }else if(downloadMsg.state === AppUtil.STATE.STATE_ERROR) {
            Toast.makeText(this@MainActivity, "下载异常:(" + downloadMsg.url + " " + downloadMsg.md5 + ") " + downloadMsg.code + " " + downloadMsg.msg, Toast.LENGTH_SHORT).show()
        }
    }

    private var actionDisposable: Disposable? = null
    private lateinit var adapter:ListAdapter<String, MViewHolder>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        url = findViewById(R.id.tv_apk_address)
        val recyclerView = findViewById<RecyclerView>(R.id.rv_url_history)
        val itemCallback = object : DiffUtil.ItemCallback<String>() {

            override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
                return oldItem == newItem
            }
        }

        adapter = object : ListAdapter<String, MViewHolder>(itemCallback) {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MViewHolder {
                return MViewHolder(LayoutInflater.from(baseContext).inflate(R.layout.item_history_url, parent, false), consumer)
            }

            override fun onBindViewHolder(holder: MViewHolder, position: Int) {
                holder.update(getItem(position))
            }
        }
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        recyclerView.adapter = adapter
        recyclerView.addItemDecoration(XItemDecoration(LinearLayoutManager.VERTICAL, 0, 2, 0, 0, Color.WHITE))
        updateHistoryList(adapter)

        url!!.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) {
                SharedPreferencesUtil.instance("default").saveData(this@MainActivity, "APK_URL", url!!.text.toString())
                addUrl(url!!.text.toString())
                updateHistoryList(adapter)
            }
        }
        url!!.setText(SharedPreferencesUtil.instance("default").getData(this@MainActivity, "APK_URL", url!!.text.toString()))
        progressBar = findViewById(R.id.pg_progress)
        findViewById<View>(R.id.btn_install).setOnClickListener {
            val appUtil = AppUtil()
            val apkPath = ContextCompat.getExternalFilesDirs(AIApplication.getAppCtx(), "apks")[0].absolutePath + File.separator + "target.apk"
            appUtil.registerReceiver(consumer)
            appUtil.downloadFile(url!!.text.toString(), apkPath, null, 0)
        }

        findViewById<View>(R.id.btn_start_service).setOnClickListener {
            startService(Intent(this, AppService::class.java))

            pullServerAppCache()
        }

        startService(Intent(this, AppService::class.java))
        pullServerAppCache()
        actionDisposable = actionSubject.observeOn(AndroidSchedulers.mainThread()).subscribe {
            when (it.action) {
                0 -> {
                    it.arg?.let {
                        addUrl(it)
                        updateHistoryList(adapter)
                    }
                }
                1 -> {
                    it.arg?.let {
                        rmUrl(it)
                        updateHistoryList(adapter)
                    }
                }
                2 ->{
                    it.data?.let {
                        updateHistoryList(adapter, it as List<MutableMap<String, String>>)
                    }
                }
            }
        }

        btn_settings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        handleIntent(intent)

        if(SharedPreferencesUtil.instance("BOOT").getData(this, "LAST_BOOT_RECEIVE_TIME", 0L) < bootTime()){
            Toast.makeText(this, "建议设置开机启动以便更好的接收推送！", Toast.LENGTH_LONG).show()
        }

    }

    private fun pullServerAppCache(){
        val server = PreferenceManager
                .getDefaultSharedPreferences(baseContext)
                .getString(baseContext.getString(R.string.pref_app_cache_server_key), null)
        HttpUtil.getResult(server?: Constants.FIXED_APP_CACHE, object : Callback {
            override fun onFailure(call: Call, e: IOException) {

            }

            override fun onResponse(call: Call, response: Response) {
                response.takeIf { it.isSuccessful }?.let {
                    val type = object : TypeToken<List<MutableMap<String, String>>>() {}.type
                    val result = Gson().fromJson<List<MutableMap<String, String>>>(it.body()?.string(), type)
                    actionSubject.onNext(Action(2, null, result))
                }
            }

        })
    }
    // 返回开机时间，单位微妙
    private fun bootTime():Long {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            System.currentTimeMillis() - SystemClock.elapsedRealtimeNanos() / 1000000
        } else {
            System.currentTimeMillis() - SystemClock.elapsedRealtime()
        };
    }

    private fun updateHistoryList(adapter: ListAdapter<String, MViewHolder>, list:List<MutableMap<String, String>>? = null) {
        val items = loadHistory()
        list?.forEach {
            val url = it["url"]
            val name = it["name"]
            if(url != null) {
                items.add(0,"${name?:"Unknown"}: $url")
            }
        }
        adapter.submitList(items)
        adapter.notifyDataSetChanged()
    }

    private fun addUrl(url: String) {
        val history = loadHistory()
        if (!history.contains(url)) {
            history.add(url)
            SharedPreferencesUtil.instance("default").saveData(this@MainActivity, "APK_URL_HISTORY", Gson().toJson(history))
        }

        SharedPreferencesUtil.instance("default").saveData(this@MainActivity, "APK_URL", url)
    }


    private fun rmUrl(url: String) {
        val history = loadHistory()
        if (history.contains(url)) {
            history.remove(url)
            SharedPreferencesUtil.instance("default").saveData(this@MainActivity, "APK_URL_HISTORY", Gson().toJson(history))
        }
    }



    private inner class MViewHolder : RecyclerView.ViewHolder {
        private var urlBtn: Button? = null
        private var receiver: Consumer<AppUtil.DownloadMsg>? = null
        private var appUtil: AppUtil? = null

        constructor(itemView: View) : super(itemView) {
            urlBtn = itemView.findViewById(R.id.b_url)
        }

        constructor(itemView: View, receiver: Consumer<AppUtil.DownloadMsg>) : super(itemView) {
            this.receiver = receiver
            urlBtn = itemView.findViewById(R.id.b_url)
            appUtil = AppUtil()
            appUtil?.registerReceiver(receiver)
        }


        fun update(item: String) {
            urlBtn!!.text = item
            urlBtn!!.setOnClickListener {
                val url = urlBtn!!.text.toString()
                val appUtil = AppUtil()
                val apkPath = ContextCompat.getExternalFilesDirs(AIApplication.getAppCtx(), "apks")[0].absolutePath + File.separator + "target.apk"
                appUtil.registerReceiver(consumer)
                val actUrl = if(url.indexOf("https://") >= 0){
                    url.substring(url.indexOf("https://"))
                }else{
                    url.substring(url.indexOf("http://"))
                }
                appUtil.downloadFile(actUrl, apkPath, null, 0)
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?){
        val url = intent?.getStringExtra("app_push_url")
        val appCode = intent?.getIntExtra("app_push_app_code", 0)
        val md5 = intent?.getStringExtra("app_push_app_md5")
        val pkg = intent?.getStringExtra("app_push_app_pkg")
        if(url != null && md5 != null) {
            val appUtil = AppUtil()
            val apkPath = ContextCompat.getExternalFilesDirs(AIApplication.getAppCtx(), "apks")[0].absolutePath + File.separator + "target.apk"
            appUtil.registerReceiver(consumer)
            appUtil.downloadFile(url, apkPath, md5, 0)
        }
    }

    data class Action (
        var action: Int = 0,
        var arg: String? = null,
        var data:Any? = null
    )

    companion object {
        var actionSubject = PublishSubject.create<Action>()
    }

    override fun onPause() {
        super.onPause()
        MobclickAgent.onPause(this)
    }

    override fun onResume() {
        super.onResume()
        MobclickAgent.onResume(this)
    }
}
