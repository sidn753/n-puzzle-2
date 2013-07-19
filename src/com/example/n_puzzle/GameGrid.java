package com.example.n_puzzle;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;
import android.view.DragEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.RelativeLayout;

/**Data structure to hold a set of bitmap sections.
 * 
 * @author StephenO
 */
public class GameGrid extends RelativeLayout implements OnClickListener, View.OnLongClickListener,
        View.OnDragListener {

	public static final String TAG = GameGrid.class.getSimpleName();	
	
	private ArrayList<ImageSegmentView> mSegmentViews;
	private Bitmap mImage;
	private int mDivisions;
	private int mImageWidth, mImageHeight;
	private int mViewTotalWidth, mViewTotalHeight;
	private int mViewSegmentWidth, mViewSegmentHeight;
    private ImageSegmentView[][] mSegmentGrid;
	private Point blankTile;

    /**If the user has clicked the solveGoal button, we have to change the onclick listener*/
    private boolean touchEnabled = true;
    private boolean solved = false;


	/**The GamePlayActivity containing this Game Grid*/
	private GamePlayActivity mContext;
	
	public GameGrid(GamePlayActivity context, Bitmap bitmap, int numDivisions, GameState state) 
			throws IllegalArgumentException{
		super(context);
		mContext = context;
		
		if(bitmap == null) throw new IllegalArgumentException("gamegrid image is null");
        mImage = bitmap;

		mDivisions = numDivisions;
		mSegmentViews = new ArrayList<ImageSegmentView>();
        mSegmentGrid = new ImageSegmentView[numDivisions][numDivisions];
			
		//set variables for the image and the 
		setDimensionVariables();
				
		//Set the view group's total dimensions
		LayoutParams gameGridParams = new LayoutParams(mViewTotalWidth, mViewTotalHeight);
		this.setLayoutParams(gameGridParams);
		
		//build the imageviews that we'll move around
		buildSegments(context);
		
		//Place the tiles in reverse order to ensure solvability
        if(state == null){
		    putDefaultPlacement();
        }

        //place views according to the passed correctPlaces parameter
        else{
            putSegmentsInPlace(state);
        }
	}



	/**Build cropped segments of the image that can be swapped around
	 * 
	 * @param context
	 */
	private void buildSegments(Context context){
		ImageSegmentBuilder builder = new ImageSegmentBuilder();
        mSegmentGrid = new ImageSegmentView[mDivisions][mDivisions];

        int index = 0;

		for(int row = 0; row < mDivisions; row++){
			for(int col = 0; col < mDivisions; col++){

                if(row == mDivisions -1 && col == mDivisions -1) break;   //leave the last tile blank

				ImageSegmentView segmentView = new ImageSegmentView(context, row, col, index);
				Bitmap segmentBitmap = builder.buildImageSegment(row, col);
				segmentView.setImageBitmap(segmentBitmap);
				segmentView.setBackgroundResource(R.drawable.border);
				
				segmentView.setOnClickListener(this);
                segmentView.setOnLongClickListener(this);
                mSegmentGrid[row][col] = segmentView;
				mSegmentViews.add(segmentView);

                index++;
			}			
		}

        //the blank tile begins in the bottom right
		blankTile = new Point(mDivisions - 1, mDivisions - 1);
	}
	
	
	/**Get the pixel parameters (x, y, w, h) where the given segment should be placed.
	 * 
	 * @param row the row of the segment
	 * @param col the column of the segment
	 * @return the layout parameters for where the segment should be placed
	 */
	private LayoutParams getPlacementParams(int row, int col) {
		LayoutParams imageParams = new LayoutParams(mViewSegmentWidth, mViewSegmentHeight);
		imageParams.leftMargin = col * mViewSegmentWidth;
		imageParams.topMargin = row * mViewSegmentHeight;
		return imageParams;
	}


    /**Per the specification- Most shuffles of the tiles result in unsolvable puzzles.
     * The simplest method to ensure solvability is simply to place the tiles in reverse.
     * There is one small swap necessary for odd tiled puzzles, detailed below.
     *  http://cdn.cs76.net/2012/spring/projects/android-staff/android-staff.pdf
     */
    public void putDefaultPlacement(){
        putSegmentsInPlace(GameState.buildDefaultShuffle(mDivisions));

    }

    /**Place the tiles in the places indicated in the 2d array passed in
     * The integer 0 represents the tile that would be in the top left corner of the correctly assembled picture.
     * The integers increment from left to right, and then up to down.
     * The integer -1 represents the blank tile.
     *
     * @param state 2d array of integers representing tile placements.
     */
    public void putSegmentsInPlace(GameState state){
        this.removeAllViews();

        Log.d(TAG, "Putting gameState: \n" + state.toString());

        for(int row = 0; row < mDivisions; row++){
            for(int col = 0; col < mDivisions; col++){
                int place = state.getByLocation(row, col);

                if(place == -1){ //-1 indicates that the blank tile
                    blankTile.y = row;
                    blankTile.x = col;

                    continue;
                }

                ImageSegmentView thisSegment = mSegmentViews.get(place);

                LayoutParams params = getPlacementParams(row, col);
                this.addView(thisSegment, params);

                mSegmentGrid[row][col] = thisSegment;
                thisSegment.moveTo(row, col);
            }
        }

    }
	
	/***Direction a an image segment view to a new location
	 * 
	 * @param segmentView the segment to move
	 * @param row the row to move to
	 * @param col the column to move to
	 * @return the moved segment is returned for chaining
	 */
	private ImageSegmentView moveSegment(ImageSegmentView segmentView, int row, int col){
        //Get the blank tile's coords, update blank tile to the touched view's space
        int newRow = blankTile.y;
        int newCol = blankTile.x;
        blankTile.y = row;
        blankTile.x = col;

        //change the grid's coordinate model
        mSegmentGrid[row][col] = null;
        mSegmentGrid[newRow][newCol] = segmentView;

        //Change the view's internal coordinates
        segmentView.moveTo(newRow, newCol);

        //remove and replace the view in the grid view
		LayoutParams params = getPlacementParams(newRow, newCol);
		this.removeView(segmentView);
		this.addView(segmentView, params);

        mContext.moveMade();    //alert the parent that a move was made
		return segmentView;

	}


	/**Set all of the dimension measurements
	 */
	public void setDimensionVariables(){
		//Fetch the image and its dimensions
		mImageHeight = mImage.getHeight();
		mImageWidth = mImage.getWidth();
		

		//Fetch the dimensions of the display
        int displayWidth = getResources().getDisplayMetrics().widthPixels;
		Log.d(TAG, "DisplayWidth: " + displayWidth);

        int displayHeight = getResources().getDisplayMetrics().heightPixels;
		Log.d(TAG, "DisplayHeight: " + displayHeight);


		//Find the ratio of the display width to the image's width and of the
		//display height to the image's height
		double wRatio = (double)(displayWidth) /mImageWidth;
        Log.d(TAG, "wRatio: " + wRatio);

		double hRatio = (double)(displayHeight) / mImageHeight;
        Log.d(TAG, "hRatio: " + hRatio);


		/*If the width ratio is smaller, then we should use the display width 
		 * as the width of the view, and scale the height according to the width ratio.
		 * If the height ratio is smaller, then we should use the display height as
		 * the view height and scale the width according to the height ratio.
		 */		
		boolean wRatioSmaller = (wRatio < hRatio);		


		if(wRatioSmaller){
			mViewTotalWidth = displayWidth;
			mViewTotalHeight = (int) (mImageHeight * wRatio);
		}
		else{
			mViewTotalWidth = (int) (mImageWidth * hRatio);
			mViewTotalHeight = displayHeight;
		}

		//Set the dimensions of the view segments that we'll be moving around
		mViewSegmentWidth = mViewTotalWidth / mDivisions;
		mViewSegmentHeight = mViewTotalHeight / mDivisions;

	}

	@Override
	/**Direction an image segment into the blank tile if it is adjacent to the blank tile
	 */
	public void onClick(View touchedView) {
        Log.d(TAG, "onClick");
        if(!solved)        
        touched((ImageSegmentView) touchedView);

    }

    @Override
    public boolean onLongClick(View touchedView) {
        Log.d(TAG, "onLongClick");
        touched((ImageSegmentView) touchedView);
        return true;
    }


    private View mDraggedView;
    @Override
    public boolean onDrag(View touchedView, DragEvent dragEvent) {
        if(dragEvent.getAction() == DragEvent.ACTION_DRAG_STARTED){
            Log.d(TAG, "drag started");
            mDraggedView = touchedView;
        }
        else if(dragEvent.getAction() == DragEvent.ACTION_DRAG_ENDED){
            Log.d(TAG, "drag ended");
            touched((ImageSegmentView) mDraggedView);
        }
        return true;
    }

    private void touched(ImageSegmentView touchedView) {
        //If the game is in solveMode, stop solvemode
        if(!solved && !touchEnabled){
            setTouchEnabled(true);
            mContext.stopMoveMaker();
            return;
        }

        //Otherwise make a move based on the view that was touched
        ImageSegmentView touchedSegment = (ImageSegmentView) touchedView;
        int row = touchedSegment.getCurrentRow();
        int col = touchedSegment.getCurrentCol();
        int blankRow = blankTile.y;
        int blankCol = blankTile.x;


        //is the touched tile adjacent to the blank tile?
        int distance = Math.abs(blankRow - row) + Math.abs(blankCol - col);
        boolean adjacent = (distance == 1);

        if(adjacent){
              this.moveSegment(touchedSegment, row, col);
        }

        //end the game if everything is in its correct position
        if(isSolved()){
            mContext.winGame();
        }
    }


    public void makeMove(GameState.Direction move){
        int row = blankTile.y;
        int col = blankTile.x;
        switch(move){
            case UP:
                row--;
                break;
            case RIGHT:
                col++;
                break;
            case DOWN:
                row++;
                break;
            case LEFT:
                col--;
                break;

        }
        ImageSegmentView segmentToMove = mSegmentGrid[row][col];
        this.moveSegment(segmentToMove, row, col);
    }


	/**Returns true iff all of the imageSegments are in their "correct" positions,
	 * i.e., they are in the correctPlaces they were in the original unshuffled image
	 *
	 * @return true if all the ImageSegments are in their correct positions
	 */
	public boolean isSolved(){
		if(solved) return true;
		
		for(ImageSegmentView segment: mSegmentViews){
			if(!segment.inOriginalPosition()){
				solved = false;
				return false;
			}
		}
		
		solved = true;
		return solved;
	}


    /**Create a gamestate to save tile placements
     */
    public GameState getGameState(){
        int[][] places = new int[mDivisions][mDivisions];

        for(int row = 0; row < mDivisions; row++){
            for(int col = 0; col < mDivisions; col++){

                ImageSegmentView segmentView = mSegmentGrid[row][col];
                if(row == blankTile.y && col == blankTile.x){
                    places[row][col] = -1;
                }
                else if(segmentView !=null){
                    places[row][col] = segmentView.getCorrectIndex();
                }
                else{
                    Log.d(TAG, String.format(
                            "Null segmentView that is not the blank tile, exiting. " +
                                    "Blanktile = (y, x) %s. %s; y, x = " +
                                    "%s, %s", blankTile.y, blankTile.x, row, col));

                    throw new IllegalStateException(TAG + ": Null segment view not blank tile.");
                }
            }
        }
        return new GameState(places);
    }


    private ImageSegmentView getSegment(int row, int col){
        return mSegmentGrid[row][col];
    }

    public void setTouchEnabled(boolean enabled){
        touchEnabled = enabled;
    }

    public boolean isTouchEnabled(){
        return touchEnabled;
    }

    public void setSolved(boolean solved){
    	this.solved = solved;
    }
    
    /**recycle all the segments*/
    public void freeMemory(){
    	for(ImageSegmentView segment : mSegmentViews){
    		BitmapDrawable bmd = (BitmapDrawable)segment.getDrawable();
    		bmd.getBitmap().recycle();
    	}
    }


    /**Helper class- builds cropped segments of the image associated with
	 * this game grid 
	 */
	private class ImageSegmentBuilder{
		
		private int mImageSegmentWidth, mImageSegmentHeight;
		
		protected ImageSegmentBuilder(){
			
			mImageSegmentWidth = mImageWidth/ mDivisions;
			mImageSegmentHeight = mImageHeight/ mDivisions;
		}
		
		/**Returns a cropped segment of the image_select_menu image. The width of the
		 * crop is based on the difficulty rating associated with the parent game grid.
		 * Row and col determine which division of the image to crop, with (0,0) at
		 * the top left corner.
		 * 
		 * @param row the row of the segment
		 * @param col the column of the segment
		 * @return a cropped segment of the image
		 */
		public Bitmap buildImageSegment(int row, int col){
			
			Bitmap segmentBitmap = Bitmap.createBitmap(mImage,  col * mImageSegmentWidth, 
					row * mImageSegmentHeight,  mImageSegmentWidth, mImageSegmentHeight);
			return segmentBitmap;
		}
	}


}

