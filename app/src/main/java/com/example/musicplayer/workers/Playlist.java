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
    private Uri playlistUri;
    public Playlist(String ID, String name, ArrayList<HashMap<String,String>> songs){
        this.ID = ID;
        this.name = name;
        this.songs = songs;
        playlistUri = MediaStore.Audio.Playlists.Members.getContentUri("external", Long.parseLong(ID));
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
        return "";
    }

    public void addSongs(ArrayList<HashMap<String,String>>songs, Context context){
        ContentResolver contentResolver = context.getContentResolver();
        String[] cols = new String[] {
                "count(*)"
        };
        final int base = songs.size();
        ContentValues[] contentValues = new ContentValues[songs.size()];
        for(int i = 0;i<songs.size();i++){
            ContentValues contentValue = new ContentValues();
            contentValue.put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, base+i+1);
            contentValue.put(MediaStore.Audio.Playlists.Members.AUDIO_ID,songs.get(i).get("ID"));
            contentValues[i] = contentValue;
            this.songs.add(songs.get(i));
        }
        contentResolver.bulkInsert(playlistUri,contentValues);
        contentResolver.notifyChange(Uri.parse("content://media"), null);
    }

    public void removeSongs(String[]songIDs, Context context){
        ContentResolver contentResolver = context.getContentResolver();
        int[] positions = new int[songIDs.length];
        int count  = 0;
        for(String id : songIDs){
            contentResolver.delete(playlistUri,MediaStore.Audio.Playlists.Members.AUDIO_ID + "=" + id,null);
            for(int i = 0;i<songs.size();i++){
                if(songs.get(i).get("ID").equals(id)){
                    positions[count] = i;
                    break;
                }
            }
            count++;
        }
        for(int i = 0;i<positions.length;i++){
            songs.remove(positions[i]);
        }

    }
}
