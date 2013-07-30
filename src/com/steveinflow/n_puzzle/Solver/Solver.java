package com.steveinflow.n_puzzle.Solver;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;

import android.graphics.Point;
import android.util.Log;

import com.steveinflow.n_puzzle.GamePlayActivity;
import com.steveinflow.n_puzzle.GameState.GameState;
import com.steveinflow.n_puzzle.Solver.Heuristics.Heuristic;

/**
 * The solver utilizes an A* algorithm to search gamestates until the 
 * provided goal is reached.
 * 
 * Created by stepheno on 6/24/13.
 */
public class Solver {
    public static final String TAG = "Solver";

    private SolveGameTask mParent;
    private Heuristic mHeuristic;
    private boolean debug_print_everything;
    private GameState mBeginState;
    private long mNodesChecked = 0;

    private PriorityQueue<Node> mPossibleSuccessors;

    /**Store visited states in a hashset to avoid looking at them twice.*/
    private Set<GameState> mVisitedStates;

    /**These tiles are already solved and should not be moved. Dont
     * consider any branches that result from moving these.*/
    private ArrayList<Point> mFrozenTiles;

    public enum Status { SOLVING, SOLVED, FAILED, IDLE, CANCELLED; }
    private Status mStatus = Status.IDLE;


    /** 
     * @param parentThread The SolveGameTask (AsyncTask) that contains this solver 
     * @param goal the heuristic that will compare gamestates to determine what is best,
     * 		and will check gamestates to see if they solve the goal
     * @param beginState the origin state to solve from
     * @param frozenTiles a list of tiles (as Cartesian points) that should not be involved
     * 	in any moves while solving this goal.
     */
    public Solver(SolveGameTask parentThread, Heuristic goal, GameState beginState,
                  ArrayList<Point> frozenTiles){
        mParent = parentThread;
        mHeuristic = goal;
        debug_print_everything = false; //mHeuristic instanceof SolveLast6;
        
        mBeginState = beginState;
        mFrozenTiles = frozenTiles;

        Log.d(TAG, "Starting solver for goal: " + mHeuristic.getDescription());
        Log.d(TAG, "BeginState: \n" + beginState.toString());
        Log.d(TAG, "csv: " + beginState.toCSV());
        Log.d(TAG, "Frozen Tiles: " + printFrozen());
    }
    
    /**util method to print the current frozen tiles*/
    public String printFrozen(){
    	StringBuffer buffer = new StringBuffer();
    	for(Point p : mFrozenTiles){
    		buffer.append(p.toString());
    		buffer.append(";");
    	}
    	
    	return buffer.toString();
    }

    public Status getStatus(){
        return mStatus;
    }

    /**debug method to print all states that have been visited*/
    public void printVisited(){
    	for(GameState state : mVisitedStates){
    		Log.d(TAG, "visited " + state.toCSV());
    	}
    }
    
    public Node solveGoal(){
        mStatus = Status.SOLVING;

        mVisitedStates = new HashSet<GameState>();

        //initialize priority queue
        mPossibleSuccessors = new PriorityQueue<Node>(
                100, (Comparator<Node>) mHeuristic);

        Node originNode = new Node(mBeginState);

        //If the goal has already been achieved, don't do anything
        if(mHeuristic.checkIfSolved(originNode)){
            return originNode;
        }

        //Don't look at the opening state more than once
        addToVisitedStates(mBeginState);

        /*Check out the legal moves for the first state. Add
        the resulting states to the priority queue*/
        addSuccessors(originNode);

        Node nextNode = null;

        //Continue searching until the solution is reached
        while(mStatus == Status.SOLVING){
            if(mParent.isCancelled()){
                mStatus = Status.CANCELLED;
                break;
            }

            mParent.getUpdate(mNodesChecked);

            nextNode = mPossibleSuccessors.poll();
            mNodesChecked++;

            if(GamePlayActivity.DEBUG_VERBOSE) Log.d(TAG, "Solver checking node #" + mNodesChecked);

            if(nextNode == null){
                Log.d(TAG, "Solver terminating before solution found, no successors left");
                mStatus = Status.FAILED;
                break;
            }
            
            GameState endState = nextNode.getEndState();
            
            //Check if this node solves the goal, return it if so
            if(mHeuristic.checkIfSolved(nextNode)){

            	Log.d(TAG, "Solution found!");            	
	            if(debug_print_everything){
	            	Log.d(TAG, endState.toString());
	            	Log.d(TAG, nextNode.getMoveQueue().toString());
	            }
	            mStatus = Status.SOLVED;
	            break;
            }
            else{
            	//returns false if an out of memory error occurs
                if(!addToVisitedStates(endState)) break;
                
                addSuccessors(nextNode);
            }
        }

        if(mStatus == Status.CANCELLED){
            return null;
        }

        return nextNode;
    }
    
    public boolean addToVisitedStates(GameState gameState){
    	try{
        	mVisitedStates.add(gameState);
        	
        	if(debug_print_everything){
        		Log.d(TAG, "adding state to visistedStates: " + gameState.toCSV());
        		Log.d(TAG, "visited states: " + mVisitedStates.size());
        	}
        }
        catch(OutOfMemoryError e){
        	Log.d(TAG, "Ran out of internal memory while solving");
        	mStatus = Status.FAILED;
        	return false;
        }
    	
    	return true;
    }

    private void addSuccessors(Node node){
        final GameState previousState = node.getEndState();
        ArrayList<GameState.Direction> legalMoves = previousState.getLegalMoves((mFrozenTiles));

        if(debug_print_everything){
        	Log.d(TAG, "adding successors for gamestate: " + previousState.toCSV());
        }
        
        /*for each legal move, check out the gamestate that would
        result from making that move. */
        for(GameState.Direction nextMove : legalMoves){

        	if(debug_print_everything) Log.d(TAG, "trying move: " + nextMove);
        	
            //makeMove returns the state that results from making that move
            GameState resultantState = previousState.makeMove(nextMove);
            
            String csv = "";
            if(debug_print_everything){  csv = resultantState.toCSV();}
            
            //Skip this state if we've already seen it
			if(mVisitedStates.contains(resultantState)){
            	if(debug_print_everything){Log.d(TAG, "resultant state has already been seen, skipping: " + csv);}
                continue;
            }
            else{
                
            	Node nextNode = new Node(node, nextMove);
                
                if(debug_print_everything){
					Log.d(TAG, "Adding successor path: " + nextNode.getMoveQueue().toString());
					Log.d(TAG, "New node endState csv: " + csv);
                }

                mPossibleSuccessors.add(nextNode);
            }
        }
    }
}
