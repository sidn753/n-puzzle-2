package com.example.n_puzzle.Solver;

import android.os.Handler;
import android.util.Log;

import com.example.n_puzzle.GamePlayActivity;
import com.example.n_puzzle.GameState;

/**
 * The movemaker is supplied with a queue of moves to make to
 * solveGoal the game. It starts a thread that supplies the next move
 * every .5 seconds.
 *
 * Created by stepheno on 6/30/13.
 */
public class MoveMaker implements Runnable{
    public final String TAG = "MoveMaker";

    private MoveQueue moves;
    private Handler mHandler;
    private GamePlayActivity mContext;
    private final long DELAY = 100;
    private boolean isStopped = false;


    public MoveMaker(GamePlayActivity context, MoveQueue moves){
        this.moves = moves;
        mHandler = new Handler();
        mContext = context;

        Log.d(TAG, "Initialized new Move Maker");
    }

    /**The thread uses a handler
     * to run itself again every x seconds.*/
    @Override
    public void run() {
        if(!isStopped){
            GameState.Direction nextMove = moves.poll();
            Log.d(TAG, "MoveMaker making move " + nextMove);

            mContext.makeMove(nextMove);

            if(moves != null && !moves.isEmpty() && mHandler != null){
                mHandler.postDelayed(this, DELAY);
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

        mHandler.removeCallbacks(this);
        moves.removeAll(moves);
        moves = null;
        mHandler = null;
    }
}
