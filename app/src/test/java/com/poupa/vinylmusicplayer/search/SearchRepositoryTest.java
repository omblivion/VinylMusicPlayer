package com.poupa.vinylmusicplayer.search;

import static org.junit.Assert.*;

import com.poupa.vinylmusicplayer.App;
import com.poupa.vinylmusicplayer.discog.Discography;
import com.poupa.vinylmusicplayer.util.PreferenceUtil;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Locale;
import java.util.concurrent.CancellationException;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, application = App.class)
public class SearchRepositoryTest {
    static class Library extends Discography {
        int captures;
        long revision = 1;
        @Override public long getSearchRevision() { return revision; }
        @Override public SearchSnapshot.Source captureSearchLibrary() {
            captures++;
            return SearchFixtures.library(3736, revision);
        }
    }

    @Test public void reusesSnapshotAcrossQueries() {
        Library library = new Library();
        SearchRepository repository = new SearchRepository(library);
        SearchSnapshot snapshot = repository.getSnapshot(() -> false);
        assertEquals(1, snapshot.search("dancing", () -> false).size());
        assertEquals(2, snapshot.search("cafe", () -> false).size());
        assertSame(snapshot, repository.getSnapshot(() -> false));
        assertEquals(1, library.captures);
        repository.clear();
        assertNotSame(snapshot, repository.getSnapshot(() -> false));
    }

    @Test public void rebuildsOnRevisionPreferenceOrLocaleChange() {
        Library library = new Library();
        SearchRepository repository = new SearchRepository(library);
        SearchSnapshot first = repository.getSnapshot(() -> false);
        library.revision++;
        SearchSnapshot second = repository.getSnapshot(() -> false);
        assertNotSame(first, second);
        PreferenceUtil.getInstance().setSongSortOrder("title_key DESC");
        SearchSnapshot third = repository.getSnapshot(() -> false);
        assertNotSame(second, third);
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.JAPANESE);
            assertNotSame(third, repository.getSnapshot(() -> false));
        } finally {
            Locale.setDefault(previous);
        }
        repository.invalidate();
        assertThrows(CancellationException.class, () -> repository.getSnapshot(() -> true));
        int before = library.captures;
        repository.getSnapshot(() -> false);
        assertEquals(before + 1, library.captures);
    }
}
