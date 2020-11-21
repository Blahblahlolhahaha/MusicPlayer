package com.example.musicplayer.workers;

import android.content.ContentResolver;
import android.database.Cursor;
import android.provider.MediaStore;

import org.json.JSONArray;

import java.util.ArrayList;

public class SongManager {
    public ArrayList songs;
    public SongManager(ArrayList s){
        songs = s;
    }
}
