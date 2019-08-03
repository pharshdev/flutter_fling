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
  RemoteMediaPlayer _selectedPlayer;
  String _mediaState = "null";
  String _mediaCondition = "null";
  String _mediaPosition = "null";
  FlutterFling fling;

  @override
  void initState() {
    super.initState();
    fling = FlutterFling();
    getSelectedDevice();
  }

  getCastDevices() async {
    FlutterFling.startDiscoveryController((status, player) {
      _flingDevices = List();
      if (status == PlayerDiscoveryStatus.Found) {
        setState(() {
          _flingDevices.add(player);
        });
      } else {
        setState(() {
          _flingDevices.remove(player);
        });
      }
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
      _selectedPlayer = selectedDevice;
    });
  }

  castMediaTo(RemoteMediaPlayer player) async {
    _selectedPlayer = player;
    await FlutterFling.play((state, condition, position) {
      setState(() {
        _mediaState = '$state';
        _mediaCondition = '$condition';
        _mediaPosition = '$position';
      });
    },
            player: _selectedPlayer,
            mediaUri:
                "https://ran.openstorage.host/dl/IJ4CGyOjKl1BjOyTAxFnGA/1565422242/889127646/5ca3772258fd44.44825533/D%20C%20Proper.mkv",
            mediaTitle: "media title")
        .then((_) => getSelectedDevice());
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
            Text('Media Condition: $_mediaCondition'),
            Text('Media Position: $_mediaPosition'),
            Text(
                'Selected Device: ${_selectedPlayer != null ? _selectedPlayer.name : 'null'}'),
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
            RaisedButton(
              child: Text('Search'),
              onPressed: () => getCastDevices(),
            ),
            RaisedButton(
              child: Text('Dispose Controller'),
              onPressed: () async {
                await FlutterFling.stopDiscoveryController();
                setState(() {
                  _flingDevices = List();
                  _mediaState = 'null';
                  _mediaCondition = 'null';
                  _mediaPosition = '0';
                  _selectedPlayer = null;
                });
              },
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
              onPressed: () async {
                await FlutterFling.stopPlayer();
                setState(() {
                  _flingDevices = null;
                });
              },
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
              child: Text('Seek to 30sec'),
              onPressed: () async =>
                  await FlutterFling.seekToPlayer(position: 30000),
            )
          ],
        ),
      ),
    );
  }
}
