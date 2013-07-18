package com.example.n_puzzle;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;
import com.example.n_puzzle.DifficultyManager.DifficultyManagerCaller;
import com.example.n_puzzle.Solver.Heuristics.Heuristic;
import com.example.n_puzzle.Solver.Node;
import com.example.n_puzzle.Solver.MoveMaker;
import com.example.n_puzzle.Solver.MoveQueue;
import com.example.n_puzzle.Solver.SolutionStrategy;
import com.example.n_puzzle.Solver.SolveGameTask;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;

public class GamePlayActivity extends Activity implements DifficultyManagerCaller{
	public static final String TAG = GamePlayActivity.class.getSimpleName();
	public static final boolean DEBUG_VERBOSE = false;

	/**Run the preview activity only if it hasn't been run before*/
	public static boolean initialized = false;

	private LinearLayout mLayout;
	private GameGrid mGameGrid;
	private DifficultyManager.Difficulty mDifficulty;
	private TextView txt_countdown;
	private ImageView mPreviewImage;
    private Bitmap mBitmap;

    /**Solving members*/
    private MoveMaker mMoveMaker;
    private SolveGameTask mSolveTask;
    private SolutionStrategy mSolutionStrategy;
    private MoveQueue mMoveQueue;

	/**Number of seconds to countdown from during the preview*/
	private final long PREVIEW_COUNTDOWN = 4;
	private CountDownTimer mCountdown;

    /**The placements of the tiles will be saved in this 2d array*/
    private GameState mState = null;

	private int mNumMoves;


    @Override
	protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");

        //Remove title bar
        //this.requestWindowFeature(Window.FEATURE_NO_TITLE);

		super.onCreate(savedInstanceState);
		startPreview();			
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.gameplay_menu, menu);
        return true;
    }

/////////////////////////////////////////////////////////////////////////
//Preview and countdown
/////////////////////////////////////////////////////////////////////////
	/** The preview displays the solved image and a 3 second countdowntimer. When the timer
	 * finishes, the image is split into segments and shuffled, and the gameplay begins.
	 */
	private void startPreview(){
		setContentView(R.layout.activity_preview);
		init();
        startCountDown();
	}

	private void init() {
        mPreviewImage = (ImageView) findViewById(R.id.previewImage);
        try{
            //decode the image from the uri in the opening intent
            Intent i = getIntent();

            Uri data = i.getData();
            if(data == null){
                Toast.makeText(this, "N-Puzzle failed to get the image data",
                        Toast.LENGTH_LONG).show();
                backToSelect();
            }
            else{
                //fetch and decode the image data
                InputStream inputStream = getContentResolver().openInputStream(data);
                mBitmap = BitmapFactory.decodeStream(inputStream);

                //Set the image
                mPreviewImage.setImageBitmap(mBitmap);
            }
        }
        catch(FileNotFoundException f){
            Toast.makeText(this, "The file couldn't be loaded", Toast.LENGTH_SHORT).show();
            backToSelect();
        }

        //on out of memory error, alert user and go back to the image selection screen
        catch(OutOfMemoryError e){
        	Toast.makeText(this, "Image file is too big!", Toast.LENGTH_LONG).show();
			backToSelect();	
        }

        mNumMoves = 0;
	}

	
	/**Initialize and start the countdowntimer.
     * During the countdown, shuffle the image asynchronously.
	 * When the countdown finishes, start the gameplay.
	 */
	private void startCountDown() {
        txt_countdown = (TextView)findViewById(R.id.txt_countdown);
        txt_countdown.setText(Long.toString(PREVIEW_COUNTDOWN));
        mCountdown = new CountDownTimer(PREVIEW_COUNTDOWN * 1000, 1000){


        	@Override
			public void onTick(long arg0) {
				txt_countdown.setText(Long.toString(arg0/1000));	//update the countdown textview
			}
        	
			@Override
			public void onFinish() {
				mPreviewImage = null;
				System.gc();
				initialized = true; 	//countdown has run, don't run countdown again on startup
                    startGame();
			}
			        	
        }.start();

		mDifficulty = DifficultyManager.getCurrentDifficulty(this);

        //Get the default GameGrid state (tiles in reverse order)
        mState = new GameGrid(this, mBitmap, mDifficulty.getNumDivisions(), null).getGameState();
        shuffle();

	}

    private void shuffle(){
        new ShuffleTask(this).execute(mState);
    }

    public void onPostShuffle(GameState endState){
        if(DEBUG_VERBOSE){
            Log.d(TAG, "onPostShuffle, gamestate is \n" + endState);
        }

        mState = endState;
    }

/////////////////////////////////////////////////////////////////////////
//Start game
/////////////////////////////////////////////////////////////////////////
	private void startGame(){
		setContentView(R.layout.activity_gameplay);
		mLayout = (LinearLayout)findViewById(R.id.gameview);
		putGameGrid();
        restartSolving();
	}

	/**Create a game grid for this gameplay session.
	 * Pass the difficulty to determine the number of rows and columns to
	 * split the image into.
	 * If the program runs out of memory, catch the error and go 
	 * back to the image select screen.*/
	private void putGameGrid() {
		try{		
			mGameGrid = new GameGrid(this, mBitmap, mDifficulty.getNumDivisions(), mState);
			mLayout.addView(mGameGrid);
		}		
		//Go back to image selection if we run out of memory
		catch(OutOfMemoryError e){
			Toast.makeText(this, "Image file is too big!", Toast.LENGTH_LONG).show();
			backToSelect();
		}
	}



/////////////////////////////////////////////////////////////////////////
//Solver
/////////////////////////////////////////////////////////////////////////
    /**The game solver runs in a background thread.
     * Whenever the user makes a move the solver
     * must be restarted.
     */
    private void restartSolving(){
        Log.d(TAG, "Restarting solver");

        if(mSolveTask != null){
            mSolveTask.cancel(true);
            mSolveTask = null;
        }

        if(mMoveQueue == null){
            mMoveQueue = new MoveQueue();
        }

        mMoveQueue.clear();
        mState = mGameGrid.getGameState();
        mSolutionStrategy = new SolutionStrategy(mState, mDifficulty);

        solveNextTask(mState);

    }

    public void solveNextTask(GameState gameState){

        Heuristic heuristic = mSolutionStrategy.getNextGoal();
        ArrayList<Point> frozenTiles = mSolutionStrategy.getFrozenTiles();

        mSolveTask = new SolveGameTask(this, heuristic, frozenTiles, gameState);
        mSolveTask.execute();
    }

    public void processSolvedGoal(Node result) {

        Log.d(TAG, "Solver finished. adding moves to MoveQueue");


        mSolutionStrategy.processSolvedGoal();

        mMoveQueue.addAll(result.getMoveQueue());
        GameState endState = result.getEndState();

        /*If we're solving the end of a row, append the row end maneuver
        to the movequeue before solving the next goal.
         Also alter the ending gamestate to account for the maneuver being made*/
        if(mSolutionStrategy.readyForLineEndManeuver()){
            endState = appendLineEndManeuver(endState);
        }

        solveNextTask(endState);
    }

    public GameState appendLineEndManeuver(GameState endState){

        Log.d(TAG, "Adding line end maneuver");
        MoveQueue maneuver = mSolutionStrategy.lineEndManeuver();
        mMoveQueue.addAll(maneuver);

        /*The beginning state for the next goal will be different due
        to the line end maneuver that we're appending. So we virtually
        perform each move in the maneuver on the previous endstate,
        and use the final state as the beginning state for the next goal.
         */
        for(GameState.Direction move : maneuver){
            Log.d(TAG, "Adding move to movequeue: " + move);

            GameState nextState = endState.makeMove(move);

            if(nextState == null){
                Log.e(TAG, "Illegal move made, stopping solver");
                return null;
            }
            endState = nextState;
        }
        return endState;
    }


///////////////////////////////////////////////////////////////////////////
//Options
//////////////////////////////////////////////////////////////////////////
	@Override
	/**The gameplay options menu allows you to:
	 * 1) Reshuffle the game board.
	 * 2) Go back to the image select screen
	 * 3) Change the difficulty- this restarts the game at the new difficulty
	 * 4) Start an AI solver to solve the puzzle for you.*/
	public boolean onOptionsItemSelected(MenuItem item){
		int itemId = item.getItemId();

		switch(itemId){
			case R.id.reshuffle:
				Intent restartIntent = new Intent(this, GamePlayActivity.class);
                restartIntent.setData(getIntent().getData());
                this.finish();
                startActivity(restartIntent);
				break;
			case R.id.back_to_select:
				backToSelect();
				break;
			case R.id.change_difficulty:
				changeDifficulty();
				break;
			case R.id.solve:
                startMoveMaker();
				break;
		}
		return true;
	}

///////////////////////////////////////////////////////////////////////////
//Move Maker
//////////////////////////////////////////////////////////////////////////
    /**Have the solver AI solve the game in real time
     */
    private void startMoveMaker() {
        mGameGrid.toggleSolveMode();
        mMoveMaker = new MoveMaker(this, mMoveQueue);
        mMoveMaker.run();
    }

    public void makeMove(GameState.Direction move){

        if(move != null && mGameGrid != null){

            //check if the move is legal with the current gameState
            GameState gameState = mGameGrid.getGameState();
            ArrayList<GameState.Direction> legalMoves = gameState.possibleMoves(null);
            if(legalMoves.contains(move)){
                mGameGrid.makeMove(move);
            }

            //if the move is illegal, stop the solver
            else{
                Log.d(TAG, String.format("Stopping solver. An illegal move was added to the Queue" +
                        "\n%s\nMove %s", gameState.toString(), move));
                stopMoveMaker();
            }
        }
    }

    public void stopMoveMaker(){
        if(mMoveMaker != null){
            mMoveMaker.stop();
        }
    }


    void moveMade(){
        mNumMoves++;
        Log.d("movemade", "" + mNumMoves);
        if(!mGameGrid.isInSolveMode()){
            restartSolving();
        }
    }

    public void moveMakerFinished(){
        if(mGameGrid.isSolved()){
            winGame();
        }
        else{
            mGameGrid.toggleSolveMode();;
            restartSolving();
        }
    }

/////////////////////////////////////////////////////////////////////////
//Difficulty Change
/////////////////////////////////////////////////////////////////////////
    /**Create a dialog menu to change the difficulty.
	 * Save the selected difficulty in sharedPreferences.
	 * Restart the activity with the new difficulty*/
	private void changeDifficulty() {
		AlertDialog difficultyDialog = DifficultyManager.changeDifficulty(this);
		difficultyDialog.show();
	}
	
	
	@Override
	/**The DifficultyDialog calls this method when a user selects a new difficulty.
	 * Toast the difficulty change and restart the game.
	 * The game will retrieve the new difficulty from sharedPreferences when it starts.*/
	public void handleDifficultySelection(String newDifficulty) {
		DifficultyManager.makeDifficultyChangedToast(this, newDifficulty).show();
        Intent restartIntent = new Intent(this, GamePlayActivity.class);
        restartIntent.setData(getIntent().getData());
        startActivity(restartIntent);
	}


/////////////////////////////////////////////////////////////////////////
//End game
/////////////////////////////////////////////////////////////////////////
	/**Start the game over activity
	 */
	protected void winGame(){
        Toast.makeText(this, "Game Complete! It took " + mNumMoves +
                " moves", Toast.LENGTH_LONG).show();

        mGameGrid.toggleSolveMode();
	}

    /**Free the memory used by the GameGrid*/
    private void freeMemory() {
        if(mSolveTask != null){
            mSolveTask.cancel(true);
            mSolveTask = null;
        }
        mGameGrid = null;
        mPreviewImage = null;
        System.gc();
    }

	/**take the user back to the image select activity */
	public void backToSelect(){
		Intent i = new Intent(this, ImageSelectionActivity.class);
		this.finish();
		startActivity(i);
	}

	/** free image memory when this activity finishes.	 */
	@Override
	public void finish(){
		super.finish();
		freeMemory();
	}

    /**On orientation change we want to keep the location of the
     * grid tiles. Save the locations in mState and use that to
     * build the grid back to it's saved state in start game
     * @param newConfig
     */
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

        mState = mGameGrid.getGameState();
        mLayout.removeAllViews();
        this.freeMemory();
        this.startGame();
	}


/////////////////////////////////////////////////////////////////////////
//Boilerplate
/////////////////////////////////////////////////////////////////////////
    @Override
    public void onPause(){
        Log.d(TAG, "onPause");
        super.onPause();
    }

    @Override
    public void onStop(){
        Log.d(TAG, "onStop");
        super.onStop();
    }

    /** free image memory when this activity finishes.	 */
    @Override
    public void onDestroy(){
        Log.d(TAG, "OnDestroy");
        freeMemory();
        super.onDestroy();
    }

    @Override
    public void onRestart(){
        Log.d(TAG, "onRestart");
        super.onRestart();
    }

    @Override
    public void onStart(){
        Log.d(TAG, "onStart");
        super.onStart();
    }

    @Override
    public void onResume(){
        Log.d(TAG, "onResume");
        super.onResume();
    }

    @Override
    public void onSaveInstanceState(Bundle b){
        Log.d(TAG, "onSaveinstanceState");
        super.onSaveInstanceState(b);

    }

    @Override
    public void onRestoreInstanceState(Bundle b){
        Log.d(TAG, "onRestoreInstancestate");
        super.onRestoreInstanceState(b);
    }

}