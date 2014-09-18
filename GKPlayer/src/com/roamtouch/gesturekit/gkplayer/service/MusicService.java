package com.roamtouch.gesturekit.gkplayer.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class MusicService extends Service{
	// These are the Intent actions that we are prepared to handle. Notice that the fact these
    // constants exist in our class is a mere convenience: what really defines the actions our
    // service can handle are the <action> tags in the <intent-filters> tag for our service in
    // AndroidManifest.xml.
    public static final String ACTION_PLAY = "com.example.android.musicplayer.action.PLAY";
    public static final String ACTION_PAUSE = "com.example.android.musicplayer.action.PAUSE";
    public static final String ACTION_STOP = "com.example.android.musicplayer.action.STOP";
    public static final String ACTION_SKIP = "com.example.android.musicplayer.action.SKIP";
    public static final String ACTION_REWIND = "com.example.android.musicplayer.action.REWIND";
    public static final String ACTION_URL = "com.example.android.musicplayer.action.URL";
    
	private MusicBinder binder;
		
	@Override
	public void onCreate() {
		super.onCreate();
		binder = new MusicBinder();
		binder.onCreate(this);
	}

	/**
     * Called when we receive an Intent. When we receive an intent sent to us via startService(),
     * this is the method that gets called. So here we react appropriately depending on the
     * Intent's action, which specifies what is being requested of us.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        Log.i("MusicService", action);
        if (action.equals(ACTION_PLAY)) binder.processPlayRequest();
        else if (action.equals(ACTION_PAUSE)) binder.processPauseRequest();
        else if (action.equals(ACTION_SKIP)) binder.processSkipRequest();
        else if (action.equals(ACTION_STOP)) binder.processStopRequest();
        else if (action.equals(ACTION_REWIND)) binder.processRewindRequest();
        else if (action.equals(ACTION_URL)) binder.processAddRequest(intent);

        return START_NOT_STICKY; // Means we started the service, but don't want it to
                                 // restart in case it's killed.
    }
	
	@Override
	public IBinder onBind(Intent intent) {
		return(binder);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		binder.onDestroy();		
	}
}
