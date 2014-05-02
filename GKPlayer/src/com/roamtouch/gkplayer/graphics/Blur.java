package com.roamtouch.gkplayer.graphics;

import android.graphics.Bitmap;

public class Blur {
	static{
		System.loadLibrary("Blur");
	}
	
	public native static void functionToBlur(Bitmap in, Bitmap out, int radius);
}
