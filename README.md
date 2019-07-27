# flutter_fling

Simple plugin to cast media to FireStick and FireTv.

NOTE: This plugin currently fling/cast from Android Devices. iOS support soon.

### Installation

#### Add package to pubspec.yaml

```sh
flutter_fling: ^1.0.0
```

#### Add import

```sh
import 'package:flutter_fling/flutter_fling.dart';
```

#### Usage

##### Get Players

```sh
List<RemoteMediaPlayers> remotePlayers = await FlutterFling.players;
```

##### Play media

```sh
await FlutterFling.play(
        player: desiredRemoteMediaPlayer,
        mediaUri: "video link",
        mediaTitle: "Some Video");
```

##### Pause Player

```sh
await FlutterFling.pausePlayer();
```

##### Resume Player

```sh
await FlutterFling.playPlayer();
```

##### Stop Player

```sh
await FlutterFling.pausePlayer();
```

##### Seek Forward Player

```sh
await FlutterFling.seekForwardPlayer();
```

##### Seek Back Player

```sh
await FlutterFling.seekBackPlayer();
```

#### Mute/Unmute Player

```sh
await FlutterFling.mutePlayer(bool);
```

##### Player state

```sh
await FlutterFling.playerState();
```
###### Possible player states: 
Preparing Media, Ready to Play, Playing, Paused, Finished, Seeking, Error, No source.