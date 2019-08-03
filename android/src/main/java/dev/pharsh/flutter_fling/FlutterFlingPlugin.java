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
    private static final String DISCOVERY_CONTROLLER_STREAM = "flutter_fling/discoveryControllerStream";
    private static final String PLAYER_STATE_STREAM = "flutter_fling/playerStateStream";
    private final FlingSdk flingSdk;

    static class Status {
        long mPosition;
        MediaState mState;
        MediaPlayerStatus.MediaCondition mCond;
    }

    private FlutterFlingPlugin(Registrar registrar) {
        flingSdk = new FlingSdk(registrar);
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
                flingSdk.startDiscoveryController();
                result.success(null);
                break;
            case "stopDiscoveryController":
                flingSdk.stopDiscoveryController();
                result.success(null);
                break;
            case "removePlayerListener":
                flingSdk.removePlayerListeners();
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
            case "mutePlayer":
                String muteState = call.argument("muteState");
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
            case "seekToPlayer":
                String pos = call.argument("position");
                if (pos != null) {
                    flingSdk.seekTo(Long.parseLong(pos));
                    result.success(null);
                } else
                    result.error("position is null", "", null);
                break;
            default:
                result.notImplemented();
                break;
        }
    }

    static class FlingSdk {
        final Registrar registrar;
        private Set<RemoteMediaPlayer> mPlayers;
        private DiscoveryController mController;
        private RemoteMediaPlayer mCurrentDevice;
        private CustomMediaPlayer.StatusListener mListener;
        private final FlutterFlingPlugin.Status mStatus = new FlutterFlingPlugin.Status();
        private QueuingEventSink discoveryControllerEventSink = new QueuingEventSink();
        private EventChannel discoveryControllerEventChannel;
        private QueuingEventSink playerStateEventSink = new QueuingEventSink();
        private EventChannel playerStateEventChannel;

        FlingSdk(Registrar registrar) {
            this.registrar = registrar;
        }

        void startDiscoveryController() {
            stopDiscoveryController();
            mPlayers = new HashSet<>();
            mController = new DiscoveryController(registrar.context());
            discoveryControllerEventChannel = new EventChannel(registrar.messenger(), DISCOVERY_CONTROLLER_STREAM);
            discoveryControllerEventChannel.setStreamHandler(
                    new EventChannel.StreamHandler() {
                        @Override
                        public void onListen(Object o, EventChannel.EventSink sink) {
                            discoveryControllerEventSink.setDelegate(sink);
                        }

                        @Override
                        public void onCancel(Object o) {
                            discoveryControllerEventSink.setDelegate(null);
                        }
                    });

            this.mController.start("amzn.thin.pl", new DiscoveryController.IDiscoveryListener() {
                @Override
                public void playerDiscovered(RemoteMediaPlayer player) {
                    //add media player to the application’s player list.
                    Log.v("FLUTTER_FLING", player.getName() + " discovered, adding to players...");
                    mPlayers.add(player);
                    Map<String, String> playerMap = new HashMap<>();
                    playerMap.put("deviceName", player.getName());
                    playerMap.put("deviceUid", player.getUniqueIdentifier());
                    playerMap.put("event", "found");
                    discoveryControllerEventSink.success(playerMap);
                }

                @Override
                public void playerLost(RemoteMediaPlayer player) {
                    //remove media player from the application’s player list.
                    Log.v("FLUTTER_FLING", player.getName() + " lost, removing from players...");
                    mPlayers.remove(player);
                    Map<String, String> playerMap = new HashMap<>();
                    playerMap.put("deviceName", player.getName());
                    playerMap.put("deviceUid", player.getUniqueIdentifier());
                    playerMap.put("event", "lost");
                    discoveryControllerEventSink.success(playerMap);
                }

                @Override
                public void discoveryFailure() {
                    Log.v("FLUTTER_FLING", " Discovery failure");
                    discoveryControllerEventSink.error("discovery failure", "", null);
                }
            });
        }

        void stopDiscoveryController() {
            removePlayerListeners();
            if (mController != null) {
                mController.stop();
                discoveryControllerEventChannel.setStreamHandler(null);
                mController = null;
            }
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
                    playerStateEventSink.success(playerStatus);
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
            playerStateEventChannel = new EventChannel(registrar.messenger(), PLAYER_STATE_STREAM);
            mListener = new Monitor();
            mCurrentDevice.addStatusListener(mListener);
            long MONITOR_INTERVAL = 1000L;
            mCurrentDevice.setPositionUpdateInterval(MONITOR_INTERVAL);
            playerStateEventChannel.setStreamHandler(new EventChannel.StreamHandler() {
                @Override
                public void onListen(Object o, EventChannel.EventSink sink) {
                    playerStateEventSink.setDelegate(sink);
                }

                @Override
                public void onCancel(Object o) {
                    playerStateEventSink.setDelegate(null);
                }
            });

            mCurrentDevice.setMediaSource(name, title, true, false);

        }

        void stopPlayer() {
            removePlayerListeners();
            if (mCurrentDevice != null) {
                mCurrentDevice.stop();
            }
        }

        void removePlayerListeners() {
            if (mCurrentDevice != null) {
                mCurrentDevice.removeStatusListener(mListener);
                mListener = null;
                playerStateEventChannel.setStreamHandler(null);
            }
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

        void seekTo(long position) {
            if (mCurrentDevice != null)
                mCurrentDevice.seek(CustomMediaPlayer.PlayerSeekMode.Absolute, position);
        }

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
    }


}


