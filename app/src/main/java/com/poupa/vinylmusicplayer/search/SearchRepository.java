package com.poupa.vinylmusicplayer.search;

import com.poupa.vinylmusicplayer.discog.Discography;
import com.poupa.vinylmusicplayer.util.PreferenceUtil;

import java.util.Locale;
import java.util.Objects;
import java.util.function.BooleanSupplier;

/** One warm snapshot, no Activity or per-query cache. Preparation runs only on the search worker. */
public class SearchRepository {
    private final Discography library;
    private volatile long invalidation;
    private volatile SearchSnapshot snapshot;
    private Key key;

    public SearchRepository(Discography library) {
        this.library = library;
    }

    public SearchSnapshot getSnapshot(BooleanSupplier cancelled) {
        SearchSnapshot.checkCancelled(cancelled);
        PreferenceUtil preferences = PreferenceUtil.getInstance();
        Locale locale = Locale.getDefault();
        long epoch = invalidation;
        Key requested = new Key(library.getSearchRevision(), epoch, preferences.getSongSortOrder(),
                preferences.getArtistSortOrder(), preferences.getAlbumSortOrder(), locale.toLanguageTag());
        SearchSnapshot cached = snapshot;
        if (cached != null && requested.equals(key)) return cached;
        SearchSnapshot.Source source = library.captureSearchLibrary();
        SearchSnapshot prepared = SearchSnapshot.build(source, requested.songs, requested.artists,
                requested.albums, locale, cancelled);
        SearchSnapshot.checkCancelled(cancelled);
        // Never resurrect a snapshot cleared on the main thread (e.g. permission revocation).
        synchronized (this) {
            if (epoch == invalidation) {
                key = new Key(source.revision, epoch, requested.songs, requested.artists,
                        requested.albums, requested.locale);
                snapshot = prepared;
            }
        }
        return prepared;
    }

    public synchronized void invalidate() {
        invalidation++;
        snapshot = null;
        key = null;
    }

    public void clear() {
        invalidate();
    }

    private static final class Key {
        final long revision, epoch;
        final String songs, artists, albums, locale;
        Key(long revision, long epoch, String songs, String artists, String albums, String locale) {
            this.revision = revision;
            this.epoch = epoch;
            this.songs = songs;
            this.artists = artists;
            this.albums = albums;
            this.locale = locale;
        }
        @Override public boolean equals(Object other) {
            if (!(other instanceof Key)) return false;
            Key that = (Key) other;
            return revision == that.revision && epoch == that.epoch
                    && songs.equals(that.songs) && artists.equals(that.artists)
                    && albums.equals(that.albums) && locale.equals(that.locale);
        }
        @Override public int hashCode() {
            return Objects.hash(revision, epoch, songs, artists, albums, locale);
        }
    }
}
