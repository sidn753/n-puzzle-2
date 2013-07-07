package com.example.n_puzzle;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.widget.Toast;

/**The difficulty manager has methods for changing, storing, and retrieving difficulty
 * preferences.
 */
public abstract class DifficultyManager {
			
	/**An activity must implement this interface in order to call
	 * changeDifficulty(). The changeDifficulty dialog calls this method 
	 * at the end of the onClick event to pass control back to the calling class. */
	public interface DifficultyManagerCaller{
		public void handleDifficultySelection(String newDifficulty);
	}
	
	
	/**Return the last difficulty stored in shared preferences.
	 * Use medium as the default.
	 * 
	 * @param callingActivity The activity that is calling this method
	 * @return the last difficulty stored in sharedPreferences, or medium by default.
	 */
	private static String getCurrentDifficultyString(Activity callingActivity){
		String difficultyPrefKey = callingActivity.getString(R.string.difficulty_preference_key);
		return getSharedPref(callingActivity).getString(difficultyPrefKey, Difficulty.DIFFICULTY_MEDIUM.toString());
	}

    public static Difficulty getCurrentDifficulty(Activity callingActivity){
        return Difficulty.getDifficultyFromString(getCurrentDifficultyString(callingActivity));
    }
	
	/**Get the number of image rows and columns to make based on the current difficulty.
	 * 
	 * @param callingActivity The activity calling this method
	 * @return The number of divisions associated with that difficulty
	 */
	public static int getNumDivisions(Activity callingActivity){
		String lastDifficulty = getCurrentDifficultyString(callingActivity);
		return Difficulty.getDifficultyFromString(lastDifficulty).getNumDivisions();
	}
	
	
	/**Return a toast indicating the difficulty has been changed.
	 * @param callingActivity The activity calling this method
	 * @param difficulty the difficulty to display
	 * @return a toast indicating the difficulty has been changed
	 */
	public static Toast makeDifficultyChangedToast(Activity callingActivity, String difficulty){
		String text = "Difficulty changed to " + difficulty;
		return Toast.makeText(callingActivity, text, Toast.LENGTH_SHORT);
	}
	
	/**Create a dialog menu to change the difficulty.
	 * Save the selected difficulty in sharedPreferences. 
	 * 
	 * @param callingActivity The activity that is calling this method
	 * @return An AlertDialog containing difficulty options
	 */
	public static AlertDialog changeDifficulty(Activity callingActivity) {
		
		/*The calling activity must implement DifficultyManagerCaller to handle
		the callback in the onclicklistener*/
		if(!(callingActivity instanceof DifficultyManagerCaller)){
			throw new IllegalStateException(callingActivity.getClass().toString() +
					" must implement DifficultyManagerCaller " +
					"in order to call DifficultyManager.changeDifficulty().");
		}
		
		//Build a dialog to display difficulty options in a menu
		AlertDialog.Builder builder = new AlertDialog.Builder(callingActivity);	
		builder.setTitle(R.string.difficulty_menu_title);
		
		//dialog onClickListener
		builder.setItems(R.array.difficulties, 
				new DifficultyManager.DifficultyDialogueOnClickListener(callingActivity));
		
		return builder.create();		
		
	}
	
	
	/**Get sharedPreferences for the calling activity
	 * 
	 * @param callingActivity The activity which is calling this method
	 * @return The SharePreferences for the application calling this method
	 */
	protected static SharedPreferences getSharedPref(Activity callingActivity){
		String sharedPrefFileName = callingActivity.getString(R.string.n_puzzle_preferences_filename);

		return callingActivity.getSharedPreferences(sharedPrefFileName, Context.MODE_PRIVATE);
	}
	
	
	/**A DialogOnClickListener for the Difficulty dialog menu.
	 * It stores an activity.
	 * The activity is used to getByLocation and edit shared preferences
	 * to save the selected difficulty.
	 *  
	 * @author StephenO
	 */
	final static class DifficultyDialogueOnClickListener implements DialogInterface.OnClickListener{
		private Activity mCallingActivity;
		
		/**Pass in the calling activity*/
		public DifficultyDialogueOnClickListener(Activity callingActivity){
			mCallingActivity = callingActivity;
		}
	
		
		@Override
		/**When a selection is made from the difficulty menu,
		 * save the selected difficulty to shared preferences
		 */
		public void onClick(DialogInterface dialog, int which) {
			
			String difficultyPrefKey = mCallingActivity.getString(R.string.difficulty_preference_key);
			
			//Commit the selection to sharedpreferences
			SharedPreferences.Editor editor = getSharedPref(mCallingActivity).edit();
			String newDifficulty = Difficulty.getDifficultyFromMenuIndex(which).toString();
			editor.putString(difficultyPrefKey, newDifficulty).commit();
			
			/*This call passes control back to the calling thread
			 * and lets it handle the new difficulty. */
			DifficultyManagerCaller dmc = (DifficultyManagerCaller)mCallingActivity;
			dmc.handleDifficultySelection(newDifficulty);
		}
	}
	
	
	/**The difficulty enum allows switching between different representations of a given difficulty.
	 * It stores 3 pieces of data associated with a difficulty:
	 * menuIndex: where the difficulty appears in the select difficulty menu
	 * numDivisions: the number of rows and columns to split the image into to start the game
	 * title: the string representation of this difficulty
	 * 
	 * @author StephenO
	 */
	public enum Difficulty{
		DIFFICULTY_EASY(0, 3, "Easy"),
		DIFFICULTY_MEDIUM(1, 4, "Medium"),
		DIFFICULTY_HARD(2, 5, "Hard");
		
		/**The index of this difficulty in the choose difficulty menu*/
		private final int menuIndex;	
		/**The degree of division to divide the image to, e.g. for EASY: 3 = split the image into 3 x 3 matrix*/
		private final int numDivisions;
		/**String representation of this difficulty*/
		private final String title;
		
		/**Constructor*/
		Difficulty(int menuIndex, int numDivisions, String title){
			this.numDivisions = numDivisions;
			this.menuIndex = menuIndex;
			this.title = title;
		}		
		
		public int getMenuIndex(){ return menuIndex;}
		public int getNumDivisions(){ return numDivisions;}
		public String toString(){ return title;}
		
		
		/**Returns a difficulty associated with the given index. 
		 * @param which the index of the item in the select difficulty menu
		 * @return the difficulty associated with the given menu index
		 */
		public static Difficulty getDifficultyFromMenuIndex(int which){
			switch(which){
			case 0:	return DIFFICULTY_EASY;
			case 1: return DIFFICULTY_MEDIUM;
			case 2: return DIFFICULTY_HARD;
			default: return DIFFICULTY_MEDIUM;
			}	
		}
		
		
		/**Return the difficulty associated with the given string*/
		public static Difficulty getDifficultyFromString(String diffString){
			if (diffString.equals(DIFFICULTY_EASY.toString())){
				return DIFFICULTY_EASY;
			}
			else if(diffString.equals(DIFFICULTY_HARD.toString())){
				return DIFFICULTY_HARD;
			}
			else{
				return DIFFICULTY_MEDIUM;
			}
		}
	}
	
}
