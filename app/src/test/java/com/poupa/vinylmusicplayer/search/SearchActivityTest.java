package com.poupa.vinylmusicplayer.search;

import static org.junit.Assert.*;

import android.Manifest;
import android.content.ComponentName;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.poupa.vinylmusicplayer.App;
import com.poupa.vinylmusicplayer.R;
import com.poupa.vinylmusicplayer.model.Album;
import com.poupa.vinylmusicplayer.model.Artist;
import com.poupa.vinylmusicplayer.model.Song;
import com.poupa.vinylmusicplayer.service.MusicService;
import com.poupa.vinylmusicplayer.ui.activities.SearchActivity;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, application = App.class)
public class SearchActivityTest {
    @Before public void permitMusic() {
        Shadows.shadowOf(RuntimeEnvironment.getApplication()).grantPermissions(Manifest.permission.READ_MEDIA_AUDIO);
        // These are search-screen tests, not a MediaPlayer/service binding fixture.
        Shadows.shadowOf(RuntimeEnvironment.getApplication()).declareComponentUnbindable(
                new ComponentName(RuntimeEnvironment.getApplication(), MusicService.class));
    }

    @Test public void rendersPendingEmptyAndResultsDistinctly() {
        try (var lifecycle = Robolectric.buildActivity(SearchActivity.class).setup()) {
            SearchActivity activity = lifecycle.get();
            RecyclerView list = activity.findViewById(R.id.recycler_view);
            activity.onPending();
            assertEquals(View.VISIBLE, activity.findViewById(R.id.search_progress).getVisibility());
            assertEquals(View.GONE, activity.findViewById(android.R.id.empty).getVisibility());
            Song song = SearchFixtures.song(1, "Needle", "Artist", "Album");
            activity.onResults(List.of(song, new Artist(1, "Artist"), new Album()));
            assertEquals(6, list.getAdapter().getItemCount());
            assertEquals(View.GONE, activity.findViewById(R.id.search_progress).getVisibility());
            activity.onResults(List.of());
            assertEquals(View.VISIBLE, activity.findViewById(android.R.id.empty).getVisibility());
            assertEquals(activity.getString(R.string.no_results),
                    ((TextView) activity.findViewById(android.R.id.empty)).getText().toString());
        }
    }

    @Test public void restoresQueryAcrossRecreation() {
        try (var lifecycle = Robolectric.buildActivity(SearchActivity.class).setup()) {
            SearchActivity activity = lifecycle.get();
            activity.onQueryTextChange("Needle");
            Bundle saved = new Bundle();
            lifecycle.saveInstanceState(saved);
            assertEquals("Needle", saved.getString(SearchActivity.QUERY));
            try (var recreated = Robolectric.buildActivity(SearchActivity.class).create(saved).start().resume()) {
                Bundle savedAgain = new Bundle();
                recreated.saveInstanceState(savedAgain);
                assertEquals("Needle", savedAgain.getString(SearchActivity.QUERY));
            }
        }
    }

    @Test public void clearsOnPermissionLossAndReloadsAfterGrant() {
        try (var lifecycle = Robolectric.buildActivity(SearchActivity.class).setup()) {
            SearchActivity activity = lifecycle.get();
            RecyclerView list = activity.findViewById(R.id.recycler_view);
            activity.onResults(List.of(SearchFixtures.song(1, "Needle", "A", "Album")));
            assertEquals(2, list.getAdapter().getItemCount());
            lifecycle.pause();
            Shadows.shadowOf(RuntimeEnvironment.getApplication()).denyPermissions(Manifest.permission.READ_MEDIA_AUDIO);
            lifecycle.resume();
            assertEquals(0, list.getAdapter().getItemCount());
            assertEquals(View.GONE, activity.findViewById(R.id.search_progress).getVisibility());
            activity.onQueryTextChange("Needle");
            assertEquals(View.GONE, activity.findViewById(R.id.search_progress).getVisibility());
            lifecycle.pause();
            Shadows.shadowOf(RuntimeEnvironment.getApplication()).grantPermissions(Manifest.permission.READ_MEDIA_AUDIO);
            lifecycle.resume();
            assertEquals(View.VISIBLE, activity.findViewById(R.id.search_progress).getVisibility());
        }
    }
}
