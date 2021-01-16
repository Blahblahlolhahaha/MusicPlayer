package com.example.musicplayer.workers;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;

public class Playlist {

    private String ID;
    private String name;
    private ArrayList<HashMap<String,String>> songs;
    private Uri playlistUri = ContentUris.withAppendedId(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,Long.parseLong(ID));
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

    public void addSongs(String[]songIDs, Context context){
        ContentResolver contentResolver = context.getContentResolver();
        String[] cols = new String[] {
                "count(*)"
        };
        Uri uri = MediaStore.Audio.Playlists.getContentUri("external" + ID);
        Cursor cursor = contentResolver.query(uri,cols,null,null);
        cursor.moveToFirst();
        final int base = cursor.getInt(0);
        for(int i = 0;i<songIDs.length;i++){
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, base+i+1);
            contentValues.put(MediaStore.Audio.Playlists.Members.AUDIO_ID,songIDs[i]);
        }
    }

    public void removeSongs(String[]songIDs, Context context){
        ContentResolver contentResolver = context.getContentResolver();
        for(String id : songIDs){
            contentResolver.delete(playlistUri,MediaStore.Audio.Playlists.Members.AUDIO_ID + "=" + id,null);
        }
    }
}
