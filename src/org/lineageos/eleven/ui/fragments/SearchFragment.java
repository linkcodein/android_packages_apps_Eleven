/*
 * Copyright (C) 2024 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.lineageos.eleven.ui.fragments;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.lineageos.eleven.Config;
import org.lineageos.eleven.R;
import org.lineageos.eleven.adapters.SongListAdapter;
import org.lineageos.eleven.loaders.SearchLoader;
import org.lineageos.eleven.model.Song;
import org.lineageos.eleven.service.MusicPlaybackTrack;
import org.lineageos.eleven.utils.MusicUtils;
import org.lineageos.eleven.widgets.LoadingEmptyContainer;
import org.lineageos.eleven.widgets.NoResultsContainer;

import java.util.List;

public class SearchFragment extends BaseFragment implements
        LoaderManager.LoaderCallbacks<List<Song>> {

    private static final int SEARCH_LOADER = 0;
    private static final long SEARCH_DELAY_MS = 400;

    private RecyclerView mListView;
    private SongListAdapter mAdapter;
    private androidx.appcompat.widget.SearchView mSearchView;
    private LoadingEmptyContainer mLoadingEmptyContainer;
    private Handler mSearchHandler;
    private String mLastQuery;
    private List<Song> mSearchedSongs;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        mAdapter = createAdapter();
        mSearchHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void onViewCreated() {
        super.onViewCreated();

        mListView = mRootView.findViewById(R.id.list_base);
        mListView.setAdapter(mAdapter);
        mListView.setLayoutManager(new LinearLayoutManager(getContainingActivity()));
        mListView.setItemAnimator(new DefaultItemAnimator());

        mLoadingEmptyContainer = mRootView.findViewById(R.id.loading_empty_container);
        mLoadingEmptyContainer.showLoading();
        mLoadingEmptyContainer.setVisibility(View.VISIBLE);

        setupNoResultsContainer(mLoadingEmptyContainer.getNoResultsContainer());

        mSearchView = mRootView.findViewById(R.id.search_view);
        mSearchView.setIconified(false);
        mSearchView.requestFocus();
        mSearchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(final String query) {
                mSearchHandler.removeCallbacks(mSearchRunnable);
                startSearch(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(final String newText) {
                mSearchHandler.removeCallbacks(mSearchRunnable);
                if (newText != null && newText.length() >= 1) {
                    mSearchHandler.postDelayed(mSearchRunnable, SEARCH_DELAY_MS);
                } else {
                    mLoadingEmptyContainer.showNoResults();
                    mAdapter.unload();
                    mLastQuery = null;
                }
                return false;
            }
        });

        LoaderManager.getInstance(this).initLoader(SEARCH_LOADER, null, this);
    }

    private final Runnable mSearchRunnable = new Runnable() {
        @Override
        public void run() {
            if (mSearchView != null) {
                startSearch(mSearchView.getQuery().toString());
            }
        }
    };

    @Override
    public void onCreateOptionsMenu(@NonNull final Menu menu,
                                    @NonNull final MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.shuffle_all, menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == R.id.menu_shuffle_all) {
            if (mSearchedSongs != null && !mSearchedSongs.isEmpty()) {
                final long[] ids = new long[mSearchedSongs.size()];
                for (int i = 0; i < mSearchedSongs.size(); i++) {
                    ids[i] = mSearchedSongs.get(i).mSongId;
                }
                MusicUtils.playAll(ids, -1, -1, Config.IdType.NA, false);
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void startSearch(final String query) {
        if (query == null || query.trim().isEmpty()) {
            return;
        }
        mLastQuery = query.trim();
        mLoadingEmptyContainer.showLoading();
        LoaderManager.getInstance(this).restartLoader(SEARCH_LOADER, null, this);
    }

    private void onItemClick(final int position) {
        if (mSearchedSongs == null || mSearchedSongs.isEmpty()) {
            return;
        }
        final long[] ids = new long[mSearchedSongs.size()];
        for (int i = 0; i < mSearchedSongs.size(); i++) {
            ids[i] = mSearchedSongs.get(i).mSongId;
        }
        MusicUtils.playAll(ids, position, -1, Config.IdType.NA, false);
    }

    @NonNull
    @Override
    public Loader<List<Song>> onCreateLoader(final int id, final Bundle args) {
        return new SearchLoader(getContainingActivity(), mLastQuery);
    }

    @Override
    public void onLoadFinished(@NonNull final Loader<List<Song>> loader, final List<Song> data) {
        if (data == null || data.isEmpty()) {
            mSearchedSongs = null;
            mAdapter.unload();
            mLoadingEmptyContainer.setVisibility(View.VISIBLE);
            mLoadingEmptyContainer.showNoResults();
            return;
        }
        mSearchedSongs = data;
        mLoadingEmptyContainer.setVisibility(View.GONE);
        mAdapter.setData(data);
        mAdapter.setCurrentlyPlayingTrack(MusicUtils.getCurrentTrack());
    }

    @Override
    public void onLoaderReset(@NonNull final Loader<List<Song>> loader) {
        mAdapter.unload();
    }

    @Override
    public void onMetaChanged() {
        super.onMetaChanged();
        final MusicPlaybackTrack currentTrack = MusicUtils.getCurrentTrack();
        if (currentTrack != null) {
            mAdapter.setCurrentlyPlayingTrack(currentTrack);
        }
    }

    @Override
    public void restartLoader() {
        if (mLastQuery != null && !mLastQuery.isEmpty()) {
            LoaderManager.getInstance(this).restartLoader(SEARCH_LOADER, null, this);
        }
    }

    @Override
    public void onPlaylistChanged() {
        super.onPlaylistChanged();
    }

    private void setupNoResultsContainer(final NoResultsContainer empty) {
        final Context context = getContext();
        if (context != null) {
            TypedValue typedValue = new TypedValue();
            context.getTheme().resolveAttribute(R.attr.colorOnPrimaryContainer, typedValue, true);
            empty.setTextColor(typedValue.data);
        }
        empty.setMainText(R.string.search_no_results);
    }

    private SongListAdapter createAdapter() {
        return new SongListAdapter(
                getContainingActivity(),
                R.layout.list_item_normal,
                -1,
                Config.IdType.NA,
                this::onItemClick
        );
    }

    @Override
    protected String getTitle() {
        return getString(R.string.menu_search);
    }

    @Override
    protected int getLayoutToInflate() {
        return R.layout.search_fragment;
    }

    @Override
    protected boolean needsElevatedActionBar() {
        return false;
    }
}
