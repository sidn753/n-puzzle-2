package com.example.n_puzzle.Solver.Heuristics;

import android.graphics.Point;

import com.example.n_puzzle.GameState;
import com.example.n_puzzle.Solver.MoveQueue;
import com.example.n_puzzle.Solver.Node;

/**
 * This heuristic judges a GameState by how far the target tile is from
 * the location we want it to be in. The target tile and the destination are
 * provided in the constructor.
 *
 * The target tile is represented by its index in the left-to-right then up-to-down
 * order of tiles in the unshuffled image.
 *
 * Created by stepheno on 7/3/13.
 */
public class TargetToDestination extends Heuristic {

    /**The index of the tile we would like to move*/
    private int mTargetIndex;
    /**The location (row, column) where we'd like to move the target*/
    Point mDestination;

    private TargetToDestination(){}

    public TargetToDestination(int targetIndex, Point destination){
        this.mTargetIndex = targetIndex;
        this.mDestination = destination;
    }

    /**For each GameState, find the distance of the target tile
     * from its desired location. Compare these distances. The lower
     * number wins.
     *
     * @param futureState1 the first state to compare
     * @param futureState2 the seconds tate to compare
     * @return A negative number if gameState1 is worse, 0 if the
     *      gameStates are equal, or a positive number if gameState1
     *      is better
     */
    @Override
    public int compare(Node futureState1, Node futureState2) {

        GameState gameState1 = futureState1.getEndState();
        GameState gameState2 = futureState2.getEndState();

        Point targetLoc1 = gameState1.getLocation(mTargetIndex);
        Point targetLoc2 = gameState2.getLocation(mTargetIndex);
        
        MoveQueue moveQueue1 = futureState1.getMoveQueue();
        MoveQueue moveQueue2 = futureState2.getMoveQueue();

        int distance1 = getDistance(targetLoc1, mDestination);
        int distance2 = getDistance(targetLoc2, mDestination);
        
        final int DISTANCE_WEIGHT = 5;
        
        //Use the length of the movequeue as a secondary heuristic- shorter path length is better
        int total1 = distance1 * DISTANCE_WEIGHT + moveQueue1.size();
        int total2 = distance2 * DISTANCE_WEIGHT + moveQueue2.size();

        //if distance 2 is greater, then gamestate 1 is better
        int result = total1 - total2;
        return result;
    }

    /**If the target is in its destination in the given gamestate,
     * the goal is solved.
     */
    @Override
    public boolean checkIfSolved(Node futureState){
        GameState gameState = futureState.getEndState();
        Point location = gameState.getLocation(mTargetIndex);
        return getDistance(location, mDestination) == 0;
    }

    @Override
    public String getDescription() {
        return String.format("Get tile with index %s to RC(%s, %s)",
                mTargetIndex, mDestination.y, mDestination.x);
    }

}
