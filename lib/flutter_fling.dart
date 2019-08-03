import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_fling/remote_media_player.dart';

enum PlayerDiscoveryStatus { Found, Lost }
enum MediaState {
  NoSource,
  PreparingMedia,
  ReadyToPlay,
  Playing,
  Paused,
  Seeking,
  Finished,
  Error
}
enum MediaCondition {
  Good,
  WarningContent,
  WarningBandwidth,
  ErrorContent,
  ErrorChannel,
  ErrorUnknown
}

typedef void DiscoveryCallback(
    PlayerDiscoveryStatus status, RemoteMediaPlayer player);

typedef void PlayerStateCallback(
    MediaState state, MediaCondition condition, int position);

class FlutterFling {
  static const MethodChannel _channel = const MethodChannel('flutter_fling');
  static const _discoveryControllerChannel =
      const EventChannel('flutter_fling/discoveryControllerStream');
  static const _playerStateChannel =
      const EventChannel('flutter_fling/playerStateStream');

  static Future<void> startDiscoveryController(
      DiscoveryCallback callback) async {
    try {
      await _channel.invokeMethod('startDiscoveryController');

      _discoveryControllerChannel.receiveBroadcastStream().listen((json) {
        debugPrint(json.toString());
        callback(
            json['event'] == 'found'
                ? PlayerDiscoveryStatus.Found
                : PlayerDiscoveryStatus.Lost,
            RemoteMediaPlayer.fromJson(json));
      });
    } on PlatformException catch (e) {
      print('error starting discovery: ${e.details}');
    }
  }

  static Future<void> play(PlayerStateCallback callback,
      {@required String mediaUri,
      @required String mediaTitle,
      @required RemoteMediaPlayer player}) async {
    if (mediaUri != null && mediaTitle != null && player != null) {
      try {
        await _channel.invokeMethod('play', <String, dynamic>{
          'mediaSourceUri': mediaUri,
          'mediaSourceTitle': mediaTitle ?? 'Video',
          'deviceUid': player.uid
        });
        _playerStateChannel.receiveBroadcastStream().listen((json) {
          callback(
              MediaState.values.firstWhere(
                  (value) => value.toString() == 'MediaState.' + json['state']),
              MediaCondition.values.firstWhere((value) =>
                  value.toString() == 'MediaCondition.' + json['condition']),
              json['position']);
        });
      } on PlatformException catch (e) {
        print(e.details);
      }
    }
  }

  static Future<void> stopDiscoveryController() async {
    try {
      await _channel.invokeMethod('stopDiscoveryController');
    } on PlatformException catch (e) {
      print('error stopping discovery: ${e.details}');
    }
  }

  static Future<RemoteMediaPlayer> get selectedPlayer async {
    dynamic player;
    try {
      player = await _channel.invokeMethod('getSelectedPlayer');
    } on PlatformException catch (e) {
      print(e.details);
    }
    if (player == null) {
      return null;
    } else {
      return RemoteMediaPlayer.fromJson(player);
    }
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
      await _channel.invokeMethod('mutePlayer',
          <String, dynamic>{'muteState': muteState ? 'true' : 'false'});
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

  static Future<void> seekToPlayer({@required int position}) async {
    try {
      await _channel.invokeMethod(
          'seekToPlayer', <String, String>{'position': position.toString()});
    } on PlatformException catch (e) {
      print(e.details);
    }
  }
}
