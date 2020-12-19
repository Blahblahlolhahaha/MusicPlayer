package com.example.musicplayer.workers;

import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.provider.MediaStore;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.HashMap;

public class SongManager {
    private ArrayList<HashMap<String,String>> songs;
    private HashMap<String,Object> album;
    private HashMap<String,String> artist;

    public SongManager(ArrayList<HashMap<String,String>> s, HashMap<String, Object> a, HashMap<String,String> b){
        songs = s;
        album = a;
        artist = b;
    }

    public ArrayList<HashMap<String, String>> getSongs() {
        return songs;
    }

    public HashMap<String, Object> getAlbum() {
        return album;
    }

    public HashMap<String, String> getArtist() {
        return artist;
    }
}
