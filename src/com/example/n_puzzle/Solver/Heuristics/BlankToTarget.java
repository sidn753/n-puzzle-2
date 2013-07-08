package com.example.n_puzzle.Solver.Heuristics;

import android.graphics.Point;
import android.util.Log;

import com.example.n_puzzle.GamePlayActivity;
import com.example.n_puzzle.GameState;
import com.example.n_puzzle.Solver.Node;

/**
 * Moving the blank tile to be adjacent to a tile helps us
 * to then move that tile where we would like it to go.
 *
 * This heuristic judges game states based on how far the blank
 * tile is from the location adjacent to the target tile in the given direction.
 *
 * Created by stepheno on 7/3/13.
 */
public class BlankToTarget extends Heuristic {

    public final String TAG = "BlankToTarget";

    /**The index of the tile we would like to move the blank tile toward*/
    private int mTargetIndex;
    /**The direction adjacent to the target where we would like to place the blank tile*/
    private GameState.Direction mDirection;

    private BlankToTarget(){}

    public BlankToTarget(int targetIndex, GameState.Direction direction){
        mTargetIndex = targetIndex;
        mDirection = direction;
    }


    /**For each gamestate, find the distance from the blank tile
     * to the location adjacent to the target tile in the stored direction.
     * Compare these distances. The lesser distance is better.
     *
     * @param futureState1 the first state to compare
     * @param futureState2 the second state to compare
     * @return a negative number if gamestate1 is better, 0 if they are equal,
     *      and a positive number if gamestate 2 is better
     */
    @Override
    public int compare(Node futureState1, Node futureState2) {
        GameState gameState1 = futureState1.getEndState();
        GameState gameState2 = futureState2.getEndState();

        Point blankTile1 = gameState1.getBlankTile();
        Point targetLoc1 = gameState1.getLocation(mTargetIndex);
        Point targetAdjacent1 = GameState.getAdjacent(targetLoc1, mDirection);

        Point blankTile2 = gameState2.getBlankTile();
        Point targetLoc2 = gameState2.getLocation(mTargetIndex);
        Point targetAdjacent2 = GameState.getAdjacent(targetLoc2, mDirection);

        int distance1 = getDistance(blankTile1, targetAdjacent1);
        int distance2 = getDistance(blankTile2, targetAdjacent2);

        //Ue the length of the movequeues in the heuristic
        int total1 = distance1 + futureState1.getMoveQueue().size();
        int total2 = distance2 + futureState2.getMoveQueue().size();

        //if distance 2 is greater, then gamestate 1 is better
        int result = total1 - total2;

        if(GamePlayActivity.DEBUG_VERBOSE){
            Log.d(TAG, String.format("Solver: comparing gamestates for heuristic %s: " +
                    "\n%s\n" +
                    "compared to \n%s\n" +
                    "The result is %s.",
                    this.getDescription(), gameState1.toString(), gameState2.toString(), result));
        }

        return result;
    }

    /**If the blank tile is in it's directed place, the goal is solved.
     *
     * @param futureState
     * @return
     */
    @Override
    public boolean checkIfSolved(Node futureState){
        GameState gameState = futureState.getEndState();
        Point blankTile = gameState.getBlankTile();
        Point targetLocation = gameState.getLocation(mTargetIndex);
        Point adjacentLocation = GameState.getAdjacent(targetLocation, mDirection);
        int distance = getDistance(blankTile, adjacentLocation);

        if(GamePlayActivity.DEBUG_VERBOSE){
            Log.d(TAG, "Solver: checking gamestate for solution. \n" + gameState.toString());
            Log.d(TAG, String.format("Solver: Target location is %s, " +
                    "Adjacent location is %s, Blank tile location is %s",
                    targetLocation, adjacentLocation, blankTile));
            Log.d(TAG, "Solver: distance between blank tile and destination: " + distance);
        }

        return distance == 0;
    }

    @Override
    public String getDescription() {
        return String.format("Get blank tile %s from the tile with index %s",
                mDirection, mTargetIndex);
    }
}
