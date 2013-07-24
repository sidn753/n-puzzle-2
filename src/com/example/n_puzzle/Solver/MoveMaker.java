package com.example.n_puzzle.Solver;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.os.Handler;
import android.util.Log;

import com.example.n_puzzle.GamePlayActivity;
import com.example.n_puzzle.GameState;
import com.example.n_puzzle.R;

/**
 * The movemaker is supplied with a queue of moves to make to
 * solveGoal the game. It starts a thread that supplies the next move
 * every .5 seconds.
 *
 * Created by stepheno on 6/30/13.
 */
public class MoveMaker implements Runnable{
    public final static String TAG = "MoveMaker";

    private MoveQueue moves;
    private Handler mHandler;
    private GamePlayActivity mContext;
    public final static long FAST_DELAY = 100;
    public final static long SLOW_DELAY = 1000;
    public final static String SPEED_KEY = "speed";
    private long mSpeed;
    
    private boolean isStopped = false;

    public MoveMaker(GamePlayActivity context, MoveQueue moves){
        this.moves = moves;
        mHandler = new Handler();
        mContext = context;

        getSpeed();
        Log.d(TAG, "Initialized new Move Maker");
    }
    
    /**Create a dialog to change the speed.
     * @param caller the calling activity
     */
    public static void changeSpeed(GamePlayActivity caller){
    	final GamePlayActivity callingActivity = caller;
    	
		//Build a dialog to display speed options in a menu
		AlertDialog.Builder builder = new AlertDialog.Builder(callingActivity);	
		builder.setTitle(R.string.change_speed);
		
		builder.setItems(R.array.speeds, new OnClickListener(){
			@Override
			public void onClick(DialogInterface arg0, int which) {
				String sharedPrefFileName = callingActivity.getString(R.string.n_puzzle_preferences_filename);
				SharedPreferences.Editor editor =  callingActivity.getSharedPreferences(sharedPrefFileName, Context.MODE_PRIVATE).edit();
				
				long speed = -1;
				if(which == 0) speed = FAST_DELAY;
				else speed = SLOW_DELAY;
				
				editor.putLong(SPEED_KEY, speed).commit();
				Log.d(TAG, "Speed changed, delay: " + speed);
				
				callingActivity.speedChanged();
			}
			
		});
		builder.show();
    }
    
    
    /**Get the last speed stored in shared preferences. Use Fast as the default*/
    private void getSpeed(){
    	String sharedPrefFileName = mContext.getString(R.string.n_puzzle_preferences_filename);
		SharedPreferences prefs =  mContext.getSharedPreferences(sharedPrefFileName, Context.MODE_PRIVATE);
		mSpeed = prefs.getLong(SPEED_KEY, FAST_DELAY);
		
		Log.d(TAG, "Speed retreived: " + mSpeed);
    }

    /**The thread uses a handler
     * to run itself again every $mSpeed milliseconds.*/
    @Override
    public void run() {
        if(!isStopped){
            GameState.Direction nextMove = moves.poll();
            //Log.d(TAG, "MoveMaker making move " + nextMove);

            mContext.makeMove(nextMove);

            if(moves != null && !moves.isEmpty() && mHandler != null){
                mHandler.postDelayed(this, mSpeed);
            }
            else{
                mContext.moveMakerFinished();
            }
        }
    }

    /**Stop execution. Kill all references.*/
    public void stop(){
        isStopped = true;
        Log.d(TAG, "Stopping MoveMaker");

        if(mHandler!= null) mHandler.removeCallbacks(this);
        if(moves != null) moves.removeAll(moves);
        moves = null;
        mHandler = null;
    }

}
