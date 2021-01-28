# JyFaceCompare

A new flutter plugin project.

## Getting Started

```dart

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

  ///创建预览视图
  Container(
      decoration: BoxDecoration(
        border: Border.all(color: Colors.black, width: 1.0),
      ),
      alignment: Alignment.center,
      height: 288,
      width: 240,
      child: JyFaceCompareView(
        controller: _controller,
        onJyFaceCompareViewCreated: _onJyFaceCompareViewCreated,
      ),
    )
```

