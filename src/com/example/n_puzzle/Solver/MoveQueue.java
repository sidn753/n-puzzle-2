package com.example.n_puzzle.Solver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;
import java.util.Random;

import android.graphics.Point;
import android.util.Log;

import com.example.n_puzzle.GameState.GameState;
import com.example.n_puzzle.GameState.GameState.Direction;

/**
 * Created by stepheno on 6/30/13.
 */
public class MoveQueue implements Queue<GameState.Direction> {
	private static final String TAG = "MoveQueue";
	
    private ArrayList<GameState.Direction> moves;

    public MoveQueue(){
        moves = new ArrayList<GameState.Direction>();
    }

    @Override
    public synchronized boolean add(GameState.Direction move) {
        moves.add(move);
        return true;
    }

    @Override
    public synchronized GameState.Direction poll() {
        if(moves.size() == 0) return null;

        GameState.Direction move = moves.get(0);
        moves.remove(0);
        return move;
    }

    @Override
    public synchronized boolean addAll(Collection<? extends GameState.Direction> otherMoves) {
        if(!(otherMoves instanceof MoveQueue)) return false;

        MoveQueue castedOther = (MoveQueue)otherMoves;
        moves.addAll(castedOther.moves);
        return true;
    }
    
    @Override
    public String toString(){
    	StringBuffer buffer = new StringBuffer();
    	for(Direction move : moves){
    		buffer.append(move);
    		buffer.append(";");
    	}
    	
    	return buffer.toString();
    }
    
    /**In situations where the solver gets stuck, it's helpful to simply
     * try all available moves and see if they lead to a solution.
     * 
     * @param beginState the gamestate that the game began with after the first shuffle
     * @return the gamestate after making the next available move
     */
	public synchronized GameState addNextAvailableMove(GameState beginState, ArrayList<Point> frozenTiles, int tryCount){
		GameState endState = this.getStateAfterMoves(beginState);
		
		Log.d(TAG, "try " + tryCount + " for: \n" + endState.toString());
		
    	ArrayList<GameState.Direction> possibleMoves= endState.possibleMoves(frozenTiles);
    	
    	int size = possibleMoves.size();
    	if(size == 0){
    		Log.d(TAG, "Adding next available move but somehow there are no legal moves left");
    		Log.d(TAG, "BeginState" + beginState.toCSV());
    		Log.d(TAG, "EndState:" + endState.toCSV());
    		return null;
    	}
    	
    	if(tryCount >= size){
    		Log.d(TAG, "Tried all moves");
    		return null;
    	}
    	else{
    		Direction move =  possibleMoves.get(tryCount);
    		Log.d(TAG, "Choosing move: " + move);
    		this.add(move);
    		return endState.makeMove(move);
    	}
    }

    
    /**In situations where the solver gets stuck, it's helpful to make a
     * random move and try solving again
     * 
     * @param beginState the gamestate that the game began with after the first shuffle
     * @param frozenTiles any tiles that have already been solved and should not be moved
     * @return the gamestate that results from making a random move
     */
    public synchronized GameState addRandomMove(GameState beginState, ArrayList<Point> frozenTiles){
    	
    	GameState endState = this.getStateAfterMoves(beginState);
    	ArrayList<GameState.Direction> possibleMoves= endState.possibleMoves(frozenTiles);
    	
    	int size = possibleMoves.size();
    	if(size == 0){
    		Log.d(TAG, "Adding random move but somehow there are no legal moves left");
    		Log.d(TAG, "BeginState" + beginState.toCSV());
    		Log.d(TAG, "EndState:" + endState.toCSV());
    		return null;
    	}
    	
		Direction move = possibleMoves.get(new Random().nextInt(size));
		
    	Log.d(TAG, "Adding random move: " + move);
    	this.add(move);
    	
    	return endState.makeMove(move);
    }
    
    public synchronized void removeLastMove(){
    	moves.remove(moves.size() - 1);
    }
    
    @Override
    public synchronized void clear() {
        moves.removeAll(moves);
    }

    public synchronized GameState getStateAfterMoves(GameState beginState){
        GameState endState = beginState;

        for(GameState.Direction move : moves){
            endState = endState.makeMove(move);
        }
        return endState;
    }

/////////////////////////////////////////////////////////////////
//Boilerplate
/////////////////////////////////////////////////////////////////

    public boolean contains(Object o) {
        return false;
    }

    @Override
    public boolean containsAll(Collection<?> objects) {
        return false;
    }

    @Override
    public boolean isEmpty() {
        return moves.isEmpty();
    }


    public Iterator<GameState.Direction> iterator() {
        return moves.iterator();
    }

    @Override
    public boolean remove(Object o) {
        return false;
    }

    @Override
    public boolean removeAll(Collection<?> objects) {
        return false;
    }

    @Override
    public boolean retainAll(Collection<?> objects) {
        return false;
    }

    @Override
    public int size() {
        return moves.size();
    }

    @Override
    public Object[] toArray() {
        return new Object[0];
    }

    @Override
    public <T> T[] toArray(T[] ts) {
        return null;
    }

    @Override
    public boolean offer(GameState.Direction move) {
        return false;
    }

    @Override
    public GameState.Direction remove() {
        return null;
    }

    @Override
    public GameState.Direction element() {
        return null;
    }

    @Override
    public GameState.Direction peek() {
        return moves.get(0);
    }
}
