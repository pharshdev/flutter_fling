import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_fling/remote_media_player.dart';

class FlutterFling {
  static const MethodChannel _channel = const MethodChannel('flutter_fling');

  static Future<List<RemoteMediaPlayer>> get devices async {
    List<dynamic> devices;
    try {
      devices = await _channel.invokeMethod('getDevices');
    } on PlatformException catch (e) {
      print('devices error: ${e.details}');
    }
    return devices != null
        ? devices.map(RemoteMediaPlayer.fromJson).toList()
        : List();
  }

  static Future<void> selectDevice(String uid) async {
    try {
      await _channel.invokeMethod('selectDevice', <String, dynamic>{
        'uid': uid,
      });
    } on PlatformException catch (e) {
      print(e.details);
    }
  }

  static Future<RemoteMediaPlayer> get selectedDevice async {
    dynamic device;
    try {
      device = await _channel.invokeMethod('getSelectedDevice');
    } on PlatformException catch (e) {
      print(e.details);
    }
    if (device == null)
      return null;
    else
      return RemoteMediaPlayer.fromJson(device);
  }

  static Future<void> stopDevice() async {
    try {
      await _channel.invokeMethod('stopDevice');
    } on PlatformException catch (e) {
      print(e.details);
    }
  }

  static Future<void> play(
      {@required String mediaUri, String mediaTitle}) async {
    try {
      await _channel.invokeMethod('playMedia', <String, dynamic>{
        'mediaSourceUri': mediaUri,
        'mediaSourceTitle': mediaTitle ?? 'Some Video',
      });
    } on PlatformException catch (e) {
      print(e.details);
    }
  }

  static Future<String> get mediaState async {
    String state;

    try {
      state = await _channel.invokeMethod('mediaState');
    } on PlatformException catch (e) {
      print(e.details);
    }
    return state;
  }
}
