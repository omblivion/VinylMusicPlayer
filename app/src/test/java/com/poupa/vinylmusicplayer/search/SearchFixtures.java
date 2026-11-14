package com.poupa.vinylmusicplayer.search;

import com.poupa.vinylmusicplayer.model.Album;
import com.poupa.vinylmusicplayer.model.Artist;
import com.poupa.vinylmusicplayer.model.Song;

import java.util.ArrayList;
import java.util.List;

public final class SearchFixtures {
    private SearchFixtures() {}

    public static Song song(long id, String title, String artist, String album) {
        return new Song(id, title, (int) id, 2020, 90000, "/synthetic/" + id,
                id, id, 10, album, new ArrayList<>(List.of(artist)));
    }

    public static SearchSnapshot.Source library(int count) {
        return library(count, 1);
    }

    public static SearchSnapshot.Source library(int count, long revision) {
        List<Song> songs = new ArrayList<>();
        List<Album> albums = new ArrayList<>();
        List<Artist> artists = new ArrayList<>();
        String[] titles = {"Café", "Cafe\u0301", "Dancing Queen", "東京!", "INDUSTRY"};
        for (int i = 0; i < count; i++) {
            Song song = song(i + 1, i < titles.length ? titles[i] : "Track " + i,
                    "ABBA", "Gold");
            songs.add(song);
        }
        Album album = new Album(new ArrayList<>(songs));
        albums.add(album);
        Artist artist = new Artist(9, "ABBA");
        artist.albums.add(album);
        artists.add(artist);
        return new SearchSnapshot.Source(revision, songs, artists, albums);
    }
}
