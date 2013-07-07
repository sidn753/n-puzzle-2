package com.example.n_puzzle.Solver;

import com.example.n_puzzle.GameState;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;

/**
 * Created by stepheno on 6/30/13.
 */
public class MoveQueue implements Queue<GameState.Direction> {
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
    public synchronized void clear() {
        moves.removeAll(moves);
    }

    public GameState getStateAfterMoves(GameState beginState){
        GameState endState = null;

        for(GameState.Direction move : moves){
            endState = beginState.makeMove(move);
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
