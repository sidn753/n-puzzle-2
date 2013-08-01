package com.steveinflow.n_puzzle.Solver.Heuristics;

import android.graphics.Point;
import android.util.Log;

import com.steveinflow.n_puzzle.GamePlayActivity;
import com.steveinflow.n_puzzle.GameState.GameState;
import com.steveinflow.n_puzzle.Solver.MoveQueue;
import com.steveinflow.n_puzzle.Solver.Node;

/**
 * The SolutionStrategy calls for everything to be solved and frozen
 * except the last 6 tiles. Once there are only 6 tiles left to move,
 * we use A* search with Manhattan distances as a heuristic.
 *
 * Created by stepheno on 7/6/13.
 */
public class SolveLast6 extends Heuristic{
    public static final String TAG = "SolveLast6";

    /**@return a negative integer, zero, or a positive integer 
     * as the first argument is less than, equal to, or greater than the second.*/
    @Override
    public int compare(Node futureState1, Node futureState2) {
        GameState gameState1 = futureState1.getEndState();
        GameState gameState2 = futureState2.getEndState();

        MoveQueue moveQueue1 = futureState1.getMoveQueue();
        MoveQueue moveQueue2 = futureState2.getMoveQueue();

        int distance1 = getSumOfManhattanDistances(gameState1);
        int distance2 = getSumOfManhattanDistances(gameState2);

        //Use the length of the movequeue as a secondary heuristic- shorter path length is better
        int total1 = distance1 + moveQueue1.size();
        int total2 = distance2 + moveQueue2.size();

        //if distance 2 is greater, then gamestate 1 should have higher priority
        int result = total1 - total2;

        return result;

    }

    /**Determine how far each tile is from where it should be. Sum
     * these distances.
     * 
     * @param gameState the placement of tiles to evaluate
     * @return the sum of distances for each tile from its correct location
     */
    private int getSumOfManhattanDistances(GameState gameState) {
        int total = 0;
        for(int index = 0; index < gameState.getNumTiles() -1; index++){
            total += getManhattanDistance(gameState, index);
        }

        return total;
    }

    /**Manhattan distance = difference in row + difference in col between two points.
     * This method determines how far the tile with the given index is
     * from where it should be in the unshuffled image.
     * 
     * @param gameState the placement of tiles
     * @param index the index of the tile to evaluate
     * @return the distance of the tile's current placement from its correct placement.
     */
    private int getManhattanDistance(GameState gameState, int index){
        Point correctLocation = GameState.getCorrectLocationForIndex(index,
                gameState.getNumDivisions());
        Point actualLocation = gameState.findActualLocation(index);

        int distance = Math.abs(correctLocation.y - actualLocation.y) +
                Math.abs(correctLocation.x - actualLocation.x);

        return distance;
    }

    /**the game is solved if the sum of Manhattan distances is 0 ==
     * every tile is in its correct place.
     */
    @Override
    public boolean checkIfSolved(Node futureState) {
        GameState gameState = futureState.getEndState();

        int distance = getSumOfManhattanDistances(futureState.getEndState());

        if(GamePlayActivity.DEBUG_VERBOSE){
            Log.d(TAG, String.format("Checking if solved: \n%s \nDistance is %s",
                    gameState.toString(), distance));
        }
        return distance == 0;
    }

    @Override
    public String getDescription() {
        return "Solving the final 6 tiles in the bottom right corner.";
    }
}
