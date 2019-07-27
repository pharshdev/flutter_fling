import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_fling/remote_media_player.dart';

class FlutterFling {
  static const MethodChannel _channel = const MethodChannel('flutter_fling');
  
  // static RemoteMediaPlayer _device;

  static Future<List<RemoteMediaPlayer>> get players async {
    try {
      await _channel.invokeMethod('startDiscoveryController');
      await Future.delayed(Duration(seconds: 3));
    } on PlatformException catch (e) {
      print('error starting discovery: ${e.details}');
    }
      print('THIS SHOULD PRINT AFTER 3 SEC');
    List<dynamic> players;
    try {
      players = await _channel.invokeMethod('getPlayers');
      print('PLAYERS: ${players.toString()}');
    } on PlatformException catch (e) {
      print('players fetch error: ${e.details}');
    }
    return players != null
        ? players.map(RemoteMediaPlayer.fromJson).toList()
        : List();
  }

  // static Future<void> selectDevice({@required RemoteMediaPlayer device}) async {
  //   _device = device;
  //   if (selectedDevice != null)
  //     try {
  //       await _channel.invokeMethod('selectDevice', <String, dynamic>{
  //         'uid': _device.uid,
  //       });
  //     } on PlatformException catch (e) {
  //       print(e.details);
  //     }
  // }

  static Future<RemoteMediaPlayer> get selectedPlayer async {
    dynamic player;
    try {
      player = await _channel.invokeMethod('getSelectedPlayer');
    } on PlatformException catch (e) {
      print(e.details);
    }
    if (player == null) {
      // _device = null;
      return null;
    } else {
      return RemoteMediaPlayer.fromJson(player);
    }
    // return _device;
  }

  static Future<void> stopPlayer() async {
    try {
      await _channel.invokeMethod('stopPlayer');
    } on PlatformException catch (e) {
      print(e.details);
    }
  }

  static Future<void> pausePlayer() async {
    try {
      await _channel.invokeMethod('pausePlayer');
    } on PlatformException catch (e) {
      print(e.details);
    }
  }

  static Future<void> playPlayer() async {
    try {
      await _channel.invokeMethod('playPlayer');
    } on PlatformException catch (e) {
      print(e.details);
    }
  }

  static Future<void> mutePlayer(bool muteState) async {
    try {
      await _channel.invokeMethod(
          'mutePlayer', <String, dynamic>{'muteState': muteState});
    } on PlatformException catch (e) {
      print(e.details);
    }
  }

  static Future<void> seekForwardPlayer() async {
    try {
      await _channel.invokeMethod('seekForwardPlayer');
    } on PlatformException catch (e) {
      print(e.details);
    }
  }

  static Future<void> seekBackPlayer() async {
    try {
      await _channel.invokeMethod('seekBackPlayer');
    } on PlatformException catch (e) {
      print(e.details);
    }
  }

  // static Future<void> playOnSelectedDevice(
  //     {@required String mediaUri, String mediaTitle}) async {
  //   try {
  //     await _channel.invokeMethod('playMedia', <String, dynamic>{
  //       'mediaSourceUri': mediaUri,
  //       'mediaSourceTitle': mediaTitle ?? 'Some Video',
  //     });
  //   } on PlatformException catch (e) {
  //     print(e.details);
  //   }
  // }

  static Future<void> play(
      {@required String mediaUri,
      @required String mediaTitle,
      @required RemoteMediaPlayer device}) async {
    if (mediaUri != null && mediaTitle != null && device != null) {
      // _device = device;
      try {
        await _channel.invokeMethod('play', <String, dynamic>{
          'mediaSourceUri': mediaUri,
          'mediaSourceTitle': mediaTitle ?? 'Video',
          'deviceUid': device.uid
        });
      } on PlatformException catch (e) {
        print(e.details);
      }
    }
  }

  static Future<String> get playerState async {
    String state;

    try {
      state = await _channel.invokeMethod('playerState');
    } on PlatformException catch (e) {
      print(e.details);
    }
    return state;
  }
}
