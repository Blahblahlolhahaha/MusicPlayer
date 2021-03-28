package com.example.musicplayer.workers;

import android.support.v4.media.MediaBrowserCompat;

import java.util.ArrayList;

public class Artist extends Category{
    String tracks,albums;

    public Artist(String ID, String name,String tracks,String albums, ArrayList<MediaBrowserCompat.MediaItem> songs) {
        super(ID, name, songs);
        this.tracks = tracks;
        this.albums = albums;
    }

    public String getAlbums() {
        return albums;
    }

    public String getTracks() {
        return tracks;
    }
}
