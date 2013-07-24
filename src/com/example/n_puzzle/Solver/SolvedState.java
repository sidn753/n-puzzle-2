package com.example.n_puzzle.Solver;

import com.example.n_puzzle.GameState.GameState;

import java.util.ArrayList;

/**
 * The correct layout holder contains information on what the correct rows and cols are for each
 * image slice- where it should be in the solved image.
 *
 * Created by stepheno on 6/24/13.
 */
public class SolvedState extends GameState {
    public static final String TAG = "SolvedState";

    public ArrayList<CorrectPlace> correctPlaces;


    /**Create all of the slices in order, left to right, up to down,
     *  and set their rows and columns.
     *
     * @param numDivisions how many rows, or how many columns
     */
    public SolvedState(int numDivisions){
        super(numDivisions);
        correctPlaces = new ArrayList<CorrectPlace>();

        int index = 0;
        for(int i = 0; i < numDivisions; i++){
            for(int j = 0; j < numDivisions; j++){
                super.mPlaces[i][j] = index;
                correctPlaces.add(new CorrectPlace(index, i, j));
                index++;
            }
        }

    }

    /**Looks at every placement in the gamestate,
     * and sums up how far each tile is from where it should be.
     *
     * @param gameState the current placement of tiles
     * @return sum of how far each tile is from where it should be.
     */
    public int sumOfDistances(GameState gameState){
        int sum = 0;
        int divisions = gameState.getNumDivisions();

        for(int row = 0; row < divisions; row++){
            for(int col = 0; col < divisions; col++){
                int place = gameState.getByLocation(row, col);

                //Blank tile is -1, skip it
                if(place == -1){
                    continue;
                }

                int distance = getDistance(place, row, col);

                /*Log.d(TAG, String.format("Getting distance for row %s, col %s." +
                        "Tile index is %s. Distance is %s.",
                        row, col, place, distance));*/

                sum+= distance;
            }
        }
        return sum;
    }

    /**Return how far the given slice is from it's correct position
     *
     * @param index the position of the slice, in order of creation
     * @param cRow the current row of the slice
     * @param cCol the current col of the slice
     * @return total distance of the slice from it's correct position
     */
    private int getDistance(int index, int cRow, int cCol){
        int distance =  correctPlaces.get(index).getDistance(cRow,cCol);
        return distance;
    }

    public class CorrectPlace {
        public int row;
        public int col;
        public int place;

        public CorrectPlace(int index, int row, int col){
            this.row = row;
            this.col = col;
        }


        public int getDistance(int cRow, int cCol){
            return Math.abs(cRow - this.row) + Math.abs(cCol - this.col);
        }

    }
}
