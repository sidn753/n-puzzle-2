package com.steveinflow.n_puzzle;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

/**
 * Created by stepheno on 6/23/13.
 */
public class PictureReceiver extends Activity {

    public static final String TAG = PictureReceiver.class.getSimpleName();

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);

        Log.d(TAG, "picture intent received");

        Intent shareIntent  = getIntent();
        Uri data = shareIntent.getParcelableExtra(Intent.EXTRA_STREAM);

        Intent intent = new Intent(this, GamePlayActivity.class);
        intent.setData(data);
        startActivity(intent);

    }
}
