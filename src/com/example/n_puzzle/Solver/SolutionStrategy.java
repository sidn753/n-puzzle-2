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

    /**During line end maneuvers, you have to
     * temporarily freeze the index to solve in a
     * place where it doesn't belong, and
     * then unfreeze it once the maneuver is over.
     */
    private Point temporaryFreeze;

    public SolutionStrategy(GameState gameState, DifficultyManager.Difficulty difficulty){
        ROW_LENGTH = difficulty.getNumDivisions();
        COL_LENGTH = ROW_LENGTH;
        mFrozenTiles = new ArrayList<Point>();

        indexToSolve = getFirstUnsolvedIndex(gameState);

    }

    public int getFirstUnsolvedIndex(GameState gameState){
        int index = 0;

        for(int i = 0; i < gameState.getNumTiles(); i++){
            Point correct = GameState.getCorrectLocationForIndex(i, ROW_LENGTH);
            Point actual = gameState.getLocation(i);
            if(!correct.equals(actual)){
                break;
            }

            //freeze the index so that it doesn't get moved
            Log.d(TAG, index + " is already solved, freezing");
            freezeIndex(index);
            index++;
        }
        return index;
    }


    public Heuristic getNextGoal(){
        Heuristic nextHeuristic = null;

        if(moveBlank){

            //this indicates that everything has been solved, including the last 6 tiles
            if(indexToSolve > ROW_LENGTH * COL_LENGTH){
                Log.d(TAG, "Solved everything, solver stopping");
                return null;
            }

            //Get blank tile in place for row end maneuver
            if(atEndOfRow){

                //Inserting a tile into the end of a row
                //requires the blank tile to be below the tile two
                //spaces to the left of the insertion point.
                int indexToPutBlankBelow = lastIndexSolved - 2;

                //temporarily freeze the place where the indexTo Solve is
                freezeTemp(lastIndexSolved);

                nextHeuristic = new BlankToTarget(indexToPutBlankBelow, GameState.Direction.DOWN);
                Log.d(TAG, "End of row: Getting blank tile below index " +
                    indexToPutBlankBelow);
            }

            else if(atEndOfCol){
                //Put the blank tile directly to the right of the index to solve
                int indexToPutBlankBeside = lastIndexSolved;

                //temporarily freeze the place where the indexTo Solve is
                freezeTemp(lastIndexSolved);

                nextHeuristic = new BlankToTarget(indexToPutBlankBeside, GameState.Direction.RIGHT);
                Log.d(TAG, "End of col: Getting blank tile beside index " +
                    indexToPutBlankBeside);
            }

            //This indicates that every tile has been checked.
            //The only thing left to solve is the last 6 tiles.
            else if(indexToSolve == ROW_LENGTH * COL_LENGTH){
                return new SolveLast6();
            }

            else{
                nextHeuristic = new BlankToTarget(indexToSolve, GameState.Direction.UP);
                Log.d(TAG, "Getting blank tile to index " + indexToSolve);
            }
        }


        //moveBlank false
        else{
            if(indexToSolve >= ROW_LENGTH * COL_LENGTH) return null;

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
    public void processSolvedGoal(){
        if(moveBlank){
            moveBlank = false;
        }
        else{
            lastIndexSolved = indexToSolve;
            freezeIndex(indexToSolve);
            getNextIndex();
            moveBlank = true;
        }

        Log.d(TAG, "Goal solved, turned moveBlank flag to " + moveBlank);
    }


//////////////////////////////////////////////////////////////////////////
//Freezing and unfreezing
//////////////////////////////////////////////////////////////////////////
    /**After a goal has been solved, freeze the tile so it is no longer
     * moved in subsequent solutions. Only the last 6 tiles remain unfrozen.
     * The line end maneuvers bypass the frozen tiles restriction, but leave
     * the tiles in their frozen places when complete.
     */
    private void freezeIndex(int index){
        Point pointToFreeze = GameState.getCorrectLocationForIndex(index, ROW_LENGTH);

        if(pointToFreeze == null) return;

        Log.d(TAG, "Freezing point (R, C) " + pointToFreeze.y + ", " +pointToFreeze.x);

        if(!mFrozenTiles.contains(pointToFreeze))
            mFrozenTiles.add(pointToFreeze);

    }

    public void getNextIndex(){
        indexToSolve++;

        //skip tiles in the last 6
        while(inLastSix(indexToSolve)){
            Log.d(TAG, indexToSolve + " is in the last 6 tiles, skipping it.");
            indexToSolve++;
        }
    }

    /**When inserting into the end of a line, the tile to insert is placed adjacent to
     * the desired location. Then the blank tile is moved into place.
     *
     * While the blank tile is being moved, the tile to insert has to stay in its place.
     * So we freeze the tile to insert in place while the blank tile is being moved into place.
     *
     * @param index the tile that is being inserted.
     */
    private void freezeTemp(int index){
        Point pointToFreeze = GameState.getCorrectLocationForIndex(index, ROW_LENGTH);

        //Inserting at end of row- the tile is placed below where it wants to be
        if(atEndOfRow){
            pointToFreeze.y = pointToFreeze.y + 1;
        }

        //Inserting at end of col- the tile is placed to the right of where it wants to be
        else if(atEndOfCol){
            pointToFreeze.x = pointToFreeze.x + 1;
        }

        Log.d(TAG, "Temporary freeze (R, C) "+ pointToFreeze.y + ", " +pointToFreeze.x);

        mFrozenTiles.add(pointToFreeze);
        temporaryFreeze = pointToFreeze;

    }

    /**The tile to insert is frozen while the blank tile is being prepared for line end
     * maneuvers. After the line end maneuver is fetched, the tile should be unfrozen again.
     */
    private void temporaryUnfreeze(){
        mFrozenTiles.remove(temporaryFreeze);
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

    /**Returns true if the last solved index is at the end of a row or column
     * AND the last goal solved was moving the blank tile.
     */
    public boolean readyForLineEndManeuver(){
        //The blank tile is always put in place before a line end maneuver
        if(moveBlank) return false;
        return atEndOfCol || atEndOfRow;
    }

    public MoveQueue lineEndManeuver(){
        MoveQueue result = null;
        if(atEndOfRow){
            result = rowEndManeuver();
        }
        else if(atEndOfCol){
            result = colEndManeuver();
        }

        atEndOfRow = false;
        atEndOfCol = false;

        //After the row end maneuver, we'll move the blank to the next
        //index to solve.
        Log.d(TAG, "Lind end maneuver added, turning moveBlank flag to true");
        moveBlank = true;

        //unfreeze the tile that was temporarily frozen for the line end maneuver
        temporaryUnfreeze();

        return result;
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
        return mColEndManeuver;
    }


    public ArrayList<Point> getFrozenTiles(){
        return mFrozenTiles;
    }

}
