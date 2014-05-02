package com.roamtouch.gkplayer.graphics;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.media.AudioManager;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

public class VolumeControl extends ImageView {
	private static final int START_ANGLE = 270;
	private static final int MAX_VOLUME_ANGLE = 340;
	private static final double MAX_VOLUME_ANGLE_RADIANS = Math.toRadians(MAX_VOLUME_ANGLE);
	
	private AudioManager audio;
	private int maxVolume = 0;
	private int currentVolume = 0;
	private int to = 0;
	private PointF buttonCenter = new PointF();
	private float buttonRadius = 0;
	
	private Paint paint = new Paint();
	private RectF rect = new RectF();
	private int halfStrokeWidth = 10;
	//auxiliar vars for efficiency
	PointF center = new PointF();
	float radius;
	
	private boolean dragging = false;
	

	public VolumeControl(Context context) {
		super(context);
		audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		maxVolume = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		currentVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
		to = currentVolume * 340 / maxVolume;
		setBackgroundColor(0x00000000);
		paint.setStyle(Paint.Style.STROKE);
		paint.setAntiAlias(true);
		paint.setColor(0xDDDFDCD3);
				
		setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch(event.getAction()){
				case MotionEvent.ACTION_DOWN:
					return checkButton(event.getX(), event.getY());
				case MotionEvent.ACTION_MOVE:
					if(dragging)
						updateVolume(event.getX(), event.getY());
					return true;
				case MotionEvent.ACTION_UP:
					dragging = false;
					return true;
				}
				return false;
			}
		});
	}
	
	@Override
	public void onLayout(boolean changed, int left, int top, int right, int bottom){
		super.onLayout(changed, left, top, right, bottom);
		halfStrokeWidth = (int)((right-left)*0.018f);
		paint.setStrokeWidth(halfStrokeWidth*2);
		buttonRadius = paint.getStrokeWidth() * 2;
		rect.set(halfStrokeWidth*4, halfStrokeWidth*4, right - left - halfStrokeWidth*4, bottom - top - halfStrokeWidth*4);
		center.x = rect.width() / 2;
		center.y = rect.height() / 2;
		radius = rect.width() / 2;
		calculateButtonPosition();
	}
	
	@Override
	public void onDraw(Canvas canvas){
		super.onDraw(canvas);
		paint.setStyle(Paint.Style.STROKE);
		canvas.drawArc(rect, START_ANGLE, to, false, paint);
		drawButton(canvas, paint, to);
	}
	
	private void drawButton(Canvas canvas, Paint paint, int angle){
		paint.setStyle(Paint.Style.FILL);
		canvas.drawCircle(buttonCenter.x, buttonCenter.y, buttonRadius, paint);
		
	}
	
	private void calculateVolume(){
		currentVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
		to = currentVolume * MAX_VOLUME_ANGLE / maxVolume;
	}
	
	public void adjust(int direction){
		audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, 0);
		calculateVolume();
		calculateButtonPosition();
		postInvalidate();
	}
	
	public int getHalfStrokeWidth(){
		return halfStrokeWidth;
	}
	
	private void calculateButtonPosition(){
		calculateButtonPosition(Math.toRadians(to));
	}
	
	private void calculateButtonPosition(double radians){
		buttonCenter.x = (float)(center.x + Math.sin(radians) * center.x) + halfStrokeWidth*4;
		buttonCenter.y = (float)(center.y - Math.cos(radians) * center.y) + halfStrokeWidth*4;
		postInvalidate();
	}

	private boolean checkButton(float x, float y){
		//buttonRadius*2 because otherwise circle's area is too small
		if(Math.pow(x - buttonCenter.x, 2) + Math.pow(y - buttonCenter.y, 2) <= Math.pow(buttonRadius*2, 2)){ 
			dragging = true;
			return true;
		}else
			return false;
	}
	
	private void updateVolume(float x, float y){
		double angle;
		
		if(x >= center.x)
			if(y > center.y)
				angle = Math.PI / 2 + Math.atan((y - center.y) / (x - center.x) );
			else
				angle = Math.atan((x - center.x) / (center.y - y));
		else
			if(y > center.y)
				angle = Math.PI + Math.atan( (center.x - x) / (y - center.y) );
			else
				angle = Math.PI * 1.5d + Math.atan((y - center.y) / (x - center.x) );
		
		if(angle > MAX_VOLUME_ANGLE_RADIANS){
			if(to == 0)
				return;
			else if(to == MAX_VOLUME_ANGLE)
				return;
			else if(to > 270){
				to = MAX_VOLUME_ANGLE;
				calculateButtonPosition(MAX_VOLUME_ANGLE_RADIANS);
			}else{
				to = 0;
				calculateButtonPosition(0);
			}
			//dragging = false;
		}else{
			int degrees = (int)Math.toDegrees(angle);
			if(to == 0 && degrees > 90)
				return;
			else if(to == MAX_VOLUME_ANGLE && degrees < MAX_VOLUME_ANGLE - 90)
				return;
			else{
				to = degrees;
				calculateButtonPosition(angle);
			}
		}
		audio.setStreamVolume(AudioManager.STREAM_MUSIC, to*maxVolume/MAX_VOLUME_ANGLE, 0);
		
	}
}
