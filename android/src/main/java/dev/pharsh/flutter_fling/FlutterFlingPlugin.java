package dev.pharsh.flutter_fling;

import android.util.Log;

import com.amazon.whisperplay.fling.media.controller.DiscoveryController;
import com.amazon.whisperplay.fling.media.controller.RemoteMediaPlayer;
import com.amazon.whisperplay.fling.media.service.CustomMediaPlayer;
import com.amazon.whisperplay.fling.media.service.MediaPlayerStatus;
import com.amazon.whisperplay.fling.media.service.MediaPlayerStatus.MediaState;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/**
 * FlutterFlingPlugin
 */
public class FlutterFlingPlugin implements MethodCallHandler {
    private static final String DISCOVERY_STREAM = "flutter_fling/discoveryStream";
    private static final String PLAYER_STATE_STREAM = "flutter_fling/playerStateStream";
    private final FlingSdk flingSdk;

    static class Status {
        long mPosition;
        MediaState mState;
        MediaPlayerStatus.MediaCondition mCond;
    }

    private FlutterFlingPlugin(Registrar registrar) {
        flingSdk = new FlingSdk(new EventChannel(registrar.messenger(), DISCOVERY_STREAM),
                new EventChannel(registrar.messenger(), PLAYER_STATE_STREAM), registrar);
    }

    public static void registerWith(Registrar registrar) {
        final FlutterFlingPlugin flutterFlingPlugin = new FlutterFlingPlugin(registrar);

        final MethodChannel channel = new MethodChannel(registrar.messenger(), "flutter_fling");
        channel.setMethodCallHandler(flutterFlingPlugin);
    }

    @Override
    public void onMethodCall(MethodCall call, Result result) {
        switch (call.method) {
            case "startDiscoveryController":
                flingSdk.startDiscovery();
                result.success(null);
                break;
            case "stopDiscoveryController":
                flingSdk.stopDiscoveryController();
                result.success(null);
                break;
            case "play":
                final String playerUid = call.argument("deviceUid");
                final String mediaSourceUri = call.argument("mediaSourceUri");
                final String mediaSourceTitle = call.argument("mediaSourceTitle");
                if (playerUid == null || mediaSourceUri == null || mediaSourceTitle == null)
                    result.error("playerUid/mediaSourceUri/mediaSourceTitle cannot be null", "", null);
                else {
                    flingSdk.setPlayer(playerUid);
                    flingSdk.fling(mediaSourceUri, mediaSourceTitle);
                    result.success(null);
                }
                break;
//            case "selectDevice":
//                final String uid = call.argument("uid");
//                if (uid != null) {
//                    setDeviceForPlayback(uid);
//                    result.success(null);
//                } else
//                    result.error("uid cannot be null", "", null);
//                break;
            case "getSelectedPlayer":
                RemoteMediaPlayer selectedDevice = flingSdk.getSelectedPlayer();
                if (selectedDevice == null) {
                    result.error("device not selected or null", "", null);
                } else {
                    HashMap<String, String> playerMap = new HashMap<>();
                    playerMap.put("deviceName", selectedDevice.getName());
                    playerMap.put("deviceUid", selectedDevice.getUniqueIdentifier());
                    result.success(playerMap);
                }
                break;
//            case "playMedia":
//                final String mediaSource = call.argument("mediaSourceUri");
//                final String mediaTitle = call.argument("mediaSourceTitle");
//                if (mediaSource != null && mediaTitle != null && mCurrentDevice != null) {
//                    fling(mCurrentDevice, mediaSource, mediaTitle);
//                    result.success(null);
//                } else
//                    result.error("mediaSource or mediaTitle or device is null", "", null);
//                break;
            case "stopPlayer":
                flingSdk.stopPlayer();
                result.success(null);
                break;
            case "playPlayer":
                flingSdk.playPlayer();
                result.success(null);
                break;
            case "pausePlayer":
                flingSdk.pausePlayer();
                result.success(null);
                break;
//            case "muteStatus":
//                final RemoteMediaPlayer.AsyncFuture<Boolean> muteStatus = flingSdk.isPlayerMute();
//                muteStatus.wait();
//                result.success("");
            case "mutePlayer":
                final String muteState = call.argument("muteState");
//                Log.v("TAG", "MUTE ARGUMENT RECEIVED : " + muteState);
                flingSdk.setMute(muteState.equals("true"));
                result.success(null);
                break;
            case "seekForwardPlayer":
                flingSdk.seekForwardPlayer();
                result.success(null);
                break;
            case "seekBackPlayer":
                flingSdk.seekBackPlayer();
                result.success(null);
                break;
            default:
                result.notImplemented();
                break;
        }
    }

    static class FlingSdk {
        final Registrar registrar;
        private Set<RemoteMediaPlayer> mPlayers = new HashSet<>();
        private DiscoveryController mController;
        private RemoteMediaPlayer mCurrentDevice;
        private CustomMediaPlayer.StatusListener mListener = new Monitor();
        private final FlutterFlingPlugin.Status mStatus = new FlutterFlingPlugin.Status();
        private QueuingEventSink playerDiscoveryEventSink = new QueuingEventSink();
        private final EventChannel playerDiscoveryEventChannel;
        private QueuingEventSink playerStatusEventSink = new QueuingEventSink();
        private final EventChannel playerStatusEventChannel;

        FlingSdk(EventChannel playerDiscoveryEventChannel, EventChannel playerStatusEventChannel, Registrar registrar) {
            this.playerDiscoveryEventChannel = playerDiscoveryEventChannel;
            this.playerStatusEventChannel = playerStatusEventChannel;
            this.registrar = registrar;
            mController = new DiscoveryController(registrar.context());
        }

        void startDiscovery() {
            playerDiscoveryEventChannel.setStreamHandler(
                    new EventChannel.StreamHandler() {
                        @Override
                        public void onListen(Object o, EventChannel.EventSink sink) {
                            playerDiscoveryEventSink.setDelegate(sink);
                        }

                        @Override
                        public void onCancel(Object o) {
                            playerDiscoveryEventSink.setDelegate(null);
                        }
                    });


            this.mController.start("amzn.thin.pl", new DiscoveryController.IDiscoveryListener() {
                @Override
                public void playerDiscovered(RemoteMediaPlayer player) {
                    //add media player to the application’s player list.
                    Log.v("PLUGIN_TAG", player.getName() + " discovered, adding to players...");
                    mPlayers.add(player);
                    Map<String, String> playerMap = new HashMap<>();
                    playerMap.put("deviceName", player.getName());
                    playerMap.put("deviceUid", player.getUniqueIdentifier());
                    playerMap.put("event", "found");
                    playerDiscoveryEventSink.success(playerMap);
                }

                @Override
                public void playerLost(RemoteMediaPlayer player) {
                    //remove media player from the application’s player list.
                    Log.v("PLUGIN_TAG", player.getName() + " lost, removing from players...");
                    mPlayers.remove(player);
                    Map<String, String> playerMap = new HashMap<>();
                    playerMap.put("deviceName", player.getName());
                    playerMap.put("deviceUid", player.getUniqueIdentifier());
                    playerMap.put("event", "lost");
                    playerDiscoveryEventSink.success(playerMap);
                }

                @Override
                public void discoveryFailure() {
                    Log.v("PLUGIN_TAG", " Discovery failure");
                    playerDiscoveryEventSink.error("discovery failure", "", null);
                }
            });
        }

        void stopDiscoveryController() {
            this.mController.stop();
        }

        private class Monitor implements CustomMediaPlayer.StatusListener {
            @Override
            public void onStatusChange(MediaPlayerStatus status, long position) {
                synchronized (mStatus) {
                    mStatus.mState = status.getState();
                    mStatus.mCond = status.getCondition();
                    mStatus.mPosition = position;
                    Map<String, Object> playerStatus = new HashMap<>();
                    playerStatus.put("state", getPlayerState());
                    playerStatus.put("condition", getPlayerCondition());
                    playerStatus.put("position", position);
                    playerStatusEventSink.success(playerStatus);
                }
            }
        }


        RemoteMediaPlayer getSelectedPlayer() {
            if (mCurrentDevice != null) return mCurrentDevice;
            else return null;
        }

        void setPlayer(String uid) {
            if (mCurrentDevice != null)
                mCurrentDevice.stop();
            for (RemoteMediaPlayer player : mPlayers
            ) {
                if (player.getUniqueIdentifier().equals(uid)) {
                    mCurrentDevice = player;
                    break;
                }
            }
        }

        void fling(final String name, final String title) {
            mCurrentDevice.addStatusListener(mListener);
            long MONITOR_INTERVAL = 1000L;
            mCurrentDevice.setPositionUpdateInterval(MONITOR_INTERVAL);
            playerStatusEventChannel.setStreamHandler(new EventChannel.StreamHandler() {
                @Override
                public void onListen(Object o, EventChannel.EventSink sink) {
                    playerStatusEventSink.setDelegate(sink);
                }

                @Override
                public void onCancel(Object o) {
                    playerStatusEventSink.setDelegate(null);
                }
            });

            mCurrentDevice.setMediaSource(name, title, true, false);
        }

        void stopPlayer() {
            if (mCurrentDevice != null) mCurrentDevice.stop();
        }

        void playPlayer() {
            if (mCurrentDevice != null) mCurrentDevice.play();
        }

        void pausePlayer() {
            if (mCurrentDevice != null) mCurrentDevice.pause();
        }

        void seekForwardPlayer() {
            if (mCurrentDevice != null)
                mCurrentDevice.seek(CustomMediaPlayer.PlayerSeekMode.Relative, 10000);
        }

        void seekBackPlayer() {
            if (mCurrentDevice != null)
                mCurrentDevice.seek(CustomMediaPlayer.PlayerSeekMode.Relative, -10000);
        }

//    RemoteMediaPlayer.AsyncFuture<Boolean> isPlayerMute() {
//        if (mCurrentDevice != null) return mCurrentDevice.isMute();
//        return null;
//    }

        void setMute(boolean muteState) {
            if (mCurrentDevice != null) mCurrentDevice.setMute(muteState);
        }


        String getPlayerState() {
            if (this.mStatus.mState == null) return "null";
            String stateString;
            switch (this.mStatus.mState) {
                case Error:
                    stateString = "Error";
                    break;
                case Finished:
                    stateString = "Finished";
                    break;
                case Paused:
                    stateString = "Paused";
                    break;
                case Playing:
                    stateString = "Playing";
                    break;
                case PreparingMedia:
                    stateString = "PreparingMedia";
                    break;
                case ReadyToPlay:
                    stateString = "ReadyToPlay";
                    break;
                case NoSource:
                    stateString = "NoSource";
                    break;
                case Seeking:
                    stateString = "Seeking";
                    break;
                default:
                    stateString = "default case";
                    break;
            }
            return stateString;
        }

        String getPlayerCondition() {
            if (this.mStatus.mCond == null) return "null";
            String cond;
            switch (this.mStatus.mCond) {
                case Good:
                    cond = "Good";
                    break;
                case WarningBandwidth:
                    cond = "WarningBandwidth";
                    break;
                case WarningContent:
                    cond = "WarningContent";
                    break;
                case ErrorChannel:
                    cond = "ErrorChannel";
                    break;
                case ErrorContent:
                    cond = "ErrorContent";
                    break;
                case ErrorUnknown:
                    cond = "ErrorUnknown";
                    break;
                default:
                    cond = "default case";
                    break;
            }
            return cond;
        }

        //    public enum MediaState {
//        NoSource, PreparingMedia, ReadyToPlay, Playing, Paused, Seeking, Finished, Error
//    };
//    public enum MediaCondition {
//        Good, WarningContent, WarningBandwidth, ErrorContent, ErrorChannel, ErrorUnknown
//    };
    }


}


