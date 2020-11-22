package com.example.musicplayer.workers;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;

public class MusicPlayer extends Service implements MediaPlayer.OnPreparedListener {
    private static final String ACTION_PLAY = "com.example.action.PLAY";
    MediaPlayer mediaPlayer = null;
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Bundle bundle = intent.getExtras();
        ArrayList<HashMap<String,String>> songs = (ArrayList<HashMap<String, String>>) bundle.get("songs");
        int start = bundle.getInt("start");
        mediaPlayer = MediaPlayer.create(getApplicationContext(), Uri.parse(songs.get(start).get("data")));
        mediaPlayer.setOnPreparedListener(this);
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        mediaPlayer.start();
    }
}
