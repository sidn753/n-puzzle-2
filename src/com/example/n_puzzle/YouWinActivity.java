package com.example.n_puzzle;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class YouWinActivity extends Activity{

	@Override
	protected void onCreate(Bundle savedInstance){
		super.onCreate(savedInstance);
		this.setContentView(R.layout.activity_gameover);
		
		TextView txt_numMoves = (TextView)findViewById(R.id.num_moves);
		int numMoves = getIntent().getExtras().getInt("numMoves");
		txt_numMoves.setText("Number of moves: " + numMoves);
	}

}
