package com.tonyostudios.ambience;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcelable;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;



import java.util.ArrayList;
import java.util.Collections;

/**
 * AmbientService is an Android Service that is used to control media playback
 * for audio. Ambient Service is advanced and has all basic audio control
 * types like play,pause,stop,resume,previous,skip, shuffle and repeat. The
 * Service also creates A notification in the Notification Drawer to control playback.
 * Use the Ambience Class to communicate with the AmbientService
 * @author TonyoStudios.com. Created on 11/18/2014. Updated on 12/01/2014.
 * @version 1.3
 *
 */
public class AmbientService extends Service implements MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener,
        AudioManager.OnAudioFocusChangeListener {


    /**
     * Tag used to identify the AmbientService
     */
    public final static String TAG = "com.tonyostudios.ambience.AmbientService";

    /**
     * Tag used to identify the AmbientService incoming broadcast receiver
     */
    public final static String AMBIENT_SERVICE_BROADCASTER = TAG + ".INCOMING_REQUEST_BROADCASTER";

    /**
     * Tag used to identify the AmbientService playlist
     */
    public final static String PLAYLIST = TAG + ".PLAYLIST";

    /**
     * Tag used to identify the AmbientService play position
     */
    public final static String PLAY_POSITION = TAG + ".PLAY_POSITION";

    /**
     * Tag used to identify the AmbientService volume level
     */
    public final static String VOLUME_LEVEL = TAG + ".VOLUME_VALUE";

    /**
     * Tag used to identify the AmbientService seek position
     */
    public final static String SEEK_POSITION = TAG + ".SEEK_POSITION";

    /**
     * Tag used to identify the AmbientService playback state
     */
    public final static String PLAYBACK_STATE = TAG + ".PLAYBACK_STATE";

    /**
     * Tag used to identify the AmbientService repeat mode
     */
    public final static String REPEAT_MODE = TAG + ".REPEAT_MODE";

    /**
     * Tag used to identify a track added to the AmbientService playlist
     */
    public final static String ADD_TRACK = TAG + ".ADD_TRACK";

    /**
     * Tag used to identify a track that needs to be removed from the the AmbientService playlist
     */
    public final static String REMOVE_TRACK = TAG + ".REMOVE_TRACK";

    /**
     * Tag used to identify the AmbientService shuffle mode
     */
    public final static String SHUFFLE_MODE = TAG + ".SHUFFLE_MODE";

    /**
     * Tag used to identify the AmbientService launch activity for notifications
     */
    public final static String ACTIVITY_LAUNCHER = TAG + ".ACTIVITY_LAUNCHER";

    /**
     * Tag used to identify the AmbientService progress track
     */
    public final static String TRACK_PROGRESS = TAG + ".TRACK_PROGRESS";

    /**
     * Tag used to identify the AmbientService track duration
     */
    public final static String TRACK_DURATION = TAG + ".TRACK_DURATION";

    /**
     * Tag used to identify the AmbientService current playing track
     */
    public final static String CURRENT_TRACK = TAG + ".CURRENT_PLAYING_TRACK";

    /**
     * Value used to update the handler/user interface
     */
    public final static int AUDIO_PROGRESS_UPDATE_TIME = 100;

    /**
     * Tag used to identify the AmbientService notification ID
     */
    private final int NOTIFICATION_CONTROL_ID = 101;


    /**
     * AmbientService shuffle modes
     */
    public static enum ShuffleMode
    {
        ON,
        OFF
    }

    /**
     * AmbientService repeat modes
     */
    public static enum RepeatMode
    {
        REPEAT_ONE,
        REPEAT_ALL,
        ShuffleMode, RepeatMode, OFF
    }

    /**
     * AmbientService playback states
     */
    public static enum PlaybackState
    {
        PLAY,
        STOP,
        PAUSE,
        RESUME,
        SKIP,
        PREVIOUS,
        PREPPING_TRACK,
        END_OF_PLAYLIST,
        CURRENT_PLAYING_TRACK_INFO,
        ERROR,
        SERVICE_STARTED,
        SERVICE_STOPPED
    }

    /**
     * Media player used by the service to play AmbientTracks
     */
    private MediaPlayer mPlayer;

    /**
     * Handler used to communicate updates to the UI thread
     */
    private Handler mHandler;

    /**
     * Allows service to keep the wifi-radio on when needed
     */
    private WifiManager.WifiLock mWifiLock;

    /**
     * Provides access to volume and ringer controls
     */
    private AudioManager mAudioManager;

    /**
     * Compat notification manager
     */
    private NotificationManagerCompat mNotificationManager;

    /**
     * ArrayList used to hold the original playlist before mPlaylist is shuffled
     */
    private ArrayList<AmbientTrack> mOriginalPlaylist = new ArrayList<AmbientTrack>();

    /**
     * ArrayList used to manage AmbientTracks sent to the AmbientService for processing
     */
    private ArrayList<AmbientTrack> mPlaylist = new ArrayList<AmbientTrack>();

    /**
     * Holds the current playing AmbientTrack
     */
    private AmbientTrack mAmbientTrack;

    /**
     * The current position of the current track in the playlist
     */
    private int playPosition = 0;


    /**
     * Holds the intent filter action name used to launch an activity when a notification is tapped
     * in the navigation drawer.
     */
    private String mActivityLauncher;

    /**
     * The bundle passed to the IncomingRequestReceiver. This bundle may contain
     * data or actions requested by the Ambience Class
     */
    private Bundle mBundle;

    /**
     * Holds the current repeat mode for the playlist
     */
    private RepeatMode mRepeatMode = RepeatMode.OFF;

    /**
     * Holds the current shuffle mode for the playlist
     */
    private ShuffleMode mShuffleState = ShuffleMode.OFF;


    /**
     * Holds the current volume level of the media player
     */
    private float mVolume = 0.5f;



    /**
     * Method used to bind a service to an Android Component such as an activity
     * @param intent intent object
     * @return binder object
     * @see  "http://developer.android.com/guide/components/bound-services.html"
     */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Called by the system every time a client explicitly starts the service by calling
     * startService(Intent), providing the arguments it supplied and a unique integer token
     * representing the start request. The Ambient Service is sticky by default.
     * @param intent The Intent supplied to startService(Intent), as given.
     * @param flags  Additional data about the request
     * @param startId  A unique integer representing this specific request to start.
     * @return The return value indicates what semantics the system should use for the service's
     * current started state.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    /**
     * The IncomingRequestBroadcast Receiver responds to all request made to the Ambient Service.
     * It is used to traffic all specific request to their intended route.
     */
    private BroadcastReceiver IncomingRequestReceiver = new BroadcastReceiver() {

        /**
         * Method used to handle the incoming request receiver intent
         * @param context Context Object
         * @param intent intent object
         */
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent == null || intent.getExtras() == null) {
                Log.e(TAG, "Passed an empty intent to IncomingRequestBroadcaster");
                return;
            }

            mBundle = intent.getExtras();


            if(mBundle.containsKey(ACTIVITY_LAUNCHER))
            {
                mActivityLauncher = mBundle.getString(ACTIVITY_LAUNCHER);
            }

            if(mBundle.containsKey(VOLUME_LEVEL))
            {
                setVolumeTo(mBundle.getFloat(VOLUME_LEVEL,0.5f));
            }

            if(mBundle.containsKey(PLAYLIST))
            {
                createPlaylist();
            }

            if(mBundle.containsKey(PLAY_POSITION))
            {
                setPlayPosition();
            }

            if(mBundle.containsKey(REMOVE_TRACK))
            {
                removeTrackFromPlaylist();
            }

            if(mBundle.containsKey(ADD_TRACK))
            {
                addTrackToPlaylist();
            }

            if(mBundle.containsKey(REPEAT_MODE))
            {
                setRepeatMode();
            }

            if(mBundle.containsKey(SHUFFLE_MODE))
            {
                setShuffleMode();
            }

            if(mBundle.containsKey(SEEK_POSITION))
            {
                seekTo(mBundle.getInt(SEEK_POSITION,0));
            }


            if(mBundle.containsKey(PLAYBACK_STATE))
            {

                try {
                    PlaybackState state = (PlaybackState) mBundle.getSerializable(PLAYBACK_STATE);

                    //PLAYBACK CONTROLS
                    switch (state) {
                        case PLAY: init();
                            break;
                        case STOP: stop();
                            break;
                        case PAUSE: pause();
                            break;
                        case RESUME: play();
                            break;
                        case SKIP: playNext();
                            break;
                        case PREVIOUS: playPrevious();
                            break;
                        default:
                            throw new IllegalStateException(TAG + ": Unknown Playback State");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, e.getMessage());
                }
            }
        }
    };

    /**
     * Called by the system when the service is first created.
     */
    @Override
    public void onCreate() {
        super.onCreate();

        createMediaPlayer();
        mHandler = new Handler();

        //get handle on audio manager, wifi lock and notification manager
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mWifiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL,TAG);


        mNotificationManager = NotificationManagerCompat.from(AmbientService.this);

        //register the incoming request receiver
        IntentFilter filter = new IntentFilter(AMBIENT_SERVICE_BROADCASTER);
        registerReceiver(IncomingRequestReceiver,filter);

        sendUpdateBroadcast(PlaybackState.SERVICE_STARTED);
    }

    /**
     * resets and create a new media player object
     */
    private void createMediaPlayer()
    {
        if(mPlayer != null)
        {
            try
            {
                mPlayer.release();
            }catch (Exception e)
            {
                e.printStackTrace();
                Log.e(TAG, e.getMessage());
            }

            mPlayer = null;
        }

        mPlayer = new MediaPlayer();

        //Set listeners on media player
        mPlayer.setOnCompletionListener(this);
        mPlayer.setOnPreparedListener(this);
        mPlayer.setOnErrorListener(this);

        ///set wake-lock mode for media player
        mPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
    }

    /**
     * Method used to added the passed AmbientPlaylist to the queue
     */
    private void createPlaylist()
    {
        if( mBundle == null || !mBundle.containsKey(PLAYLIST)
                ||  mBundle.getParcelableArrayList(PLAYLIST) == null)
        {
            throw new IllegalStateException(TAG + ": needs at least one AmbientTrack item to play");
        }

        if(mOriginalPlaylist != null)
        {
            mOriginalPlaylist.clear();
        }
        else
        {
            mOriginalPlaylist = new ArrayList<AmbientTrack>();
        }


        if(mPlaylist == null)
        {
            mPlaylist = new ArrayList<AmbientTrack>();
        }

        ArrayList<Parcelable> newTracks =  mBundle.getParcelableArrayList(PLAYLIST);

        for(int j = 0; j < newTracks.size(); j++)
        {
            mPlaylist.add((AmbientTrack)newTracks.get(j));
        }


        //Copy position to maintain shuffle & un-shuffle state
        for(int x = 0; x < mPlaylist.size(); x++)
        {
            mOriginalPlaylist.add(mPlaylist.get(x));
        }

        playPosition = 0;

    }

    /**
     * Called to remove a track from the current playlist
     */
    private void removeTrackFromPlaylist()
    {
        if(mBundle.getParcelable(REMOVE_TRACK) != null && mOriginalPlaylist != null
                && mPlaylist != null)
        {
            mOriginalPlaylist.remove((AmbientTrack)mBundle.getParcelable(REMOVE_TRACK));
            mPlaylist.remove((AmbientTrack)mBundle.getParcelable(REMOVE_TRACK));
        }
    }

    /**
     * Called to append a track to the current playlist
     */
    private void addTrackToPlaylist()
    {
        if(mBundle.getParcelable(ADD_TRACK) != null && mOriginalPlaylist != null
                && mPlaylist != null)
        {
            mOriginalPlaylist.add((AmbientTrack)mBundle.getParcelable(ADD_TRACK));
            mPlaylist.add((AmbientTrack)mBundle.getParcelable(ADD_TRACK));

            if(mShuffleState == ShuffleMode.ON && !mBundle.containsKey(SHUFFLE_MODE))
            {
                toggleShuffle(); //If shuffle is on reshuffle track
            }
        }
    }

    /**
     * Sets the repeat mode for the Ambient Playlist
     */
    private void setRepeatMode()
    {
        if(mBundle == null || !mBundle.containsKey(REPEAT_MODE)
                ||mBundle.getSerializable(REPEAT_MODE) == null )
        {
            Log.e(TAG,": No valid repeat mode");
            return;
        }

        mRepeatMode = (RepeatMode) mBundle.getSerializable(REPEAT_MODE);
    }

    /**
     * Sets the shuffle mode for the Ambient Playlist
     */
    private void setShuffleMode()
    {
        if(mBundle == null || !mBundle.containsKey(SHUFFLE_MODE)
                ||mBundle.getSerializable(SHUFFLE_MODE) == null )
        {
            Log.e(TAG,"No valid shuffle mode");
            return;
        }

        mShuffleState = (ShuffleMode) mBundle.getSerializable(SHUFFLE_MODE);

        toggleShuffle();
    }

    /**
     * Helper method used to toggle the shuffle state of the Ambient Playlist
     */
    private void toggleShuffle()
    {
        if(mPlaylist != null && mOriginalPlaylist != null)
        {
            if(mShuffleState == ShuffleMode.ON)
            {
                Collections.shuffle(mPlaylist);
                setCurrentAmbientTrackPosition();

            }else
            {
                mPlaylist.clear();

                for(int x = 0; x < mOriginalPlaylist.size(); x++)
                {
                    mPlaylist.add(mOriginalPlaylist.get(x));
                }

                setCurrentAmbientTrackPosition();
            }
        }
    }

    /**
     * Helper method used to get the position of the current playing AmbientTrack
     * before and after shuffle
     */
    private void  setCurrentAmbientTrackPosition()
    {
        if(mPlaylist != null)
        {
            for(int x = 0; x < mPlaylist.size(); x++)
            {
                if(mPlaylist.get(x) == mAmbientTrack)
                {
                    playPosition = x;
                    break;
                }
            }
        }
    }

    /**
     * Sets the play position of an Ambient track from the Ambient Playlist
     */
    private void setPlayPosition()
    {
        if( mBundle == null || !mBundle.containsKey(PLAY_POSITION))
        {
            throw new IllegalStateException(TAG + ": invalid play position");
        }

        playPosition = mBundle.getInt(PLAY_POSITION,0);

        if(playPosition < 0 || playPosition >= mPlaylist.size())
        {
            playPosition = 0;
        }
    }

    /**
     * Alerts the AmbientService about audio focus gain,
     * loss, etc.
     * @param focusChange value of focus changed
     */
    @Override
    public void onAudioFocusChange(int focusChange) {
        if(mPlayer == null)
        {
            return;
        }

        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:

                if (!mPlayer.isPlaying())
                {
                    play();
                }

                mPlayer.setVolume(mVolume,mVolume);
                break;

            case AudioManager.AUDIOFOCUS_LOSS:
                // Lost focus for an unbounded amount of time: stop playback and release media player
                if (mPlayer.isPlaying())
                {
                    pause();
                }

                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                // Lost focus for a short time, but we have to stop
                // playback. We don't release the media player because playback
                // is likely to resume
                if (mPlayer.isPlaying())
                {
                    pause();
                }

                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                // Lost focus for a short time, but it's ok to keep playing
                // at an attenuated level
                if (mPlayer.isPlaying())
                {
                    mPlayer.setVolume(0.1f, 0.1f);
                }

                break;
        }
    }

    /**
     * Initialize and prepare the media player with the ambient track
     */
    private void init()
    {
        sendUpdateBroadcast(PlaybackState.PREPPING_TRACK); // send prepping update to callback

        if(mPlayer == null)
        {
            createMediaPlayer();
        }

        try {

            if(mPlaylist == null || mPlaylist.get(playPosition) == null )
            {
                Log.e(TAG, ": The AmbientTrack item was null. Check the quality of your playlist before" +
                        " passing it to the AmbientService.");
                return;
            }

            mAmbientTrack = mPlaylist.get(playPosition);


            if(mPlayer.isPlaying())
            {
                mPlayer.stop();
            }
            mPlayer.reset();

            mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mPlayer.setDataSource(this, mAmbientTrack.getAudioUri()); // set audio source

            mPlayer.prepareAsync();
        }catch (Exception e)
        {
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
        }
    }

    /**
     * Plays the AmbientTrack
     */
    private void play()
    {
        try
        {
            int result = mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,AudioManager.AUDIOFOCUS_GAIN);

            if (result != AudioManager.AUDIOFOCUS_GAIN)
            {
                Log.i(TAG, ": could not get audio focus from manager");
            }

            mWifiLock.acquire();

            if(mPlayer != null)
            {
                mPlayer.start();
            }

            if(mHandler != null)
            {
                mHandler.postDelayed(mUpdateProgress, AUDIO_PROGRESS_UPDATE_TIME);
            }

            sendUpdateBroadcast(PlaybackState.PLAY); // sends a now playing update to the callback
            createNotification();
        }catch (Exception e)
        {
            e.printStackTrace();
            Log.e(TAG,e.getMessage());
        }
    }

    /**
     * Stops the current playing AmbientTrack
     */
    private void stop()
    {
        try
        {
            mAudioManager.abandonAudioFocus(this);
            mWifiLock.release();

            if(mPlayer != null && mPlayer.isPlaying())
            {
                mPlayer.stop();
            }
            sendUpdateBroadcast(PlaybackState.STOP); // sends a track has stopped update to the callback
        }catch (Exception e)
        {
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
        }
    }

    /**
     * Pause the current playing AmbientTrack
     */
    private void pause()
    {
        try
        {
            mAudioManager.abandonAudioFocus(this);
            mWifiLock.release();

            if(mPlayer != null && mPlayer.isPlaying())
            {
                mPlayer.pause();
            }

            sendUpdateBroadcast(PlaybackState.PAUSE); // sends a track has paused update to the callback
            createNotification();
        }catch (Exception e)
        {
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
        }
    }

    /**
     * Method used to play the previous AmbientTrack in the playlist
     */
    private void playPrevious()
    {
        if(mPlayer != null && mPlayer.isPlaying())
        {
            stop();
        }

        if(mPlaylist != null)
        {
            --playPosition;

            if(playPosition < 0 )
            {
                playPosition = mPlaylist.size() - 1;
            }

            init();
        }
    }

    /**
     * Method used to play the next AmbientTrack in the playlist
     */
    private void playNext()
    {
        if(mPlayer != null && mPlayer.isPlaying())
        {
            stop();
        }

        if(mPlaylist != null)
        {
            ++playPosition;

            if(playPosition >= mPlaylist.size())
            {
                sendUpdateBroadcast(PlaybackState.END_OF_PLAYLIST); // send end of playlist update to callback
                playPosition = 0;
            }

            init();
        }
    }

    /**
     * Runnable object used to update the main user interface with playback values
     */
    private Runnable mUpdateProgress = new Runnable() {
        public void run() {

            if (mHandler != null && mPlayer != null && mPlayer.isPlaying()) {

                int position = mPlayer.getCurrentPosition();
                int totalTime = mPlayer.getDuration();

                //Send bundle with all track information to callback
                Bundle bundle = new Bundle();
                bundle.putSerializable(PLAYBACK_STATE,PlaybackState.CURRENT_PLAYING_TRACK_INFO);

                bundle.putInt(TRACK_PROGRESS,position);
                bundle.putParcelable(CURRENT_TRACK,mAmbientTrack);
                bundle.putInt(TRACK_DURATION,totalTime);

                sendUpdateBroadcast(bundle);


                mHandler.postDelayed(this,AUDIO_PROGRESS_UPDATE_TIME);
            }
        }
    };


    /**
     * Alerts the AmbientService when an AmbientTrack is
     * done playing.
     * @param mp Media Player object
     */
    @Override
    public void onCompletion(MediaPlayer mp) {


        sendUpdateBroadcast(PlaybackState.STOP); //send a stop update to the callback


        if(mNotificationManager != null)
        {
            mNotificationManager.cancel(NOTIFICATION_CONTROL_ID);
        }

        if(mRepeatMode != null)
        {
            if(mRepeatMode == RepeatMode.REPEAT_ALL)
            {
                playNext();
            }else if(mRepeatMode == RepeatMode.REPEAT_ONE)
            {
                play();
            }
        }

    }

    /**
     * Method used to alert the AmbientService that an error
     * has occurred with the Media Player
     * @param mp Media Player object
     * @param what What error occurred
     * @param extra Extra error information about the error
     * @return The success of the error handling
     */
    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        String errorMessage = TAG + ".Error - What: \" + what + \", extra: \" + extra";

        Log.e(TAG, errorMessage);

        createMediaPlayer(); // reset media player to original state

        sendUpdateBroadcast(PlaybackState.ERROR); // send error update to the callback

        if(mNotificationManager != null)
        {
            mNotificationManager.cancel(NOTIFICATION_CONTROL_ID);
        }

        return false;
    }

    /**
     * Alters the AmbientService when the media player has prepared an AmbientTrack
     * and is ready for play.
     * @param mp media player object
     */
    @Override
    public void onPrepared(MediaPlayer mp) {
        play();
    }

    /**
     * Sets the volume level of the media player
     * @param volumeLevel volume level
     */
    private void setVolumeTo(float volumeLevel) {

        if(mPlayer != null)
        {
            if(volumeLevel < 0.0f || volumeLevel > 1.0f )
            {
                volumeLevel = 0.5f;
            }

            mVolume = volumeLevel;

            mPlayer.setVolume(mVolume,mVolume);
        }
    }

    /**
     * Set the seek position of the current AmbientTrack
     */
    private void seekTo(int position)
    {
        if(mPlayer != null && position >= 0 && position <= mPlayer.getDuration())
        {
            mPlayer.seekTo(position);
        }
    }

    /**
     * Method used to send an intent to the Ambience Broadcast Receiver to update the callback
     * component.
     * @param bundle A bundle object containing the current AmbientTrack component
     */
    private void sendUpdateBroadcast(Bundle bundle)
    {
        Intent intent = new Intent(Ambience.AMBIENCE_BROADCASTER);
        intent.putExtras(bundle);
        sendBroadcast(intent);
    }

    /**
     * Method used to send an intent to the Ambience Broadcast Receiver to update the callback
     * component.
     * @param value  Playback state of the Current AmbientTrack
     */
    private void sendUpdateBroadcast(PlaybackState value)
    {
        Intent intent = new Intent(Ambience.AMBIENCE_BROADCASTER);
        intent.putExtra(PLAYBACK_STATE,value);
        sendBroadcast(intent);
    }


    /**
     * Method used to bundle the Ambient Service current information
     * @return A bundle containing the ambientService current playing track,
     * playlist, shuffle mode, repeat mode, volume level and action launcher string
     *
     */
    private Bundle buildAmbientServiceBundle()
    {
        Bundle bundle = new Bundle();

        if(mActivityLauncher != null)
        {
            bundle.putString(ACTIVITY_LAUNCHER,mActivityLauncher);
        }

        if(mAmbientTrack != null)
        {
            bundle.putParcelable(CURRENT_TRACK,mAmbientTrack);
        }


        if(mRepeatMode != null)
        {
            bundle.putSerializable(REPEAT_MODE,mRepeatMode);
        }

        if(mShuffleState != null)
        {
            bundle.putSerializable(SHUFFLE_MODE,mShuffleState);
        }

        if(mPlaylist != null)
        {
            bundle.putParcelableArrayList(PLAYLIST,mPlaylist);
        }


        bundle.putInt(PLAY_POSITION,playPosition);

        bundle.putFloat(VOLUME_LEVEL,mVolume);

        return bundle;
    }

    /**
     * Called before the service terminates. All resources used by the AmbientService are
     * cleaned up in this method.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();

        /*
         * RELEASE ALL AMBIENT SERVICE RESOURCES
         */
        if(mPlayer != null)
        {
            try
            {
                if(mPlayer.isPlaying())
                {
                    stop();
                }

                mPlayer.release();
                mPlayer = null;
            }catch (Exception e)
            {
                e.printStackTrace();
                Log.e(TAG, e.getMessage());
            }
        }

        mHandler = null;
        mAudioManager = null;

        try
        {
            mWifiLock.release();
        }catch (Exception e)
        {
            e.printStackTrace();
            Log.e(TAG,e.getMessage());
        }

        mWifiLock = null;
        mBundle = null;

		if(mOriginalPlaylist != null)
		{
			mOriginalPlaylist.clear();
		}
		
		if(mPlaylist != null)
		{
			mPlaylist.clear();
		}
		
        mPlaylist = null;
        mOriginalPlaylist = null;
        mActivityLauncher = null;
        mAmbientTrack = null;
        mVolume = 0.5f;
        playPosition = 0;


        if(mNotificationManager != null)
        {
            mNotificationManager.cancel(NOTIFICATION_CONTROL_ID);
            mNotificationManager = null;
        }

        unregisterReceiver(IncomingRequestReceiver);
        sendUpdateBroadcast(PlaybackState.SERVICE_STOPPED);
    }

    /**
     * Creates a notification for the AmbientTrack in the Notification Drawer.
     * The notification contains the track Image, track name, and the album the
     * track belongs to. Also, the notification contains media playback controls
     * and can launch an action.
     *
     * A bundle object is appended to the notification pendingIntent. This bundle contains
     * the ambientService current playing track, playlist, shuffle mode, repeat mode,
     * volume level and action launcher string. A user app can use this information to restore
     * state.
     *
     * The AmbientService uses the Picasso Library
     * by Square to get the album image from memory, cache or internet.
     * @see "http://square.github.io/picasso/"
     */
    public void createNotification()
    {
        if(mAmbientTrack == null)
        {
            Log.e(TAG,": AmbientTrack is null. Cannot create a notification");
            return;
        }

        final Drawable placeholderDrawable = getResources().getDrawable(R.drawable.unknown_album);

        try {

            Picasso.with(this).load(mAmbientTrack.getAlbumImageUri())
                    .placeholder(placeholderDrawable)
                    .error(placeholderDrawable)
                    .into(new Target() {
                        @Override
                        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom loadedFrom) {

                            if(bitmap == null)
                            {
                                bitmap = ((BitmapDrawable) placeholderDrawable).getBitmap();
                            }

                            buildNotification(bitmap);
                        }

                        @Override
                        public void onBitmapFailed(Drawable drawable) {

                            if(drawable == null)
                            {
                                drawable = placeholderDrawable;
                            }

                            buildNotification(((BitmapDrawable) drawable).getBitmap());
                        }

                        @Override
                        public void onPrepareLoad(Drawable drawable) {

                            if(drawable == null)
                            {
                                drawable = placeholderDrawable;
                            }

                            buildNotification(((BitmapDrawable) drawable).getBitmap());
                        }
                    });
        }catch (Exception e)
        {
            e.printStackTrace();
            Log.e(TAG,e.getMessage());

            buildNotification(((BitmapDrawable) placeholderDrawable).getBitmap());
        }

    }

    /**
     * Helper method used to build a notification. Notifications are compatible with Android wearables
     * @param bitmap the AmbientTracks album cover bitmap
     */
    private void buildNotification(Bitmap bitmap)
    {
        if(mNotificationManager == null)
        {
            mNotificationManager = NotificationManagerCompat.from(this);
        }

        NotificationCompat.WearableExtender wearableExtender = new NotificationCompat.WearableExtender();


        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.app_icon); //replace with app logo by naming your app logo app_icon



        //Set activity to launch from notification drawer
        if(mActivityLauncher != null)
        {
            Intent activityIntent = new Intent();
            activityIntent.setAction(mActivityLauncher);
            activityIntent.putExtras(buildAmbientServiceBundle()); // pass ambient service state

            PendingIntent activityPending = PendingIntent.getActivity(AmbientService.this,800,
                    activityIntent,PendingIntent.FLAG_UPDATE_CURRENT);

            builder.setContentIntent(activityPending);
        }

        if(mAmbientTrack.getName() != null && !mAmbientTrack.getName().equals(""))
        {
            builder.setContentTitle(mAmbientTrack.getName());

        }
        else
        {
            builder.setContentTitle(getString(R.string.unknown_track));
        }


        if(mAmbientTrack.getAlbumName() != null && !mAmbientTrack.getAlbumName().equals(""))
        {
            builder.setContentText(mAmbientTrack.getAlbumName());
        }
        else
        {
            builder.setContentText(getString(R.string.unknown_artist));
        }

        builder.setLargeIcon(bitmap);


        Intent previousIntent = new Intent(AMBIENT_SERVICE_BROADCASTER);
        previousIntent.putExtra(PLAYBACK_STATE,PlaybackState.PREVIOUS);
        int drawablePrevious = android.R.drawable.ic_media_previous;

        PendingIntent PrevIntent = PendingIntent.getBroadcast(AmbientService.this,
                801,previousIntent,PendingIntent.FLAG_UPDATE_CURRENT);



        Intent playIntent = new Intent(AMBIENT_SERVICE_BROADCASTER);


        int drawableId = android.R.drawable.ic_media_pause;


        // SONG IS PLAYING SET INTENT TO PAUSE
        if(mPlayer.isPlaying())
        {
            playIntent.putExtra(PLAYBACK_STATE,PlaybackState.PAUSE);

        }
        else
        {
            playIntent.putExtra(PLAYBACK_STATE,PlaybackState.RESUME);
            drawableId = android.R.drawable.ic_media_play;
        }


        PendingIntent pIntent = PendingIntent.getBroadcast(AmbientService.this,802,
                playIntent,PendingIntent.FLAG_UPDATE_CURRENT);




        Intent forwardIntent = new Intent(AMBIENT_SERVICE_BROADCASTER);
        forwardIntent.putExtra(PLAYBACK_STATE,PlaybackState.SKIP);
        int drawableForward = android.R.drawable.ic_media_next;

        PendingIntent NextIntent = PendingIntent.getBroadcast(AmbientService.this,803,
                forwardIntent,PendingIntent.FLAG_UPDATE_CURRENT);



        builder.addAction(drawablePrevious,"",PrevIntent);
        builder.addAction(drawableId,"",pIntent);
        builder.addAction(drawableForward,"",NextIntent);
        builder.extend(wearableExtender);

        mNotificationManager.notify(NOTIFICATION_CONTROL_ID,builder.build());

    }

}
