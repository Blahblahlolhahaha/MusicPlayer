package com.example.musicplayer.workers;

import android.net.Uri;
import android.support.v4.media.MediaBrowserCompat;

import java.util.ArrayList;

public class Genre {
    private String ID;
    private String name;
    private ArrayList<MediaBrowserCompat.MediaItem> songs;
    private Uri playlistUri;
    public Genre(String ID, String name, ArrayList<MediaBrowserCompat.MediaItem> songs){
        this.ID = ID;
        this.name = name;
        this.songs = songs;
    }

    public String getID() {
        return ID;
    }

    public String getName() {
        return name;
    }

    public ArrayList<MediaBrowserCompat.MediaItem> getSongs() {
        return songs;
    }

    public String getFirstSongAlbum(){
        if(songs.size() != 0){
            return songs.get(0).getDescription().getExtras().getString("album");
        }
        else{
            return "";
        }
    }
}
