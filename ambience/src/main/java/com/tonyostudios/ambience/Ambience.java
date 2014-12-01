package com.tonyostudios.ambience;

import android.app.UiModeManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Parcelable;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Ambience is a Broadcast Receiver
 * registered and used by an Android component (Activity,Fragment or Service) to
 * send and receive update playback control status and information
 * to and from the AmbientService.
 * @author TonyoStudios.com. Created on 11/18/2014. Updated on 12/01/2014.
 * @version 1.3
 */
public class Ambience extends BroadcastReceiver {

    /**
     * Tag used to identify the  Ambience class
     */
    private static final String TAG = "com.tonyostudios.ambience.Ambience";

    /**
     * Tag used to identify the  Ambience Broadcaster
     */
    public static final String AMBIENCE_BROADCASTER = TAG + ".BROADCASTER";

    /**
     * Holds instance of Ambience
     */
    private static Ambience mAmbience;

    /**
     * Holds context object
     */
    private static Context mContext;

    /**
     * Holds AmbientListener callback
     */
    private static AmbientListener  mCallback;

    /**
     * Boolean value used to indicate if Ambience is
     * listening for updates from the AmbientService
     */
    private static boolean isListeningForUpdates = false;

    /**
     * Boolean value used to indicate if the AmbientService
     * is started or has stopped
     */
    private static boolean isAmbientServiceStarted = false;

    /**
     * private constructor
     */
    private Ambience()
    {
        super();
    }

    /**
     * Method used to initialize the Ambience. This method starts the AmbientService
     * if it is not alreadyStarted.
     * @param context A context object
     * @return An Ambience object
     */
    public static Ambience turnOn(Context context)
    {
        if(context == null)
        {
            throw new NullPointerException(TAG + " context cannot be null");
        }

        mContext = null;
        mContext = context;

        if(mAmbience == null)
        {
            mAmbience = new Ambience();
        }

        mAmbience.startAmbientService();

        return mAmbience;
    }

    /**
     * Method used to access the single Instance of Ambience
     * @return An Ambience object
     */
    public static Ambience activeInstance()
    {
        if(mAmbience == null)
        {
            throw new NullPointerException(TAG + " - No Active Instance of Ambience. Did you forget " +
                    "to call turnOn?");
        }

        return mAmbience;
    }

    /**
     * Method used to register Ambience with the Broadcast Manager
     * @param callback An Android component that will handle all playback
     *                  control callbacks from the AmbientService. The Android
     *                  component can be an Activity, Fragment or Service. Only
     *                  a single Android component can receive updates from the
     *                  AmbientService.
     *
     *@return An Ambience object
     */
    public Ambience listenForUpdatesWith(AmbientListener callback)
    {
        if(callback == null)
        {
            throw new NullPointerException(TAG + ": callback cannot be null");
        }

        // If callback object has changed, replace it with new callback
        if(callback != mCallback)
        {
            mCallback = null;
            mCallback = callback;
        }

        if(!isListeningForUpdates() && mContext != null && mAmbience != null)
        {
            IntentFilter filter = new IntentFilter(AMBIENCE_BROADCASTER);
            mContext.registerReceiver(mAmbience,filter);

            isListeningForUpdates = true;
        }

        return mAmbience;
    }

    /**
     * Method used to unregister Ambience  with the Broadcast Manager. The Callback Listener
     * will no longer receive updates.
     * @return Instance of Ambient Service Controller
     */
    public static Ambience stopListeningForUpdates()
    {
        if(isListeningForUpdates() && mContext != null && mAmbience != null)
        {
            mContext.unregisterReceiver(mAmbience);
            isListeningForUpdates = false;
        }

        return mAmbience;
    }

    /**
     * Method used to check if Ambience is listening for updates from the AmbientService
     * @return A boolean value indicating if Ambience is listening for updates from AmbientService
     */
    public static boolean isListeningForUpdates()
    {
        return isListeningForUpdates;
    }

    /**
     * Method used to check if the AmbientService has started or stopped
     * @return boolean value indicating if the AmbientService has started or stopped
     */
    public static boolean hasAmbientServiceStarted()
    {
        return isAmbientServiceStarted;
    }

    /**
     * Method used to start the AmbientService
     */
    private void startAmbientService()
    {
        if(!hasAmbientServiceStarted())
        {
            if(isAndroidTvOrCar())
            {
                mContext.startService(new Intent(mContext, AmbientMediaBrowserService.class));

                return;
            }


            mContext.startService(new Intent(mContext, AmbientService.class));
        }
    }

    /**
     * Method used to stop the AmbientService
     */
    private static void stopAmbientService()
    {
        if(hasAmbientServiceStarted())
        {
            if(isAndroidTvOrCar())
            {
                mContext.stopService(new Intent(mContext, AmbientMediaBrowserService.class));

                return;
            }

            mContext.stopService(new Intent(mContext, AmbientService.class));
        }
    }


    /**
     * Method used to release all resources held by Ambience.
     * This method un-registers Ambience and stops the AmbientService. This is a destructive method.
     * All media playback, updates and notifications will be cancelled.
     */
    public static void turnOff()
    {
        //unregister receiver & stop Ambient Service
        stopAmbientService();
        stopListeningForUpdates();

        mContext = null;
        mAmbience = null;
        mCallback = null;
        isListeningForUpdates = false;
        isAmbientServiceStarted = false;
    }

    /**
     * Sends a volume request to the AmbientService
     * @param volume The amount to increase or decrease the audio player's volume
     * @return An Ambience object
     */
    public Ambience setVolumeTo(float volume)
    {
        if(volume < 0.0f || volume > 1.0f)
        {
            volume = 0.5f;
        }

        Intent intent = getAmbientServiceIntentInstance();
        intent.putExtra(AmbientService.VOLUME_LEVEL, volume);
        sendIntentToAmbientService(intent);

        return mAmbience;
    }


    /**
     * Sets the Ambience Playlist
     * @param playlist Ambient Track Playlist Array
     * @return An Ambience object
     */
    public Ambience setPlaylistTo(AmbientTrack[] playlist)
    {
        if(playlist == null)
        {
            return mAmbience;
        }

        sendPlaylistToAmbientService(new ArrayList<Parcelable>(Arrays.asList(playlist)));

        return mAmbience;
    }

    /**
     * Sets the Ambience Playlist
     * @param playlist Ambient Track Playlist ArrayList
     * @return An Ambience object
     */
    public Ambience setPlaylistTo(ArrayList<AmbientTrack> playlist)
    {
        if(playlist == null)
        {
            return mAmbience;
        }

        ArrayList<Parcelable> list = new ArrayList<Parcelable>();

        for(int x = 0; x < playlist.size(); x++)
        {
            AmbientTrack track = playlist.get(x);
            if(track!= null)
            {
                list.add(track);
            }
        }

        sendPlaylistToAmbientService(list);

        return mAmbience;
    }

    /**
     * Method used to set the Ambience Playlist and shuffle it
     * @param playlist Ambient Track Playlist ArrayList
     * @return An Ambience object
     */
    public Ambience shuffleAndSetPlaylistTo(ArrayList<AmbientTrack> playlist)
    {
        shufflePlaylist();
        setPlaylistTo(playlist);

        return mAmbience;
    }

    /**
     * Method used to set the Ambience Playlist and shuffle it
     * @param playlist Ambient Track Playlist Array
     * @return An Ambience object
     */
    public Ambience shuffleAndSetPlaylistTo(AmbientTrack[] playlist)
    {
        shufflePlaylist();
        setPlaylistTo(playlist);

        return mAmbience;
    }

    /**
     * Method used to send the AmbientTrack playlist to the AmbientSercice
     * @param playlist Ambient Track Playlist Parcelable ArrayList
     */
    private void sendPlaylistToAmbientService(ArrayList<Parcelable> playlist)
    {
        for(int x = 0; x < playlist.size(); x++)
        {
            if(playlist.get(x) == null)
            {
                throw new NullPointerException(TAG + ": AmbientTrack at position" + x + " cannot be null.");
            }
        }

        Intent intent = getAmbientServiceIntentInstance();
        intent.putParcelableArrayListExtra(AmbientService.PLAYLIST,playlist);
        sendIntentToAmbientService(intent);
    }

    /**
     * Sends a shuffle request to the AmbientService
     * @return An Ambience object
     */
    public Ambience shufflePlaylist()
    {
        Intent intent = getAmbientServiceIntentInstance();
        intent.putExtra(AmbientService.SHUFFLE_MODE,AmbientService.ShuffleMode.ON);
        sendIntentToAmbientService(intent);

        return mAmbience;
    }

    /**
     * Sends a unShuffle request to the AmbientService
     * @return An Ambience object
     */
    public Ambience unShufflePlaylist()
    {
        Intent intent = getAmbientServiceIntentInstance();
        intent.putExtra(AmbientService.SHUFFLE_MODE,AmbientService.ShuffleMode.OFF);
        sendIntentToAmbientService(intent);

        return mAmbience;
    }

    /**
     * Adds a single track to the AmbientTrack playlist
     * @param track A AmbientTrack object
     * @return An Ambience object
     */
    public Ambience addTrackToPlaylist(AmbientTrack track)
    {
        if(track == null)
        {
            return mAmbience;
        }

        Intent intent = getAmbientServiceIntentInstance();
        intent.putExtra(AmbientService.ADD_TRACK,track);
        sendIntentToAmbientService(intent);

        return mAmbience;
    }


    /**
     * Removes a single track in the AmbientTrack playlist
     * @param track The AmbientTrack object to remove
     * @return An Ambience object
     */
    public Ambience removeTrackFromPlaylist(AmbientTrack track)
    {
        if(track == null)
        {
            return mAmbience;
        }

        Intent intent = getAmbientServiceIntentInstance();
        intent.putExtra(AmbientService.REMOVE_TRACK,track);
        sendIntentToAmbientService(intent);

        return mAmbience;
    }

    /**
     * Sends a request to repeat a single track to the AmbientService
     * @return An Ambience object
     */
    public Ambience repeatASingleTrack()
    {
        Intent intent = getAmbientServiceIntentInstance();
        intent.putExtra(AmbientService.REPEAT_MODE,AmbientService.RepeatMode.REPEAT_ONE);
        sendIntentToAmbientService(intent);

        return mAmbience;
    }


    /**
     * Sends a request to repeat the playlist to the AmbientService
     * @return An Ambience object
     */
    public Ambience repeatAllTracks()
    {
        Intent intent = getAmbientServiceIntentInstance();
        intent.putExtra(AmbientService.REPEAT_MODE,AmbientService.RepeatMode.REPEAT_ALL);
        sendIntentToAmbientService(intent);

        return mAmbience;
    }

    /**
     * Sends a request to turn off repeat to the AmbientService
     * @return An Ambience object
     */
    public Ambience turnRepeatOff()
    {
        Intent intent = getAmbientServiceIntentInstance();
        intent.putExtra(AmbientService.REPEAT_MODE,AmbientService.RepeatMode.OFF);
        sendIntentToAmbientService(intent);

        return mAmbience;
    }


    /**
     * Method used to append a launch activity request to the Ambience notification
     * @param intentFilterAction intent Filter Action Name to launch a specific activity. This should
     *                           be a unique identifier for your activity
     * @return Instance of Ambience
     */
    public Ambience setNotificationLaunchActivity(String intentFilterAction)
    {
        if(intentFilterAction == null)
        {
            return mAmbience;
        }

        Intent intent = getAmbientServiceIntentInstance();
        intent.putExtra(AmbientService.ACTIVITY_LAUNCHER,intentFilterAction);
        sendIntentToAmbientService(intent);

        return mAmbience;
    }

    /**
     * Method used to send a seek request to the AmbientService
     * @param progress Seek progress value
     * @return Instance of Ambience
     */
    public Ambience seekTo(int progress)
    {
        Intent intent = getAmbientServiceIntentInstance();
        intent.putExtra(AmbientService.SEEK_POSITION,progress);
        sendIntentToAmbientService(intent);

        return mAmbience;
    }


    /**
     * Helper method used to create a new Intent object
     * used to send messages to the AmbientService
     * @return A new Intent object used to send messages to the AmbientService
     */
    private Intent getAmbientServiceIntentInstance()
    {
        return new Intent(AmbientService.AMBIENT_SERVICE_BROADCASTER);
    }

    /**
     * Helper method used to send an intent to AmbientService
     * @param intent Intent Object
     */
    private void sendIntentToAmbientService(Intent intent)
    {
        if(mContext != null && intent != null)
        {
            mContext.sendBroadcast(intent);
        }
    }

    /**
     * Method used to play a track at a certain position
     * in the playlist.
     * @param position Track position
     */
    public void PlayFromPosition(int position)
    {
        Intent intent = getAmbientServiceIntentInstance();
        intent.putExtra(AmbientService.PLAY_POSITION,position);
        sendIntentToAmbientService(intent);

        play();
    }

    /**
     * Method used to play a track
     */
    public void play()
    {
        Intent intent = getAmbientServiceIntentInstance();
        intent.putExtra(AmbientService.PLAYBACK_STATE, AmbientService.PlaybackState.PLAY);
        sendIntentToAmbientService(intent);
    }

    /**
     * Method used to stop a current playing track
     */
    public void stop()
    {
        Intent intent = getAmbientServiceIntentInstance();
        intent.putExtra(AmbientService.PLAYBACK_STATE,AmbientService.PlaybackState.STOP);
        sendIntentToAmbientService(intent);
    }

    /**
     * Method used to resume the current track
     */
    public void resume()
    {
        Intent intent = getAmbientServiceIntentInstance();
        intent.putExtra(AmbientService.PLAYBACK_STATE,AmbientService.PlaybackState.RESUME);
        sendIntentToAmbientService(intent);
    }

    /**
     * Method used to skip the current track
     */
    public void skip()
    {
        Intent intent = getAmbientServiceIntentInstance();
        intent.putExtra(AmbientService.PLAYBACK_STATE,AmbientService.PlaybackState.SKIP);
        sendIntentToAmbientService(intent);
    }

    /**
     * Method used to play the previous track
     */
    public void previous()
    {
        Intent intent = getAmbientServiceIntentInstance();
        intent.putExtra(AmbientService.PLAYBACK_STATE,AmbientService.PlaybackState.PREVIOUS);
        sendIntentToAmbientService(intent);
    }

    /**
     * Method used to pause the current track
     */
    public void pause()
    {
        Intent intent = getAmbientServiceIntentInstance();
        intent.putExtra(AmbientService.PLAYBACK_STATE,AmbientService.PlaybackState.PAUSE);
        sendIntentToAmbientService(intent);
    }


    /**
     * This method receives playback control updates from the AmbientService.
     * @param context A context object
     * @param intent An intent object
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent == null || intent.getExtras() == null)
        {
            Log.e(TAG, "Passed an empty or null intent to Ambience");
            return;
        }

        if(intent.getSerializableExtra(AmbientService.PLAYBACK_STATE) == null)
        {
            Log.e(TAG, "Intent does not contain a known playback state ");
            return;
        }

        if(mCallback == null)
        {
            Log.e(TAG, ": The callback(AmbientListener) object is null.");
            return;
        }

        try
        {
            AmbientService.PlaybackState state =
                    (AmbientService.PlaybackState) intent.getSerializableExtra(AmbientService.PLAYBACK_STATE);

            // Alert the callback with the currentPlayback State.
            switch (state)
            {
                case PLAY: mCallback.ambienceTrackIsPlaying();
                    break;
                case STOP: mCallback.ambienceTrackHasStopped();
                    break;
                case PAUSE: mCallback.ambienceTrackIsPaused();
                    break;
                case PREPPING_TRACK: mCallback.ambienceIsPreppingTrack();
                    break;
                case END_OF_PLAYLIST: mCallback.ambiencePlaylistCompleted();
                    break;
                case CURRENT_PLAYING_TRACK_INFO:
                {
                    if(intent.hasExtra(AmbientService.TRACK_PROGRESS))
                    {
                        mCallback.ambienceTrackCurrentProgress(intent.getIntExtra(AmbientService.TRACK_PROGRESS,0));
                    }

                    if(intent.hasExtra(AmbientService.TRACK_DURATION))
                    {
                        mCallback.ambienceTrackDuration(intent.getIntExtra(AmbientService.TRACK_DURATION,0));
                    }

                    if(intent.hasExtra(AmbientService.CURRENT_TRACK))
                    {
                        mCallback.ambiencePlayingTrack((AmbientTrack)intent.getParcelableExtra(AmbientService.CURRENT_TRACK));
                    }

                    break;
                }
                /*
                 * No specific error message is passed. The callback must assume that the track
                 * could not be played and attempt to retry or get feedback from the user.
                 */
                case ERROR: mCallback.ambienceErrorOccurred();
                    break;

                case SERVICE_STARTED:
                {
                    mCallback.ambienceServiceStarted(activeInstance());
                    isAmbientServiceStarted = true;
                    break;
                }
                case SERVICE_STOPPED:
                {
                    mCallback.ambienceServiceStopped(activeInstance());
                    isAmbientServiceStarted = false;
                    break;
                }

                default:
                    throw new IllegalStateException(TAG + ": Unknown Playback State");
            }
        }catch (Exception e)
        {
            e.printStackTrace();
            Log.e(TAG,e.getMessage());
        }
    }

    /**
     * Called to check if the application is running on an androidTV or androidAuto. This method is used to launch
     * the appropriate android service specific for TV or Auto.
     * @return boolean value that indicates if an application is running on
     * a TV or Auto.
     */
    private static boolean isAndroidTvOrCar()
    {
        if(mContext != null)
        {
            UiModeManager uiModeManager = (UiModeManager) mContext.getSystemService(Context.UI_MODE_SERVICE);

            if (uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION
                    || uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_CAR ) {

                return true;
            }

        }

        return false;
    }


    /**
     * Interface Listener that must be implemented by An Android Component such as an Activity, Fragment or Service
     * to receive updates from Ambience. Some methods in this interface are called
     * more frequently than others.
     */
    public static abstract interface AmbientListener {

        /**
         * Method used to update an Android Component when the track is being prep for play.
         */
        public void ambienceIsPreppingTrack();

        /**
         * Method used to update an Android Component with the current playing
         * track's duration time. This method is called often.
         * @param time Track duration time.
         */
        public void ambienceTrackDuration(int time);

        /**
         * Method used to update an Android Component with
         * the current playing track. This method is called often
         * @param track Current playing track
         */
        public void ambiencePlayingTrack(AmbientTrack track);

        /**
         * Method used to update an Android Component with
         * current progress time of the current playing track. This method is called often
         * @param time Current progress of the track
         */
        public void ambienceTrackCurrentProgress(int time);

        /**
         * Method used to alert an Android Component when a track begins playing
         */
        public void ambienceTrackIsPlaying();

        /**
         * Method used to alert an Android Component when a track is paused
         */
        public void ambienceTrackIsPaused();

        /**
         * Method used to alert an Android Component when a track has stopped
         */
        public void ambienceTrackHasStopped();

        /**
         *  Method used to update an Android Component when the AmbientService has completed playing
         *  a playlist.
         */
        public void ambiencePlaylistCompleted();

        /**
         *  Method used to update an Android Component when an error occurred in the AmbientService.
         *  No specific error message is passed. The Android Component must assume that the track
         *  could not be played. Use this method to retry playing the track or take another action.
         *  @see "Logcat for the specific error message from the AmbientService"
         */
        public void ambienceErrorOccurred();


        /**
         * Called when the AmbientService is started and waiting requests
         * @param activeInstance The active instance of ambience
         */
        public void ambienceServiceStarted(Ambience activeInstance);


        /**
         * Called when the AmbientService is stopped and is no longer listening for requests
         * @param activeInstance The active The active instance of ambience
         */
        public void ambienceServiceStopped(Ambience activeInstance);
    }
}
