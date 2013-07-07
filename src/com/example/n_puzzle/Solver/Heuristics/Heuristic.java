package com.example.n_puzzle.Solver.Heuristics;

import android.graphics.Point;

import com.example.n_puzzle.Solver.Node;

import java.util.Comparator;

/**
 * Created by stepheno on 7/3/13.
 */
public abstract class Heuristic implements Comparator<Node> {
    @Override
    public abstract int compare(Node futureState1, Node futureState2);

    public abstract boolean checkIfSolved(Node gameState);

    public abstract String getDescription();


    /**Determine the distance between two points.
     *
     * @return The Manhattan distance of the first location
     *      from the second location.
     */
    public int getDistance(Point location1, Point location2){
        if(location1 == null || location2 == null){
            throw new IllegalArgumentException("Heuristic: " +
                    "getDistance called with a null point");
        }

        int yDist = Math.abs(location1.y - location2.y);
        int xDist = Math.abs(location1.x - location2.x);
        return xDist + yDist;
    }
}

