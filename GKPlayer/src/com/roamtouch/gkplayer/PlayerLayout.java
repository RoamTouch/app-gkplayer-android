package com.roamtouch.gkplayer;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint.Align;
import android.graphics.Path;
import android.graphics.Path.Direction;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

import com.roamtouch.gkplayer.graphics.ProgressView;
import com.roamtouch.gkplayer.graphics.VolumeControl;
import com.roamtouch.gkplayer.service.ContentListener;
import com.roamtouch.gkplayer.service.Song;

public class PlayerLayout extends ViewGroup implements ContentListener{
	private static final String TAG = "Randon Music Player";
	private ImageView background;
	private ImageView cover;
	private ProgressView progress;
	private VolumeControl volume;
	private View buttons;
	private String text = "";
	private float defaultTextSize = 0;
	private Path path = new Path();
	private TextPaint textPaint = new TextPaint();

	private Rect volumeRect = new Rect();
	private Rect coverRect = new Rect();
	private Rect progressRect = new Rect();

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

		cover = new ImageView(context);
		addView(cover);

		progress = new ProgressView(context);
		addView(progress);

		volume = new VolumeControl(context);
		addView(volume);

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

		double sizeFactor = 0.7;
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
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		background.layout(l, t, r, b);
		volume.layout(volumeRect.left, volumeRect.top, volumeRect.right, volumeRect.bottom);
		cover.layout(coverRect.left, coverRect.top, coverRect.right, coverRect.bottom);
		progress.layout(progressRect.left, progressRect.top, progressRect.right, progressRect.bottom);
		buttons.layout(l, b-230, r, b);
	}

	@Override
	public void dispatchDraw(Canvas canvas){
		super.dispatchDraw(canvas);
		canvas.drawTextOnPath(text, path, 0, 0, textPaint);
	}

	@Override
	public void setSongLayout(Song song, final Bitmap backgroundBitmap, final Bitmap coverBitmap) {		
		text = song.getTitle() + " / " + song.getArtist() + " / " + song.getAlbum() + "   ";

		if(getMeasuredWidth() > getMeasuredHeight())
			defaultTextSize = (getMeasuredHeight() - volumeRect.width()) / 2;
		else
			defaultTextSize =  (getMeasuredWidth() - volumeRect.width()) / 2;
		textPaint.setTextSize(defaultTextSize);
		double circumference = Math.PI * volumeRect.width();

		textPaint.setTextSize(defaultTextSize);
		while(textPaint.measureText(text) > circumference)
			textPaint.setTextSize(textPaint.getTextSize() - 1f);



		post(new Runnable() {

			@Override
			public void run() {
				if(getVisibility() != View.VISIBLE)
					setVisibility(View.VISIBLE);
				background.setImageBitmap(backgroundBitmap);
				cover.setImageBitmap(coverBitmap);
				invalidate();
			}
		});
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
