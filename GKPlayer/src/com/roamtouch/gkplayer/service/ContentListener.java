package com.roamtouch.gkplayer.service;

import android.graphics.Bitmap;


public interface ContentListener {
	public void setSongLayout(Song song, Bitmap background, Bitmap cover);
	public void onProgress(long currentDuration, long totalDuration);
	public void onError(String error);
}
