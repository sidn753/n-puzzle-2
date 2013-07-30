package com.steveinflow.n_puzzle.GameState;

import com.steveinflow.n_puzzle.GamePlayActivity;
import com.steveinflow.n_puzzle.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class PreviewActivity extends Activity {
	public static final String TAG = PreviewActivity.class.getSimpleName();

	private String mPictureName;	/**The image name*/
	private int mPictureId;	/**The picture ID*/
	private TextView txt_countdown;
	private final long COUNTDOWN = 4;
	private CountDownTimer mCountdown;
	
	private ImageView imgV;
	
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);
        
        //getByLocation the picture name that was passed with the intent.
        //Use that picture name to find the drawable id from resources
        mPictureName = getPictureName();
        mPictureId = getPictureId(mPictureName);
        txt_countdown = (TextView)findViewById(R.id.txt_countdown);
        txt_countdown.setText(Long.toString(COUNTDOWN));
        
        //Set the image
        imgV = (ImageView) findViewById(R.id.previewImage);
        imgV.setImageResource(mPictureId);
               
        mCountdown = new CountDownTimer(COUNTDOWN * 1000, 1000){

			@Override
			public void onFinish() {
				openGameView(imgV);
			}

			@Override
			public void onTick(long arg0) {
				txt_countdown.setText(Long.toString(arg0/1000));
			}
			        	
        }.start();
                
	} 
	
	@Override
	public void onStop(){
		super.onStop();
		mCountdown.cancel();
	}
	
	/**Create an intent to open the game view.
	 * Pass in the dimensions of the imageView- we'll use
	 * this to size the GridView use by the GameView activity.
	 * 
	 * @param v the view that was clicked on
	 */
	private void openGameView(View v){
		Intent i = new Intent(this, GamePlayActivity.class);
		
		i.putExtra("pictureName", mPictureName);
		i.putExtra("pictureId", mPictureId);
		//((BitmapDrawable)imgV.getDrawable()).getBitmap().recycle();
		this.finish();
		startActivity(i);
	}
	
	/**Use the picture name to retrieve the drawable resource with that name*/
	private int getPictureId(String pictureName) {
		int resId = getResources().getIdentifier(pictureName, "drawable", getPackageName());
		return resId;
	}

	/**Retrieve the passed picture name from the intent that opened this activity.
	 */
	private String getPictureName() {
		String pictureName = getIntent().getExtras().getString("pictureName");
		return pictureName;
	}
	
}
