package com.cesarvaliente.tldrlegal;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v17.leanback.app.BackgroundManager;
import android.support.v17.leanback.app.DetailsFragment;
import android.support.v17.leanback.widget.Action;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.ClassPresenterSelector;
import android.support.v17.leanback.widget.DetailsOverviewRow;
import android.support.v17.leanback.widget.DetailsOverviewRowPresenter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ImageCardView;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnActionClickedListener;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v4.app.ActivityOptionsCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

public class VideoDetailsFragment extends DetailsFragment {
    private static final String TAG = "VideoDetailsFragment";

    private static final int ACTION_WATCH_TRAILER = 1;
    private static final int ACTION_RENT = 2;
    private static final int ACTION_BUY = 3;

    private static final int DETAIL_THUMB_WIDTH = 274;
    private static final int DETAIL_THUMB_HEIGHT = 274;

    private static final int NUM_COLS = 10;

    private static final String MOVIE = "Movie";

    private Movie mSelectedMovie;

    private Drawable mDefaultBackground;
    private Target mBackgroundTarget;
    private DisplayMetrics mMetrics;
    private DetailsOverviewRowPresenter mDorPresenter;
    private DetailRowBuilderTask mDetailRowBuilderTask;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate DetailsFragment");
        super.onCreate(savedInstanceState);

        mDorPresenter =
                new DetailsOverviewRowPresenter(new DetailsDescriptionPresenter());

        BackgroundManager backgroundManager = BackgroundManager.getInstance(getActivity());
        backgroundManager.attach(getActivity().getWindow());
        mBackgroundTarget = new PicassoBackgroundManagerTarget(backgroundManager);

        mDefaultBackground = getResources().getDrawable(R.drawable.default_background);

        mMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(mMetrics);

        mSelectedMovie = (Movie) getActivity().getIntent().getSerializableExtra(MOVIE);
        mDetailRowBuilderTask = (DetailRowBuilderTask) new DetailRowBuilderTask().execute(mSelectedMovie);
        mDorPresenter.setSharedElementEnterTransition(getActivity(),
                DetailsActivity.SHARED_ELEMENT_NAME);

        updateBackground(mSelectedMovie.getBackgroundImageURI());
        setOnItemViewClickedListener(new ItemViewClickedListener());

    }

    @Override
    public void onStop() {
        mDetailRowBuilderTask.cancel(true);
        super.onStop();
    }

    private class DetailRowBuilderTask extends AsyncTask<Movie, Integer, DetailsOverviewRow> {
        @Override
        protected DetailsOverviewRow doInBackground(Movie... movies) {
            mSelectedMovie = movies[0];

            DetailsOverviewRow row = new DetailsOverviewRow(mSelectedMovie);
            try {
                Bitmap poster = Picasso.with(getActivity())
                                       .load(mSelectedMovie.getCardImageUrl())
                                       .resize(Utils.convertDpToPixel(getActivity().getApplicationContext(), DETAIL_THUMB_WIDTH),
                                               Utils.convertDpToPixel(getActivity().getApplicationContext(), DETAIL_THUMB_HEIGHT))
                                       .centerCrop()
                                       .get();
                row.setImageBitmap(getActivity(), poster);
            } catch (IOException e) {
            }

            row.addAction(new Action(ACTION_WATCH_TRAILER, getResources().getString(
                    R.string.watch_trailer_1), getResources().getString(R.string.watch_trailer_2)));
            row.addAction(new Action(ACTION_RENT, getResources().getString(R.string.rent_1),
                    getResources().getString(R.string.rent_2)));
            row.addAction(new Action(ACTION_BUY, getResources().getString(R.string.buy_1),
                    getResources().getString(R.string.buy_2)));
            return row;
        }

        @Override
        protected void onPostExecute(DetailsOverviewRow detailRow) {
            ClassPresenterSelector ps = new ClassPresenterSelector();
            // set detail background and style
            mDorPresenter.setBackgroundColor(getResources().getColor(R.color.detail_background));
            mDorPresenter.setStyleLarge(true);
            mDorPresenter.setOnActionClickedListener(new OnActionClickedListener() {
                @Override
                public void onActionClicked(Action action) {
                    if (action.getId() == ACTION_WATCH_TRAILER) {
                        Intent intent = new Intent(getActivity(), PlaybackOverlayActivity.class);
                        intent.putExtra(getResources().getString(R.string.movie), mSelectedMovie);
                        intent.putExtra(getResources().getString(R.string.should_start), true);
                        startActivity(intent);
                    } else {
                        Toast.makeText(getActivity(), action.toString(), Toast.LENGTH_SHORT).show();
                    }
                }
            });

            ps.addClassPresenter(DetailsOverviewRow.class, mDorPresenter);
            ps.addClassPresenter(ListRow.class,
                    new ListRowPresenter());

            ArrayObjectAdapter adapter = new ArrayObjectAdapter(ps);
            adapter.add(detailRow);

            String subcategories[] = {
                    getString(R.string.related_movies)
            };
            List<Movie> list = MovieList.list;
            Collections.shuffle(list);
            ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(new CardPresenter());
            for (int j = 0; j < NUM_COLS; j++) {
                listRowAdapter.add(list.get(j % 5));
            }

            HeaderItem header = new HeaderItem(0, subcategories[0], null);
            adapter.add(new ListRow(header, listRowAdapter));

            setAdapter(adapter);
        }

    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {

            if (item instanceof Movie) {
                Movie movie = (Movie) item;
                Log.d(TAG, "Item: " + item.toString());
                Intent intent = new Intent(getActivity(), DetailsActivity.class);
                intent.putExtra(DetailsActivity.MOVIE, movie);

                Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                        getActivity(),
                        ((ImageCardView) itemViewHolder.view).getMainImageView(),
                        DetailsActivity.SHARED_ELEMENT_NAME).toBundle();
                getActivity().startActivity(intent, bundle);
            }
        }
    }

    protected void updateBackground(URI uri) {
        Log.d(TAG, "uri" + uri);
        Log.d(TAG, "metrics" + mMetrics.toString());
        Picasso.with(getActivity())
               .load(uri.toString())
               .resize(mMetrics.widthPixels, mMetrics.heightPixels)
               .error(mDefaultBackground)
               .into(mBackgroundTarget);
    }

}
