class RemoteMediaPlayer {
  String name;
  String uid;

  RemoteMediaPlayer(this.name, this.uid);

  static RemoteMediaPlayer fromJson(dynamic json) {
    return RemoteMediaPlayer(json['deviceName'], json['deviceUid']);
  }
}
