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
package org.lineageos.eleven.loaders;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio;

import org.lineageos.eleven.model.Song;
import org.lineageos.eleven.utils.Lists;
import org.lineageos.eleven.utils.MusicUtils;

import java.util.ArrayList;
import java.util.List;

public class SearchLoader extends WrappedAsyncTaskLoader<List<Song>> {

    private final String mQuery;

    public SearchLoader(final Context context, final String query) {
        super(context);
        mQuery = query;
    }

    @Override
    public List<Song> loadInBackground() {
        final ArrayList<Song> result = Lists.newArrayList();

        if (mQuery == null || mQuery.trim().isEmpty()) {
            return result;
        }

        final String[] projection = new String[]{
                Audio.Media._ID,
                Audio.Media.TITLE,
                Audio.Media.ARTIST,
                Audio.Media.ALBUM_ID,
                Audio.Media.ALBUM,
                Audio.Media.DURATION,
                Audio.Media.YEAR,
        };

        String escapedQuery = mQuery.trim()
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("%", "\\%")
                .replace("_", "\\_");
        final String likeValue = "%" + escapedQuery + "%";
        final String selection = MusicUtils.MUSIC_ONLY_SELECTION
                + " AND ( " + Audio.Media.TITLE + " LIKE ? ESCAPE '\\'"
                + " OR " + Audio.Media.ARTIST + " LIKE ? ESCAPE '\\'"
                + " OR " + Audio.Media.ALBUM + " LIKE ? ESCAPE '\\'"
                + " )";
        final String[] selectionArgs = new String[]{likeValue, likeValue, likeValue};

        Cursor cursor = null;
        try {
            cursor = getContext().getContentResolver().query(
                    MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL),
                    projection,
                    selection,
                    selectionArgs,
                    Audio.Media.TITLE + " ASC");

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    final long id = cursor.getLong(0);
                    final String songName = cursor.getString(1);
                    final String artist = cursor.getString(2);
                    final long albumId = cursor.getLong(3);
                    final String album = cursor.getString(4);
                    final long duration = cursor.getLong(5);
                    final int durationInSecs = (int) duration / 1000;
                    final int year = cursor.getInt(6);

                    result.add(new Song(id, songName, artist, album, albumId,
                            durationInSecs, year));
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return result;
    }
}
