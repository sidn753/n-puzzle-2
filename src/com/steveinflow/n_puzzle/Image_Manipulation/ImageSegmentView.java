package com.steveinflow.n_puzzle.Image_Manipulation;

import android.content.Context;
import android.view.ViewDebug.ExportedProperty;
import android.widget.ImageView;

public class ImageSegmentView extends ImageView {

	private int mCorrectRow, mCorrectCol;
	private int mCurrentRow, mCurrentCol;
    private int mCorrectIndex;


    public int getCorrectIndex() {
        return mCorrectIndex;
    }


	private ImageSegmentView(Context context) {
		super(context);
	}
	
	public ImageSegmentView(Context context, int row, int col, int index){
		super(context);
		this.mCorrectRow = row;
		this.mCorrectCol = col;

        this.mCorrectIndex = index;

	}
	
	@Override
	@ExportedProperty(category = "drawing")
	public float getAlpha() {
		return super.getAlpha();
	}


	/**place the image in a different row or column
	 * 
	 * @param row the row to move to
	 * @param col the column to move to
	 * @return returns itself
	 */	
	public ImageSegmentView moveTo(int row, int col){
		this.mCurrentRow = row;
		this.mCurrentCol = col;
		return this;
	}
	
	/**returns true iff the view is in its "correct" position, i.e. the place
	 * where it was in the original unshuffled image
	 * 
	 * @return true if image is in the "correct" position, false otherwise
	 */
	public boolean inOriginalPosition() {
		return mCurrentRow == mCorrectRow && mCurrentCol == mCorrectCol;  
	}
	
	public int getCurrentRow(){
		return mCurrentRow;
	}
	
	public int getCurrentCol(){
		return mCurrentCol;
	}
	
}
