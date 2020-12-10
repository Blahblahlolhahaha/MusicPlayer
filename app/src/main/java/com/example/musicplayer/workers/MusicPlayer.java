package com.example.musicplayer.workers;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Icon;
import android.media.MediaMetadata;
import android.media.MediaPlayer;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.example.musicplayer.R;
import com.example.musicplayer.interfaces.Callback;

import java.util.ArrayList;
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
    private MediaSession mediaSession;
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent.getAction()!=null){
            switch (intent.getAction()){
                case "action_previous":
                    previous();
                    break;
                case "action_pause":
                    pause();
                    break;
                case "action_play":
                    play();
                    break;
                case "action_next":
                    next();
                    break;
                default:
            }
        }
        else{
            mediaSession = new MediaSession(getApplicationContext(),"mediaSession");
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
            createNotificationChannel();
            configureMediaSession();
            createMusicPlayer();
        }
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
        callback.setLogo(false);
        createNotification();
    }
    public void play(){
        mediaPlayer.start();
        callback.setLogo(true);
        createNotification();
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

    public MediaSession getMediaSession(){
        return mediaSession;
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

    public void createNotification(){
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        MediaController mediaController = mediaSession.getController();
        MediaMetadata mediaMetadata = mediaController.getMetadata();
        Notification.MediaStyle mediaStyle = new Notification.MediaStyle().setMediaSession(mediaSession.getSessionToken());
        Intent intent = new Intent( getApplicationContext(), MusicPlayer.class );
        intent.putExtra("previous",0);
        intent.putExtra("pause",1);
        intent.putExtra("play",2);
        intent.putExtra("next",3);
        Notification.Action playAction = getPlayingStatus()? new Notification.Action.Builder(
                Icon.createWithResource(getApplicationContext(),R.drawable.pause),
                "pause",
                PendingIntent.getService(
                        getApplicationContext(),
                        intent.getIntExtra("pause",0),
                        intent.setAction("action_pause"),
                        PendingIntent.FLAG_UPDATE_CURRENT
                )).build() :
                new Notification.Action.Builder(
                        Icon.createWithResource(getApplicationContext(),R.drawable.play),
                        "play",
                        PendingIntent.getService(
                                getApplicationContext(),
                                intent.getIntExtra("play",0),
                                intent.setAction("action_play"),
                                PendingIntent.FLAG_UPDATE_CURRENT
                        )).build();

        Notification notification = new Notification.Builder(getApplicationContext(),"MusicPlayer")
                .setContentTitle(mediaMetadata.getString(MediaMetadata.METADATA_KEY_TITLE))
                .setSubText(mediaMetadata.getString(MediaMetadata.METADATA_KEY_ARTIST))
                .setLargeIcon(mediaMetadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART))
                .setSmallIcon(Icon.createWithResource(getApplicationContext(),R.drawable.ic_launcher_background))
                .setOngoing(true)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setStyle(mediaStyle)
                .addAction(new Notification.Action.Builder(Icon.createWithResource(getApplicationContext(),R.drawable.previous),"previous",PendingIntent.getService(getApplicationContext(),intent.getIntExtra("previous",0),intent.setAction("action_previous"),PendingIntent.FLAG_UPDATE_CURRENT)).build())
                .addAction(playAction)
                .addAction(new Notification.Action.Builder(Icon.createWithResource(getApplicationContext(),R.drawable.next),"next",PendingIntent.getService(getApplicationContext(),intent.getIntExtra("next",0),intent.setAction("action_next"),PendingIntent.FLAG_UPDATE_CURRENT)).build())
                .build();
        notificationManager.notify(10000,notification);
    }

    private void createMusicPlayer(){
        HashMap<String,String> currentSong = songs.get(current);
        mediaPlayer = MediaPlayer.create(getApplicationContext(), Uri.parse(currentSong.get("data")));
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
    public void reset(int position){
        songs = (ArrayList<HashMap<String, String>>) original.clone();
        playAnotherSong(position);
        if(shuffle){
            shuffle();
        }
    }
    private void configureMediaSession(){
        mediaSession.setCallback(new MediaSession.Callback() {
            @Override
            public void onSeekTo(long pos) {
                mediaPlayer.seekTo((int)pos);
            }

            @Override
            public void onSkipToPrevious() {
                previous();
            }

            @Override
            public void onSkipToNext() {
                 next();
            }

            @Override
            public void onPause() {
                pause();
            }

            @Override
            public void onStop() {
                mediaPlayer.stop();
                mediaPlayer.release();
            }
        });
        mediaSession.setPlaybackState(new PlaybackState.Builder()
                .setActions(PlaybackState.ACTION_PLAY|PlaybackState.ACTION_PAUSE|PlaybackState.ACTION_SKIP_TO_NEXT|PlaybackState.ACTION_SKIP_TO_PREVIOUS)
                .setState(PlaybackState.STATE_PLAYING,0,1.0f)
                .build()
        );
        mediaSession.setActive(true);
    }
    private void createNotificationChannel(){
        String channelName = "MusicPlayer";
        String description = "Play music!";
        int importance = NotificationManager.IMPORTANCE_LOW;
        NotificationChannel notificationChannel  = new NotificationChannel(channelName,channelName,importance);
        notificationChannel.setDescription(description);
        notificationChannel.setVibrationPattern(null);
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(notificationChannel);
    }
}
