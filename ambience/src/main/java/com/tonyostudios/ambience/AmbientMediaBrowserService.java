package com.tonyostudios.ambience;


import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.MediaPlayer;
import android.media.Rating;
import android.media.browse.MediaBrowser;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcelable;
import android.os.PowerManager;
import android.os.ResultReceiver;
import android.service.media.MediaBrowserService;
import android.util.Log;
import android.view.KeyEvent;


import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * AmbientTvService is a specific Android Service for androidTV and androidAuto that is used to control media playback
 * for audio. AmbientService is advanced and has all basic audio control
 * types like play,pause,stop,resume,previous,skip, shuffle and repeat. The
 * Service also creates A notification in the Notification Drawer to control playback.
 * Use the Ambience Class to communicate with the AmbientService
 * @author TonyoStudios.com. Created on 11/18/2014. Updated on 12/01/2014.
 * @version 1.3
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class AmbientMediaBrowserService extends MediaBrowserService implements MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener,
        AudioManager.OnAudioFocusChangeListener {


    /**
     * Tag used to identify the AmbientService media token
     */
    public final static String MEDIA_SESSION_TOKEN_TAG = AmbientService.TAG + ".MEDIA_SESSION_TOKEN_TAG";

    /**
     * Holds the MediaSession object
     */
    private MediaSession mSession;

    /**
     * Holds the Media Session state value
     */
    private long mState = PlaybackState.STATE_NONE;

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
     * Holds the intent filter action name used to launch an activity when a now playing card
     * is clicked on.
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
    private AmbientService.RepeatMode mRepeatMode = AmbientService.RepeatMode.OFF;

    /**
     * Holds the current shuffle mode for the playlist
     */
    private AmbientService.ShuffleMode mShuffleState = AmbientService.ShuffleMode.OFF;


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
                Log.e(AmbientService.TAG, "Passed an empty intent to IncomingRequestBroadcaster");
                return;
            }

            mBundle = intent.getExtras();


            if(mBundle.containsKey(AmbientService.ACTIVITY_LAUNCHER))
            {
                mActivityLauncher = mBundle.getString(AmbientService.ACTIVITY_LAUNCHER);
                updateCardLaunchActivity();
            }

            if(mBundle.containsKey(AmbientService.VOLUME_LEVEL))
            {
                setVolumeTo(mBundle.getFloat(AmbientService.VOLUME_LEVEL,0.5f));
            }

            if(mBundle.containsKey(AmbientService.PLAYLIST))
            {
                createPlaylist();
            }

            if(mBundle.containsKey(AmbientService.PLAY_POSITION))
            {
                setPlayPosition();
            }

            if(mBundle.containsKey(AmbientService.REMOVE_TRACK))
            {
                removeTrackFromPlaylist();
            }

            if(mBundle.containsKey(AmbientService.ADD_TRACK))
            {
                addTrackToPlaylist();
            }

            if(mBundle.containsKey(AmbientService.REPEAT_MODE))
            {
                setRepeatMode();
            }

            if(mBundle.containsKey(AmbientService.SHUFFLE_MODE))
            {
                setShuffleMode();
            }

            if(mBundle.containsKey(AmbientService.SEEK_POSITION))
            {
                seekTo(mBundle.getInt(AmbientService.SEEK_POSITION,0));
            }


            if(mBundle.containsKey(AmbientService.PLAYBACK_STATE))
            {

                try {
                    AmbientService.PlaybackState state =
                            (AmbientService.PlaybackState) mBundle.getSerializable(AmbientService.PLAYBACK_STATE);

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
                            throw new IllegalStateException(AmbientService.TAG + ": Unknown Playback State");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(AmbientService.TAG, e.getMessage());
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
        createMediaSession();

        mHandler = new Handler();

        //get handle on audio manager, wifi lock and notification manager
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mWifiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL,AmbientService.TAG);



        //register the incoming request receiver
        IntentFilter filter = new IntentFilter(AmbientService.AMBIENT_SERVICE_BROADCASTER);
        registerReceiver(IncomingRequestReceiver,filter);

        sendUpdateBroadcast(AmbientService.PlaybackState.SERVICE_STARTED);

    }

    /**
     * Called to create a set a media session object for AndroidTV Now Playing Card
     */
    private void createMediaSession()
    {

        if(mSession != null)
        {
            mSession.release();
            mSession = null;
        }

        mSession = new MediaSession(this, MEDIA_SESSION_TOKEN_TAG);
        mSession.setCallback(new MediaSessionCallback());
        mSession.setFlags(MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS | MediaSession.FLAG_HANDLES_MEDIA_BUTTONS);

        setSessionToken(mSession.getSessionToken());

        updateCardLaunchActivity();

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
            bundle.putString(AmbientService.ACTIVITY_LAUNCHER,mActivityLauncher);
        }

        if(mAmbientTrack != null)
        {
            bundle.putParcelable(AmbientService.CURRENT_TRACK,mAmbientTrack);
        }


        if(mRepeatMode != null)
        {
            bundle.putSerializable(AmbientService.REPEAT_MODE,mRepeatMode);
        }

        if(mShuffleState != null)
        {
            bundle.putSerializable(AmbientService.SHUFFLE_MODE,mShuffleState);
        }

        if(mPlaylist != null)
        {
            bundle.putParcelableArrayList(AmbientService.PLAYLIST,mPlaylist);
        }


        bundle.putInt(AmbientService.PLAY_POSITION,playPosition);

        bundle.putFloat(AmbientService.VOLUME_LEVEL,mVolume);

        return bundle;
    }

    /**
     * Called to set the launch activity of th Now Playing Card on AndroidTV
     */
    private void updateCardLaunchActivity()
    {
        Intent intent = new Intent();

        //Set activity to launch from notification drawer
        if(mActivityLauncher != null)
        {
            intent.setAction(mActivityLauncher);
            intent.putExtras(buildAmbientServiceBundle());
        }

        PendingIntent pi = PendingIntent.getActivity(this, 99 /*request code*/,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);
        mSession.setSessionActivity(pi);
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
                Log.e(AmbientService.TAG, e.getMessage());
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
        if( mBundle == null || !mBundle.containsKey(AmbientService.PLAYLIST)
                ||  mBundle.getParcelableArrayList(AmbientService.PLAYLIST) == null)
        {
            throw new IllegalStateException(AmbientService.TAG + ": needs at least one AmbientTrack item to play");
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


        ArrayList<Parcelable> newTracks =  mBundle.getParcelableArrayList(AmbientService.PLAYLIST);

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
        if(mBundle.getParcelable(AmbientService.REMOVE_TRACK) != null && mOriginalPlaylist != null
                && mPlaylist != null)
        {
            mOriginalPlaylist.remove((AmbientTrack)mBundle.getParcelable(AmbientService.REMOVE_TRACK));
            mPlaylist.remove((AmbientTrack)mBundle.getParcelable(AmbientService.REMOVE_TRACK));
        }
    }

    /**
     * Called to append a track to the current playlist
     */
    private void addTrackToPlaylist()
    {
        if(mBundle.getParcelable(AmbientService.ADD_TRACK) != null && mOriginalPlaylist != null
                && mPlaylist != null)
        {
            mOriginalPlaylist.add((AmbientTrack)mBundle.getParcelable(AmbientService.ADD_TRACK));
            mPlaylist.add((AmbientTrack)mBundle.getParcelable(AmbientService.ADD_TRACK));

            if(mShuffleState == AmbientService.ShuffleMode.ON && !mBundle.containsKey(AmbientService.SHUFFLE_MODE))
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
        if(mBundle == null || !mBundle.containsKey(AmbientService.REPEAT_MODE)
                ||mBundle.getSerializable(AmbientService.REPEAT_MODE) == null )
        {
            Log.e(AmbientService.TAG,": No valid repeat mode");
            return;
        }

        mRepeatMode = (AmbientService.RepeatMode) mBundle.getSerializable(AmbientService.REPEAT_MODE);
    }

    /**
     * Sets the shuffle mode for the Ambient Playlist
     */
    private void setShuffleMode()
    {
        if(mBundle == null || !mBundle.containsKey(AmbientService.SHUFFLE_MODE)
                ||mBundle.getSerializable(AmbientService.SHUFFLE_MODE) == null )
        {
            Log.e(AmbientService.TAG,"No valid shuffle mode");
            return;
        }

        mShuffleState = (AmbientService.ShuffleMode) mBundle.getSerializable(AmbientService.SHUFFLE_MODE);

        toggleShuffle();
    }

    /**
     * Helper method used to toggle the shuffle state of the Ambient Playlist
     */
    private void toggleShuffle()
    {
        if(mPlaylist != null && mOriginalPlaylist != null)
        {
            if(mShuffleState == AmbientService.ShuffleMode.ON)
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
        if( mBundle == null || !mBundle.containsKey(AmbientService.PLAY_POSITION))
        {
            throw new IllegalStateException(AmbientService.TAG + ": invalid play position");
        }

        playPosition = mBundle.getInt(AmbientService.PLAY_POSITION,0);

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
                    if(mSession != null && mSession.isActive())
                    {
                        mSession.setActive(false);
                    }

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
        sendUpdateBroadcast(AmbientService.PlaybackState.PREPPING_TRACK); // send prepping update to callback

        if(mPlayer == null)
        {
            createMediaPlayer();
        }

        try {

            if(mPlaylist == null || mPlaylist.get(playPosition) == null )
            {
                Log.e(AmbientService.TAG, ": The AmbientTrack item was null. Check the quality of your playlist before" +
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
            Log.e(AmbientService.TAG, e.getMessage());
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
                Log.i(AmbientService.TAG, ": could not get audio focus from manager");
            }

            mWifiLock.acquire();

            if(mPlayer != null)
            {
                mPlayer.start();
                mState = PlaybackState.ACTION_PLAY;
            }

            if(mSession != null)
            {
                mSession.setPlaybackState(getPlaybackState());

                if(!mSession.isActive())
                {
                    mSession.setActive(true);
                }
            }

            metadataBitmapHelper();

            if(mHandler != null)
            {
                mHandler.postDelayed(mUpdateProgress, AmbientService.AUDIO_PROGRESS_UPDATE_TIME);
            }

            sendUpdateBroadcast(AmbientService.PlaybackState.PLAY); // sends a now playing update to the callback
        }catch (Exception e)
        {
            e.printStackTrace();
            Log.e(AmbientService.TAG,e.getMessage());
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
                mState = PlaybackState.ACTION_STOP;
            }

            if(mSession != null)
            {
                mSession.setPlaybackState(getPlaybackState());
            }

            sendUpdateBroadcast(AmbientService.PlaybackState.STOP); // sends a track has stopped update to the callback
        }catch (Exception e)
        {
            e.printStackTrace();
            Log.e(AmbientService.TAG, e.getMessage());
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
                mState = PlaybackState.ACTION_PAUSE;
            }

            if(mSession != null)
            {
                mSession.setPlaybackState(getPlaybackState());
            }

            sendUpdateBroadcast(AmbientService.PlaybackState.PAUSE); // sends a track has paused update to the callback
        }catch (Exception e)
        {
            e.printStackTrace();
            Log.e(AmbientService.TAG, e.getMessage());
        }
    }

    /**
     * Method used to play the previous AmbientTrack in the playlist
     */
    private void playPrevious()
    {
        mState = PlaybackState.ACTION_SKIP_TO_PREVIOUS;

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
        mState = PlaybackState.ACTION_SKIP_TO_NEXT;

        if(mPlayer != null && mPlayer.isPlaying())
        {
            stop();
        }

        if(mPlaylist != null)
        {
            ++playPosition;

            if(playPosition >= mPlaylist.size())
            {
                sendUpdateBroadcast(AmbientService.PlaybackState.END_OF_PLAYLIST); // send end of playlist update to callback
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
                bundle.putSerializable(AmbientService.PLAYBACK_STATE, AmbientService.PlaybackState.CURRENT_PLAYING_TRACK_INFO);

                bundle.putInt(AmbientService.TRACK_PROGRESS,position);
                bundle.putParcelable(AmbientService.CURRENT_TRACK,mAmbientTrack);
                bundle.putInt(AmbientService.TRACK_DURATION,totalTime);

                sendUpdateBroadcast(bundle);


                mHandler.postDelayed(this,AmbientService.AUDIO_PROGRESS_UPDATE_TIME);
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


        sendUpdateBroadcast(AmbientService.PlaybackState.STOP); //send a stop update to the callback

        if (!mSession.isActive()) {
            mSession.setActive(false);
        }

        if(mSession != null)
        {
            mSession.release();
            mSession = null;
            createMediaSession();
        }


        if(mRepeatMode != null)
        {
            if(mRepeatMode == AmbientService.RepeatMode.REPEAT_ALL)
            {
                playNext();
            }else if(mRepeatMode == AmbientService.RepeatMode.REPEAT_ONE)
            {
                play();
            }
        }

    }

    /**
     * Method used to alert the AmbientService that an error
     * has occured with the Media Player
     * @param mp Media Player object
     * @param what What error occurred
     * @param extra Extra error information about the error
     * @return The success of the error handling
     */
    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        String errorMessage = AmbientService.TAG + ".Error - What: \" + what + \", extra: \" + extra";

        Log.e(AmbientService.TAG, errorMessage);

        createMediaPlayer(); // reset media player to original state

        sendUpdateBroadcast(AmbientService.PlaybackState.ERROR); // send error update to the callback

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
    private void sendUpdateBroadcast(AmbientService.PlaybackState value)
    {
        Intent intent = new Intent(Ambience.AMBIENCE_BROADCASTER);
        intent.putExtra(AmbientService.PLAYBACK_STATE, value);
        sendBroadcast(intent);
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
                Log.e(AmbientService.TAG, e.getMessage());
            }
        }

        if(mSession != null)
        {
            if(mSession.isActive())
            {
                mSession.setActive(false);
            }

            mSession.release();
        }

        mSession = null;
        mState = 0;

        mHandler = null;
        mAudioManager = null;

        try
        {
            mWifiLock.release();
        }catch (Exception e)
        {
            e.printStackTrace();
            Log.e(AmbientService.TAG,e.getMessage());
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

        unregisterReceiver(IncomingRequestReceiver);
        sendUpdateBroadcast(AmbientService.PlaybackState.SERVICE_STOPPED);
    }

    /**
     * Called to get the root information for browsing by a particular client.
     *The implementation should verify that the client package has permission to access browse media
     *information before returning the root id; it should return null if the client is not allowed
     * to access this information.
     * @param clientPackageName The package name of the application which is requesting access to browse media.
     * @param clientUid The uid of the application which is requesting access to browse media.
     * @param rootHints An optional bundle of service-specific arguments to send to the media browse
     *                  service when connecting and retrieving the root id for browsing, or null if none.
     *                  The contents of this bundle may affect the information returned when browsing.
     * @return A BrowserRoot object
     */
    @Override
    public BrowserRoot onGetRoot(String clientPackageName, int clientUid, Bundle rootHints) {
        //not implemented
        return null;
    }

    /**
     * Called to get information about the children of a media item.
     * @param parentId The id of the parent media item whose children are to be queried.
     * @param result The list of children, or null if the id is invalid.
     */
    @Override
    public void onLoadChildren(String parentId, Result<List<MediaBrowser.MediaItem>> result) {
        //not implemented
    }

    /**
     * Called to get the playback state of the media player. This method updates the media session
     */
    private PlaybackState getPlaybackState()
    {
        long position = android.media.session.PlaybackState.PLAYBACK_POSITION_UNKNOWN;

        if (mPlayer != null && mPlayer.isPlaying()) {
            position = mPlayer.getCurrentPosition();
        }
        android.media.session.PlaybackState.Builder stateBuilder = new android.media.session.PlaybackState.Builder()
                .setActions(getAvailableActions());
        stateBuilder.setState((int)mState, position, 1.0f);

        return stateBuilder.build();
    }

    /**
     * Called to get the available actions for the now playing card
     * @return actions value
     */
    private long getAvailableActions() {

        long actions = android.media.session.PlaybackState.ACTION_PLAY |
                android.media.session.PlaybackState.ACTION_PLAY_FROM_MEDIA_ID |
                android.media.session.PlaybackState.ACTION_PLAY_FROM_SEARCH;

        if (mPlaylist == null || mPlaylist.size() <= 0) {
            return actions;
        }
        if (mState == android.media.session.PlaybackState.STATE_PLAYING) {
            actions |= android.media.session.PlaybackState.ACTION_PAUSE;
        }
        if (playPosition > 0) {
            actions |= android.media.session.PlaybackState.ACTION_SKIP_TO_PREVIOUS;
        }
        if ( playPosition < mPlaylist.size() - 1) {
            actions |= android.media.session.PlaybackState.ACTION_SKIP_TO_NEXT;
        }
        return actions;
    }

    /**
     * Called to get the AmbientTrack bitmap and update the Now Playing Card's meta data
     * on AndroidTV. The AmbientService uses the Picasso Library
     * by Square to get the album image from memory, cache or internet.
     * @see "http://square.github.io/picasso/"
     */
    private void metadataBitmapHelper () {

        if(mAmbientTrack == null)
        {
            Log.e(AmbientService.TAG,": AmbientTrack is null. Cannot create  now playing card");
            return;
        }


        final Drawable placeholderDrawable = getResources().getDrawable(R.drawable.unknown_album);

        try
        {
            Picasso.with(this)
                    .load(mAmbientTrack.getAlbumImageUri())
                    .placeholder(placeholderDrawable)
                    .error(placeholderDrawable)
                    .into(new Target() {
                        @Override
                        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom loadedFrom) {

                            if (bitmap == null) {
                                bitmap = ((BitmapDrawable) placeholderDrawable).getBitmap();
                            }

                            updateMetadata(bitmap);

                        }

                        @Override
                        public void onBitmapFailed(Drawable drawable) {

                            if(drawable == null)
                            {
                                drawable = placeholderDrawable;
                            }

                            updateMetadata(((BitmapDrawable) drawable).getBitmap());
                        }

                        @Override
                        public void onPrepareLoad(Drawable drawable) {
                            if(drawable == null)
                            {
                                drawable = placeholderDrawable;
                            }

                            updateMetadata(((BitmapDrawable) drawable).getBitmap());
                        }
                    });

        }catch (Exception e)
        {
            e.printStackTrace();
            Log.e(AmbientService.TAG,e.getMessage());

            updateMetadata(((BitmapDrawable) placeholderDrawable).getBitmap());
        }

    }

    /**
     * Method used to update the meta data of the Now Playing Card with data from the current
     * playing Ambient Track
     * @param bitmap bitmap downloaded from memory, cache or internet async
     */
    private void updateMetadata (Bitmap bitmap)
    {
        MediaMetadata.Builder metadataBuilder = new MediaMetadata.Builder();

        // To provide most control over how an item is displayed set the
        // display fields in the metadata
        metadataBuilder.putString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE,mAmbientTrack.getName());
        metadataBuilder.putString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE,mAmbientTrack.getAlbumName());

        metadataBuilder.putString(MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI,mAmbientTrack.getAlbumImageUri().toString());

        // And at minimum the title and artist for legacy support
        metadataBuilder.putString(MediaMetadata.METADATA_KEY_TITLE,mAmbientTrack.getName());
        metadataBuilder.putString(MediaMetadata.METADATA_KEY_ARTIST,mAmbientTrack.getArtistName());

        // A small bitmap for the artwork is also recommended
        metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ART,bitmap);

        if(mSession != null)
        {
            // Add any other fields you have for your data as well
            mSession.setMetadata(metadataBuilder.build());

        }

        if (mSession != null && !mSession.isActive()) {
            mSession.setActive(true);

        }

    }

    /**
     * Receives media buttons, transport controls, and commands from controllers and the system.
     * A callback may be set using setCallback(MediaSession.Callback).
     */
    private class MediaSessionCallback extends MediaSession.Callback
    {

        /**
         * constructor
         */
        public MediaSessionCallback() {
            super();
        }

        /**
         * Called when a controller has sent a command to this session. The owner of the session may handle custom commands but is not required to.
         * @param command The command name.
         * @param args Optional parameters for the command, may be null.
         * @param cb A result receiver to which a result may be sent by the command, may be null.
         */
        @Override
        public void onCommand(String command, Bundle args, ResultReceiver cb) {
            super.onCommand(command, args, cb);

        }



        /**
         * Called when a media button is pressed and this session has the highest priority or a
         * controller sends a media button event to the session. The default behavior will call the
         * relevant method if the action for it was set.
         * @param mediaButtonIntent an intent containing the KeyEvent as an extra
         * @return True if the event was handled, false otherwise.
         */
        @Override
        public boolean onMediaButtonEvent(Intent mediaButtonIntent) {

            if (mSession != null
                    && Intent.ACTION_MEDIA_BUTTON.equals(mediaButtonIntent.getAction())) {
                KeyEvent ke = mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                if (ke != null && ke.getAction() == KeyEvent.ACTION_DOWN) {
                    PlaybackState state = getPlaybackState();
                    long validActions = state == null ? 0 : state.getActions();
                    switch (ke.getKeyCode()) {
                        case KeyEvent.KEYCODE_MEDIA_PLAY:
                            if ((validActions & PlaybackState.ACTION_PLAY) != 0) {
                                onPlay();
                                return true;
                            }
                            break;
                        case KeyEvent.KEYCODE_MEDIA_PAUSE:
                            if ((validActions & PlaybackState.ACTION_PAUSE) != 0) {
                                onPause();
                                return true;
                            }
                            break;
                        case KeyEvent.KEYCODE_MEDIA_NEXT:
                            if ((validActions & PlaybackState.ACTION_SKIP_TO_NEXT) != 0) {
                                onSkipToNext();
                                return true;
                            }
                            break;
                        case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                            if ((validActions & PlaybackState.ACTION_SKIP_TO_PREVIOUS) != 0) {
                                onSkipToPrevious();
                                return true;
                            }
                            break;
                        case KeyEvent.KEYCODE_MEDIA_STOP:
                            if ((validActions & PlaybackState.ACTION_STOP) != 0) {
                                onStop();
                                return true;
                            }
                            break;
                        case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                            if ((validActions & PlaybackState.ACTION_FAST_FORWARD) != 0) {
                                onFastForward();
                                return true;
                            }
                            break;
                        case KeyEvent.KEYCODE_MEDIA_REWIND:
                            if ((validActions & PlaybackState.ACTION_REWIND) != 0) {
                                onRewind();
                                return true;
                            }
                            break;
                        case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                        case KeyEvent.KEYCODE_HEADSETHOOK:
                            boolean isPlaying = state == null ? false
                                    : state.getState() == PlaybackState.STATE_PLAYING;
                            boolean canPlay = (validActions & (PlaybackState.ACTION_PLAY_PAUSE
                                    | PlaybackState.ACTION_PLAY)) != 0;
                            boolean canPause = (validActions & (PlaybackState.ACTION_PLAY_PAUSE
                                    | PlaybackState.ACTION_PAUSE)) != 0;
                            if (isPlaying && canPause) {
                                onPause();
                                return true;
                            } else if (!isPlaying && canPlay) {
                                onPlay();
                                return true;
                            }
                            break;
                    }
                }
            }
            return false;
        }

        /**
         * Override to handle requests to begin playback.
         */
        @Override
        public void onPlay() {
            super.onPlay();
            play();
        }

        /**
         * Override to handle requests to play a specific mediaId that was provided by
         * your app's MediaBrowserService.
         * @param mediaId media id value
         * @param extras extra information
         */
        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            super.onPlayFromMediaId(mediaId, extras);
            //not implemented
        }

        /**
         * Override to handle requests to begin playback from a search query. An empty query
         * indicates that the app may play any music. The implementation should attempt to make a
         * smart choice about what to play.
         * @param query query string
         * @param extras extra information
         */
        @Override
        public void onPlayFromSearch(String query, Bundle extras) {
            super.onPlayFromSearch(query, extras);
            //not implemented
        }

        /**
         * Override to handle requests to play an item with a given id from the play queue.
         * @param id item id
         */
        @Override
        public void onSkipToQueueItem(long id) {
            super.onSkipToQueueItem(id);
            //not implemented
        }

        /**
         * Override to handle requests to pause playback.
         */
        @Override
        public void onPause() {
            super.onPause();
            pause();
        }

        /**
         * Override to handle requests to skip to the next media item.
         */
        @Override
        public void onSkipToNext() {
            super.onSkipToNext();
            playNext();
        }

        /**
         * Override to handle requests to skip to the previous media item.
         */
        @Override
        public void onSkipToPrevious() {
            super.onSkipToPrevious();
            playPrevious();
        }

        /**
         * Override to handle requests to fast forward.
         */
        @Override
        public void onFastForward() {
            super.onFastForward();
            //not implemented
        }

        /**
         * Override to handle requests to rewind.
         */
        @Override
        public void onRewind() {
            super.onRewind();
            //not implemented
        }

        /**
         * Override to handle requests to stop playback.
         */
        @Override
        public void onStop() {
            super.onStop();
            stop();
        }

        /**
         * Override to handle requests to seek to a specific position in ms.
         * @param pos New position to move to, in milliseconds.\
         */
        @Override
        public void onSeekTo(long pos) {
            super.onSeekTo(pos);
            seekTo((int)pos);
        }

        /**
         * Override to handle the item being rated.
         * @param rating rating value
         */
        @Override
        public void onSetRating(Rating rating) {
            super.onSetRating(rating);
            //not implemented
        }

        /**
         * Called when a MediaController wants a PlaybackState.CustomAction to be performed.
         * @param action The action that was originally sent in the PlaybackState.CustomAction.
         * @param extras Optional extras specified by the MediaController.
         */
        @Override
        public void onCustomAction(String action, Bundle extras) {
            super.onCustomAction(action, extras);
            //not implemented
        }
    }

}
