package com.poupa.vinylmusicplayer.discog;

import static org.junit.Assert.*;

import com.poupa.vinylmusicplayer.App;
import com.poupa.vinylmusicplayer.model.Song;
import com.poupa.vinylmusicplayer.search.SearchFixtures;
import com.poupa.vinylmusicplayer.search.SearchSnapshot;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Method;
import java.util.Locale;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, application = App.class)
public class SearchCaptureTest {
    private static void add(Discography library, Song song) throws Exception {
        Method method = Discography.class.getDeclaredMethod("addSong", Song.class, boolean.class);
        method.setAccessible(true);
        method.invoke(library, song, true);
    }

    @Test public void incrementsRevisionForLibraryMutations() throws Exception {
        Discography library = new Discography();
        long initial = library.getSearchRevision();
        Song song = SearchFixtures.song(1, "Needle", "A", "Album");
        add(library, song);
        assertTrue(library.getSearchRevision() > initial);
        long added = library.getSearchRevision();
        add(library, song);
        assertEquals(added, library.getSearchRevision());
        SearchSnapshot.Source old = library.captureSearchLibrary();
        library.removeSongByPath(song.data);
        assertTrue(library.getSearchRevision() > added);
        SearchSnapshot snapshot = SearchSnapshot.build(old, "title_key", "artist_key",
                "album_key", Locale.ROOT, () -> false);
        assertEquals(1, snapshot.search("Needle", () -> false).size());
        long removed = library.getSearchRevision();
        library.clear();
        assertTrue(library.getSearchRevision() > removed);
    }
}
