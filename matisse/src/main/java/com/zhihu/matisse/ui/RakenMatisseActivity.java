package com.zhihu.matisse.ui;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import com.zhihu.matisse.R;
import com.zhihu.matisse.internal.entity.Album;
import com.zhihu.matisse.internal.entity.Item;
import com.zhihu.matisse.internal.entity.SelectionSpec;
import com.zhihu.matisse.internal.model.AlbumCollection;
import com.zhihu.matisse.internal.model.SelectedItemCollection;
import com.zhihu.matisse.internal.ui.RakenMediaSelectionFragment;
import com.zhihu.matisse.internal.ui.adapter.AlbumMediaAdapter;

import java.util.ArrayList;

public class RakenMatisseActivity extends AppCompatActivity implements
        AlbumCollection.AlbumCallbacks,
        RakenMediaSelectionFragment.SelectionProvider,
        AlbumMediaAdapter.CheckStateListener,
        AlbumMediaAdapter.OnMediaClickListener,
        SwipeRefreshLayout.OnRefreshListener {

    public static final String EXTRA_RESULT_SELECTION = "extra_result_selection";
    public static final String EXTRA_RESULT_SELECTION_PATH = "extra_result_selection_path";
    private final AlbumCollection mAlbumCollection = new AlbumCollection();

    private SelectedItemCollection mSelectedCollection = new SelectedItemCollection(this);
    private SelectionSpec mSpec;

    private View mContainer;
    private TextView mTitleView;

    private View mProgressView;

    private MenuItem mDoneView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSpec = SelectionSpec.getInstance();
        if (!mSpec.hasInited) {
            setResult(RESULT_CANCELED);
            finish();
            return;
        }
        setContentView(R.layout.activity_raken_matisse);

        if (mSpec.needOrientationRestriction()) {
            setRequestedOrientation(mSpec.orientation);
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_back);
        }

        mContainer = findViewById(R.id.container);
        mTitleView = findViewById(R.id.title);
        mProgressView = findViewById(R.id.progress);

        mSelectedCollection.onCreate(savedInstanceState);

        mAlbumCollection.onCreate(this, this);
        mAlbumCollection.onRestoreInstanceState(savedInstanceState);
        mAlbumCollection.loadAlbums();
        mProgressView.setVisibility(View.VISIBLE);

        updateToolbar();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_raken_matisse, menu);
        mDoneView = menu.findItem(R.id.done);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mSelectedCollection.onSaveInstanceState(outState);
        mAlbumCollection.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mAlbumCollection.onDestroy();
        mSpec.onCheckedListener = null;
        mSpec.onSelectedListener = null;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (item.getItemId() == R.id.done) {
            Intent result = new Intent();
            ArrayList<Uri> selectedUris = (ArrayList<Uri>) mSelectedCollection.asListOfUri();
            result.putParcelableArrayListExtra(EXTRA_RESULT_SELECTION, selectedUris);
            ArrayList<String> selectedPaths = (ArrayList<String>) mSelectedCollection.asListOfString();
            result.putStringArrayListExtra(EXTRA_RESULT_SELECTION_PATH, selectedPaths);
            setResult(RESULT_OK, result);
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        setResult(Activity.RESULT_CANCELED);
        super.onBackPressed();
    }

    private void updateToolbar() {
        final int selectedCount = mSelectedCollection.count();
        mTitleView.setText(getString(R.string.title_selected, selectedCount));

        if (selectedCount == 0 && mDoneView != null) {
            mDoneView.setEnabled(false);
        } else if (mDoneView != null) {
            mDoneView.setEnabled(true);
        }
    }

    @Override
    public void onAlbumLoad(final Cursor cursor) {
        // select default album.
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {

            @Override
            public void run() {
                cursor.moveToPosition(mAlbumCollection.getCurrentSelection());
                Album album = Album.valueOf(cursor);
                onAlbumSelected(album);

                mProgressView.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onAlbumReset() {
    }

    private void onAlbumSelected(Album album) {
        if (album.isAll() && album.isEmpty()) {
            mContainer.setVisibility(View.GONE);
        } else {
            mContainer.setVisibility(View.VISIBLE);
            Fragment fragment = RakenMediaSelectionFragment.newInstance(album);
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.container, fragment, RakenMediaSelectionFragment.class.getSimpleName())
                    .commitAllowingStateLoss();
        }
    }

    @Override
    public void onUpdate() {
        updateToolbar();

        if (mSpec.onSelectedListener != null) {
            mSpec.onSelectedListener.onSelected(
                    mSelectedCollection.asListOfUri(), mSelectedCollection.asListOfString());
        }
    }

    @Override
    public void onMediaClick(Album album, Item item, int adapterPosition) {
    }

    @Override
    public SelectedItemCollection provideSelectedItemCollection() {
        return mSelectedCollection;
    }

    @Override
    public void onRefresh() {
        mAlbumCollection.loadAlbums();
    }
}