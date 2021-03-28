package com.example.musicplayer.workers;

import android.support.v4.media.MediaBrowserCompat;

import java.util.ArrayList;

public class Album extends Category{
    private String artist;
    public Album(String ID, String name, String artist, ArrayList<MediaBrowserCompat.MediaItem> songs) {
        super(ID, name, songs);
        this.artist = artist;

    }

    public String getArtist() {
        return artist;
    }
}
