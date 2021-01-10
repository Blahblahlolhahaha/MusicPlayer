package com.example.musicplayer.workers;

import java.util.ArrayList;
import java.util.HashMap;

public class SongManager {
    private final ArrayList<HashMap<String,String>> songs;
    private final ArrayList<HashMap<String,String>> albums;
    private final ArrayList<HashMap<String,String>> artists;
    private final ArrayList<Playlist> playlists;
    public SongManager(ArrayList<HashMap<String,String>> s, ArrayList<HashMap<String,String>> a, ArrayList<HashMap<String,String>> b,ArrayList<Playlist> c){
        songs = s;
        albums = a;
        artists = b;
        playlists = c;
    }

    public ArrayList<HashMap<String, String>> getSongs() {
        return songs;
    }

    public ArrayList<HashMap<String,String>> getAlbums() {
        return albums;
    }

    public ArrayList<HashMap<String,String>> getArtists() {
        return artists;
    }

    public ArrayList<Playlist> getPlaylists() {
        return playlists;
    }
}
