package com.steveinflow.n_puzzle.Solver.Heuristics;

import android.graphics.Point;
import android.util.Log;

import com.steveinflow.n_puzzle.GamePlayActivity;
import com.steveinflow.n_puzzle.Solver.Node;

import java.util.Comparator;

/**A Heuristic is a way of comparing gamestates.
 * 
 * Created by stepheno on 7/3/13.
 */
public abstract class Heuristic implements Comparator<Node> {
	
    @Override
    /**Return which node is "better"*/
    public abstract int compare(Node futureState1, Node futureState2);

    /**Return true if  the gamestate is a solution to the goal*/
    public abstract boolean checkIfSolved(Node gameState);

    /**Plain English description of what this heuristic is aiming for*/
    public abstract String getDescription();

    /**Utility method to determine xy distance
     * between two points.
     *
     * @return The Manhattan distance of the first location
     *      from the second location.
     */
    public int getDistance(Point location1, Point location2){
        if(GamePlayActivity.DEBUG_VERBOSE){
            Log.d("Heuristic", String.format("Point 1: %s. Point 2: %s", location1, location2));
        }

        if(location1 == null || location2 == null){
            throw new IllegalArgumentException("Heuristic: " +
                    "getDistance called with a null point");
        }

        int yDist = Math.abs(location1.y - location2.y);
        int xDist = Math.abs(location1.x - location2.x);
        return xDist + yDist;
    }
}

