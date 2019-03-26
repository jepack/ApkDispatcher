package com.jepack.dispatcher

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.preference.PreferenceManager
import android.support.v4.content.ContextCompat
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.buffer
import okio.sink
import okio.source
import java.io.File
import java.io.IOException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * 应用下载
 */
class AppUtil {

    val downloadSubject = PublishSubject.create<DownloadMsg>()
    private var downloadDisposable:Disposable? = null
    private var downloadMsgDisposable:Disposable? = null
    private var taskMap = mutableMapOf<Int, AtomicReference<STATE>>()
    init {
        downloadDisposable = downloadSubject.observeOn(Schedulers.io()).filter {
            it.action in arrayOf(ACTION.ACTION_PAUSE, ACTION.ACTION_START, ACTION.ACTION_STOP, ACTION.ACTION_CONTINUE)
        }.subscribe( {dlMsg->
            when(dlMsg.action){
                ACTION.ACTION_CONTINUE ->{
                    dlMsg.takeIf { dlMsg.url != null && it.targetFile != null && it.id != null && it.id is Int}?.let { msg ->
                        downloadFileImpl(taskMap[msg.id!!], downloadSubject, msg.url!!, null, msg.targetFile!!, msg.md5, id = msg.id as Int, continuePos = File(msg.targetFile).takeIf { it.exists() }?.length()?:0)
                    }
                }
                ACTION.ACTION_START -> {
                    dlMsg.takeIf { dlMsg.url != null && it.targetFile != null && it.id != null && it.id is Int}?.let {
                        taskMap[it.id!!] = taskMap[it.id!!]?: AtomicReference(STATE.STATE_STARTED)
                        downloadFileImpl(taskMap[it.id!!], downloadSubject, it.url!!, null, it.targetFile!!, it.md5, id = it.id as Int, continuePos = it.total)
                    }
                }
                ACTION.ACTION_PAUSE ->{

                }
                ACTION.ACTION_STOP ->{
                    dlMsg.id?.let {
                        taskMap[it]?.set(STATE.STATE_STOPPING)
                    }?:posAction(ACTION.ACTION_MSG, 0, 0, STATE.STATE_ERROR, "", "", "", code = CODE.CODE_IO_EXCEPTION.code, msg = "Task id must be specified")
                }
                else ->{

                }

            }
        },{
            downloadSubject.onNext(DownloadMsg(ACTION.ACTION_MSG, 0L, 0L, code = CODE.CODE_UNKNOWN_ERROR.code))
        })
    }

    fun registerReceiver(consumer: Consumer<DownloadMsg>){
        downloadMsgDisposable = downloadSubject.observeOn(AndroidSchedulers.mainThread()).filter {
            it.action == ACTION.ACTION_MSG
        }.subscribe( consumer, Consumer<Throwable>{

        })
    }

    enum class CODE(val code:Int) {
        CODE_IO_EXCEPTION(9000),
        CODE_CHECK_MD5_FAILED(9001),
        CODE_UNKNOWN_HOST(9002),
        CODE_NULL_RESPONSE(9003),
        CODE_UNKNOWN_ERROR(9004),
        CODE_NO_ERROR(9005);

    }

    enum class STATE {
        STATE_NONE,
        STATE_STARTED,
        STATE_WAITING,
        STATE_DOWNLOADING,
        STATE_STOPPED,
        STATE_STOPPING,
        STATE_ERROR,
        STATE_COMPLETE
    }

    enum class ACTION {
        ACTION_MSG,
        ACTION_PAUSE,
        ACTION_START,
        ACTION_STOP,
        ACTION_CONTINUE
    }

    fun downloadFile(url: String, targetFile: String, md5:String?, continuePos:Long = 0L):Int{
        val id = nextId()
        downloadSubject.onNext(DownloadMsg(ACTION.ACTION_START, continuePos,
                0, STATE.STATE_NONE,
                url,
                targetFile,
                md5,
                CODE.CODE_NO_ERROR.code,
                "",
                id
        ))
        return id
    }

    private fun setTaskState(taskReference:AtomicReference<STATE>?, state: STATE): STATE {
        taskReference?.set(state)
        return state
    }
    private fun downloadFileImpl(taskReference: AtomicReference<STATE>?, subject: Subject<DownloadMsg>,
                                 url: String, headers: Map<String, String>?, targetFile: String, md5:String?,
                                 id:Int?, continuePos:Long = 0L, autoResume: Boolean = false, autoDelete: Boolean = true){
        val checkMd5 = PreferenceManager
                .getDefaultSharedPreferences(AIApplication.getAppCtx())
                .getBoolean(AIApplication.getAppCtx().getString(R.string.pref_md5_check_key), true)
        posMsg(0, 0, setTaskState(taskReference, STATE.STATE_WAITING))

        val result = File(targetFile).takeIf { autoDelete && it.exists() }?.delete()
        if(result == true) LogUtil.d("File auto deleted!")

        val httpClient = OkHttpClient()
        val requestBuilder = Request.Builder()
        headers?.mapValues { (key, value) -> requestBuilder.addHeader(key, value) }
        addContinueHeader(requestBuilder, continuePos, 0)
        val request = requestBuilder.url(url).build()

        val call = httpClient.newBuilder().readTimeout(8, TimeUnit.SECONDS).connectTimeout(8, TimeUnit.SECONDS)
                .build().newCall(request)

        posMsg(continuePos, 0, setTaskState(taskReference, STATE.STATE_STARTED))
        var total = 0L
        var contentLength = 0L
        try {

            //取消
            taskReference?.get()?.takeIf { it in arrayOf(STATE.STATE_STOPPING, STATE.STATE_STOPPED) }?.let{ return }
            val response = call.execute()
            response?.takeIf { it.isSuccessful }?.body()?.let{ responseBody ->
                val source = responseBody.byteStream().source()
                val sink = File(targetFile).sink().buffer()
                var len = 0L
                val bufferSize = 20 * 1024L //200kb
                contentLength = responseBody.contentLength()

                //取消
                taskReference?.get()?.takeIf { it in arrayOf(STATE.STATE_STOPPING, STATE.STATE_STOPPED) }?.let{ return }

                while ((source.read(sink.buffer, bufferSize)).let{
                            len = it
                            it
                        } != -1L) {

                    //取消
                    taskReference?.get()?.takeIf { it in arrayOf(STATE.STATE_STOPPING, STATE.STATE_STOPPED) }?.let{ return }

                    sink.emit()
                    total += len
                    posMsg(total, contentLength, setTaskState(taskReference, STATE.STATE_DOWNLOADING), url, targetFile, md5, id = id)
                }

                //取消
                taskReference?.get()?.takeIf { it in arrayOf(STATE.STATE_STOPPING, STATE.STATE_STOPPED) }?.let{ return }

                //下载完成校验MD5
                if (checkMd5 && !TextUtil.isEmpty(md5)) {
                    if (!FileUtils.fileIsExpire(targetFile, md5!!.toUpperCase())) {
                        posMsg(total, contentLength, setTaskState(taskReference, STATE.STATE_COMPLETE), url, targetFile, md5, id = id)
                    } else {
                        posMsg(total, contentLength, setTaskState(taskReference, STATE.STATE_ERROR), url, targetFile, md5, code = CODE.CODE_CHECK_MD5_FAILED.code, id = id)
                    }
                } else if(response.isSuccessful){
                    posMsg(total, contentLength, setTaskState(taskReference, STATE.STATE_COMPLETE), url, targetFile, md5, id = id)
                }else{
                    posMsg(total, contentLength, setTaskState(taskReference, STATE.STATE_ERROR), url, targetFile, md5, code = CODE.CODE_UNKNOWN_ERROR.code, id = id)
                }
            }?: response?.let{subject.onNext(DownloadMsg(ACTION.ACTION_MSG, 0, 0, setTaskState(taskReference, STATE.STATE_ERROR), url, targetFile, md5, code = response.code(), id = id))}
            ?:posMsg(0, 0, setTaskState(taskReference, STATE.STATE_ERROR), url, targetFile, md5, code = CODE.CODE_NULL_RESPONSE.code, id = id)

        }catch (e: UnknownHostException){
            posMsg(0, 0, setTaskState(taskReference, STATE.STATE_ERROR), url, targetFile, md5, code = CODE.CODE_UNKNOWN_ERROR.code, id = id, msg = e.message)
        }catch (e: IOException){
            if(taskReference?.get() == STATE.STATE_DOWNLOADING) {
                posAction(ACTION.ACTION_CONTINUE, total, contentLength, setTaskState(taskReference, STATE.STATE_ERROR), url, targetFile, md5, code = CODE.CODE_UNKNOWN_ERROR.code, id = id, msg = e.message)
            }
            posMsg(0, 0, setTaskState(taskReference, STATE.STATE_ERROR), url, targetFile, md5, code = CODE.CODE_IO_EXCEPTION.code, id = id, msg = e.message)
        }catch (e:Exception){
            posMsg(0, 0, setTaskState(taskReference, STATE.STATE_ERROR), url, targetFile, md5, code = CODE.CODE_IO_EXCEPTION.code, id = id, msg = e.message)
        }
        //创建RequestBody
    }

    private fun addContinueHeader(builder:Request.Builder, rangeStart:Long, rangeEnd:Long){
        builder.addHeader("Range", "bytes:$rangeStart-${rangeEnd.takeIf { it > 0 }?:""}")
    }

    private fun posMsg(
            total:Long = 0,
            contentLength:Long = 0,
            state: STATE = STATE.STATE_NONE,
            url:String? = null,
            targetFile: String? = null,
            md5: String? = null,
            code:Int = CODE.CODE_NO_ERROR.code,
            msg:String? = null,
            id:Int? = null){
        posAction(ACTION.ACTION_MSG, total, contentLength, state, url, targetFile, md5, code, msg, id)
    }

    private fun posAction(action: ACTION,
                          total:Long = 0,
                          contentLength:Long = 0,
                          state: STATE = STATE.STATE_NONE,
                          url:String? = null,
                          targetFile: String? = null,
                          md5: String? = null,
                          code:Int = CODE.CODE_NO_ERROR.code,
                          msg:String? = null,
                          id:Int? = null){
        downloadSubject.onNext(DownloadMsg(action, total, contentLength, state, url, targetFile, md5, code, msg, id))

    }

    public companion object {

        private val taskIdGenerator = AtomicInteger(0)
        private fun nextId():Int{
            return taskIdGenerator.incrementAndGet()
        }

        fun installApkByIntent(context: Context, targetPath: String?) {
            //没有ROOT权限需要在Receiver中安装和启动应用
            if (targetPath != null) {
                val intent = Intent(Intent.ACTION_VIEW)

                intent.setDataAndType(Uri.fromFile(File(targetPath)),
                        "application/vnd.android.package-archive")
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
        }
        fun getApkDir():String{
            return ContextCompat.getExternalFilesDirs(AIApplication.getAppCtx(), "apks")[0].absolutePath + File.separator
        }

        fun checkInstallState(pkg:String, appCode:Int):Int{
            try {
                val pkgInfo = AIApplication.getAppCtx().packageManager.getPackageArchiveInfo(pkg, 0)
                pkgInfo?.let {
                    return if(it.versionCode < appCode){
                        2
                    }else{
                        1
                    }
                }?:return 0
            } catch (e: Exception) {
                return 0
            }
        }
    }


    class DownloadMsg(var action: ACTION,
                      var total:Long = 0L,
                      var contentLength:Long = 0L,
                      var state: STATE = STATE.STATE_NONE,
                      val url:String? = null,
                      val targetFile: String? = null,
                      val md5: String? = null,
                      var code:Int = CODE.CODE_NO_ERROR.code,
                      var msg:String? = null,
                      var id:Int? = null,
                      var autoResume:Boolean = false,
                      var autoDelete:Boolean = true
                      ){
        constructor(action: ACTION, state: STATE, id: Int?):this(action, 0, 0, state, id = id)
    }
}