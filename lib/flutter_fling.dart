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
  static const _playerDiscoveryChannel =
      const EventChannel('flutter_fling/discoveryStream');
  static const _playerStateChannel =
      const EventChannel('flutter_fling/playerStateStream');

  static startPlayerDiscovery(DiscoveryCallback callback) async {
    try {
      await _channel.invokeMethod('startDiscoveryController');

      _playerDiscoveryChannel.receiveBroadcastStream().listen((json) {
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

  static dispose() async {
    try {
      await _channel.invokeMethod('stopDiscoveryController');
    } on PlatformException catch (e) {
      print(e.details);
    }
  }

  // void stopPlayerDiscovery() async {
  //   try {
  //     await _channel.invokeMethod('stopDiscoveryController');

  //     if (_discoveryStreamSubscription != null)
  //       _discoveryStreamSubscription.cancel();
  //     _discoveryStreamSubscription = null;
  //   } on PlatformException catch (e) {
  //     print('error stopping discovery: ${e.details}');
  //   }
  //   print("PLAYER DISCOVERY STOPPED");
  // }

  // static Future<List<RemoteMediaPlayer>> get players async {
  //   try {
  //     await _channel.invokeMethod('startDiscoveryController');
  //     await Future.delayed(Duration(seconds: 3));
  //   } on PlatformException catch (e) {
  //     print('error starting discovery: ${e.details}');
  //   }
  //   List<dynamic> players;
  //   try {
  //     players = await _channel.invokeMethod('getPlayers');
  //   } on PlatformException catch (e) {
  //     print('players fetch error: ${e.details}');
  //   }
  //   return players != null
  //       ? players.map(RemoteMediaPlayer.fromJson).toList()
  //       : List();
  // }

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

  // static Future<String> get playerState async {
  //   String state;

  //   try {
  //     state = await _channel.invokeMethod('playerState');
  //   } on PlatformException catch (e) {
  //     print(e.details);
  //   }
  //   return state;
  // }
}
