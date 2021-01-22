package com.chuangdun.flutter.plugin.JyFaceCompare

import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.util.Log
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.Interface.onFacecompareAutnResult
import com.camera.CameraConstant
import com.camera.JYCamera
import com.camera.impl.CameraCallback
import com.common.Facecompare
import com.google.common.util.concurrent.*
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.platform.PlatformView
import java.io.ByteArrayOutputStream
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit


private const val EVENT_CAMERA_OPENED = 0
private const val EVENT_PREVIEW = 1
private const val EVENT_PREVIEW_STOP = 2
private const val EVENT_CAMERA_CLOSED = 3
private const val EVENT_COMPARE_START = 4
private const val EVENT_COMPARE_RESULT = 5
private const val TAG = "JyFaceCompareView"
class JyFaceCompareView(private val context: Context, messenger: BinaryMessenger, id: Int, createParams: Any) : PlatformView,
        MethodChannel.MethodCallHandler, EventChannel.StreamHandler{


    private val textureView: TextureView = TextureView(context)
    private val methodChannel = MethodChannel(messenger, "${VIEW_REGISTRY_NAME}_$id")
    private var eventChannel = EventChannel(messenger, "${VIEW_EVENT_REGISTRY_NAME}_$id")
    private val threadFactory = ThreadFactoryBuilder().setNameFormat("JyFaceComparePool_%d").build()
    private val mBlockingQueue = LinkedBlockingQueue<CompareTask>(1)
    private val threadPool = ThreadPoolExecutor(
            1, 1, 0L, TimeUnit.MILLISECONDS,
            LinkedBlockingQueue<Runnable>(), threadFactory)
    private val service = MoreExecutors.listeningDecorator(threadPool)
    private val uiHandler = Handler()
    private val handlerThread = HandlerThread("JyFaceCompareHandlerThread")
    private val subThreadHandler:Handler
    private var eventSink: EventChannel.EventSink? = null
    private val mCamera: JYCamera
    init {
        textureView.layoutParams = ViewGroup.LayoutParams(352, 288)
        methodChannel.setMethodCallHandler(this)
        eventChannel.setStreamHandler(this)
        handlerThread.start()
        subThreadHandler = initSubThreadHandler()
        mCamera = initCamera()
        initFaceCompare()
    }

    private fun initSubThreadHandler():Handler{
        return object : Handler(handlerThread.looper) {
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)
                when (msg.what) {
                    EVENT_COMPARE_START -> {
                        //开始人脸比对
                        uiHandler.post {  eventSink?.success(mapOf(
                                "event" to EVENT_COMPARE_START
                        ))}
                        startCompare()
                    }
                    else -> {
                    }
                }
            }
        }
    }

    private fun initCamera():JYCamera{
        return JYCamera.Builder(context)
                .setCameraType(CameraConstant.CAMERA_1)
                .setCameraPreviewSize(352, 288)
                .setCameraPictureSize(640, 480)
                .setCameraCallback(object : CameraCallback {
                    override fun onOpenedCamera() {
                        Log.d(TAG, "Camera opened.")
                        uiHandler.post {
                            eventSink?.success(mapOf(
                                    "event" to EVENT_CAMERA_OPENED
                            ))
                        }
                    }

                    override fun onPreviewFrame(data: ByteArray, bitmap: Bitmap, width: Int, height: Int) {
                        //Log.d(TAG, "Preview onFrame: width:$width, height:$height")
                        uiHandler.post {
                            eventSink?.success(mapOf(
                                    "event" to EVENT_PREVIEW
                            ))
                        }
                        mBlockingQueue.offer(CompareTask(bitmap, bitmap))
                    }

                    override fun onClosedCamera() {
                        Log.d(TAG, "Camera closed")
                        uiHandler.post {
                            eventSink?.success(mapOf(
                                    "event" to EVENT_CAMERA_CLOSED
                            ))
                        }
                    }

                    override fun onStopPreview() {
                        Log.d(TAG, "Preview stop")
                        uiHandler.post {
                            eventSink?.success(mapOf(
                                    "event" to EVENT_PREVIEW_STOP
                            ))
                        }
                    }
                })
                .build()
    }

    private fun initFaceCompare():Unit{
        Facecompare.getInstance().faceInit(context,
                onFacecompareAutnResult { result: Boolean, msg: String ->
                    Log.e(TAG, "人脸比对初始化结果:$result, $msg")
                    uiHandler.post {
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                })
    }

    private fun startCompare() {
        try {
            mBlockingQueue.clear()
            val compareTask: CompareTask = mBlockingQueue.take()
            val futureTask = ListenableFutureTask.create<CompareResult>(compareTask)
            service.submit(futureTask)
            Futures.addCallback(
                    futureTask,
                    object : FutureCallback<CompareResult> {
                        override fun onFailure(t: Throwable) {
                            Log.w(TAG, String.format("人脸特征提取--失败，线程: %s", Thread.currentThread().name), t)
                            subThreadHandler.sendEmptyMessage(EVENT_COMPARE_START)
                        }
                        override fun onSuccess(result: CompareResult?) {
                            if (result!!.similar < 85){
                                subThreadHandler.sendEmptyMessage(EVENT_COMPARE_START)
                            }else{
                                val outputStream = ByteArrayOutputStream()
                                result.bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                                uiHandler.post {  eventSink?.success(mapOf(
                                        "event" to EVENT_COMPARE_RESULT,
                                        "similar" to result.similar,
                                        "bitmap" to outputStream.toByteArray()
                                ))}
                            }
                        }
                    },
                    service)
        } catch (e: InterruptedException) {
            Log.e(TAG, "人脸比对异常", e)
            subThreadHandler.sendEmptyMessage(EVENT_COMPARE_START)
        }
    }


    override fun getView(): View {
        Log.i(TAG, "JyFaceCompareView:getView")
        return textureView
    }

    override fun onFlutterViewAttached(flutterView: View) {
        Log.i(TAG, "JyFaceCompareView:onFlutterViewAttached")
    }

    override fun onFlutterViewDetached() {
        Log.i(TAG, "JyFaceCompareView:onFlutterViewDetached")
    }

    override fun dispose() {
        Log.i(TAG, "JyFaceCompareView:dispose")
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        Log.i(TAG, "JyFaceCompareView:onMethodCall")
        when(call.method){
            "startPreview" -> {
                mCamera.doStartPreview(1, textureView)
            }
            "stopPreview" -> {
                mCamera.doStopPreview()
            }
            "stopCamera" -> {
                mCamera.doStopCamera()
            }
            "startCompare" -> {
                subThreadHandler.sendEmptyMessage(EVENT_COMPARE_START)
            }
            "releaseCamera" -> {
                mCamera.releaseAll()
            }
        }
    }

    override fun onListen(arguments: Any?, events: EventChannel.EventSink) {
        this.eventSink = events
    }

    override fun onCancel(arguments: Any?) {
        this.eventSink = null
    }
}