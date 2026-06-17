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
package org.lineageos.eleven.utils;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.audiofx.AudioEffect;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.widget.Toast;

import org.lineageos.eleven.Config;
import org.lineageos.eleven.R;
import org.lineageos.eleven.ui.activities.HomeActivity;
import org.lineageos.eleven.ui.activities.SettingsActivity;

import java.util.List;

/**
 * Various navigation helpers.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public final class NavUtils {

    private NavUtils() {
    }

    /**
     * Opens the profile of an artist.
     *
     * @param context    The {@link Activity} to use.
     * @param artistName The name of the artist
     */
    public static void openArtistProfile(final Activity context, final String artistName) {
        // Create a new bundle to transfer the artist info
        final Bundle bundle = new Bundle();
        bundle.putLong(Config.ID, MusicUtils.getIdForArtist(context, artistName));
        bundle.putString(Config.MIME_TYPE, MediaStore.Audio.Artists.CONTENT_TYPE);
        bundle.putString(Config.ARTIST_NAME, artistName);

        // Create the intent to launch the profile activity
        final Intent intent = new Intent(context, HomeActivity.class);
        intent.setAction(HomeActivity.ACTION_VIEW_ARTIST_DETAILS);
        intent.putExtras(bundle);
        context.startActivity(intent);
    }

    /**
     * Opens the profile of an album.
     *
     * @param context    The {@link Activity} to use.
     * @param albumName  The name of the album
     * @param artistName The name of the album artist
     * @param albumId    The id of the album
     */
    public static void openAlbumProfile(final Activity context, final String albumName,
                                        final String artistName, final long albumId) {
        // Create a new bundle to transfer the artist info
        final Bundle bundle = new Bundle();
        bundle.putString(Config.ALBUM_YEAR, MusicUtils.getReleaseDateForAlbum(context, albumId));
        bundle.putInt(Config.SONG_COUNT, MusicUtils.getSongCountForAlbumInt(context, albumId));
        bundle.putString(Config.ARTIST_NAME, artistName);
        bundle.putString(Config.MIME_TYPE, MediaStore.Audio.Albums.CONTENT_TYPE);
        bundle.putLong(Config.ID, albumId);
        bundle.putString(Config.NAME, albumName);

        // Create the intent to launch the profile activity
        final Intent intent = new Intent(context, HomeActivity.class);
        intent.setAction(HomeActivity.ACTION_VIEW_ALBUM_DETAILS);
        intent.putExtras(bundle);
        context.startActivity(intent);
    }

    public static void openSmartPlaylist(final Activity context,
                                         final Config.SmartPlaylistType type) {
        // Create the new bundle to transfer the playlist info
        final Intent intent = new Intent(context, HomeActivity.class);
        intent.setAction(HomeActivity.ACTION_VIEW_SMART_PLAYLIST);
        intent.putExtra(Config.SMART_PLAYLIST_TYPE, type.mId);
        context.startActivity(intent);
    }

    /**
     * Opens the playlist view
     *
     * @param context      The {@link Activity} to use.
     * @param playlistId   the id of the playlist
     * @param playlistName the playlist name
     */
    public static void openPlaylist(final Activity context, final long playlistId,
                                    final String playlistName) {
        final Bundle bundle = new Bundle();
        bundle.putLong(Config.ID, playlistId);
        bundle.putString(Config.MIME_TYPE, MediaStore.Audio.Playlists.CONTENT_TYPE);
        bundle.putString(Config.NAME, playlistName);

        // Create the intent to launch the profile activity
        final Intent intent = new Intent(context, HomeActivity.class);
        intent.setAction(HomeActivity.ACTION_VIEW_PLAYLIST_DETAILS);
        intent.putExtras(bundle);
        context.startActivity(intent);
    }

    /**
     * @return the intent to launch the effects panel/dsp manager
     */
    public static Intent createEffectsIntent() {
        final Intent effects = new Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL);
        effects.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, MusicUtils.getAudioSessionId());
        return effects;
    }

    /**
     * Opens the sound effects panel AudioFX in LineageOS
     *
     * @param context     The {@link Activity} to use.
     * @param requestCode The request code passed into startActivityForResult
     */
    public static void openEffectsPanel(final Activity context, final int requestCode) {
        try {
            // The google MusicFX apps need to be started using startActivityForResult
            context.startActivityForResult(createEffectsIntent(), requestCode);
        } catch (final ActivityNotFoundException notFound) {
            Toast.makeText(context, context.getString(R.string.no_effects_for_you),
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Returns true if the device currently exposes an audio effects control
     * panel, either through a system built-in DSP, an OEM MusicFX
     * replacement or a third-party effects app.
     *
     * The check is intentionally permissive: it reports a panel as long as
     * the platform exposes the
     * {@link AudioEffect#ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL} intent
     * to any installed activity, OR any audio effect implementation is
     * available to be attached. This way both stock and custom ROMs that
     * ship their own DSP manager (for example LineageOS' AudioFX, Google's
     * MusicFX, Sony's DSEE, Dolby, etc.) are picked up dynamically as the
     * user installs or uninstalls them.
     *
     * @param context A {@link Context} to use.
     * @return true if a panel can be opened right now.
     */
    public static boolean hasEffectsPanel(final Context context) {
        if (context == null) {
            return false;
        }
        final PackageManager packageManager = context.getPackageManager();
        // First, look for an activity that resolves the effects control
        // intent. This is the standard way to discover a built-in DSP
        // manager.
        final Intent effectsIntent = createEffectsIntent();
        final List<ResolveInfo> activities = packageManager.queryIntentActivities(
                effectsIntent, PackageManager.MATCH_DEFAULT_ONLY);
        if (activities != null && !activities.isEmpty()) {
            return true;
        }
        // No activity is registered for the panel intent, but a DSP
        // implementation might still be available. Fall back to the
        // audio effects framework which is what the platform itself uses
        // to discover built-in effects.
        try {
            final AudioEffect.Descriptor[] effects = AudioEffect.queryEffects();
            if (effects != null && effects.length > 0) {
                return true;
            }
        } catch (final Throwable t) {
            // queryEffects() can throw on some devices that have no audio
            // effect framework; treat as no panel.
        }
        return false;
    }

    /**
     * Opens to {@link SettingsActivity}.
     *
     * @param activity The {@link Activity} to use.
     */
    public static void openSettings(final Activity activity) {
        final Intent intent = new Intent(activity, SettingsActivity.class);
        activity.startActivity(intent);
    }

    /**
     * Returns true if the app currently has the "Modify system settings"
     * permission (a.k.a. {@link android.Manifest.permission#WRITE_SETTINGS}).
     * Use this instead of relying on whether the user was prompted, as
     * the prompt just opens the system settings screen and the user can
     * close it without toggling the switch.
     *
     * @param context The {@link Context} to use.
     * @return true if the app can write to {@link Settings.System}.
     */
    public static boolean hasWriteSettingsPermission(final Context context) {
        if (context == null) {
            return false;
        }
        return Settings.System.canWrite(context);
    }
}
