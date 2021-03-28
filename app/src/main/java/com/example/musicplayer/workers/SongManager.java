package com.example.musicplayer.workers;

import android.support.v4.media.MediaBrowserCompat;

import java.util.ArrayList;

public class SongManager {
    private final ArrayList<MediaBrowserCompat.MediaItem> songs;
    private final ArrayList<Album> albums;
    private final ArrayList<Artist> artists;
    private final ArrayList<Category> genres;
    private final ArrayList<Playlist> playlists;
    public SongManager(ArrayList<MediaBrowserCompat.MediaItem> s, ArrayList<Album> a, ArrayList<Artist> b, ArrayList<Category> c, ArrayList<Playlist> d){
        songs = s;
        albums = a;
        artists = b;
        genres = c;
        playlists = d;
    }

    public ArrayList<MediaBrowserCompat.MediaItem> getSongs() {
        return songs;
    }

    public ArrayList<Album> getAlbums() {
        return albums;
    }

    public ArrayList<Artist> getArtists() {
        return artists;
    }

    public ArrayList<Playlist> getPlaylists() {
        return playlists;
    }

    public ArrayList<Category> getGenres() {
        return genres;
    }
}
