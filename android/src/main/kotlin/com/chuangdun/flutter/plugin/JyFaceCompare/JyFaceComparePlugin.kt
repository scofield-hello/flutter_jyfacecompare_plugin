package com.chuangdun.flutter.plugin.JyFaceCompare

import androidx.annotation.NonNull
import io.flutter.embedding.engine.plugins.FlutterPlugin

private const val TAG = "JyFaceComparePlugin"
const val VIEW_REGISTRY_NAME = "JyFaceCompareView"
const val VIEW_EVENT_REGISTRY_NAME = "JyFaceCompareViewEvent"
/** JyFaceComparePlugin */
class JyFaceComparePlugin: FlutterPlugin {

  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    val viewFactory = JyFaceCompareViewFactory(flutterPluginBinding.applicationContext, flutterPluginBinding.binaryMessenger)
    flutterPluginBinding.platformViewRegistry.registerViewFactory(VIEW_REGISTRY_NAME, viewFactory)
  }


  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
  }
}
