package com.example.n_puzzle.Solver.Heuristics;

import android.graphics.Point;
import android.util.Log;

import com.example.n_puzzle.GamePlayActivity;
import com.example.n_puzzle.GameState;
import com.example.n_puzzle.Solver.MoveQueue;
import com.example.n_puzzle.Solver.Node;

/**
 * The SolutionStrategy calls for everything to be solved and frozen
 * except the last 6 tiles. Once there are only 6 tiles left to move,
 * it's fine to use brute force search to solve.
 *
 * Created by stepheno on 7/6/13.
 */
public class SolveLast6 extends Heuristic{
    public static final String TAG = "SolveLast6";

    @Override
    public int compare(Node futureState1, Node futureState2) {
        GameState gameState1 = futureState1.getEndState();
        GameState gameState2 = futureState2.getEndState();

        MoveQueue moveQueue1 = futureState1.getMoveQueue();
        MoveQueue moveQueue2 = futureState2.getMoveQueue();

        int distance1 = getSumOfManhattanDistances(gameState1);
        int distance2 = getSumOfManhattanDistances(gameState2);

        int total1 = distance1 + moveQueue1.size();
        int total2 = distance2 + moveQueue2.size();

        //if distance 2 is greater, then gamestate 1 is better
        int result = distance1 - distance2;

        return result;

    }

    private int getSumOfManhattanDistances(GameState gameState) {
        int total = 0;
        for(int index = 0; index < gameState.getNumTiles() -1; index++){
            total += getManhattanDistance(gameState, index);
        }

        return total;
    }

    private int getManhattanDistance(GameState gameState, int index){
        Point correctLocation = GameState.getCorrectLocationForIndex(index,
                gameState.getNumDivisions());
        Point actualLocation = gameState.findActualLocation(index);

        int distance = Math.abs(correctLocation.y - actualLocation.y) +
                Math.abs(correctLocation.x - actualLocation.x);

        return distance;
    }

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
