package com.poupa.vinylmusicplayer.search;

import com.poupa.vinylmusicplayer.model.Album;
import com.poupa.vinylmusicplayer.model.Artist;
import com.poupa.vinylmusicplayer.model.Song;
import com.poupa.vinylmusicplayer.sort.AlbumSortOrder;
import com.poupa.vinylmusicplayer.sort.ArtistSortOrder;
import com.poupa.vinylmusicplayer.sort.SongSortOrder;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;
import java.util.regex.Pattern;

/** A privately owned, pre-sorted search index. No live library models escape into it. */
public final class SearchSnapshot {
    private static final Pattern DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    public static final class Source {
        public final long revision;
        private final List<Song> songs;
        private final List<Artist> artists;
        private final List<Album> albums;

        /** Caller must protect its input against concurrent mutations for this capture. */
        public Source(long revision, List<Song> songs, List<Artist> artists, List<Album> albums) {
            this.revision = revision;
            Copier copy = new Copier();
            this.songs = new ArrayList<>(songs.size());
            this.artists = new ArrayList<>(artists.size());
            this.albums = new ArrayList<>(albums.size());
            for (Song song : songs) this.songs.add(copy.song(song));
            for (Artist artist : artists) this.artists.add(copy.artist(artist));
            for (Album album : albums) this.albums.add(copy.album(album));
        }
    }

    private static final class Entry {
        final Object model;
        final String text;
        Entry(Object model, String text) {
            this.model = model;
            this.text = normalize(text);
        }
    }

    private final List<Entry> entries;

    private SearchSnapshot(List<Entry> entries) {
        this.entries = Collections.unmodifiableList(entries);
    }

    public static SearchSnapshot build(Source source, String songOrder, String artistOrder,
            String albumOrder, Locale locale, BooleanSupplier cancelled) {
        checkCancelled(cancelled);
        List<Song> songs = new ArrayList<>(source.songs);
        List<Artist> artists = new ArrayList<>(source.artists);
        // Captured slices can overlap for collaborations/compilations. Merge once, by ID.
        Map<Long, Map<Long, Song>> merged = new LinkedHashMap<>();
        for (Album slice : source.albums) {
            checkCancelled(cancelled);
            if (slice.songs.isEmpty()) continue;
            Map<Long, Song> album = merged.computeIfAbsent(slice.getId(), id -> new LinkedHashMap<>());
            for (Song song : slice.songs) {
                checkCancelled(cancelled);
                album.putIfAbsent(song.id, song);
            }
        }
        List<Album> albums = new ArrayList<>(merged.size());
        for (Map<Long, Song> albumSongs : merged.values()) {
            checkCancelled(cancelled);
            ArrayList<Song> tracks = new ArrayList<>(albumSongs.values());
            tracks.sort(cancellable(SongSortOrder.BY_DISC_TRACK, cancelled));
            albums.add(new Album(tracks));
        }
        songs.sort(cancellable(SongSortOrder.fromPreference(songOrder).comparator, cancelled));
        artists.sort(cancellable(ArtistSortOrder.fromPreference(artistOrder).comparator, cancelled));
        albums.sort(cancellable(AlbumSortOrder.fromPreference(albumOrder).comparator, cancelled));
        List<Entry> entries = new ArrayList<>(songs.size() + artists.size() + albums.size());
        for (Song song : songs) {
            checkCancelled(cancelled);
            entries.add(new Entry(song, song.title));
        }
        for (Artist artist : artists) {
            checkCancelled(cancelled);
            entries.add(new Entry(artist, artist.getName()));
        }
        for (Album album : albums) {
            checkCancelled(cancelled);
            entries.add(new Entry(album, album.getTitle()));
        }
        return new SearchSnapshot(entries);
    }

    public List<Object> search(String query, BooleanSupplier cancelled) {
        checkCancelled(cancelled);
        String needle = normalize(query.trim());
        if (needle.isEmpty()) return Collections.emptyList();
        List<Object> matches = new ArrayList<>();
        Copier copy = new Copier();
        for (Entry entry : entries) {
            checkCancelled(cancelled);
            if (entry.text.contains(needle)) {
                if (entry.model instanceof Song) matches.add(copy.song((Song) entry.model));
                else if (entry.model instanceof Artist) matches.add(copy.artist((Artist) entry.model));
                else matches.add(copy.album((Album) entry.model));
            }
        }
        checkCancelled(cancelled);
        return Collections.unmodifiableList(matches);
    }

    private static String normalize(String text) {
        return DIACRITICS.matcher(Normalizer.normalize(text, Normalizer.Form.NFD)
                .toLowerCase(Locale.ROOT)).replaceAll("");
    }

    static void checkCancelled(BooleanSupplier cancelled) {
        if (Thread.currentThread().isInterrupted() || cancelled.getAsBoolean()) {
            throw new CancellationException();
        }
    }

    private static <T> Comparator<T> cancellable(Comparator<T> comparator, BooleanSupplier cancelled) {
        return (left, right) -> {
            checkCancelled(cancelled);
            return comparator.compare(left, right);
        };
    }

    /** Song's ordinary copy constructor aliases mutable lists, so it is insufficient here. */
    private static final class Copier {
        private final Map<Song, Song> songs = new IdentityHashMap<>();
        private final Map<Album, Album> albums = new IdentityHashMap<>();

        Song song(Song original) {
            return songs.computeIfAbsent(original, source -> {
                Song copy = new Song(source);
                copy.artistNames = new ArrayList<>(source.artistNames);
                copy.albumArtistNames = new ArrayList<>(source.albumArtistNames);
                copy.genres = new ArrayList<>(source.genres);
                return copy;
            });
        }

        Album album(Album original) {
            return albums.computeIfAbsent(original, source -> {
                ArrayList<Song> tracks = new ArrayList<>(source.songs.size());
                for (Song track : source.songs) tracks.add(song(track));
                return new Album(tracks);
            });
        }

        Artist artist(Artist original) {
            Artist copy = new Artist(original.id, original.name);
            for (Album album : original.albums) copy.albums.add(album(album));
            return copy;
        }
    }
}
