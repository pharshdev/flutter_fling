

# flutter_fling

  

Simple plugin to cast media to FireStick and FireTv.

Utilises Amazon's Fling SDK.

NOTE: This plugin currently fling/cast from Android Devices. iOS support soon.

  
## Installation

### Add package to pubspec.yaml

```sh
flutter_fling: ^2.0.2
```

### Add import

```sh
import 'package:flutter_fling/flutter_fling.dart';
```

### Usage

#### Get Players

```sh
FlutterFling.startPlayerDiscovery((status, player) {
	if (_flingDevices ==  null) _flingDevices =  List();
	if (status ==  PlayerDiscoveryStatus.FOUND) 
		_flingDevices.add(player);
	else 
		_flingDevices.remove(player);
});
```
-   Note: set  `callback`  as  `null`  to remove listener. You should clean up callback to prevent from leaking references.

#### Play media and listen for media state

```sh
FlutterFling.play((state, condition, position) {
		_mediaState =  '$state';
		_mediaCondition =  '$condition';
		_mediaPosition =  '$position';
	},
	player: _selectedPlayer,
	mediaUri: "media_link_here",
	mediaTitle:  "Some Video",
)
```
-   Note: set  `callback`  as  `null`  to remove listener. You should clean up callback to prevent from leaking references.

#### Pause Player

```sh
await FlutterFling.pausePlayer();
```

#### Resume Player

```sh
await FlutterFling.playPlayer();
```

#### Stop Player

```sh
await FlutterFling.stopPlayer();
```

#### Seek Forward Player  (+10 sec)

```sh
await FlutterFling.seekForwardPlayer();
```

#### Seek Back Player  (-10 sec)

```sh
await FlutterFling.seekBackPlayer();
```

#### Seek To Player

```sh
await FlutterFling.seekToPlayer(position: 30000)
```

#### Mute/Unmute Player

```sh
await FlutterFling.mutePlayer(bool);
```

#### Remove Controller when done
Note: This result in loss of control over playing media though playback itself isn't stopped. Required for cleanup.

```sh
await FlutterFling.stopDiscoveryController();
```

#### Possible  States:

```sh
enum  PlayerDiscoveryStatus { Found, Lost }

enum  MediaState { NoSource, PreparingMedia, ReadyToPlay, Playing, Paused, Seeking, Finished, Error }

enum  MediaCondition { Good, WarningContent, WarningBandwidth, ErrorContent, ErrorChannel, ErrorUnknown }
```