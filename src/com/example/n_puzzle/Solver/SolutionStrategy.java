package com.example.n_puzzle.Solver;

import android.graphics.Point;
import android.util.Log;

import com.example.n_puzzle.DifficultyManager;
import com.example.n_puzzle.GameState;
import com.example.n_puzzle.Solver.Heuristics.BlankToTarget;
import com.example.n_puzzle.Solver.Heuristics.Heuristic;
import com.example.n_puzzle.Solver.Heuristics.SolveLast6;
import com.example.n_puzzle.Solver.Heuristics.TargetToDestination;

import java.util.ArrayList;

/**
 * Created by stepheno on 7/3/13.
 */
public class SolutionStrategy {

    private static final String TAG = "SolutionStrategy";
    private ArrayList<Heuristic> strategies;
    private final int ROW_LENGTH;
    private final int COL_LENGTH;

    private ArrayList<Point> mFrozenTiles;

    private boolean atEndOfRow;
    private boolean atEndOfCol;
    private MoveQueue mRowEndManeuver;
    private MoveQueue mColEndManeuver;

    private int lastIndexSolved;
    private int indexToSolve = 0;
    private boolean moveBlank = true;

    public SolutionStrategy(GameState gameState, DifficultyManager.Difficulty difficulty){
        ROW_LENGTH = difficulty.getNumDivisions();
        COL_LENGTH = ROW_LENGTH;
        mFrozenTiles = new ArrayList<Point>();
    }

    public Heuristic getNextGoal(){
        Heuristic nextHeuristic = null;

        if(moveBlank){

            if(indexToSolve == ROW_LENGTH * COL_LENGTH) return new SolveLast6();

            if(indexToSolve > ROW_LENGTH * COL_LENGTH){
                Log.d(TAG, "Solved everything, solver stopping");
                return null;
            }

            //getByLocation blank tile in place for row end maneuver
            if(atEndOfRow){
                int indexToPutBlankBelow = lastIndexSolved - 2;
                nextHeuristic = new BlankToTarget(indexToPutBlankBelow, GameState.Direction.DOWN);
                Log.d(TAG, "End of row: Getting blank tile below index " +
                    indexToPutBlankBelow);
            }

            else if(atEndOfCol){
                //Put the blank tile directly to the right of the index to solve
                int indexToPutBlankBeside = lastIndexSolved;
                nextHeuristic = new BlankToTarget(indexToPutBlankBeside, GameState.Direction.RIGHT);
                Log.d(TAG, "End of col: Getting blank tile beside index " +
                    indexToPutBlankBeside);
            }
            else{
                nextHeuristic = new BlankToTarget(indexToSolve, GameState.Direction.UP);
                Log.d(TAG, "Getting blank tile to index " + indexToSolve);
            }
        }

        else{
            if(indexToSolve > ROW_LENGTH * COL_LENGTH) return null;

            Point destination = GameState.getCorrectLocationForIndex(indexToSolve, ROW_LENGTH);

            //If this tile is on the end of the row
            if(destination.x == ROW_LENGTH - 1){
                Log.d(TAG, "End of row flag turned on, index " + indexToSolve);
                atEndOfRow = true;
                destination.y = destination.y + 1;
            }

            //If this tile is on the end of a column
            if(destination.y == COL_LENGTH - 1){
                Log.d(TAG, "End of col flag turned on, index " + indexToSolve);
                atEndOfCol = true;
                destination.x = destination.x + 1;
            }

            Log.d(TAG, String.format("Solving for index %s. The Location is RC(%s, %s)",
                    indexToSolve, destination.y, destination.x));

            nextHeuristic = new TargetToDestination(indexToSolve, destination);

        }

        return nextHeuristic;
    }

    /**After every solved goal, we flip the moveBlank flag.
     * Before we try to move a tile to its proper location,
     * we getByLocation the blank tile adjacent to it-
     * This makes it easier to move the tile.
     */
    public void goalSolved(){
        if(moveBlank){
            moveBlank = false;

            if(readyForLineEndManeuver()){

            }
        }
        else{
            lastIndexSolved = indexToSolve;
            freezeCurrentIndex();
            moveBlank = true;
        }

        Log.d(TAG, "Goal solved, turned moveBlank flag to " + moveBlank);
    }



    /**After a goal has been solved, freeze the tile so it is no longer
     * moved in subsequent solutions. Only the last 6 tiles remain unfrozen.
     * The line end maneuvers bypass the frozen tiles restriction, but leave
     * the tiles in their frozen places when complete.
     */
    private void freezeCurrentIndex(){
        Point pointToFreeze = GameState.getCorrectLocationForIndex(indexToSolve, ROW_LENGTH);

        indexToSolve++;

        //skip tiles in the last 6
        while(inLastSix(indexToSolve)){
            Log.d(TAG, indexToSolve + " is in the last 6 tiles, skipping it.");
            indexToSolve++;
        }

        Log.d(TAG, "Freezing point (R, C) " + pointToFreeze.y + ", " +pointToFreeze.x);

        mFrozenTiles.add(pointToFreeze);
    }

    /**The N-Puzzle requires 6 unfrozen tiles to be left over to solve at
     * the end.
     *
     * We are going to save the bottom two rows, right most 3 columns.
     *
     * If the current index to solve would be in this block, we skip that
     * index.
     *
     * @param index the index to solve
     * @return true if this index's solution location is in the bottom-right 6
     *  tiles of the gamegrid
     */
    private boolean inLastSix(int index){
        Point location = GameState.getCorrectLocationForIndex(index, ROW_LENGTH);
        final int UPPER_BOUND = COL_LENGTH - 2;
        final int LOWER_BOUND = COL_LENGTH - 1;
        final int LEFT_BOUND = ROW_LENGTH - 3;
        final int RIGHT_BOUND = ROW_LENGTH - 1;

        boolean result = location.x <= RIGHT_BOUND && location.x >= LEFT_BOUND
                && location.y <= LOWER_BOUND && location.y >= UPPER_BOUND;

        Log.d(TAG, String.format("inLastSix for %s. Location is RC(%s, %s). " +
                "Result is %s.", index, location.y, location.x, result));
        return result;
    }


///////////////////////////////////////////////////////////////////////
//line end
///////////////////////////////////////////////////////////////////////
    public boolean readyForLineEndManeuver(){
        if(moveBlank) return false;
        return atEndOfCol || atEndOfRow;
    }

    public MoveQueue lineEndManeuver(){
        if(atEndOfRow) return rowEndManeuver();
        else if(atEndOfCol) return colEndManeuver();
        return null;
    }

    /**The row end maneuver is the method for inserting a correct tile
     * at the end of an otherwise solved row. From the proper starting state,
     * it is always the same series of moves.
     *
     * It begins with the correct row-ending tile directly beneath where it belongs,
     * and the blank tile exactly two places to its left.
     *
     * The move direction is given in reference to the blank tile, ie LEFT means: move the
     * tile that is to the left of the blank tile into the blank space.
     *
     * The series of moves is as follows:
     *
     * UP RIGHT RIGHT DOWN
     * LEFT UP LEFT DOWN
     *
      * @return the series of moves that performs this maneuver
     */
    private MoveQueue rowEndManeuver(){
        if(mRowEndManeuver == null){
            mRowEndManeuver = new MoveQueue();

            mRowEndManeuver.add(GameState.Direction.UP);
            mRowEndManeuver.add(GameState.Direction.RIGHT);
            mRowEndManeuver.add(GameState.Direction.RIGHT);
            mRowEndManeuver.add(GameState.Direction.DOWN);

            mRowEndManeuver.add(GameState.Direction.LEFT);
            mRowEndManeuver.add(GameState.Direction.UP);
            mRowEndManeuver.add(GameState.Direction.LEFT);
            mRowEndManeuver.add(GameState.Direction.DOWN);
        }

        Log.d(TAG, "Row end, getting row end maneuver");
        atEndOfRow = false;
        moveBlank = true;
        return mRowEndManeuver;
    }

    /**The col end maneuver is different from the row end maneuver.
     * We still only use 6 tiles.
     *
     * The beginning state has the tile that correctly goes into the bottom of
     * the column directly to the right of the correct location. The blank tile is
     * then directly to the right of the tile to move.
     *
     * The moves are as follows:
     *
     * LEFT UP LEFT DOWN RIGHT
     * UP LEFT DOWN RIGHT RIGHT
     * UP LEFT LEFT DOWN RIGHT
     *
     * @return the series of moves that performs this maneuver
     */
    private MoveQueue colEndManeuver(){
        if(mColEndManeuver == null){
            mColEndManeuver = new MoveQueue();

            mColEndManeuver.add(GameState.Direction.LEFT);
            mColEndManeuver.add(GameState.Direction.UP);
            mColEndManeuver.add(GameState.Direction.LEFT);
            mColEndManeuver.add(GameState.Direction.DOWN);
            mColEndManeuver.add(GameState.Direction.RIGHT);

            mColEndManeuver.add(GameState.Direction.UP);
            mColEndManeuver.add(GameState.Direction.LEFT);
            mColEndManeuver.add(GameState.Direction.DOWN);
            mColEndManeuver.add(GameState.Direction.RIGHT);
            mColEndManeuver.add(GameState.Direction.RIGHT);

            mColEndManeuver.add(GameState.Direction.UP);
            mColEndManeuver.add(GameState.Direction.LEFT);
            mColEndManeuver.add(GameState.Direction.LEFT);
            mColEndManeuver.add(GameState.Direction.DOWN);
            mColEndManeuver.add(GameState.Direction.RIGHT);
        }

        Log.d(TAG, "Col end, getting col end maneuver");

        moveBlank = true;
        atEndOfCol = false;
        return mColEndManeuver;
    }


    public ArrayList<Point> getFrozenTiles(){
        return mFrozenTiles;
    }

}
