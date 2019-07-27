package dev.pharsh.flutter_fling;

import android.util.Log;

import com.amazon.whisperplay.fling.media.controller.DiscoveryController;
import com.amazon.whisperplay.fling.media.controller.RemoteMediaPlayer;
import com.amazon.whisperplay.fling.media.service.CustomMediaPlayer;
import com.amazon.whisperplay.fling.media.service.MediaPlayerStatus;
import com.amazon.whisperplay.fling.media.service.MediaPlayerStatus.MediaState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/**
 * FlutterFlingPlugin
 */
public class FlutterFlingPlugin implements MethodCallHandler {
    private FlingSdk flingSdk = new FlingSdk();

    static class Status {
        long mPosition;
        MediaState mState;
        MediaPlayerStatus.MediaCondition mCond;
    }


    /**
     * Plugin registration.
     */
    public static void registerWith(Registrar registrar) {

        FlingSdk.registrar = registrar;

        final MethodChannel channel = new MethodChannel(registrar.messenger(), "flutter_fling");
        channel.setMethodCallHandler(new FlutterFlingPlugin());
    }

    @Override
    public void onMethodCall(MethodCall call, Result result) {
//        FlingSdk flingSdk = new FlingSdk();

        onMethodCall(call, result, flingSdk);
    }

    private void onMethodCall(MethodCall call, Result result, FlingSdk flingSdk) {
        switch (call.method) {
            case "startDiscoveryController":
                flingSdk.assignDiscoveryController();
                flingSdk.startDiscoveryController();
                result.success(null);
                break;
            case "stopDiscoveryController":
                flingSdk.stopDiscoveryController();
                result.success(null);
                break;
            case "getPlayers":
                result.success(flingSdk.getDiscoveredPlayersHashMap());
                break;
            case "play":
                final String playerUid = call.argument("deviceUid");
                final String mediaSourceUri = call.argument("mediaSourceUri");
                final String mediaSourceTitle = call.argument("mediaSourceTitle");
                if (playerUid == null || mediaSourceUri == null || mediaSourceTitle == null)
                    result.error("playerUid/mediaSourceUri/mediaSourceTitle cannot be null", "", null);
                else {
//                    flingSdk.stopDiscoveryController();
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
                final boolean muteState = call.argument("muteState");
                Log.v("TAG", "MUTE ARGUMENT RECEIVED : "+ muteState);
                flingSdk.setMute(muteState);
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
            case "playerState":
                //TODO: This should be a EventChannel
                result.success(flingSdk.getPlayerState());
                break;
            default:
                result.notImplemented();
                break;
        }
    }


}

class FlingSdk {
    //    static final String PLAYERSTATUSSTREAM = "fling/playerStatusStream";
//    EventChannel.EventSink playerStatusEvents;
    static Registrar registrar;
    //    private EventChannel playerStatusEventChannel;
    private Set<RemoteMediaPlayer> mPlayers = new HashSet<>();
    private DiscoveryController mController;
    private RemoteMediaPlayer mCurrentDevice;
    private CustomMediaPlayer.StatusListener mListener = new Monitor();
    private final FlutterFlingPlugin.Status mStatus = new FlutterFlingPlugin.Status();

    private class Monitor implements CustomMediaPlayer.StatusListener {
        @Override
        public void onStatusChange(MediaPlayerStatus status, long position) {
            synchronized (mStatus) {
                mStatus.mState = status.getState();
                mStatus.mCond = status.getCondition();
                mStatus.mPosition = position;
            }
        }
    }

    void assignDiscoveryController() {
        mController = new DiscoveryController(registrar.context());
    }

    void startDiscoveryController() {
        this.mController.start("amzn.thin.pl", this.mDiscovery);
    }

    void stopDiscoveryController() {
        this.mController.stop();
    }

    List<HashMap<String, String>> getDiscoveredPlayersHashMap() {
        final List<HashMap<String, String>> hashMaps = new ArrayList<>();
        for (RemoteMediaPlayer player :
                mPlayers) {
            HashMap<String, String> playerMap = new HashMap<>();
            playerMap.put("deviceName", player.getName());
            playerMap.put("deviceUid", player.getUniqueIdentifier());
            hashMaps.add(playerMap);
        }
        return hashMaps;
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
//        if(playerStatusEventChannel != null)
//            playerStatusEventChannel.setStreamHandler(null);
//        playerStatusEventChannel = new EventChannel(registrar.messenger(), PLAYERSTATUSSTREAM).setStreamHandler(
//                new EventChannel.StreamHandler() {
//                    @Override
//                    public void onListen(Object args, final EventChannel.EventSink events) {events.
//                        Log.w(TAG, "adding listener");
//                    }
//
//                    @Override
//                    public void onCancel(Object args) {
//                    }
//                }
//        );

        mCurrentDevice.addStatusListener(mListener); //.getAsync(new ErrorResultHandler("Cannot set status listener"));
        long MONITOR_INTERVAL = 1000L;
        mCurrentDevice.setPositionUpdateInterval(MONITOR_INTERVAL);
//                .getAsync(new ErrorResultHandler("Error attempting set update interval, ignoring"));
        mCurrentDevice.setMediaSource(name, title, true, false);//.getAsync(new ErrorResultHandler("Error attempting to Play"));
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

    private DiscoveryController.IDiscoveryListener mDiscovery = new DiscoveryController.IDiscoveryListener() {
        @Override
        public void playerDiscovered(RemoteMediaPlayer player) {
            //add media player to the application’s player list.
            Log.v("PLUGIN_TAG", player.getName() + " discovered, adding to players...");
            mPlayers.add(player);
        }

        @Override
        public void playerLost(RemoteMediaPlayer player) {
            //remove media player from the application’s player list.
            Log.v("PLUGIN_TAG", player.getName() + " lost, removing from players...");
            mPlayers.remove(player);
        }

        @Override
        public void discoveryFailure() {
            Log.v("PLUGIN_TAG", " Discovery failure");
        }
    };

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
                stateString = "Preparing Media";
                break;
            case ReadyToPlay:
                stateString = "Ready to Play";
                break;
            case NoSource:
                stateString = "No Source";
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

    //    public enum MediaState {
//        NoSource, PreparingMedia, ReadyToPlay, Playing, Paused, Seeking, Finished, Error
//    };
//    public enum MediaCondition {
//        Good, WarningContent, WarningBandwidth, ErrorContent, ErrorChannel, ErrorUnknown
//    };
}
