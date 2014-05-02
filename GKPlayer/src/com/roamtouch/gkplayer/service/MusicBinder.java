package com.roamtouch.gkplayer.service;

import java.io.IOException;
import java.util.HashSet;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Binder;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import com.roamtouch.gkplayer.GKPlayer;
import com.roamtouch.gkplayer.R;
import com.roamtouch.gkplayer.graphics.Utils;

public class MusicBinder extends Binder implements OnCompletionListener, OnPreparedListener, OnErrorListener, 
PrepareMusicRetrieverTask.MusicRetrieverPreparedListener, MusicFocusable{
	private final static String TAG = "RandomMusicPlayer";
	private Context mContext;
	private HashSet<ContentListener> clients = new HashSet<ContentListener>();
	// our media player
	private MediaPlayer mPlayer = null;
	private NotificationManager mNotificationManager;
	Notification mNotification = null;
	// The ID we use for the notification (the onscreen alert that appears at the notification
	// area at the top of the screen as an icon -- and as text as well if the user expands the
	// notification area).
	final int NOTIFICATION_ID = 1;
	
	// Our instance of our MusicRetriever, which handles scanning for media and
	// providing titles and URIs as we need.
	MusicRetriever mRetriever;

	State mState = State.Retrieving;
	Song mSong;

	AudioFocus mAudioFocus = AudioFocus.NoFocusNoDuck;
	// our AudioFocusHelper object, if it's available (it's available on SDK level >= 8)
	// If not available, this will be null. Always check for null before using!
	AudioFocusHelper mAudioFocusHelper = null;

	// whether the song we are playing is streaming from the network
	boolean mIsStreaming = false;

	// Wifi lock that we hold when streaming files from the internet, in order to prevent the
	// device from shutting off the Wifi radio
	WifiLock mWifiLock;

	// if in Retrieving mode, this flag indicates whether we should start playing immediately
	// when we are ready or not.
	boolean mStartPlayingAfterRetrieve = true;
	// if mStartPlayingAfterRetrieve is true, this variable indicates the URL that we should
	// start playing when we are ready. If null, we should play a random song from the device
	Uri mWhatToPlayAfterRetrieve = null;

	// The volume we set the media player to when we lose audio focus, but are allowed to reduce
	// the volume instead of stopping playback.
	public final float DUCK_VOLUME = 0.1f;
	
	private Bitmap background = null;
	private Bitmap cover = null;
	
	private Handler playerUpdateTimeHandler = new Handler();
	private Runnable playerUpdateTimeTask = new Runnable() {
		public void run() {
			if(clients.size() <= 0 || mPlayer == null){
				playerUpdateTimeHandler.postDelayed(this, 100);
				return;
			}
			long totalDuration = mPlayer.getDuration();
			long currentDuration = mPlayer.getCurrentPosition();
			playerUpdateTimeHandler.postDelayed(this, 100);
			for(ContentListener client : clients)
				client.onProgress(currentDuration, totalDuration);
		}
	};

	public void onCreate(Context context){
		mContext = context;
		/**
		 * Makes sure the media player exists and has been reset. This will create the media player
		 * if needed, or reset the existing media player if one already exists.
		 */
		mPlayer = new MediaPlayer();

		// Make sure the media player will acquire a wake-lock while playing. If we don't do
		// that, the CPU might go to sleep while the song is playing, causing playback to stop.
		//
		// Remember that to use this, we have to declare the android.permission.WAKE_LOCK
		// permission in AndroidManifest.xml.
		mPlayer.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK);

		// we want the media player to notify us when it's ready preparing, and when it's done
		// playing:
		mPlayer.setOnPreparedListener(this);
		mPlayer.setOnCompletionListener(this);
		mPlayer.setOnErrorListener(this);

		// Create the Wifi lock (this does not acquire the lock, this just creates it)
		mWifiLock = ((WifiManager) mContext.getSystemService(Context.WIFI_SERVICE)).createWifiLock(WifiManager.WIFI_MODE_FULL, "mylock");

		mNotificationManager = (NotificationManager) mContext.getSystemService(Service.NOTIFICATION_SERVICE);

		// Create the retriever and start an asynchronous task that will prepare it.
		mRetriever = new MusicRetriever(mContext.getContentResolver());
		(new PrepareMusicRetrieverTask(mRetriever,this)).execute();

		// create the Audio Focus Helper, if the Audio Focus feature is available (SDK 8 or above)
		if (android.os.Build.VERSION.SDK_INT >= 8)
			mAudioFocusHelper = new AudioFocusHelper(mContext, this);
		else
			mAudioFocus = AudioFocus.Focused; // no focus feature, so we always "have" audio focus
	}

	public void onDestroy(){
		// Service is being killed, so make sure we release our resources
		mState = State.Stopped;
		relaxResources();
		giveUpAudioFocus();
	}

	public void addClient(ContentListener listener){
		clients.add(listener);
		if(mSong != null)
			notifySong();
	}

	public void removeClient(ContentListener listener){
		clients.remove(listener);
	}

	/** Called when media player is done playing current song. */
	@Override
	public void onCompletion(MediaPlayer player) {
		playerUpdateTimeHandler.removeCallbacks(playerUpdateTimeTask);
		// The media player finished playing the current song, so we go ahead and start the next.
		playNextSong(null);
	}

	/** Called when media player is done preparing. */
	@Override
	public void onPrepared(MediaPlayer player) {
		// The media player is done preparing. That means we can start playing!
		if(mStartPlayingAfterRetrieve){
			mStartPlayingAfterRetrieve = false;
			processPauseRequest();
			return;
		}
		mState = State.Playing;
		updateNotification(mSong.getTitle() + " (playing)");
		configAndStartMediaPlayer();
	}

	/**
	 * Called when there's an error playing media. When this happens, the media player goes to
	 * the Error state. We warn the user about the error and reset the media player.
	 */
	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		for(ContentListener listener : clients)
			listener.onError("Media player error! Resetting.");
		Log.e(TAG, "Error: what=" + String.valueOf(what) + ", extra=" + String.valueOf(extra));

		mState = State.Stopped;
		relaxResources();
		giveUpAudioFocus();
		return true; // true indicates we handled the error
	}

	/**
	 * Releases resources used by the service for playback. This includes the "foreground service"
	 * status and notification, the wake locks and possibly the MediaPlayer.
	 *
	 * @param releaseMediaPlayer Indicates whether the Media Player should also be released or not
	 */
	void relaxResources() {
		// stop being a foreground service
		((Service)mContext).stopForeground(true);

		// we can also release the Wifi lock, if we're holding it
		if (mWifiLock.isHeld()) mWifiLock.release();
	}

	/**
	 * Starts playing the next song. If manualUrl is null, the next song will be randomly selected
	 * from our Media Retriever (that is, it will be a random song in the user's device). If
	 * manualUrl is non-null, then it specifies the URL or path to the song that will be played
	 * next.
	 */
	void playNextSong(String manualUrl) {
		mState = State.Stopped;
		relaxResources(); // release everything except MediaPlayer
		
		background = null;
		cover = null;

		try {
			if (manualUrl != null) {
				// set the source of the media player to a manual URL or path
				mPlayer.reset();
				mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
				mPlayer.setDataSource(manualUrl);
				mIsStreaming = manualUrl.startsWith("http:") || manualUrl.startsWith("https:");
			}
			else {
				mIsStreaming = false; // playing a locally available song

				mSong = mRetriever.getRandomItem();
				if (mSong == null) {
					updateNotification("No song to play :-(");
					return;
				}

				// set the source of the media player a a content URI
				mPlayer.reset();
				mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
				mPlayer.setDataSource(mContext, mSong.getURI());
				notifySong();
			}


			mState = State.Preparing;
			setUpAsForeground(mSong.getTitle() + " (loading)");

			// starts preparing the media player in the background. When it's done, it will call
			// our OnPreparedListener (that is, the onPrepared() method on this class, since we set
			// the listener to 'this').
			//
			// Until the media player is prepared, we *cannot* call start() on it!
			mPlayer.prepareAsync();

			// If we are streaming from the internet, we want to hold a Wifi lock, which prevents
			// the Wifi radio from going to sleep while the song is playing. If, on the other hand,
			// we are *not* streaming, we want to release the lock if we were holding it before.
			if (mIsStreaming) mWifiLock.acquire();
			else if (mWifiLock.isHeld()) mWifiLock.release();
		}
		catch (IOException ex) {
			Log.e("MusicService", "IOException playing next song: " + ex.getMessage());
			ex.printStackTrace();
		}
	}

	/** Updates the notification. */
	void updateNotification(String text) {
		PendingIntent pi = PendingIntent.getActivity(mContext, 0, new Intent(mContext, GKPlayer.class), PendingIntent.FLAG_UPDATE_CURRENT);
		mNotification.setLatestEventInfo(mContext, "RandomMusicPlayer", text, pi);
		mNotificationManager.notify(NOTIFICATION_ID, mNotification);
	}

	void giveUpAudioFocus() {
		if (mAudioFocus == AudioFocus.Focused && mAudioFocusHelper != null && mAudioFocusHelper.abandonFocus())
			mAudioFocus = AudioFocus.NoFocusNoDuck;
	}

	@Override
	public void onMusicRetrieverPrepared() {
		// Done retrieving!
		mState = State.Stopped;

		// If the flag indicates we should start playing after retrieving, let's do that now.
		if (mStartPlayingAfterRetrieve) {
			tryToGetAudioFocus();
			playNextSong(mWhatToPlayAfterRetrieve == null ?
					null : mWhatToPlayAfterRetrieve.toString());
			mState = State.Paused;
		}
	}

	void tryToGetAudioFocus() {
		if (mAudioFocus != AudioFocus.Focused && mAudioFocusHelper != null && mAudioFocusHelper.requestFocus())
			mAudioFocus = AudioFocus.Focused;
	}

	/**
	 * Configures service as a foreground service. A foreground service is a service that's doing
	 * something the user is actively aware of (such as playing music), and must appear to the
	 * user as a notification. That's why we create the notification here.
	 */
	void setUpAsForeground(String text) {
		PendingIntent pi = PendingIntent.getActivity(mContext, 0, new Intent(mContext, GKPlayer.class), PendingIntent.FLAG_UPDATE_CURRENT);
		mNotification = new Notification();
		mNotification.tickerText = text;
		mNotification.icon = R.drawable.ic_stat_playing;
		mNotification.flags |= Notification.FLAG_ONGOING_EVENT;
		mNotification.setLatestEventInfo(mContext, "RandomMusicPlayer", text, pi);
		((Service)mContext).startForeground(NOTIFICATION_ID, mNotification);
	}

	@Override
	public void onGainedAudioFocus() {
		Toast.makeText(mContext, "gained audio focus.", Toast.LENGTH_SHORT).show();
		mAudioFocus = AudioFocus.Focused;

		// restart media player with new focus settings
		if (mState == State.Playing)
			configAndStartMediaPlayer();
	}

	@Override
	public void onLostAudioFocus(boolean canDuck) {
		Toast.makeText(mContext, "lost audio focus." + (canDuck ? "can duck" : "no duck"), Toast.LENGTH_SHORT).show();
		mAudioFocus = canDuck ? AudioFocus.NoFocusCanDuck : AudioFocus.NoFocusNoDuck;

		// start/restart/pause media player with new focus settings
		if (mPlayer != null && mPlayer.isPlaying())
			configAndStartMediaPlayer();
	}

	/**
	 * Reconfigures MediaPlayer according to audio focus settings and starts/restarts it. This
	 * method starts/restarts the MediaPlayer respecting the current audio focus state. So if
	 * we have focus, it will play normally; if we don't have focus, it will either leave the
	 * MediaPlayer paused or set it to a low volume, depending on what is allowed by the
	 * current focus settings. This method assumes mPlayer != null, so if you are calling it,
	 * you have to do so from a context where you are sure this is the case.
	 */
	void configAndStartMediaPlayer() {
		if (mAudioFocus == AudioFocus.NoFocusNoDuck) {
			// If we don't have audio focus and can't duck, we have to pause, even if mState
			// is State.Playing. But we stay in the Playing state so that we know we have to resume
			// playback once we get the focus back.
			if (mPlayer.isPlaying()) mPlayer.pause();
			return;
		}
		else if (mAudioFocus == AudioFocus.NoFocusCanDuck)
			mPlayer.setVolume(DUCK_VOLUME, DUCK_VOLUME);  // we'll be relatively quiet
		else
			mPlayer.setVolume(1.0f, 1.0f); // we can be loud

		playerUpdateTimeHandler.removeCallbacks(playerUpdateTimeTask);
		playerUpdateTimeHandler.postDelayed(playerUpdateTimeTask, 100);
		if (!mPlayer.isPlaying()) mPlayer.start();
	}
	
	public void processPlayRequest() {
        if (mState == State.Retrieving) {
            // If we are still retrieving media, just set the flag to start playing when we're
            // ready
            mWhatToPlayAfterRetrieve = null; // play a random song
            mStartPlayingAfterRetrieve = true;
            return;
        }

        tryToGetAudioFocus();

        if (mState == State.Stopped) {
            // If we're stopped, just go ahead to the next song and start playing
            playNextSong(null);
        }
        else if (mState == State.Paused) {
            // If we're paused, just continue playback and restore the 'foreground service' state.
            mState = State.Playing;
            setUpAsForeground(mSong.getTitle() + " (playing)");
            configAndStartMediaPlayer();
        }
    }

	public void processPauseRequest() {
        if (mState == State.Retrieving) {
        	Log.d("Music Service", "state retrieving");
            // If we are still retrieving media, clear the flag that indicates we should start
            // playing when we're ready
            mStartPlayingAfterRetrieve = false;
            return;
        }

        if (mState == State.Playing) {
            // Pause media player and cancel the 'foreground service' state.
            mState = State.Paused;
            mPlayer.pause();
            relaxResources();
            giveUpAudioFocus();
        }
    }

	public void processRewindRequest() {
        if (mState == State.Playing || mState == State.Paused)
            mPlayer.seekTo(0);
    }

	public void processSkipRequest() {
        if (mState == State.Playing || mState == State.Paused) {
            tryToGetAudioFocus();
            playNextSong(null);
        }
    }

	public void processStopRequest() {
        if (mState == State.Playing || mState == State.Paused) {
            mState = State.Stopped;

            // let go of all resources...
            relaxResources();
            giveUpAudioFocus();
        }
    }
	
	void processAddRequest(Intent intent) {
        // user wants to play a song directly by URL or path. The URL or path comes in the "data"
        // part of the Intent. This Intent is sent by {@link MainActivity} after the user
        // specifies the URL/path via an alert box.
        if (mState == State.Retrieving) {
            // we'll play the requested URL right after we finish retrieving
            mWhatToPlayAfterRetrieve = intent.getData();
            mStartPlayingAfterRetrieve = true;
        }
        else if (mState == State.Playing || mState == State.Paused || mState == State.Stopped) {
            Log.i(TAG, "Playing from URL/path: " + intent.getData().toString());
            tryToGetAudioFocus();
            playNextSong(intent.getData().toString());
        }
    }
	
	private void notifySong(){
		(new Thread(new Runnable() {
			@Override
			public void run() {
				if(background == null || cover == null){
					Bitmap source = Utils.getAlbumArt(mContext, mSong.getAlbumId());
					
					if(source == null){
						//background = Utils.applyBlur(Utils.getDefaultBackground());
						background = Utils.getDefaultBackground();
						cover = Utils.applyCircle(Utils.getDefaultCover());
					}else{
						//background = Utils.applyBlur(source);
						background = source;
						cover = Utils.applyCircle(source);
					}
				}
				
				for(ContentListener listener : clients)
					listener.setSongLayout(mSong, background, cover);				
				
			}
		})).start();
	}

}
