package com.steveinflow.n_puzzle;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by stepheno on 6/9/13.
 */
public class ImageAndTitleAdapter extends BaseAdapter{
    private LayoutInflater mInflater;
    private ArrayList<ImageView> images = new ArrayList<ImageView>();
    private Context mContext;
    private ArrayList<String> mPictureNames;
    private ArrayList<String> mRawPictureNames;
    private int mLayoutId;
    private int mImageViewId;
    private int mTextViewId;

    public ImageAndTitleAdapter(Context context, int layoutId,
                                int imageViewId, int textViewId, ArrayList<String> pictureNames,
                                ArrayList<String> rawPictureNames){
        this.mLayoutId = layoutId;
        this.mContext = context;
        this.mPictureNames = pictureNames;
        this.mImageViewId = imageViewId;
        this.mTextViewId = textViewId;
        this.mRawPictureNames = rawPictureNames;
        this.mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return mPictureNames.size();
    }

    @Override
    public Object getItem(int i) {
        return mPictureNames.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup viewGroup) {
        convertView = mInflater.inflate(mLayoutId, null);
        ImageView imageView = (ImageView) convertView.findViewById(mImageViewId);
        TextView textView = (TextView) convertView.findViewById(mTextViewId);

        String pictureName = mPictureNames.get(position);
        String rawPictureName = mRawPictureNames.get(position);
        imageView.setImageResource(getDrawable(rawPictureName, convertView));
        textView.setText(pictureName);

        return convertView;
    }

    private int getDrawable(String imageName, View convertView){
        //Use the picture name to find the drawable's id from resources
        int pictureId = convertView.getResources().getIdentifier(imageName,
                "drawable", mContext.getPackageName());

        Log.d("Adapter", "Fetching image ID: imageName: " + imageName + ": " + pictureId);
        return pictureId;
    }
}
