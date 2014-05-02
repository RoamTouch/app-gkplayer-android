package com.roamtouch.gkplayer.graphics;

import java.io.IOException;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.net.Uri;
import android.provider.MediaStore;

public class Utils {
	public static Bitmap getAlbumArt(Context context, int albumId){
		Bitmap reply = null;
		try{
		Uri sArtworkUri = Uri.parse("content://media/external/audio/albumart");
		Uri uriCover = ContentUris.withAppendedId(sArtworkUri, albumId);
		ContentResolver res = context.getContentResolver();
		reply = MediaStore.Images.Media.getBitmap(res, uriCover);
		}catch(IOException e){
			e.printStackTrace();
		}		
		return reply;
	}
	
	public static Bitmap applyBlur(Bitmap source){
		Bitmap reply = source.copy(Bitmap.Config.ARGB_8888, true);
		Blur.functionToBlur(source, reply, 10);
		return reply;
	}
	
	public static Bitmap applyCircle(Bitmap source){
		int size = source.getWidth() > source.getHeight() ? source.getHeight() : source.getWidth();
		Bitmap reply = Bitmap.createBitmap(size, size, Config.ARGB_8888);
        Canvas canvas = new Canvas(reply);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, size, size);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawCircle(size / 2, size / 2, size / 2, paint);
        paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
        canvas.drawBitmap(source, rect, rect, paint);
        
        return reply;
	}
	
	public static Bitmap getDefaultCover(){
		Bitmap reply = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(reply);
		Paint paint = new Paint();
		paint.setColor(0xFFCCCCCC);
		paint.setStyle(Style.FILL);
		canvas.drawRect(0, 0, 300, 300, paint);
		return reply;
	}
	
	public static Bitmap getDefaultBackground(){
		Bitmap reply = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(reply);
		Paint paint = new Paint();
		paint.setColor(0xFF66CCFD);
		paint.setStyle(Style.FILL);
		canvas.drawRect(0, 0, 300, 300, paint);
		return reply;
	}
}
