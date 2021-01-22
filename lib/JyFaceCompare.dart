import 'dart:async';
import 'dart:typed_data';

import 'package:flutter/services.dart';
import 'package:flutter/widgets.dart';

class JyFaceCompareView extends StatelessWidget {
  final _viewType = "JyFaceCompareView";
  final Map<String, dynamic> creationParams;
  final JyFaceCompareViewController controller;
  final VoidCallback onJyFaceCompareViewCreated;
  const JyFaceCompareView(
      {Key key, this.controller, this.onJyFaceCompareViewCreated, this.creationParams = const {}})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return AndroidView(
        viewType: _viewType,
        creationParams: creationParams,
        creationParamsCodec: const StandardMessageCodec(),
        onPlatformViewCreated: _onPlatformViewCreated);
  }

  void _onPlatformViewCreated(int id) {
    if (controller != null) {
      controller.onCreate(id);
    }
    if (onJyFaceCompareViewCreated != null) {
      onJyFaceCompareViewCreated();
    }
  }
}

class JyFaceCompareResult{
  final int similar;
  final Uint8List bitmap;
  const JyFaceCompareResult(this.similar, this.bitmap);
}

class JyFaceCompareEventType {
  static const EVENT_CAMERA_OPENED = 0;
  static const EVENT_PREVIEW = 1;
  static const EVENT_PREVIEW_STOP = 2;
  static const EVENT_CAMERA_CLOSED = 3;
  static const EVENT_COMPARE_START = 4;
  static const EVENT_COMPARE_RESULT = 5;
}

class JyFaceCompareViewController {
  static const _EVENT_CHANNEL_NAME = "JyFaceCompareViewEvent";
  static const _METHOD_CHANNEL_NAME = "JyFaceCompareView";
  MethodChannel _methodChannel;
  EventChannel _eventChannel;

  void _onEvent(dynamic event) {
    switch (event['event']) {
      case JyFaceCompareEventType.EVENT_CAMERA_OPENED:
        _onCameraOpened.add(null);
        break;
      case JyFaceCompareEventType.EVENT_PREVIEW:
        _onPreview.add(null);
        break;
      case JyFaceCompareEventType.EVENT_PREVIEW_STOP:
        _onPreviewStop.add(null);
        break;
      case JyFaceCompareEventType.EVENT_CAMERA_CLOSED:
        _onCameraClosed.add(null);
        break;
      case JyFaceCompareEventType.EVENT_COMPARE_START:
        _onCompareStart.add(null);
        break;
      case JyFaceCompareEventType.EVENT_COMPARE_RESULT:
        _onCompareResult.add(JyFaceCompareResult(event['similar'], event['bitmap']));
        break;
    }
  }

  onCreate(int id) {
    _methodChannel = MethodChannel("${_METHOD_CHANNEL_NAME}_$id");
    _eventChannel = EventChannel("${_EVENT_CHANNEL_NAME}_$id");
    _eventChannel.receiveBroadcastStream().listen(_onEvent);
  }

  final _onCameraOpened = StreamController<void>.broadcast();

  Stream<void> get onCameraOpened => _onCameraOpened.stream;

  final _onPreview = StreamController<void>.broadcast();

  Stream<void> get onPreview => _onPreview.stream;

  final _onPreviewStop = StreamController<void>.broadcast();

  Stream<void> get onPreviewStop => _onPreviewStop.stream;

  final _onCameraClosed = StreamController<void>.broadcast();

  Stream<void> get onCameraClosed => _onCameraClosed.stream;

  final _onCompareStart = StreamController<void>.broadcast();

  Stream<void> get onCompareStart => _onCompareStart.stream;

  final _onCompareResult = StreamController<JyFaceCompareResult>.broadcast();

  Stream<JyFaceCompareResult> get onCompareResult => _onCompareResult.stream;

  Future<void> startPreview() async {
    _methodChannel.invokeMethod("startPreview");
  }

  Future<void> stopPreview() async {
    _methodChannel.invokeMethod("stopPreview");
  }

  Future<void> stopCamera() async {
    _methodChannel.invokeMethod("stopCamera");
  }

  Future<void> startCompare() async {
    _methodChannel.invokeMethod("startCompare");
  }

  Future<void> releaseCamera() async {
    _methodChannel.invokeMethod("releaseCamera");
  }

  void dispose() {
    _onCameraClosed.close();
    _onCameraOpened.close();
    _onPreview.close();
    _onPreviewStop.close();
    _onCompareStart.close();
    _onCompareResult.close();
  }
}
