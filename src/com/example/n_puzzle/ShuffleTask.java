package com.example.n_puzzle;

import android.os.AsyncTask;
import android.util.Log;

import java.util.ArrayList;
import java.util.Random;

/**
 * Takes a gamestate, makes a random number of legal moves, returns the result.
 *
 * Created by stepheno on 7/14/13.
 */
public class ShuffleTask extends AsyncTask<GameState, Integer, GameState>{

    private static final String TAG = ShuffleTask.class.getSimpleName();

    private static final int MAX_MOVES = 100;
    private static Random rand;

    private GamePlayActivity mContext;

    public ShuffleTask(GamePlayActivity context){
        mContext = context;
    }

    public static GameState shuffle(GameState beginState){
        if(rand == null) rand = new Random();

        final int numMoves = rand.nextInt(MAX_MOVES);

        Log.d(TAG, "Shuffling");

        GameState endState = beginState;

        for(int i = 0; i < numMoves; i++){
            ArrayList<GameState.Direction> moves = endState.possibleMoves(null);
            GameState.Direction move = moves.get(rand.nextInt(moves.size()));
            endState = endState.makeMove(move);
        }

        Log.d(TAG, String.format("Shuffled %s times, resulting state: \n%s", numMoves,
                endState.toString()));

        return endState;
    }

    @Override
    protected GameState doInBackground(GameState... gameStates) {
        if(gameStates[0] == null) return null;

        return shuffle(gameStates[0]);
    }

    @Override
    protected void onPostExecute(GameState gameState) {
        super.onPostExecute(gameState);
        Log.d(TAG, "Done shuffling");
        mContext.onPostShuffle(gameState);

    }
}
