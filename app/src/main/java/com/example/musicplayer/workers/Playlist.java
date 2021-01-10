package com.example.musicplayer.workers;

import java.util.ArrayList;
import java.util.HashMap;

public class Playlist {

    private String ID;
    private String name;
    private ArrayList<HashMap<String,String>> songs;

    public Playlist(String ID, String name, ArrayList<HashMap<String,String>> songs){
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
        return songs.get(0).get("album");
    }
}
