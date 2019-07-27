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
  List<RemoteMediaPlayer> _flingDevices;
  RemoteMediaPlayer _selectedDevice;
  String _mediaState = "null";

  @override
  void initState() {
    super.initState();
    getSelectedDevice();
    getMediaState();
  }

  getCastDevices() async {
    List<RemoteMediaPlayer> flingDevices;
    try {
      flingDevices = await FlutterFling.players;
    } on PlatformException {
      print('Failed to get devices');
    }
    if (!mounted) return;

    setState(() {
      _flingDevices = flingDevices;
    });
  }

  getSelectedDevice() async {
    RemoteMediaPlayer selectedDevice;
    try {
      selectedDevice = await FlutterFling.selectedPlayer;
    } on PlatformException {
      print('Failed to get selected device');
    }
    setState(() {
      _selectedDevice = selectedDevice;
    });
  }

  castMediaTo(RemoteMediaPlayer player) async {
    _selectedDevice = player;
    await FlutterFling.play(
        device: _selectedDevice,
        mediaUri: "video link",
        mediaTitle: "Some Video");
    getMediaState();
  }

  getMediaState() async {
    String state = '';
    try {
      state = await FlutterFling.playerState;
    } on PlatformException {
      print('Failed to get media state.');
    }
    setState(() {
      _mediaState = state;
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Column(
          mainAxisAlignment: MainAxisAlignment.start,
          mainAxisSize: MainAxisSize.min,
          children: <Widget>[
            Text('Media State: $_mediaState'),
            Text(
                'Selected Device: ${_selectedDevice != null ? _selectedDevice.name : 'null'}'),
            Text("Fire devices: "),
            _flingDevices == null
                ? Text('Try casting something')
                : _flingDevices.isEmpty
                    ? Text('None nearby')
                    : ListView.builder(
                        shrinkWrap: true,
                        itemCount: _flingDevices.length,
                        itemBuilder: (context, index) {
                          return ListTile(
                            title: Text(_flingDevices[index].name),
                            subtitle: Text(_flingDevices[index].uid),
                            onTap: () => castMediaTo(_flingDevices[index]),
                          );
                        },
                      )
          ],
        ),
        floatingActionButton: Column(
          mainAxisSize: MainAxisSize.min,
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: <Widget>[
            IconButton(
              icon: Icon(Icons.cast),
              onPressed: () => getCastDevices(),
            ),
            RaisedButton(
              child: Text('Play Cast'),
              onPressed: () async => await FlutterFling.playPlayer(),
            ),
            RaisedButton(
              child: Text('Pause Cast'),
              onPressed: () async => await FlutterFling.pausePlayer(),
            ),
            RaisedButton(
              child: Text('Stop Cast'),
              onPressed: () async => await FlutterFling.stopPlayer(),
            ),
            RaisedButton(
              child: Text('Mute Cast'),
              onPressed: () async => await FlutterFling.mutePlayer(true),
            ),
            RaisedButton(
              child: Text('Unmute Cast'),
              onPressed: () async => await FlutterFling.mutePlayer(false),
            ),
            RaisedButton(
              child: Text('Forward Cast'),
              onPressed: () async => await FlutterFling.seekForwardPlayer(),
            ),
            RaisedButton(
              child: Text('Back Cast'),
              onPressed: () async => await FlutterFling.seekBackPlayer(),
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
