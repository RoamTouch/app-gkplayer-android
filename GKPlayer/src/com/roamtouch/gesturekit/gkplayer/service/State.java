package com.roamtouch.gesturekit.gkplayer.service;

public enum State {
	Retrieving, // the MediaRetriever is retrieving music
	Stopped,    // media player is stopped and not prepared to play
	Preparing,  // media player is preparing...
	Playing,    // playback active (media player ready!). (but the media player may actually be
	// paused in this state if we don't have audio focus. But we stay in this state
	// so that we know we have to resume playback once we get focus back)
	Paused      // playback paused (media player ready!)
};