package com.steveinflow.n_puzzle.Solver;

import com.steveinflow.n_puzzle.GameState.GameState;

/**A node in the search algorithm. 
 * Contains an origin state, a movequeue, and a resultant state.
 * 
 * Created by stepheno on 7/3/13.
 */
public class Node {
    private GameState beginState;
    private MoveQueue moveQueue;
    private GameState endState;

    public Node(GameState beginState, MoveQueue moveQueue, GameState endState){
        this.beginState = beginState;
        this.moveQueue = moveQueue;
        this.endState = endState;
    }
    
    /**construct an origin node*/
    public Node(GameState beginState){
    	this.beginState = beginState;
    	this.moveQueue = new MoveQueue();
    	this.endState = beginState;
    }
    
    /**construct a successor node*/
    public Node(Node previous, GameState.Direction nextMove){
    	MoveQueue nextMoves = new MoveQueue();
        nextMoves.addAll(previous.getMoveQueue());
        nextMoves.add(nextMove);
        
        this.beginState = previous.getBeginningState();
        this.moveQueue = nextMoves;
        this.endState = previous.endState.makeMove(nextMove);
    }

    public GameState getBeginningState(){
        return beginState;
    }

    public MoveQueue getMoveQueue(){
        return moveQueue;
    }

    public GameState getEndState(){
        return endState;
    }

}
