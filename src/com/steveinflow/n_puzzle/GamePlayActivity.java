package com.steveinflow.n_puzzle;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.steveinflow.n_puzzle.DifficultyManager.DifficultyManagerCaller;
import com.steveinflow.n_puzzle.GameState.GameState;
import com.steveinflow.n_puzzle.GameState.ShuffleTask;
import com.steveinflow.n_puzzle.Image_Manipulation.GameGrid;
import com.steveinflow.n_puzzle.Solver.MoveMaker;
import com.steveinflow.n_puzzle.Solver.MoveQueue;
import com.steveinflow.n_puzzle.Solver.Node;
import com.steveinflow.n_puzzle.Solver.SolutionStrategy;
import com.steveinflow.n_puzzle.Solver.SolveGameTask;
import com.steveinflow.n_puzzle.Solver.Heuristics.Heuristic;

public class GamePlayActivity extends SherlockActivity implements DifficultyManagerCaller {
	public static final String TAG = GamePlayActivity.class.getSimpleName();
	
	/**Application-wide flag for more intense logging.*/
	public static boolean DEBUG_VERBOSE = false;
	
	/**If this var isn't the empty string, the game will start with the given gamestate.
	 * For debugging purposes only so be careful: it will crash the app if it's an invalid csv or 
	 * if the difficulty does not match the last saved difficulty.*/
	public static final String DEBUG_GAMESTATE_CSV = "";

	/**Run the preview activity only if it hasn't been run before*/
	public static boolean initialized = false;
	public boolean started = false;

	private LinearLayout mLayout;
	private GameGrid mGameGrid;
	private DifficultyManager.Difficulty mDifficulty;
	private TextView txt_countdown;
	private ImageView mPreviewImageView;
    private Bitmap mBitmap;

    /**Solving members*/
    private MoveMaker mMoveMaker;
    private SolveGameTask mSolveTask;
    private SolutionStrategy mSolutionStrategy;
    private MoveQueue mMoveQueue;

	/**Number of seconds to countdown from during the preview*/
	private final long PREVIEW_COUNTDOWN = 4000;
	private long millisUntilCountDownFinished = PREVIEW_COUNTDOWN;
	private CountDownTimer mCountdown;

    /**The placements of the tiles*/
    private GameState mState = null;
    
    private boolean isMoveMakerRunning;
    private boolean isMoveMakerEmpty = false;
    
    /**is the hint image showing?*/
    private boolean hinting = false;

	private int mNumMoves;


    @Override
	protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        
        getSupportActionBar().setTitle("");

        //Remove title bar
        //this.requestWindowFeature(Window.FEATURE_NO_TITLE);

		super.onCreate(savedInstanceState);
		startPreview();			
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getSupportMenuInflater().inflate(R.menu.gameplay_menu, (Menu) menu);
        return true;
    }

/////////////////////////////////////////////////////////////////////////
//Preview and countdown
/////////////////////////////////////////////////////////////////////////
	/** The preview displays the solved image and a 3 second countdowntimer. When the timer
	 * finishes, the image is split into segments and shuffled, and the gameplay begins.
	 */
	private void startPreview(){
		showImageHint();
        startCountDown();
	}
	
	/**Show the undivided unshuffled image*/
	private void showImageHint(){
		setContentView(R.layout.activity_preview);
		mPreviewImageView = (ImageView) findViewById(R.id.previewImage);
		if(mBitmap == null){
			if(!initializeBitmap()){
				return;
			}
		}
		
		//Set the image
		mPreviewImageView.setImageBitmap(mBitmap);
		
		if(started){
			final GamePlayActivity parent = this;
			mPreviewImageView.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View view) {
					Log.d(TAG, "preview image clicked");
					parent.showGame(true);
					hinting = false;
				}
			});
		}
	}

	/**Set the bitmap*/
	private boolean initializeBitmap() {
        try{
            //decode the image from the uri in the opening intent
            Intent i = getIntent();

            Uri data = i.getData();
            if(data == null){
                imageNotFoundToast();
                backToSelect();
                return false;
            }
            else{
                //fetch and decode the image data
                InputStream inputStream = getContentResolver().openInputStream(data);
                mBitmap = BitmapFactory.decodeStream(inputStream);
                
                if(mBitmap == null){
                	imageNotFoundToast();
                	backToSelect();
                	return false;
                }
            }
            
        }
        catch(FileNotFoundException f){
            imageNotFoundToast();
            backToSelect();
            return false;
        }

        //on out of memory error, alert user and go back to the image selection screen
        catch(OutOfMemoryError e){
        	Toast.makeText(this, "Ran out of memory!", Toast.LENGTH_LONG).show();
			backToSelect();	
			return false;
        }

        mNumMoves = 0;
        return true;
	}

	private void imageNotFoundToast() {
		Toast.makeText(this, "Sorry, couldn't fetch the image.", Toast.LENGTH_SHORT).show();
	}
	
	/**Initialize and start the countdowntimer.
     * During the countdown, shuffle the image asynchronously.
	 * When the countdown finishes, start the gameplay.
	 */
	private void startCountDown() {
        txt_countdown = (TextView)findViewById(R.id.txt_countdown);
        txt_countdown.setText(Long.toString(millisUntilCountDownFinished));
        mCountdown = new CountDownTimer(millisUntilCountDownFinished, 1000){

        	@Override
			public void onTick(long timeLeft) {
        		millisUntilCountDownFinished = timeLeft;
				txt_countdown.setText(Long.toString(timeLeft/1000));	//update the countdown textview
			}
        	
			@Override
			public void onFinish() {
				mPreviewImageView = null;
				System.gc();
				initialized = true; 	//countdown has run, don't run countdown again on startup
                startGame();
			}
			        	
        }.start();

		mDifficulty = DifficultyManager.getCurrentDifficulty(this);

        //Get the default GameGrid state (tiles in reverse order)
        mState = GameState.buildDefaultShuffle(mDifficulty.getNumDivisions());
        
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
        restartSolving();
    }

/////////////////////////////////////////////////////////////////////////
//Start game
/////////////////////////////////////////////////////////////////////////
	private void startGame(){
		showGame(false);
		
		if(!DEBUG_GAMESTATE_CSV.equals("")){
			mState = GameState.fromCSV(DEBUG_GAMESTATE_CSV);
			restartSolving();
		}
		
		Log.d(TAG, "BeginState for this game: " + mState.toCSV());
		
		putGameGrid();
		if(mGameGrid == null) return;
		
		if(mGameGrid.isSolved()) winGame();
		
		started = true;
	}

	private void showGame(boolean fromHint) {
		setContentView(R.layout.activity_gameplay);
		mLayout = (LinearLayout)findViewById(R.id.gameview);
		
		if(fromHint){
			mLayout.addView(mGameGrid);
			if(mGameGrid.isSolved()) winGame();
		}
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
			Log.d(TAG, e.toString());
			Toast.makeText(this, "Image file is too big!", Toast.LENGTH_LONG).show();
			backToSelect();
		}
		catch(IllegalArgumentException e){
			Log.d(TAG, e.toString());
			imageNotFoundToast();
			backToSelect();
		}
	}



/////////////////////////////////////////////////////////////////////////
//Solver
/////////////////////////////////////////////////////////////////////////
	
	private static long startSolvingTime;
    /**The game solver runs in a background thread.
     * Whenever the user makes a move the solver
     * must be restarted.
     */
    private void restartSolving(){
        Log.d(TAG, "Restarting solver");
        startSolvingTime = System.currentTimeMillis();

        if(mSolveTask != null){
            mSolveTask.cancel(true);
            mSolveTask = null;
        }

        if(mMoveQueue == null){
            mMoveQueue = new MoveQueue();
        }

        mMoveQueue.clear();
        mSolutionStrategy = new SolutionStrategy(mState, mDifficulty);

        solveNextTask(mState);
    }

    public void solveNextTask(GameState gameState){

        Heuristic heuristic = mSolutionStrategy.getNextGoal(gameState);
        ArrayList<Point> frozenTiles = mSolutionStrategy.getFrozenTiles();

        //solver finished
        if(heuristic == null){
        	Log.d(TAG, "Solver finished: it took " + (System.currentTimeMillis() - startSolvingTime) + " milliseconds");
        }
        else{
	        mSolveTask = new SolveGameTask(this, heuristic, frozenTiles, gameState);
	        mSolveTask.execute();
        }
    }
    
   
    
    /**If the solver fails for some reason, we add a random move and try again. */
    public void solverFailed(){
    	Log.d(TAG, "Solver failed.");
    	//TODO
    }

    /**SolveGameTask calls this method in onPostExecute.
     * The result node contains the beginning state, the end state, and the moves
     * in between.
     * 
     * @param result the solution returned by the solvegametask.
     */
    public void processSolvedGoal(Node result) {

        Log.d(TAG, "Solver finished. adding moves to MoveQueue");

        mSolutionStrategy.processSolvedGoal(result.getEndState());

        mMoveQueue.addAll(result.getMoveQueue());
        
        //If Solve it for me was clicked and we ran out of moves, restart the movemaker
        if(isMoveMakerEmpty){
        	Log.d(TAG, "New moves added to movequeue, restarting movemaker");
        	startMoveMaker();
        }
        
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
			case R.id.hint:
				hintSelected();
				break;
			case R.id.change_difficulty:
				changeDifficulty();
				break;
			case R.id.solve:
                startMoveMaker();
				break;
			case R.id.change_speed:
				MoveMaker.changeSpeed(this);
				break;
		}
		return true;
	}

	/**Show the hint image, or go back to the game if it's showing*/
	private void hintSelected() {
		if(!started) return;
		
		if(hinting){
			showGame(true);
			hinting = false;
		}
		else{
			stopMoveMaker();
			mLayout.removeAllViews();
			showImageHint();
			hinting = true;
		}
	}

///////////////////////////////////////////////////////////////////////////
//Move Maker
//////////////////////////////////////////////////////////////////////////
    /**Have the solver AI solve the game in real time
     */
    private void startMoveMaker() {
    	if(isMoveMakerRunning && !isMoveMakerEmpty) return;
    	
    	if(started){
    		Log.d(TAG, "Starting MoveMaker");
	        mGameGrid.setTouchEnabled(false);
	        mMoveMaker = new MoveMaker(this, mMoveQueue);
	        isMoveMakerRunning = true;
	        isMoveMakerEmpty = false;
	        mMoveMaker.run();
    	}
    }

    public void makeMove(GameState.Direction move){

        if(move != null && mGameGrid != null){

            //check if the move is legal with the current gameState
            GameState gameState = mGameGrid.getGameState();
            ArrayList<GameState.Direction> legalMoves = gameState.getLegalMoves(null);
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
            isMoveMakerRunning = false;
        }
    }

    public void moveMade(){
        mNumMoves++;
        if(DEBUG_VERBOSE) Log.d("movemade", "" + mNumMoves);
        mState = mGameGrid.getGameState();
        if(DEBUG_VERBOSE) Log.d(TAG, mState.toCSV());
        if(mGameGrid.isTouchEnabled()){
            restartSolving();
        }
    }

    public void moveMakerFinished(){
    	Log.d(TAG, "MoveMaker finished");
    	
        if(mGameGrid != null && mGameGrid.isSolved()){
            winGame();
        }
        else{
        	Log.d(TAG, "Movemaker finished: out of moves before solving.");
            isMoveMakerEmpty = true;
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
		this.finish();
        Intent restartIntent = new Intent(this, GamePlayActivity.class);
        restartIntent.setData(getIntent().getData());
        startActivity(restartIntent);
	}
	
	public void speedChanged(){
		
		if(isMoveMakerRunning){
			stopMoveMaker();
			startMoveMaker();
		}
	}


/////////////////////////////////////////////////////////////////////////
//End game
/////////////////////////////////////////////////////////////////////////
	/**Start the game over activity
	 */
	public void winGame(){
        Toast.makeText(this, "Game Complete! It took " + mNumMoves +
                " moves", Toast.LENGTH_LONG).show();

        mGameGrid.setTouchEnabled(false);
        mGameGrid.setSolved(true);
        mGameGrid.showBlankTile();
	}

    /**Free the memory used by the GameGrid*/
    private void freeMemory() {
        if(mSolveTask != null){
            mSolveTask.cancel(true);
            mSolveTask = null;
        }
        
        if(mMoveMaker != null){
        	stopMoveMaker();
        }
        
        if(mGameGrid != null) mGameGrid.freeMemory();
        mGameGrid = null;
        mPreviewImageView = null;
        if(mCountdown != null){
        	mCountdown.cancel();
        	mCountdown = null;
        }
        System.gc();
    }

	/**take the user back to the image select activity */
	public void backToSelect(){
		Intent i = new Intent(this, ImageSelectionActivity.class);
		this.finish();
		startActivity(i);
	}

	/** free image memory when this activity finishes.*/
	@Override
	public void finish(){
		freeMemory();
		super.finish();
	}

    /**On orientation change we want to keep the location of the
     * grid tiles. Save the locations in mState and use that to
     * build the grid back to it's saved state in start game
     * @param newConfig
     */
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

        if(mGameGrid != null) mState = mGameGrid.getGameState();       
        if(mLayout != null) mLayout.removeAllViews();
        
        this.freeMemory();
        
        if(started) this.startGame();
        else{
        	startCountDown();
        }
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