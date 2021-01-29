import 'dart:async';

import 'package:JyFaceCompare/JyFaceCompare.dart';
import 'package:flutter/material.dart';
import 'package:flutter/widgets.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> with WidgetsBindingObserver {
  JyFaceCompareViewController _controller;
  String _currentState = "初始化";
  JyFaceCompareResult _compareResult;
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _controller = JyFaceCompareViewController();
    _controller.onInitSdkResult.listen((initResult) {
      print("onInitSdkResult");
      if (initResult.result) {
        Future.delayed(Duration(milliseconds: 500), () {
          _controller.startPreview();
        }).then((value) {
          _controller.startPreview();
        });
      } else {
        setState(() {
          _currentState = "人脸比对初始化失败.";
        });
      }
    });
    _controller.onCameraOpened.listen((_) {
      print("onCameraOpened");
      setState(() {
        _currentState = "相机已打开";
      });
      //开始比对，传入原始图像数据和相似度阀值
      //_controller.startCompare(bitmapData, 80);
    });
    _controller.onCameraClosed.listen((_) {
      print("onCameraClosed");
      setState(() {
        _currentState = "相机已关闭";
      });
    });
    _controller.onPreviewStop.listen((_) {
      print("onPreviewStop");
      setState(() {
        _currentState = "预览已停止";
      });
    });
    _controller.onCompareStart.listen((_) {
      print("onCompareStart");
      setState(() {
        _currentState = "开始人脸比对";
      });
    });
    _controller.onCompareResult.listen((compareResult) {
      print("onCompareResult");
      setState(() {
        _compareResult = compareResult;
        _currentState = "人脸比对完成,相似度:${compareResult.similar}";
      });
    });
  }

  void _onJyFaceCompareViewCreated() {
    print("_onJyFaceCompareViewCreated");
    _controller.initFaceSdk();
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
          appBar: AppBar(
            title: const Text('Plugin example app'),
          ),
          body: Column(
            crossAxisAlignment: CrossAxisAlignment.center,
            children: [
              Text(_currentState),
              Row(
                children: [
                  Container(
                    decoration: BoxDecoration(
                      border: Border.all(color: Colors.black, width: 1.0),
                    ),
                    alignment: Alignment.center,
                    height: 320,
                    width: 240,
                    child: JyFaceCompareView(
                      controller: _controller,
                      onJyFaceCompareViewCreated: _onJyFaceCompareViewCreated,
                      creationParams: JyFaceCompareViewParams(
                          height: 320,
                          width: 240,
                          previewWidth: 640,
                          previewHeight: 480,
                          rotate: 0),
                    ),
                  ),
                  if (_compareResult != null)
                    Container(
                      decoration: BoxDecoration(
                        border: Border.all(color: Colors.black, width: 1.0),
                      ),
                      margin: EdgeInsets.symmetric(horizontal: 16.0),
                      alignment: Alignment.center,
                      height: 320,
                      width: 240,
                      child: Image.memory(
                        _compareResult.bitmap,
                        fit: BoxFit.contain,
                      ),
                    ),
                  Padding(
                    child: OutlineButton(
                      onPressed: () {
                        //开始比对，传入原始图像数据和相似度阀值
                        //_controller.startCompare(bitmapData, 80);
                      },
                      child: Text("再次比对"),
                    ),
                    padding: EdgeInsets.symmetric(horizontal: 16.0),
                  ),
                ],
              ),
            ],
          )),
    );
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    switch (state) {
      case AppLifecycleState.resumed:
        print("didChangeAppLifecycleState:resume");
        _controller.startPreview();
        break;
      case AppLifecycleState.inactive:
        print("didChangeAppLifecycleState:inactive");
        break;
      case AppLifecycleState.paused:
        print("didChangeAppLifecycleState:pause");
        _controller.stopPreview();
        break;
      default:
        break;
    }
  }

  @override
  void dispose() {
    super.dispose();
    WidgetsBinding.instance.removeObserver(this);
    _controller.releaseFace();
    _controller.stopCamera();
    _controller.releaseCamera();
    _controller.dispose();
  }
}
