package com.example.musicplayer.workers;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.example.musicplayer.interfaces.Callback;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

public class MusicPlayer extends Service implements MediaPlayer.OnPreparedListener {
    private static final String ACTION_PLAY = "com.example.action.PLAY";
    private MediaPlayer mediaPlayer = null;
    private ArrayList<HashMap<String,String>> original;
    private ArrayList<HashMap<String,String>> songs;
    private int current;
    private boolean shuffle;
    private String repeat;
    private IBinder musicPlayerBinder = new MusicPlayerBinder();
    private Callback callback;
    private Bundle bundle;
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        bundle = intent.getExtras();
        original = (ArrayList<HashMap<String, String>>) bundle.get("songs");
        songs = (ArrayList<HashMap<String, String>>) original.clone();
        int start = bundle.getInt("start");
        shuffle = getSharedPreferences("settings",0).getBoolean("shuffle",false);
        repeat = getSharedPreferences("settings",0).getString("repeat","no");
        current = start;
        if(shuffle){
            shuffle();
        }
        createMusicPlayer();
        return START_NOT_STICKY;
    }
    public void registerCallback(Callback callback){
        this.callback = callback;
    }
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return musicPlayerBinder;
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        mediaPlayer.start();
        callback.callback(songs.get(current).get("title"),songs.get(current).get("artist"),songs.get(current).get("album"));
    }

    public class MusicPlayerBinder extends Binder{
        public MusicPlayer getService(){
            return MusicPlayer.this;
        }
    }

    public boolean getPlayingStatus(){
        return mediaPlayer.isPlaying();
    }
    public void pause(){
        mediaPlayer.pause();
    }
    public void play(){
        mediaPlayer.start();
    }
    public void next(){
        mediaPlayer.stop();
        mediaPlayer.release();
        current = current == (songs.size()-1)? 0 : current + 1;
        createMusicPlayer();
    }
    public void previous(){
        mediaPlayer.stop();
        mediaPlayer.release();
        current--;
        if(current<0){
            current = songs.size() - 1;
        }
        createMusicPlayer();
    }
    public void playAnotherSong(int position){
        mediaPlayer.stop();
        mediaPlayer.release();
        current = position;
        createMusicPlayer();
    }

    public boolean setShuffle() {
        SharedPreferences.Editor editor = getSharedPreferences("settings",0).edit();
        editor.putBoolean("shuffle", !shuffle);
        editor.apply();
        shuffle = !shuffle;
        if(shuffle){
            shuffle();
        }
        else{
            HashMap<String,String> current_song = songs.get(current);
            songs = (ArrayList<HashMap<String, String>>) original.clone();
            current = songs.indexOf(current_song);
        }
        return shuffle;
    }

    public String setRepeat(){
        SharedPreferences.Editor editor = getSharedPreferences("settings",0).edit();
        switch (repeat){
            case "no":
                editor.putString("repeat","repeat");
                repeat = "repeat";
                break;
            case "repeat":
                editor.putString("repeat","track");
                repeat = "track";
                break;
            case "track":
                editor.putString("repeat","no");
                repeat = "no";
                break;
        }
        editor.apply();
        return repeat;
    }

    public void shuffle(){
        HashMap<String,String> current_song = songs.get(current);
        songs.remove(current);
        Collections.shuffle(songs);
        Collections.reverse(songs);
        songs.add(current_song);
        Collections.reverse(songs);
        current = 0;
    }
    private void createMusicPlayer(){
        mediaPlayer = MediaPlayer.create(getApplicationContext(), Uri.parse(songs.get(current).get("data")));
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnCompletionListener(mediaPlayer1 -> {
            mediaPlayer1.release();
            current = repeat.equals("track")? current:current + 1;
            if(current == songs.size()){
                if(repeat.equals("repeat")){
                    current = 0;
                }
                else{
                    stopSelf();
                }
            }
            createMusicPlayer();
        });
    }
}
