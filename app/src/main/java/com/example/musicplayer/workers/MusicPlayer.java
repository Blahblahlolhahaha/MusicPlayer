package com.example.musicplayer.workers;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.example.musicplayer.interfaces.Callback;

import java.util.ArrayList;
import java.util.HashMap;

public class MusicPlayer extends Service implements MediaPlayer.OnPreparedListener {
    private static final String ACTION_PLAY = "com.example.action.PLAY";
    private MediaPlayer mediaPlayer = null;
    private ArrayList<HashMap<String,String>> songs;
    private int current;
    private boolean shuffle;
    private IBinder musicPlayerBinder = new MusicPlayerBinder();
    private Callback callback;
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Bundle bundle = intent.getExtras();
        songs = (ArrayList<HashMap<String, String>>) bundle.get("songs");
        int start = bundle.getInt("start");
        shuffle = getApplicationContext().getSharedPreferences("settings",0).getBoolean("shuffle",false);
        current = start;
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
        current++;
        createMusicPlayer();
    }
    public void previous(){
        mediaPlayer.stop();
        mediaPlayer.release();
        current--;
        createMusicPlayer();
    }
    public void playAnotherSong(int position){
        mediaPlayer.stop();
        mediaPlayer.release();
        current = position;
        createMusicPlayer();
    }
    private void createMusicPlayer(){
        mediaPlayer = MediaPlayer.create(getApplicationContext(), Uri.parse(songs.get(current).get("data")));
        mediaPlayer.setOnPreparedListener(this);
    }
}
