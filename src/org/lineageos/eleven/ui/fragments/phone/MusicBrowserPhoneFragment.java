/*
 * Copyright (C) 2012 Andrew Neal
 * Copyright (C) 2014 The CyanogenMod Project
 * Copyright (C) 2021 The LineageOS Project
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
package org.lineageos.eleven.ui.fragments.phone;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.lineageos.eleven.R;
import org.lineageos.eleven.adapters.PagerAdapter;
import org.lineageos.eleven.adapters.PagerAdapter.MusicFragments;
import org.lineageos.eleven.menu.CreateNewPlaylist;
import org.lineageos.eleven.slidinguppanel.SlidingUpPanelLayout;
import org.lineageos.eleven.ui.activities.SlidingPanelActivity;
import org.lineageos.eleven.ui.fragments.AlbumFragment;
import org.lineageos.eleven.ui.fragments.ArtistFragment;
import org.lineageos.eleven.ui.fragments.BaseFragment;
import org.lineageos.eleven.ui.fragments.SongFragment;
import org.lineageos.eleven.utils.MusicUtils;
import org.lineageos.eleven.utils.NavUtils;
import org.lineageos.eleven.utils.PreferenceUtils;
import org.lineageos.eleven.utils.SortOrder;

import java.util.ArrayList;
import java.util.List;

public class MusicBrowserPhoneFragment extends BaseFragment {
    public static final int INVALID_PAGE_INDEX = -1;

    private ViewPager mViewPager;

    private BottomNavigationView mBottomNavigation;

    private PagerAdapter mPagerAdapter;

    private PreferenceUtils mPreferences;

    private int mDefaultPageIdx = INVALID_PAGE_INDEX;

    private final List<MusicFragments> mVisibleFragments = new ArrayList<>();

    public MusicBrowserPhoneFragment() {
    }

    @Override
    protected int getLayoutToInflate() {
        return R.layout.fragment_music_browser_phone;
    }

    @Override
    protected String getTitle() {
        return getString(R.string.app_name);
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPreferences = PreferenceUtils.getInstance(getActivity());
    }

    @Override
    protected void onViewCreated() {
        super.onViewCreated();

        mViewPager = mRootView.findViewById(R.id.fragment_home_phone_pager);
        mBottomNavigation = getContainingActivity().findViewById(R.id.fragment_home_phone_pager_titles);

        buildVisibleFragments();
        setupPagerAdapter();
        setupNavigationListener();
        setupPageChangeListener();

        if (mDefaultPageIdx != INVALID_PAGE_INDEX) {
            navigateToPage(mDefaultPageIdx);
        } else {
            navigateToPage(mPreferences.getStartPage());
        }

        setHasOptionsMenu(true);
    }

    private void buildVisibleFragments() {
        mVisibleFragments.clear();
        if (mPreferences.getShowArtistTab()) {
            mVisibleFragments.add(MusicFragments.ARTIST);
        }
        if (mPreferences.getShowAlbumTab()) {
            mVisibleFragments.add(MusicFragments.ALBUM);
        }
        mVisibleFragments.add(MusicFragments.SONG);
        if (mPreferences.getShowPlaylistTab()) {
            mVisibleFragments.add(MusicFragments.PLAYLIST);
        }
    }

    private void setupPagerAdapter() {
        mPagerAdapter = new PagerAdapter(getActivity(), getChildFragmentManager());
        for (final MusicFragments fragment : mVisibleFragments) {
            mPagerAdapter.add(fragment.getFragmentClass(), null);
        }
        mViewPager.setAdapter(mPagerAdapter);
        mViewPager.setOffscreenPageLimit(Math.max(0, mPagerAdapter.getCount() - 1));

        Menu navMenu = mBottomNavigation.getMenu();
        navMenu.findItem(R.id.nav_artist).setVisible(mPreferences.getShowArtistTab());
        navMenu.findItem(R.id.nav_album).setVisible(mPreferences.getShowAlbumTab());
        navMenu.findItem(R.id.nav_playist).setVisible(mPreferences.getShowPlaylistTab());
    }

    private void setupNavigationListener() {
        mBottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            for (int i = 0; i < getParentFragmentManager().getBackStackEntryCount(); i++) {
                getParentFragmentManager().popBackStack();
            }
            ((SlidingPanelActivity) getContainingActivity())
                    .showPanel(SlidingPanelActivity.Panel.Browse);

            if (id == R.id.nav_artist) {
                int pos = mVisibleFragments.indexOf(MusicFragments.ARTIST);
                if (pos >= 0) mViewPager.setCurrentItem(pos);
            } else if (id == R.id.nav_album) {
                int pos = mVisibleFragments.indexOf(MusicFragments.ALBUM);
                if (pos >= 0) mViewPager.setCurrentItem(pos);
            } else if (id == R.id.nav_songs) {
                int pos = mVisibleFragments.indexOf(MusicFragments.SONG);
                if (pos >= 0) mViewPager.setCurrentItem(pos);
            } else if (id == R.id.nav_playist) {
                int pos = mVisibleFragments.indexOf(MusicFragments.PLAYLIST);
                if (pos >= 0) mViewPager.setCurrentItem(pos);
            }
            return true;
        });
    }

    private void setupPageChangeListener() {
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                updateBottomNavigationCheckedState(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        List<MusicFragments> previous = new ArrayList<>(mVisibleFragments);
        buildVisibleFragments();
        if (!previous.equals(mVisibleFragments)) {
            MusicFragments currentFragment = null;
            int currentItem = mViewPager.getCurrentItem();
            if (currentItem >= 0 && currentItem < previous.size()) {
                currentFragment = previous.get(currentItem);
            }

            mPagerAdapter = new PagerAdapter(getActivity(), getChildFragmentManager());
            for (final MusicFragments fragment : mVisibleFragments) {
                mPagerAdapter.add(fragment.getFragmentClass(), null);
            }
            mViewPager.setAdapter(mPagerAdapter);
            mViewPager.setOffscreenPageLimit(Math.max(0, mPagerAdapter.getCount() - 1));

            Menu navMenu = mBottomNavigation.getMenu();
            navMenu.findItem(R.id.nav_artist).setVisible(mPreferences.getShowArtistTab());
            navMenu.findItem(R.id.nav_album).setVisible(mPreferences.getShowAlbumTab());
            navMenu.findItem(R.id.nav_playist).setVisible(mPreferences.getShowPlaylistTab());

            int targetPosition;
            if (currentFragment != null && mVisibleFragments.contains(currentFragment)) {
                targetPosition = mVisibleFragments.indexOf(currentFragment);
            } else {
                int songPos = mVisibleFragments.indexOf(MusicFragments.SONG);
                targetPosition = songPos >= 0 ? songPos : 0;
            }

            mViewPager.setCurrentItem(targetPosition, false);
            updateBottomNavigationCheckedState(targetPosition);
        }
    }

    private void updateBottomNavigationCheckedState(int viewPagerPosition) {
        if (viewPagerPosition >= 0 && viewPagerPosition < mVisibleFragments.size()) {
            MusicFragments selected = mVisibleFragments.get(viewPagerPosition);
            int navId;
            switch (selected) {
                case ARTIST:
                    navId = R.id.nav_artist;
                    break;
                case ALBUM:
                    navId = R.id.nav_album;
                    break;
                case SONG:
                    navId = R.id.nav_songs;
                    break;
                case PLAYLIST:
                    navId = R.id.nav_playist;
                    break;
                default:
                    navId = R.id.nav_songs;
                    break;
            }
            MenuItem item = mBottomNavigation.getMenu().findItem(navId);
            if (item != null && item.isVisible()) {
                item.setChecked(true);
            }
        }
    }

    public void setDefaultPageIdx(final int pageIdx) {
        mDefaultPageIdx = pageIdx;
        navigateToPage(mDefaultPageIdx);
    }

    private void navigateToPage(final int idx) {
        if (idx != INVALID_PAGE_INDEX && mViewPager != null && !mVisibleFragments.isEmpty()) {
            if (idx >= 0 && idx < MusicFragments.values().length) {
                MusicFragments target = MusicFragments.values()[idx];
                int pos = mVisibleFragments.indexOf(target);
                if (pos >= 0) {
                    mViewPager.setCurrentItem(pos);
                }
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        int currentItem = mViewPager.getCurrentItem();
        if (currentItem >= 0 && currentItem < mVisibleFragments.size()) {
            mPreferences.setStartPage(mVisibleFragments.get(currentItem).ordinal());
        }
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull final Menu menu) {
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull final Menu menu,
                                    @NonNull final MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        inflater.inflate(R.menu.shuffle_all, menu);
        inflater.inflate(R.menu.search, menu);
        if (isArtistPage()) {
            inflater.inflate(R.menu.artist_sort_by, menu);
        } else if (isAlbumPage()) {
            inflater.inflate(R.menu.album_sort_by, menu);
        } else if (isSongPage()) {
            inflater.inflate(R.menu.song_sort_by, menu);
        } else if (isPlaylistPage()) {
            inflater.inflate(R.menu.new_playlist, menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int id = item.getItemId();
        if (id == R.id.menu_shuffle_all) {
            // Shuffle all the songs
            MusicUtils.shuffleAll(getActivity());
        } else if (id == R.id.menu_sort_by_az) {
            if (isArtistPage()) {
                mPreferences.setArtistSortOrder(SortOrder.ArtistSortOrder.ARTIST_A_Z);
                getArtistFragment().refresh();
            } else if (isAlbumPage()) {
                mPreferences.setAlbumSortOrder(SortOrder.AlbumSortOrder.ALBUM_A_Z);
                getAlbumFragment().refresh();
            } else if (isSongPage()) {
                mPreferences.setSongSortOrder(SortOrder.SongSortOrder.SONG_A_Z);
                getSongFragment().refresh();
            }
        } else if (id == R.id.menu_sort_by_za) {
            if (isArtistPage()) {
                mPreferences.setArtistSortOrder(SortOrder.ArtistSortOrder.ARTIST_Z_A);
                getArtistFragment().refresh();
            } else if (isAlbumPage()) {
                mPreferences.setAlbumSortOrder(SortOrder.AlbumSortOrder.ALBUM_Z_A);
                getAlbumFragment().refresh();
            } else if (isSongPage()) {
                mPreferences.setSongSortOrder(SortOrder.SongSortOrder.SONG_Z_A);
                getSongFragment().refresh();
            }
        } else if (id == R.id.menu_sort_by_artist) {
            if (isAlbumPage()) {
                mPreferences.setAlbumSortOrder(SortOrder.AlbumSortOrder.ALBUM_ARTIST);
                getAlbumFragment().refresh();
            } else if (isSongPage()) {
                mPreferences.setSongSortOrder(SortOrder.SongSortOrder.SONG_ARTIST);
                getSongFragment().refresh();
            }
        } else if (id == R.id.menu_sort_by_album) {
            if (isSongPage()) {
                mPreferences.setSongSortOrder(SortOrder.SongSortOrder.SONG_ALBUM);
                getSongFragment().refresh();
            }
        } else if (id == R.id.menu_sort_by_year) {
            if (isAlbumPage()) {
                mPreferences.setAlbumSortOrder(SortOrder.AlbumSortOrder.ALBUM_YEAR);
                getAlbumFragment().refresh();
            } else if (isSongPage()) {
                mPreferences.setSongSortOrder(SortOrder.SongSortOrder.SONG_YEAR);
                getSongFragment().refresh();
            }
        } else if (id == R.id.menu_sort_by_duration) {
            if (isSongPage()) {
                mPreferences.setSongSortOrder(SortOrder.SongSortOrder.SONG_DURATION);
                getSongFragment().refresh();
            }
        } else if (id == R.id.menu_sort_by_number_of_songs) {
            if (isArtistPage()) {
                mPreferences
                        .setArtistSortOrder(SortOrder.ArtistSortOrder.ARTIST_NUMBER_OF_SONGS);
                getArtistFragment().refresh();
            } else if (isAlbumPage()) {
                mPreferences.setAlbumSortOrder(SortOrder.AlbumSortOrder.ALBUM_NUMBER_OF_SONGS);
                getAlbumFragment().refresh();
            }
        } else if (id == R.id.menu_sort_by_number_of_albums) {
            if (isArtistPage()) {
                mPreferences.setArtistSortOrder(SortOrder.ArtistSortOrder.ARTIST_NUMBER_OF_ALBUMS);
                getArtistFragment().refresh();
            }
        } else if (id == R.id.menu_sort_by_filename) {
            if (isSongPage()) {
                mPreferences.setSongSortOrder(SortOrder.SongSortOrder.SONG_FILENAME);
                getSongFragment().refresh();
            }
        } else if (id == R.id.menu_new_playlist) {
            if (isPlaylistPage()) {
                CreateNewPlaylist.getInstance(new long[0])
                        .show(getChildFragmentManager(), "CreatePlaylist");
            }
        } else if (id == R.id.menu_search) {
            NavUtils.openSearch(getActivity());
        } else {
            return super.onOptionsItemSelected(item);
        }

        return true;
    }

    @Override
    protected boolean needsElevatedActionBar() {
        // our view pager already has elevation
        return false;
    }

    private boolean isArtistPage() {
        int pos = mViewPager.getCurrentItem();
        return pos >= 0 && pos < mVisibleFragments.size()
                && mVisibleFragments.get(pos) == MusicFragments.ARTIST;
    }

    public ArtistFragment getArtistFragment() {
        int pos = mVisibleFragments.indexOf(MusicFragments.ARTIST);
        if (pos >= 0) {
            return (ArtistFragment) mPagerAdapter.getFragment(pos);
        }
        return null;
    }

    private boolean isAlbumPage() {
        int pos = mViewPager.getCurrentItem();
        return pos >= 0 && pos < mVisibleFragments.size()
                && mVisibleFragments.get(pos) == MusicFragments.ALBUM;
    }

    public AlbumFragment getAlbumFragment() {
        int pos = mVisibleFragments.indexOf(MusicFragments.ALBUM);
        if (pos >= 0) {
            return (AlbumFragment) mPagerAdapter.getFragment(pos);
        }
        return null;
    }

    private boolean isSongPage() {
        int pos = mViewPager.getCurrentItem();
        return pos >= 0 && pos < mVisibleFragments.size()
                && mVisibleFragments.get(pos) == MusicFragments.SONG;
    }

    public SongFragment getSongFragment() {
        int pos = mVisibleFragments.indexOf(MusicFragments.SONG);
        if (pos >= 0) {
            return (SongFragment) mPagerAdapter.getFragment(pos);
        }
        return null;
    }

    @Override
    public void restartLoader() {
    }

    private boolean isPlaylistPage() {
        int pos = mViewPager.getCurrentItem();
        return pos >= 0 && pos < mVisibleFragments.size()
                && mVisibleFragments.get(pos) == MusicFragments.PLAYLIST;
    }
}
