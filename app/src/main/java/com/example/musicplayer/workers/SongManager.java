package com.example.musicplayer.workers;

import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.provider.MediaStore;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.HashMap;

public class SongManager {
    private final ArrayList<HashMap<String,String>> songs;
    private final ArrayList<HashMap<String,String>> album;
    private final ArrayList<HashMap<String,String>> artist;

    public SongManager(ArrayList<HashMap<String,String>> s, ArrayList<HashMap<String,String>> a, ArrayList<HashMap<String,String>> b){
        songs = s;
        album = a;
        artist = b;
    }

    public ArrayList<HashMap<String, String>> getSongs() {
        return songs;
    }

    public ArrayList<HashMap<String,String>> getAlbum() {
        return album;
    }

    public ArrayList<HashMap<String,String>>getArtist() {
        return artist;
    }
}
