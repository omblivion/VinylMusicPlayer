package com.poupa.vinylmusicplayer.search;

import static org.junit.Assert.*;

import com.poupa.vinylmusicplayer.App;
import com.poupa.vinylmusicplayer.model.Album;
import com.poupa.vinylmusicplayer.model.Artist;
import com.poupa.vinylmusicplayer.model.Song;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, application = App.class)
public class SearchSnapshotTest {
    static SearchSnapshot build(SearchSnapshot.Source source) {
        return SearchSnapshot.build(source, "title_key", "artist_key", "album_key",
                Locale.ITALIAN, () -> false);
    }

    @Test public void matchesCanonicalAccentsAndLocaleIndependently() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            SearchSnapshot snapshot = build(SearchFixtures.library(3736));
            assertEquals(2, snapshot.search("CAFE", () -> false).size());
            assertEquals(2, snapshot.search("café", () -> false).size());
            assertEquals(2, snapshot.search("cafe\u0301", () -> false).size());
            assertEquals(3, ((Song) snapshot.search("dancing", () -> false).get(0)).id);
            assertEquals(1, snapshot.search("東京!", () -> false).size());
            assertEquals(1, snapshot.search("industry", () -> false).size());
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test public void capturesDetachedGraph() {
        Song song = SearchFixtures.song(1, "Needle", "Artist", "Album");
        Album album = new Album(new ArrayList<>(List.of(song)));
        Artist artist = new Artist(7, "Artist");
        artist.albums.add(album);
        SearchSnapshot.Source source = new SearchSnapshot.Source(4,
                List.of(song), List.of(artist), List.of(album));
        song.title = "Changed";
        song.artistNames.clear();
        album.songs.clear();
        artist.albums.clear();
        SearchSnapshot snapshot = build(source);
        Song result = (Song) snapshot.search("needle", () -> false).get(0);
        assertEquals("Needle", result.title);
        assertEquals(List.of("Artist"), result.artistNames);
        Artist artistResult = (Artist) snapshot.search("artist", () -> false).get(0);
        assertEquals(1, artistResult.getSongCount());
        result.title = "Corrupted";
        artistResult.albums.clear();
        assertEquals("Needle", ((Song) snapshot.search("needle", () -> false).get(0)).title);
        assertEquals(1, ((Artist) snapshot.search("artist", () -> false).get(0)).getSongCount());
    }

    @Test public void preservesGroupingAndIds() {
        Song first = SearchFixtures.song(1, "One", "A", "Compilation");
        Song second = SearchFixtures.song(2, "Two", "B", "Compilation");
        Album a = new Album(new ArrayList<>(List.of(first, second)));
        Album b = new Album(new ArrayList<>(List.of(second)));
        Artist artist = new Artist(12, "B");
        artist.albums.add(b);
        SearchSnapshot snapshot = build(new SearchSnapshot.Source(1,
                List.of(first, second), List.of(artist), List.of(a, b)));
        Album full = (Album) snapshot.search("compilation", () -> false).get(0);
        assertEquals(10, full.getId());
        assertEquals(2, full.getSongCount());
        assertEquals(12, ((Artist) snapshot.search("B", () -> false).get(0)).id);
    }

    @Test public void preservesCategorySortAndEmptySemantics() {
        SearchSnapshot snapshot = build(SearchFixtures.library(5));
        assertTrue(snapshot.search("  ", () -> false).isEmpty());
        assertTrue(snapshot.search("missing", () -> false).isEmpty());
        assertTrue(snapshot.search("ABBA", () -> false).get(0) instanceof Artist);
        assertTrue(snapshot.search("Gold", () -> false).get(0) instanceof Album);
        SearchSnapshot reverse = SearchSnapshot.build(SearchFixtures.library(5),
                "title_key DESC", "artist_key", "album_key", Locale.ITALIAN, () -> false);
        List<Object> ascending = snapshot.search("caf", () -> false);
        List<Object> descending = reverse.search("caf", () -> false);
        // Equal title names preserve IDs; use a separate unequal-title fixture below.
        assertEquals(2, ascending.size());
        assertEquals(2, descending.size());
        Song z = SearchFixtures.song(6, "Z track", "A", "Album");
        Song a = SearchFixtures.song(7, "A track", "A", "Album");
        SearchSnapshot.Source source = new SearchSnapshot.Source(1, List.of(z, a), List.of(), List.of());
        assertEquals(7, ((Song) build(source).search("track", () -> false).get(0)).id);
        assertEquals(6, ((Song) SearchSnapshot.build(source, "title_key DESC",
                "artist_key", "album_key", Locale.ITALIAN, () -> false)
                .search("track", () -> false).get(0)).id);
    }

    @Test public void cancelsWhilePreparingAndMatching() {
        AtomicInteger checks = new AtomicInteger();
        assertThrows(CancellationException.class, () -> SearchSnapshot.build(
                SearchFixtures.library(3736), "title_key", "artist_key", "album_key",
                Locale.ITALIAN, () -> checks.incrementAndGet() > 10));
        SearchSnapshot snapshot = build(SearchFixtures.library(3736));
        checks.set(0);
        assertThrows(CancellationException.class,
                () -> snapshot.search("track", () -> checks.incrementAndGet() > 10));
    }
}
