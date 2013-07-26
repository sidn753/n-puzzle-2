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
import com.steveinflow.n_puzzle.Solver.Heuristics.SolveLast6;

/**
 * Created by stepheno on 6/24/13.
 */
public class Solver {
    public static final String TAG = "Solver";

    private SolveGameTask mParent;
    private Heuristic mHeuristic;
    private boolean last6;
    private GameState mBeginState;
    private ArrayList<Point> mFrozenTiles;
    private long mNodesChecked = 0;

    private PriorityQueue<Node> mPossibleSuccessors;

    /**Store visited states in a hashset to avoid looking at them twice.*/
    private Set<GameState> mVisitedStates;

    /**These tiles are already solved and should not be moved. Dont
     * consider any branches that result from moving these.*/
    private ArrayList<Point> frozenTiles;

    public enum Status { SOLVING, SOLVED, FAILED, IDLE, CANCELLED; }
    private Status mStatus = Status.IDLE;

    public Solver(SolveGameTask parentThread, Heuristic goal, GameState beginState,
                  ArrayList<Point> frozenTiles){
        mParent = parentThread;
        mHeuristic = goal;
        last6 = false; //mHeuristic instanceof SolveLast6;
        
        mBeginState = beginState;
        mFrozenTiles = frozenTiles;

        Log.d(TAG, "Starting solver for goal: " + mHeuristic.getDescription());
        Log.d(TAG, "BeginState: \n" + beginState.toString());
        Log.d(TAG, "csv: " + beginState.toCSV());
        Log.d(TAG, "Frozen Tiles: " + printFrozen());
    }
    
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
                100, (Comparator) mHeuristic);

        //The first state has no moves, and the ending state is the same as the beginning
        MoveQueue noMoves = new MoveQueue();
        Node firstState = new Node(mBeginState, noMoves, mBeginState);


        //If the goal has already been achieved, don't do anything
        if(mHeuristic.checkIfSolved(firstState)){
            return firstState;
        }


        //Don't look at the opening state more than once
        addToVisitedStates(mBeginState);

        /*Check out the legal moves for the first state. Add
        the resulting states to the priority queue*/
        addSuccessors(firstState);

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
	            if(last6){
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
        	
        	if(last6){
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
        final GameState ancestorState = node.getBeginningState();
        final MoveQueue previousMoves = node.getMoveQueue();
        
        ArrayList<GameState.Direction> legalMoves = previousState.getLegalMoves((mFrozenTiles));

        if(last6){
        	Log.d(TAG, "adding successors for gamestate: " + previousState.toCSV());
        }
        
        /*for each legal move, check out the gamestate that would
        result from making that move. */
        for(GameState.Direction nextMove : legalMoves){

        	if(last6) Log.d(TAG, "trying move: " + nextMove);
        	
            //makeMove returns the state that results from making that move
            GameState resultantState = previousState.makeMove(nextMove);

            //Skip this state if we've already seen it
            String csv = resultantState.toCSV();
			if(hasBeenVisited(resultantState)){
            	if(last6){
            		Log.d(TAG, "resultant state has already been seen, skipping: " + csv);
            		if(csv.equals("0,1,2,3,4,5,6,7,8,-1,10,13,12,11,9,14,")) printVisited();
            	}
                continue;
            }
            else{
                
                /*Clone the previous state's moveQueue and push the prospective move onto it.*/
                MoveQueue nextMoves = new MoveQueue();
                nextMoves.addAll(previousMoves);
                nextMoves.add(nextMove);

                /*Preserve the ancestor of the previous state, add the moves to getByLocation here,
                * and add the resultant state. Push this new node to the priorityqueue.*/
                Node nextNode = new Node(ancestorState,
                        nextMoves, resultantState);
                
                
                if(last6){
					Log.d(TAG, "Adding successor path: " + nextNode.getMoveQueue().toString());
					
					Log.d(TAG, "New node endState csv: " + csv);
					
                    /*Log.d(TAG, String.format("Adding a new node to the PriorityQueue. " +
                            "The previous state is \n%s\n" +
                            "The Direction is %s, and the resultant state is \n%s\n ",
                            previousState.toString(), nextMove, resultantState.toString()));*/
                }

                mPossibleSuccessors.add(nextNode);
            }
        }
    }

	private boolean hasBeenVisited(GameState resultantState) {
		return mVisitedStates.contains(resultantState);
	}
}
