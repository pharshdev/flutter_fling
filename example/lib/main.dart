import 'package:flutter/material.dart';

import 'package:flutter/services.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_fling/flutter_fling.dart';
import 'package:flutter_fling/remote_media_player.dart';

void main() => runApp(MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  // String _flingDevices = 'None';
  List<RemoteMediaPlayer> _flingDevices;
  RemoteMediaPlayer _selectedDevice;
  // List<String> _flingDevices;
  // String _flingDevices;
  String _mediaState = "null";

  @override
  void initState() {
    super.initState();
    getDevices();
    getSelectedDevice();
    getMediaState();
  }

  // Platform messages are asynchronous, so we initialize in an async method.
  getDevices() async {
    List<RemoteMediaPlayer> flingDevices;
    // Platform messages may fail, so we use a try/catch PlatformException.
    try {
      flingDevices = await FlutterFling.devices;
    } on PlatformException {
      print('Failed to get devices');
    }

    // If the widget was removed from the tree while the asynchronous platform
    // message was in flight, we want to discard the reply rather than calling
    // setState to update our non-existent appearance.
    if (!mounted) return;

    setState(() {
      _flingDevices = flingDevices;
    });
  }

  getSelectedDevice() async {
    RemoteMediaPlayer selectedDevice;
    try {
      selectedDevice = await FlutterFling.selectedDevice;
    } on PlatformException {
      print('Failed to get selected device');
    }
    setState(() {
      _selectedDevice = selectedDevice;
    });
  }

  getMediaState() async {
    String state = '';
    try {
      state = await FlutterFling.mediaState;
    } on PlatformException {
      print('Failed to get media state.');
    }
    setState(() {
      _mediaState = state;
    });
  }

  playMedia() async {
    await FlutterFling.play(
        mediaUri:
            "https://ran.openstorage.host/dl/SDO9PWelDZFZyuT-E9uhMw/1564663699/889127646/5ca3772258fd44.44825533/D%20C%20Proper%20HDRip%20Season%201%20%5BEng%5D%20MSub.mkv",
        mediaTitle: "Delhi Crime HD Season 1 [Eng]");
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Center(
          // padding: const EdgeInsets.all(8.0),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.start,
            mainAxisSize: MainAxisSize.min,
            children: <Widget>[
              Text('Media State: $_mediaState'),
              Text(
                  'Selected Device: ${_selectedDevice != null ? _selectedDevice.name : 'null'}'),
              Text("Fire devices: "),
              _flingDevices == null
                  ? CircularProgressIndicator()
                  : _flingDevices.isEmpty
                      ? Text('None nearby')
                      : ListView.builder(
                          shrinkWrap: true,
                          itemCount: _flingDevices.length,
                          itemBuilder: (context, index) {
                            return ListTile(
                              title: Text(_flingDevices[index].name),
                              subtitle: Text(_flingDevices[index].uid),
                              onTap: () async =>
                                  await FlutterFling.selectDevice(
                                      _flingDevices[index].uid),
                            );
                          },
                        )
            ],
          ),
        ),
        floatingActionButton: Column(
          mainAxisSize: MainAxisSize.min,
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: <Widget>[
            RaisedButton(
              child: Text('Play Video 1'),
              onPressed: () => playMedia(),
            ),
            RaisedButton(
              child: Text('Stop Device'),
              onPressed: () async => await FlutterFling.stopDevice(),
            ),
            RaisedButton(
              child: Text('Get Selected Device'),
              onPressed: () => getSelectedDevice(),
            ),
            RaisedButton(
              child: Text('Get Media State'),
              onPressed: () => getMediaState(),
            )
          ],
        ),
      ),
    );
  }
}
