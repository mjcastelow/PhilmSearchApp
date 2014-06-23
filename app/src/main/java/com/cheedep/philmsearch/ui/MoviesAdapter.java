package com.cheedep.philmsearch.ui;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.cheedep.philmsearch.R;
import com.cheedep.philmsearch.io.FlushedInputStream;
import com.cheedep.philmsearch.model.Movie;
import com.cheedep.philmsearch.services.HttpRetriever;

import org.w3c.dom.Text;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Created by Chandu on 6/22/2014.
 */
public class MoviesAdapter extends ArrayAdapter<Movie> {

    private Activity context;
    private ArrayList<Movie> movies;

    private HttpRetriever httpRetriever = new HttpRetriever();

    public MoviesAdapter(Context context, int resource, ArrayList<Movie> objects) {
        super(context, resource, objects);
        this.context = (Activity)context;
        movies = objects;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;

        if (view == null) {
            view = context.getLayoutInflater().inflate(R.layout.movie_data_row, null);
        }


        Movie movie = movies.get(position);

        if(movie != null){
            TextView nameTextView = (TextView)view.findViewById(R.id.name_text_view);
            nameTextView.setText(movie.name);

            TextView ratingTextView = (TextView)view.findViewById(R.id.rating_text_view);
            ratingTextView.setText(movie.rating);

            TextView releaseDateTextView = (TextView)view.findViewById(R.id.released_text_view);
            releaseDateTextView.setText(movie.released);

            ImageView imageView = (ImageView)view.findViewById(R.id.movie_thumb_icon);
            String url = movie.retrieveThumbnailUrl();

            if(url!=null) {
                Bitmap bitmap = fetchBitmapFromCache(url);
                if(bitmap == null){
                    new BitmapDownloaderTask(imageView).execute(url);
                }
                else
                    imageView.setImageBitmap(bitmap);
            }
            else
                imageView.setImageBitmap(null);
        }

        return view;
    }

    private LinkedHashMap<String, Bitmap> bitmapCache = new LinkedHashMap<String, Bitmap>();

    private void addBitmapToCache(String url, Bitmap bitmap) {
        if (bitmap != null) {
            synchronized (bitmapCache) {
                bitmapCache.put(url, bitmap);
            }
        }
    }

    private Bitmap fetchBitmapFromCache(String url){
        synchronized (bitmapCache) {
            final Bitmap bitmap = bitmapCache.get(url);
            if (bitmap != null) {
                // Bitmap found in cache
                // Move element to first position, so that it is removed last
                bitmapCache.remove(url);
                bitmapCache.put(url, bitmap);
                return bitmap;
            }
        }
        return null;
    }

    private class BitmapDownloaderTask extends AsyncTask<String, Void, Bitmap>{

        private String url;
        private final WeakReference<ImageView> imageViewReference;

        public BitmapDownloaderTask(ImageView imageView){
            imageViewReference = new WeakReference<ImageView>(imageView);
        }

        @Override
        protected Bitmap doInBackground(String... strings) {
            String url = strings[0];
            InputStream inputStream = httpRetriever.retrieveStream(url);
            if(inputStream == null)
                return null;
            return BitmapFactory.decodeStream(new FlushedInputStream(inputStream));
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if(isCancelled())
                bitmap = null;
            addBitmapToCache(url, bitmap);
            if(imageViewReference != null){
                ImageView imageView = imageViewReference.get();
                if(imageView != null){
                    imageView.setImageBitmap(bitmap);
                }
            }
        }
    }
}