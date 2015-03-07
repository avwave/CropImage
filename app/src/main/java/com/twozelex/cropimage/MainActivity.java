package com.twozelex.cropimage;

import android.graphics.Bitmap;
import android.net.Uri;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.os.Build;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.assist.ImageSize;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import java.net.URI;


public class MainActivity extends ActionBarActivity {
    public ImageLoader imageLoader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            DisplayImageOptions defaultOptions = new DisplayImageOptions.Builder()
                    .cacheInMemory(true)
                    .cacheOnDisk(true)
                    .considerExifParams(true)
                    .imageScaleType(ImageScaleType.EXACTLY)
                    .bitmapConfig(Bitmap.Config.RGB_565)
                    .build();
            ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(this)
                    .defaultDisplayImageOptions(defaultOptions)
                    .imageDecoder(new ZLXImageDecoder(true))
                    .writeDebugLogs()
                    .build();
            imageLoader = ImageLoader.getInstance();
            imageLoader.init(config);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        private ZLXCropView mImage;

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);

            mImage = (ZLXCropView) rootView.findViewById(R.id.crop_image);
            ImageLoader imageLoader = ImageLoader.getInstance();
            DisplayImageOptions imageOptions = new DisplayImageOptions.Builder()
                    .cacheOnDisk(true)
                    .considerExifParams(true)
                    .imageScaleType(ImageScaleType.EXACTLY)
                    .bitmapConfig(Bitmap.Config.RGB_565)
                    .build();
            imageLoader.displayImage("https://s-media-cache-ak0.pinimg.com/736x/3d/39/57/3d39577b7c62a9e6b7f49166127861c6.jpg", mImage, imageOptions, new SimpleImageLoadingListener(){

                @Override
                public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                    MainActivity activity = (MainActivity)getActivity();
                    ImageSize size = new ImageSize(100, 100);
                    ZLXImageDecoder.PhotoFileInfo info =  ZLXImageDecoder.urlToSizeMap.get(imageUri);
                    switch (info.getRotation()) {
                        case 0:
                        case 180:
                            size = new ImageSize(size.getHeight(), size.getWidth());
                            break;
                        case 90:
                        case 270:
                            break;
                    }
                    mImage.init(loadedImage, info, size, false);

                    super.onLoadingComplete(imageUri, view, loadedImage);
                }
            });
            return rootView;
        }
    }
}
