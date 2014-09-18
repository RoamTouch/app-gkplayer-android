package com.roamtouch.gesturekit.gkplayer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.LabeledIntent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint.Align;
import android.graphics.Path;
import android.graphics.Path.Direction;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Build;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ImageView.ScaleType;

import com.roamtouch.gesturekit.gkplayer.graphics.ProgressView;
import com.roamtouch.gesturekit.gkplayer.graphics.VerticalTextView;
import com.roamtouch.gesturekit.gkplayer.graphics.VolumeControl;
import com.roamtouch.gesturekit.gkplayer.service.ContentListener;
import com.roamtouch.gesturekit.gkplayer.service.Song;

@SuppressLint("NewApi") public class PlayerLayout extends ViewGroup implements ContentListener{
	private static final String TAG = "Randon Music Player";
	private ImageView background;
	private ImageView cover;
	private ProgressView progress;
	private VolumeControl volume;
	private View buttons;
	private String text = "";
	private float defaultTextSize = 0;
	
	private float songInfoTextSize = 0;
	private float songAlbumTextSize = 0;
	
	private Path path = new Path();
	private TextPaint textPaint = new TextPaint();

	private TextView textSongAlbum = null;
	private TextView textSongInfo = null;


	private Rect volumeRect = new Rect();
	private Rect coverRect = new Rect();
	private Rect progressRect = new Rect();
	private Rect buttonsRect = new Rect();
	

	private Rect songAlbumRect = new Rect();
	private Rect songInfoRect = new Rect();

	public PlayerLayout(Context context) {
		super(context);
		init(context);
	}

	public PlayerLayout(Context context, AttributeSet attributes){
		super(context, attributes, 0);
		init(context);
	}

	public PlayerLayout(Context context, AttributeSet attributes, int style){
		super(context, attributes, 0);
		init(context);
	}

	private void init(Context context){
		background = (ImageView)new ImageView(context);
		background.setScaleType(ScaleType.CENTER_CROP);
		addView(background);


		textSongInfo = new TextView(context);
		textSongInfo.setTextColor(0xFFFFFFFF);
		textSongInfo.setTypeface(Typeface.createFromAsset(context.getAssets(), "font/Roboto-Thin.ttf"));
		textSongInfo.setGravity(Gravity.LEFT | Gravity.BOTTOM);
		
		textSongInfo.setPivotX(0);
		textSongInfo.setPivotY(0);
		if (Build.VERSION.SDK_INT < 11) {

			RotateAnimation animation = new RotateAnimation(0, -90);
			animation.setDuration(1);
			animation.setFillAfter(true);
			textSongInfo.startAnimation(animation);
		} else {

			textSongInfo.setRotation(-90);
		}

		addView(textSongInfo);

		cover = new ImageView(context);
		addView(cover);

		progress = new ProgressView(context);
		addView(progress);

		volume = new VolumeControl(context);
		addView(volume);

		textSongAlbum = new TextView(context);
		textSongAlbum.setTextColor(0xFFFFFFFF);
		textSongAlbum.setTypeface(Typeface.createFromAsset(context.getAssets(), "font/Roboto-Thin.ttf"));
		textSongAlbum.setGravity(Gravity.CENTER | Gravity.TOP);

		addView(textSongAlbum);


		LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		buttons =  inflater.inflate(R.layout.buttons, null, false);
		addView(buttons);

		textPaint.setColor(0xFFFFFFFF);
		textPaint.setFakeBoldText(false);
		textPaint.setTypeface(Typeface.createFromAsset(context.getAssets(), "font/Roboto-Thin.ttf"));
		textPaint.setAntiAlias(true);
		textPaint.setTextAlign(Align.CENTER);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec){
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		int width = getMeasuredWidth();
		int height = getMeasuredHeight();

		int wSpec = MeasureSpec.makeMeasureSpec(getMeasuredWidth(), MeasureSpec.EXACTLY);
		int hSpec = MeasureSpec.makeMeasureSpec(getMeasuredHeight(), MeasureSpec.EXACTLY);

		background.measure(wSpec, hSpec);

		//set measure for text view
		int widthSongArtist = (int)(width * 0.95f);
		songAlbumRect.set((int)(width * 0.025f), 106, (int)(width * 0.975f), height);

		textSongAlbum.measure(MeasureSpec.makeMeasureSpec(widthSongArtist, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(260, MeasureSpec.EXACTLY));

		int widthSongInfo = height - volumeRect.top;
		songInfoRect.set(0, (int)(height * 0.99f), widthSongInfo, (int)(height * 0.99f) + width);
		textSongInfo.measure(MeasureSpec.makeMeasureSpec(widthSongInfo, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY));

		double sizeFactor = 0.6;
		int screenSize = getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
		if(screenSize != Configuration.SCREENLAYOUT_SIZE_SMALL && screenSize != Configuration.SCREENLAYOUT_SIZE_NORMAL 
				&& screenSize != Configuration.SCREENLAYOUT_SIZE_LARGE )
			sizeFactor = 0.3;

		
		
		
		int rectSize = width > height ? (int)(height*sizeFactor) : (int)(width*sizeFactor);
		int specAux = MeasureSpec.makeMeasureSpec(rectSize, MeasureSpec.EXACTLY);
		volume.measure(specAux, specAux);
		int marginH = (width - rectSize) / 2;
		int marginV = (height - rectSize) / 2;
		volumeRect.set(marginH, marginV, marginH + rectSize, marginV + rectSize);

		int coverMargin = (int)(rectSize * 0.06);
		coverRect.set(volumeRect.left + coverMargin, volumeRect.top + coverMargin, volumeRect.right - coverMargin, volumeRect.bottom - coverMargin);
		specAux = MeasureSpec.makeMeasureSpec(coverRect.width(), MeasureSpec.EXACTLY);
		cover.measure(specAux, specAux);

		int sizeDelta = volume.getHalfStrokeWidth() * 4;
		progressRect.set(volumeRect.left + sizeDelta, volumeRect.top + sizeDelta, volumeRect.right - sizeDelta, volumeRect.bottom - sizeDelta);
		specAux = MeasureSpec.makeMeasureSpec(volumeRect.width() - sizeDelta, MeasureSpec.EXACTLY);
		progress.measure(specAux, specAux);

		path.addCircle(width/2, height/2 , volumeRect.width()/2, Direction.CW);

		buttons.measure(wSpec, MeasureSpec.makeMeasureSpec(400, MeasureSpec.EXACTLY));
		
		int marginButtonsH = (width - (int)(rectSize * 1.5f)) / 2;
		
		buttonsRect.set(marginButtonsH, volumeRect.bottom + 8, width - marginButtonsH, volumeRect.bottom + 8 + 70);



	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		background.layout(l, t, r, b);

		textSongAlbum.layout(songAlbumRect.left, songAlbumRect.top, songAlbumRect.right, songAlbumRect.bottom);
		textSongInfo.layout(songInfoRect.left, songInfoRect.top, songInfoRect.right, songInfoRect.bottom);

		volume.layout(volumeRect.left, volumeRect.top, volumeRect.right, volumeRect.bottom);
		cover.layout(coverRect.left, coverRect.top, coverRect.right, coverRect.bottom);
		progress.layout(progressRect.left, progressRect.top, progressRect.right, progressRect.bottom);


		buttons.layout(buttonsRect.left, buttonsRect.top, buttonsRect.right, buttonsRect.bottom);
	}

	@Override
	public void dispatchDraw(Canvas canvas){
		super.dispatchDraw(canvas);
		//		canvas.drawTextOnPath(text, path, 0, 0, textPaint);
	}

	@Override
	public void setSongLayout(Song song, final Bitmap backgroundBitmap, final Bitmap coverBitmap) {
		final String textAlbumSong = song.getTitle() + " / " + song.getArtist();
		final String textInfo = song.getAlbum();

		if(getMeasuredWidth() > getMeasuredHeight())
//			songAlbumTextSize = (getMeasuredWidth() - volumeRect.width()) / 18;
			
			songAlbumTextSize = getMeasuredWidth() / 24;
		else
//			songAlbumTextSize =  (getMeasuredHeight() - volumeRect.width()) / 24;
			songAlbumTextSize = getMeasuredHeight() / 24;


		if(getMeasuredWidth() > getMeasuredHeight())
//			songInfoTextSize = (getMeasuredHeight() - volumeRect.width()) / 4;
			songInfoTextSize = getMeasuredHeight() / 12;
		else
			songInfoTextSize =  getMeasuredWidth() / 12;

		

		post(new Runnable() {

			@Override
			public void run() {
				if(getVisibility() != View.VISIBLE)
					setVisibility(View.VISIBLE);

				textSongAlbum.setTextSize(songAlbumTextSize);
				textSongInfo.setTextSize(songInfoTextSize);

				textSongAlbum.setText(textAlbumSong);
				textSongInfo.setText(textInfo);


				background.setImageBitmap(backgroundBitmap);
				cover.setImageBitmap(coverBitmap);
				invalidate();
			}
		});



		//		text = song.getTitle() + " / " + song.getArtist() + " / " + song.getAlbum() + "   ";
		//
		//		if(getMeasuredWidth() > getMeasuredHeight())
		//			defaultTextSize = (getMeasuredHeight() - volumeRect.width()) / 2;
		//		else
		//			defaultTextSize =  (getMeasuredWidth() - volumeRect.width()) / 2;
		//		textPaint.setTextSize(defaultTextSize);
		//		double circumference = Math.PI * volumeRect.width();
		//
		//		textPaint.setTextSize(defaultTextSize);
		//		while(textPaint.measureText(text) > circumference)
		//			textPaint.setTextSize(textPaint.getTextSize() - 1f);
		//
		//
		//
		//		post(new Runnable() {
		//
		//			@Override
		//			public void run() {
		//				if(getVisibility() != View.VISIBLE)
		//					setVisibility(View.VISIBLE);
		//				background.setImageBitmap(backgroundBitmap);
		//				cover.setImageBitmap(coverBitmap);
		//				invalidate();
		//			}
		//		});
	}

	@Override
	public void onProgress(long currentDuration, long totalDuration) {
		progress.setProgress(currentDuration, totalDuration);
	}

	@Override
	public void onError(String error) {
		Log.e(TAG, error);
	}

	public VolumeControl getVolume(){
		return this.volume;
	}
}
