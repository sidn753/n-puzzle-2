package com.steveinflow.n_puzzle.Solver;

import com.steveinflow.n_puzzle.GameState.GameState;

/**
 * Created by stepheno on 7/3/13.
 */
public class Node {
    private GameState beginningState;
    private MoveQueue moveQueue;
    private GameState endState;

    public Node(GameState beginningState, MoveQueue moveQueue, GameState endState){
        this.beginningState = beginningState;
        this.moveQueue = moveQueue;
        this.endState = endState;
    }

    public GameState getBeginningState(){
        return beginningState;
    }

    public MoveQueue getMoveQueue(){
        return moveQueue;
    }

    public GameState getEndState(){
        return endState;
    }

}
