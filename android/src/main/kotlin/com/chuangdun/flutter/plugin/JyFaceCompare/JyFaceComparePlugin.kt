package com.chuangdun.flutter.plugin.JyFaceCompare

import android.content.Context
import android.os.Handler
import android.util.Log
import androidx.annotation.NonNull
import com.common.Facecompare
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

private const val TAG = "JyFaceComparePlugin"
const val VIEW_REGISTRY_NAME = "JyFaceCompareView"
const val VIEW_EVENT_REGISTRY_NAME = "JyFaceCompareViewEvent"
const val SDK_METHOD_REGISTRY_NAME = "JyFaceCompareSdk"
const val SDK_EVENT_REGISTRY_NAME = "JyFaceCompareSdkEvent"

/** JyFaceComparePlugin */
class JyFaceComparePlugin: FlutterPlugin,MethodChannel.MethodCallHandler, EventChannel.StreamHandler {
  private val uiHandler = Handler()
  private lateinit var context: Context
  private lateinit var sdkMethodChannel: MethodChannel
  private lateinit var sdkEventChannel: EventChannel
  private var eventSink: EventChannel.EventSink? = null

  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    context = flutterPluginBinding.applicationContext
    sdkMethodChannel = MethodChannel(flutterPluginBinding.binaryMessenger, SDK_METHOD_REGISTRY_NAME)
    sdkEventChannel = EventChannel(flutterPluginBinding.binaryMessenger, SDK_EVENT_REGISTRY_NAME)
    sdkMethodChannel.setMethodCallHandler(this)
    sdkEventChannel.setStreamHandler(this)
    val viewFactory = JyFaceCompareViewFactory(context, flutterPluginBinding.binaryMessenger)
    flutterPluginBinding.platformViewRegistry.registerViewFactory(VIEW_REGISTRY_NAME, viewFactory)
  }


  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    sdkMethodChannel.setMethodCallHandler(null)
    sdkEventChannel.setStreamHandler(null)
  }

  override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
    Log.i(TAG, "JyFaceComparePlugin:onMethodCall:${call.method}")
    when(call.method){
      "initFaceSdk" -> {
        initFaceSdk()
      }
      "releaseFace" -> {
        Facecompare.getInstance().releaseFace()
      }
    }
  }

  override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
    this.eventSink = events
  }

  override fun onCancel(arguments: Any?) {
    this.eventSink = null
  }

  private fun initFaceSdk(){
    Facecompare.getInstance().setFaceType(Facecompare.SAD_FACE)
    Facecompare.getInstance().faceInit(context){ result: Boolean, msg: String ->
      run {
        Log.i(TAG, "人脸比对初始化结果:$result, $msg")
        uiHandler.post {  eventSink?.success(mapOf(
                "result" to result,
                "msg" to msg
        ))}
      }
    }
  }
}
