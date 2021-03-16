package com.example.musicplayer.workers;

import android.net.Uri;
import android.provider.MediaStore;

import java.util.ArrayList;
import java.util.HashMap;

public class Genre {
    private String ID;
    private String name;
    private ArrayList<HashMap<String,String>> songs;
    private Uri playlistUri;
    public Genre(String ID, String name, ArrayList<HashMap<String,String>> songs){
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

    public ArrayList<HashMap<String, String>> getSongs() {
        return songs;
    }

    public String getFirstSongAlbum(){
        if(songs.size() != 0){
            return songs.get(0).get("album");
        }
        else{
            return "";
        }
    }
}
