/*   
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.roamtouch.gkplayer;

import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.gesturekit.gesturekithelper.GestureKitHelper;
import com.gesturekit.gesturekithelper.HelpAction;
import com.roamtouch.gesturekit.GestureKit;
import com.roamtouch.gesturekit.GestureKit.GestureKitListener;
import com.roamtouch.gkplayer.graphics.VolumeControl;
import com.roamtouch.gkplayer.service.MusicBinder;
import com.roamtouch.gkplayer.service.MusicService;

/** 
 * Main activity: shows media player buttons. This activity shows the media player buttons and
 * lets the user click them. No media handling is done here -- everything is done by passing
 * Intents to our {@link MusicService}.
 * */
public class GKPlayer extends Activity implements OnClickListener {
	/**
	 * The URL we suggest as default when adding by URL. This is just so that the user doesn't
	 * have to find an URL to test this sample.
	 */
	final String SUGGESTED_URL = "http://www.vorbis.com/music/Epoq-Lepidoptera.ogg";

	protected MusicBinder musicBinder = null;
	protected ServiceConnection svcConn=new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder rawBinder) {
			musicBinder=(MusicBinder)rawBinder;
			musicBinder.addClient(player);
		}

		public void onServiceDisconnected(ComponentName className) {
			musicBinder=null;
		}
	};

	Button mPlayButton;
	Button mPauseButton;
	Button mSkipButton;
	Button mRewindButton;
	Button mStopButton;
	Button mEjectButton;

	GestureKit gesturekit;	
	Activity act;	

	private PlayerLayout player;
	private VolumeControl volume;
	private View infoButton;
	private TextView info;	
	
	public GKPlayer(){
		
	}

	/**
	 * Called when the activity is first created. Here, we simply set the event listeners and
	 * start the background service ({@link MusicService}) that will handle the actual media
	 * playback.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.main); 	
		
		this.gesturekit = new GestureKit(this, "988786d4-56dd-45d6-a558-1245954d815c"); //, (ViewGroup)findViewById(R.id.gesturekit));		
		
		this.gesturekit.setPlugin(new GestureKitHelper(this, this.gesturekit));		
		
		player = (PlayerLayout)findViewById(R.id.player);
		volume = player.getVolume();
		
		infoButton = findViewById(R.id.btn_info);
		info = (TextView) findViewById(R.id.info);
		
		infoButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				info.setText(Html.fromHtml(getString(R.string.info)));
				info.setMovementMethod(new LinkMovementMethod());
				info.setVisibility(View.VISIBLE);
			}
		});
		
		info.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				info.setVisibility(View.GONE);
			}
		});

		mPlayButton = (Button) findViewById(R.id.playbutton);
		mPauseButton = (Button) findViewById(R.id.pausebutton);
		mSkipButton = (Button) findViewById(R.id.skipbutton);
		mRewindButton = (Button) findViewById(R.id.rewindbutton);
		mStopButton = (Button) findViewById(R.id.stopbutton);
		mEjectButton = (Button) findViewById(R.id.ejectbutton);        

		mPlayButton.setOnClickListener(this);
		mPauseButton.setOnClickListener(this);
		mSkipButton.setOnClickListener(this);
		mRewindButton.setOnClickListener(this);
		mStopButton.setOnClickListener(this);
		mEjectButton.setOnClickListener(this);

		setVolumeControlStream(AudioManager.STREAM_MUSIC);
	}

	@Override
	public void onResume(){
		super.onResume();
		//gesturekit.onResume();
		
	}

	@Override
	public void onPause(){
		super.onPause();
		//gesturekit.onPause();
		
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

		switch (keyCode) {
		case KeyEvent.KEYCODE_VOLUME_UP:
			volume.adjust(AudioManager.ADJUST_RAISE);
			return true;
		case KeyEvent.KEYCODE_VOLUME_DOWN:
			volume.adjust(AudioManager.ADJUST_LOWER);
			return true;
		default:
			return super.onKeyDown(keyCode, event);
		}
	}
	
	@Override
	public void onBackPressed() {
		super.onBackPressed();
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		
		if (musicBinder == null)
			getApplicationContext().bindService(new Intent(this.getApplicationContext(), MusicService.class), svcConn, BIND_AUTO_CREATE);
		else
			musicBinder.addClient(player);
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		Log.e("GKPlayer", "onStop");
		startService(new Intent(MusicService.ACTION_PAUSE));
		if(musicBinder != null)
			musicBinder.removeClient(player);
	}

	// PONER ACA METODOS COMO EN BACK-END. 

	public void PLAY (){
		startService(new Intent(MusicService.ACTION_PLAY));   	
	}

	public void PLAY (String url){
		startService(new Intent(MusicService.ACTION_PLAY));   	
	}    

	public void PAUSE (){
		startService(new Intent(MusicService.ACTION_PAUSE));    	
	}

	public void PAUSE (String url){
		startService(new Intent(MusicService.ACTION_PAUSE));    	
	}

	public void STOP (){
		startService(new Intent(MusicService.ACTION_STOP));
	}

	public void FORWARD (String data){
		startService(new Intent(MusicService.ACTION_SKIP));
	}


	public void BACKWARD (String data){
		startService(new Intent(MusicService.ACTION_REWIND));
	}


	public void SAVE (){
		Log.v("","SAVE");
	}

	public void SHARE (){
		Log.v("","SAHRE");
	}

	public void SEND (){
		Log.v("","SEND");
	}

	public void SORT (){
		Log.v("","SORT");
	}    

	@Override
	public void onClick(View target) {
		// Send the correct intent to the MusicService, according to the button that was clicked
		if (target == mPlayButton)
			startService(new Intent(MusicService.ACTION_PLAY));
		else if (target == mPauseButton)
			startService(new Intent(MusicService.ACTION_PAUSE));
		else if (target == mSkipButton)
			startService(new Intent(MusicService.ACTION_SKIP));
		else if (target == mRewindButton)
			startService(new Intent(MusicService.ACTION_REWIND));
		else if (target == mStopButton)
			startService(new Intent(MusicService.ACTION_STOP));
		else if (target == mEjectButton) {
			showUrlDialog();
		}
	}

	public void setStartService(String service){
		startService(new Intent(service));
	}

	/** 
	 * Shows an alert dialog where the user can input a URL. After showing the dialog, if the user
	 * confirms, sends the appropriate intent to the {@link MusicService} to cause that URL to be
	 * played.
	 */
	void showUrlDialog() {
		AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
		alertBuilder.setTitle("Manual Input");
		alertBuilder.setMessage("Enter a URL (must be http://)");
		final EditText input = new EditText(this);
		alertBuilder.setView(input);

		input.setText(SUGGESTED_URL);

		alertBuilder.setPositiveButton("Play!", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dlg, int whichButton) {
				// Send an intent with the URL of the song to play. This is expected by
				// MusicService.
				Intent i = new Intent(MusicService.ACTION_URL);
				Uri uri = Uri.parse(input.getText().toString());
				i.setData(uri);
				startService(i);
			}
		});
		alertBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dlg, int whichButton) {}
		});

		alertBuilder.show();
	}
}
