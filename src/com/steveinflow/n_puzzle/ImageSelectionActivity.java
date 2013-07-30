/*******************************************************************************
 * This is the homescreen activity. It's a list of images to use for the game.
 * 
 * Dynamically loads the names of the image views in /res/drawable using reflection.
 * Formats the names of the images and loads them into a listview.
 * When the user clicks a list item, opens the gameplay activity and 
 * passes the image name.
 *   
 * @author StephenO
 *******************************************************************************/

package com.steveinflow.n_puzzle;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Locale;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import com.steveinflow.n_puzzle.DifficultyManager.DifficultyManagerCaller;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

public class ImageSelectionActivity extends SherlockListActivity implements DifficultyManagerCaller{
	public static final String TAG = ImageSelectionActivity.class.getSimpleName();
    final int REQUEST_CODE = 1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		getSupportActionBar().setTitle("");

		//Get the picture names using reflection
		ArrayList<String> pictureNames = getNames();
		ArrayList<String> pictureNamesUnformatted = getRawNames(pictureNames);

		//populate the list
        ImageAndTitleAdapter adapter = new ImageAndTitleAdapter(this,
                R.layout.list_item_picturename, R.id.thumbnail, R.id.image_title, pictureNames,
                pictureNamesUnformatted);

        this.setListAdapter(adapter);
		
		//On click, start the game using the image that was clicked
		this.getListView().setOnItemClickListener(new OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> parent, View v, int position,
					long id) {
                //Use the picture name to find the drawable's id from resources
                String pictureName = parent.getItemAtPosition(position).toString();
                int resId = getResources().getIdentifier(deformatImageName(pictureName), "drawable", getPackageName());

                Uri imageUri = getResourceUri(resId);
				openPreviewActivity(imageUri);
			}
		});
	}

    private Uri getResourceUri(int resId){

        Resources resources = getResources();
        return Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" +
                resources.getResourcePackageName(resId) +
                '/' + resources.getResourceTypeName(resId) +
                '/' + resources.getResourceEntryName(resId) );

    }

	/**Get the items that will fill the listview. Use reflection to dynamically load
	 * resource names
	 */
	private ArrayList<String> getNames() {
		
		/* Obtain a list of all the fields in the R.drawable class.
		* This is equivalent to the drawables that are in the drawable resource
		* folders.
		*/
		Field[] list = R.drawable.class.getFields();
		
		//populate arraylist with the image names
		ArrayList<String> imageNamesList = new ArrayList<String>();
		for(Field f : list){
			String imgName = f.getName();
			
			//skip the ic_launcher and border graphic
			if(imgName.startsWith("use_")){				
                imageNamesList.add(reformatImageName(imgName));
			}
		}
		
		return imageNamesList;
	}

    private ArrayList<String> getRawNames(ArrayList<String> names){
        ArrayList<String> rawNames = new ArrayList<String>();
        for(String name : names){
            rawNames.add(deformatImageName(name));
        }
        return rawNames;
    }

	
	/**reformat the image name- turn underscores into spaces and capitalize
	 * the first letter of each word.
	 * 
	 * @param input original image name
	 * @return a reformatted title for the image
	 */
	private String reformatImageName(String input) {
		String output = input.replace("use_", "");
		String[] split = output.split("_");
		StringBuffer result = new StringBuffer();
		for(String s : split){
			s = s.substring(0,1).toUpperCase(Locale.getDefault()) + s.substring(1);
			result.append(s);
			result.append(" ");
		}
		result.deleteCharAt(result.length()-1);
		
		return result.toString();
	}
	
	/**Turn a formatted image name back into a valid resource name
	 * - turn spaces back into underscores and make everything
	 * lower case
	 * 
	 * @param input formatted title with capitalization and space characters
	 * @return a valid resource name, all lower case and with underscores in place of spaces
	 */
	private String deformatImageName(String input){
		String output = input.replace(" ", "_");
		output = "use_" + output;
		output = output.toLowerCase(Locale.getDefault());
		return output;
	}

	/**Open the preview activity, pass the picture choice through the intent
	 * 
	 * @param uri the uri for the image resource that will be used in the n-puzzle game
	 */
	private void openPreviewActivity(Uri uri){
        Log.d(TAG, "Image URI:" + uri.toString());
		Intent i = new Intent(this, GamePlayActivity.class);
		i.setData(uri);
		startActivity(i);
	}
	
	@Override	
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getSupportMenuInflater().inflate(R.menu.image_select_menu, menu);
		return true;
	}	
	
	@Override
	/**Open the difficulty menu */
	public boolean onOptionsItemSelected(MenuItem item){
		if(item.getItemId() == R.id.select_change_difficulty){
            AlertDialog difficultyDialog = DifficultyManager.changeDifficulty(this);
            difficultyDialog.show();
            return true;
        }

        else if(item.getItemId() == R.id.select_gallery){
            pickImageFromGallery();
            return true;
        }

        return false;
	}

	@Override
	public void handleDifficultySelection(String newDifficulty) {
		DifficultyManager.makeDifficultyChangedToast(this, newDifficulty).show();
	}
	
	@Override
	public void onResume(){
		super.onResume();
	}

    /**This option sends out a get_content broadcast for type image.
     * This allows the user to select an image from somewhere else on the device,
     * such as the gallery.
     *
     * The user selects an image from the resulting activity, and
     * the selected image's url is returned in onActivityResult below.
     */
    public void pickImageFromGallery() {

        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, REQUEST_CODE);
    }

    /**The URL for the image selected in the pickImageFromGallery call
     * is returned in the data intent. We pass this url to the GameplayActivity
     * so that the selected image will be used for the game.
     *
     * @param requestCode matches the request code used in pickImageFromGallery
     * @param resultCode result of the child activity spawned by pickImageFromGallery.
     *                   Should be RESULT_OK
     * @param data the intent returned by pickImageFromGallery. Its extra data contains
     *             the uri of the image selected.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE && resultCode == SherlockActivity.RESULT_OK){

            Intent intent = new Intent(this, GamePlayActivity.class);
            intent.setData(data.getData());
            startActivity(intent);

        }
        super.onActivityResult(requestCode, resultCode, data);
    }
	
}
