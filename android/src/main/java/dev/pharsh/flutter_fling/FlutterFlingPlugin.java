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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/**
 * FlutterFlingPlugin
 */
public class FlutterFlingPlugin implements MethodCallHandler {
    private static Set<RemoteMediaPlayer> players = new HashSet<>();


    /**
     * Plugin registration.
     */
    public static void registerWith(Registrar registrar) {
        DiscoveryController mController = new DiscoveryController(registrar.context());
        DiscoveryController.IDiscoveryListener mDiscovery = new DiscoveryController.IDiscoveryListener() {
            @Override
            public void playerDiscovered(RemoteMediaPlayer player) {
                //add media player to the application’s player list.
                Log.v("PLUGIN_TAG", player.getName() + " discovered, adding to players...");
                players.add(player);
            }

            @Override
            public void playerLost(RemoteMediaPlayer player) {
                //remove media player from the application’s player list.
                Log.v("PLUGIN_TAG", player.getName() + " lost, removing from players...");
                players.remove(player);
            }

            @Override
            public void discoveryFailure() {
                Log.v("PLUGIN_TAG", " Discovery failure");
            }
        };
        mController.start("amzn.thin.pl", mDiscovery);

        final MethodChannel channel = new MethodChannel(registrar.messenger(), "flutter_fling");
        channel.setMethodCallHandler(new FlutterFlingPlugin());
    }

    @Override
    public void onMethodCall(MethodCall call, Result result) {
        switch (call.method) {
            case "getDevices":
                final List<HashMap<String, String>> hashMaps = new ArrayList<>();
                for (RemoteMediaPlayer player :
                        players) {
                    HashMap<String, String> playerMap = new HashMap<>();
                    playerMap.put("deviceName", player.getName());
                    playerMap.put("deviceUid", player.getUniqueIdentifier());
                    hashMaps.add(playerMap);
                }
                result.success(hashMaps);
                break;
            case "selectDevice":
                final String uid = call.argument("uid");
                for (RemoteMediaPlayer player : players
                ) {
                    if (player.getUniqueIdentifier().equals(uid)) {
                        mCurrentDevice = player;
                        break;
                    }
                }
                result.success(null);
                break;
            case "getSelectedDevice":
                if (mCurrentDevice == null) {
                    result.error("device not selected or null", "", null);

                } else {
                    HashMap<String, String> playerMap = new HashMap<>();
                    playerMap.put("deviceName", mCurrentDevice.getName());
                    playerMap.put("deviceUid", mCurrentDevice.getUniqueIdentifier());
                    result.success(playerMap);
                }
                break;
            case "playMedia":
                final String mediaSource = call.argument("mediaSourceUri");
                final String mediaTitle = call.argument("mediaSourceTitle");
                if (mediaSource != null && mediaTitle != null) {
                    fling(mCurrentDevice, mediaSource, mediaTitle);
                    result.success(null);
                } else
                    result.error("mediaSource or mediaTitle is null", "", null);
                break;
            case "stopDevice":
                mCurrentDevice.stop();
                result.success(null);
                break;
            case "mediaState":
                MediaState state = mStatus.mState;
                if (state == null) {
                    result.error("media state is null", "", null);
                } else {
                    result.success(getMediaStateString(state));
                }
                break;
            default:
                result.notImplemented();
                break;
        }

//        if (call.method.equals("getDevices")) {
//            StringBuilder playerNames = new StringBuilder();
//            for (RemoteMediaPlayer player : players
//            ) {
//                playerNames.append(player.getName()).append(" ");
//            }
//            result.success(playerNames.toString());
//        } else if (call.method.equals("playMedia")) {
//
////            fling(players, "https://ran.gogodata.online/dl/OqndHU7f4IxweaaqtIbTcg/1564658020/889127646/5ca3772258fd44.44825533/D%20C%20Proper%20HDRip%20Season%201%20%5BEng%5D%20MSub.mkv", "SomeTitle");
//            result.success(null);
//        } else {
//            result.notImplemented();
//        }
    }

    private RemoteMediaPlayer mCurrentDevice;
    private CustomMediaPlayer.StatusListener mListener = new Monitor();
    private static final long MONITOR_INTERVAL = 1000L;


    private void fling(final RemoteMediaPlayer target, final String name, final String title) {
        mCurrentDevice = target;
        mCurrentDevice.addStatusListener(mListener); //.getAsync(new ErrorResultHandler("Cannot set status listener"));
        mCurrentDevice.setPositionUpdateInterval(MONITOR_INTERVAL);
//                .getAsync(new ErrorResultHandler("Error attempting set update interval, ignoring"));
        mCurrentDevice.setMediaSource(name, title, true, false);//.getAsync(new ErrorResultHandler("Error attempting to Play"));
    }

    private String getMediaStateString(MediaState state) {
        String stateString;
        switch (state) {
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


    private final Status mStatus = new Status();

    private static class Status {
        long mPosition;
        MediaState mState;
        MediaPlayerStatus.MediaCondition mCond;
    }

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

//    public enum MediaState {
//        NoSource, PreparingMedia, ReadyToPlay, Playing, Paused, Seeking, Finished, Error
//    };
//    public enum MediaCondition {
//        Good, WarningContent, WarningBandwidth, ErrorContent, ErrorChannel, ErrorUnknown
//    };


}
