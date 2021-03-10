package com.example.musicplayer.workers;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaMetadata;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationManagerCompat;
import androidx.media.app.NotificationCompat;

import com.example.musicplayer.MainActivity;
import com.example.musicplayer.R;
import com.example.musicplayer.interfaces.Callback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class MusicPlayer extends Service implements MediaPlayer.OnPreparedListener {
    private MediaPlayer mediaPlayer = null;
    private ArrayList<HashMap<String,String>> original;
    private ArrayList<HashMap<String,String>> songs;
    private int current;
    private boolean shuffle;
    private String repeat;
    private final IBinder musicPlayerBinder = new MusicPlayerBinder();
    private Callback callback;
    private MediaSessionCompat mediaSession;
    private NotificationManagerCompat notificationManager;
    @Override
    @SuppressWarnings("unchecked")
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent.getAction()!=null){
            switch (intent.getAction()){
                //based on action provided in intent, does the corresponding action
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
                case "action_close":
                    onDestroy();
                default:
            }
        }
        else{
            //if newly started
            mediaSession = new MediaSessionCompat(getApplicationContext(),"mediaSession");
            Bundle bundle = intent.getExtras();
            original = (ArrayList<HashMap<String, String>>) bundle.get("songs");//get song list
            songs = (ArrayList<HashMap<String, String>>) original.clone();//clone cos pass-by-value is kinda sad
            int start = bundle.getInt("start");//check starting position for song
            shuffle = getSharedPreferences("settings",0).getBoolean("shuffle",false);
            repeat = getSharedPreferences("settings",0).getString("repeat","no");
            current = start;
            if(shuffle){
                // if shuffle, shuffle the queue
                shuffle();
            }
            createNotificationChannel();
            configureMediaSession();
            createMusicPlayer();
        }
        return START_STICKY;
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
        //starts the song
        mediaPlayer.start();
        callback.callback(songs.get(current).get("title"),songs.get(current).get("artist"),songs.get(current).get("album"),songs.get(current).get("duration"));
    }

    public class MusicPlayerBinder extends Binder{
        public MusicPlayer getService(){
            return MusicPlayer.this;
        }
    }

    public boolean getPlayingStatus(){
        return mediaPlayer.isPlaying();
    }

    public int getPosition(){
        return mediaPlayer.getCurrentPosition();
    }

    public void setPosition(int position){ mediaPlayer.seekTo(position);}

    public void pause(){
        mediaPlayer.pause();
        callback.setLogo(false);
        createNotification();
        mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE|PlaybackStateCompat.ACTION_SKIP_TO_NEXT|PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
                .setState(PlaybackStateCompat.STATE_PAUSED,0,1.0f)
                .build()
        );
    }
    public void play(){
        mediaPlayer.start();
        callback.setLogo(true);
        createNotification();
        mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE|PlaybackStateCompat.ACTION_SKIP_TO_NEXT|PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
                .setState(PlaybackStateCompat.STATE_PLAYING,0,1.0f)
                .build()
        );//sets playback state as playing to show correct actions on lock screen
    }
    //For next few functions, to change song, need stop mediaPlayer to prevent clashes
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
    @SuppressWarnings("unchecked")
    public boolean setShuffle() {
        //sets shuffle state
        SharedPreferences.Editor editor = getSharedPreferences("settings",0).edit();
        editor.putBoolean("shuffle", !shuffle);
        editor.apply();
        shuffle = !shuffle;
        if(shuffle){
            //shuffle queue
            shuffle();
        }
        else{
            //if shuffle is unset, returns songs to original position b4 shuffling by using the clone
            HashMap<String,String> current_song = songs.get(current);
            songs = (ArrayList<HashMap<String, String>>) original.clone();
            current = songs.indexOf(current_song);//get position of current song
        }
        return shuffle;
    }

    public MediaSessionCompat getMediaSession(){
        return mediaSession;
    }

    public String setRepeat(){
        /*
        Repeat status:
        no: no repeat
        repeat: repeat queue
        track: repeat current track
         */
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
        //shuffles list and sets current song as first song
        HashMap<String,String> current_song = songs.get(current);
        songs.remove(current);
        Collections.shuffle(songs);
        Collections.reverse(songs);
        songs.add(current_song);
        Collections.reverse(songs);
        current = 0;
    }

    public void createNotification(){
        //creates a notification to show and provide controls
        notificationManager = NotificationManagerCompat.from(this);
        MediaControllerCompat mediaController = mediaSession.getController();
        MediaMetadataCompat mediaMetadata = mediaController.getMetadata();//gets metadata of the song to show in notification
        NotificationCompat.MediaStyle mediaStyle = new NotificationCompat.MediaStyle().setMediaSession(mediaSession.getSessionToken());
        Intent intent = new Intent( getApplicationContext(), MusicPlayer.class );
        intent.putExtra("previous",0);
        intent.putExtra("pause",1);
        intent.putExtra("play",2);
        intent.putExtra("next",3);
        intent.putExtra("close",4);
        //based on playing status, sets the icon for play/pause
        androidx.core.app.NotificationCompat.Action playAction = getPlayingStatus()? new androidx.core.app.NotificationCompat.Action.Builder(
                R.drawable.pause,
                "pause",
                PendingIntent.getService(
                        getApplicationContext(),
                        intent.getIntExtra("pause",0),
                        intent.setAction("action_pause"),
                        PendingIntent.FLAG_UPDATE_CURRENT
                )).build() :
                new androidx.core.app.NotificationCompat.Action.Builder(
                        R.drawable.play,
                        "play",
                        PendingIntent.getService(
                                getApplicationContext(),
                                intent.getIntExtra("play",0),
                                intent.setAction("action_play"),
                                PendingIntent.FLAG_UPDATE_CURRENT
                        )).build();

        Notification notification = new androidx.core.app.NotificationCompat.Builder(getApplicationContext(),"MusicPlayer")
                .setContentTitle(mediaMetadata.getString(MediaMetadata.METADATA_KEY_TITLE))
                .setSubText(mediaMetadata.getString(MediaMetadata.METADATA_KEY_ARTIST))
                .setLargeIcon(mediaMetadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART))
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setOngoing(true)
                .setVisibility(androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC)
                .setStyle(mediaStyle)
                .addAction(new androidx.core.app.NotificationCompat.Action.Builder(R.drawable.previous,"previous",PendingIntent.getService(getApplicationContext(),intent.getIntExtra("previous",0),intent.setAction("action_previous"),PendingIntent.FLAG_UPDATE_CURRENT)).build())
                .addAction(playAction)
                .addAction(new androidx.core.app.NotificationCompat.Action.Builder(R.drawable.next,"next",PendingIntent.getService(getApplicationContext(),intent.getIntExtra("next",0),intent.setAction("action_next"),PendingIntent.FLAG_UPDATE_CURRENT)).build())
                .addAction(new androidx.core.app.NotificationCompat.Action.Builder(R.drawable.close,"next",PendingIntent.getService(getApplicationContext(),intent.getIntExtra("close",0),intent.setAction("action_close"),PendingIntent.FLAG_UPDATE_CURRENT)).build())
                .build();
        startForeground(10000,notification);
//        notificationManager.notify(10000,notification);
    }

    public HashMap<String,String> getCurrentSong(){
        return songs.get(current);
    }

    private void createMusicPlayer(){
        //Creates musicPlayer for playing songs
        HashMap<String,String> currentSong = songs.get(current);//get current song
        mediaPlayer = MediaPlayer.create(getApplicationContext(), Uri.parse(currentSong.get("data")));
        mediaPlayer.setOnPreparedListener(this);//play song
        mediaPlayer.setOnCompletionListener(mediaPlayer1 -> {
            //when finished plays next song based on repeat status
            mediaPlayer1.release();
            current = repeat.equals("track")? current:current + 1; //repeat current song
            if(current == songs.size()){
                if(repeat.equals("repeat")){
                    current = 0;//goes back to first song
                }
                else{
                    stopSelf();//stops playing song
                }
            }
            createMusicPlayer();
        });
        mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE|PlaybackStateCompat.ACTION_SKIP_TO_NEXT|PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
                .setState(PlaybackStateCompat.STATE_PLAYING,0,1.0f)
                .build()
        );
    }
    @SuppressWarnings("unchecked")
    public void reset(int position,ArrayList<HashMap<String, String>> newSongs){
        //plays new song list at a position
        original = (ArrayList<HashMap<String, String>>) newSongs.clone();
        songs = (ArrayList<HashMap<String, String>>) original.clone();
        playAnotherSong(position);
        if(!mediaPlayer.isPlaying()){
            callback.setLogo(true);
        }
        if(shuffle){
            shuffle();
        }
    }
    private void configureMediaSession(){
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            //set methods to manage controls from the lock screen
            @Override
            public void onSeekTo(long pos) {
                mediaPlayer.seekTo((int)pos);
            }

            @Override
            public void onPlay() {
                super.onPlay();
                play();
            }

            @Override
            public void onSkipToPrevious() {
                super.onSkipToPrevious();
                previous();
            }

            @Override
            public void onSkipToNext() {
                super.onSkipToNext();
                 next();
            }

            @Override
            public void onPause() {
                super.onPause();
                pause();
            }

            @Override
            public void onStop() {
                mediaPlayer.stop();
                mediaPlayer.release();
            }
        });
        mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE|PlaybackStateCompat.ACTION_SKIP_TO_NEXT|PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
                .setState(PlaybackStateCompat.STATE_STOPPED,0,1.0f)
                .build()
        );

        mediaSession.setActive(true);
    }
    private void createNotificationChannel(){
        //create noti channel for the song
        String channelName = "MusicPlayer";
        String description = "Play music!";
        int importance = NotificationManager.IMPORTANCE_LOW;//prevents vibration when a new song plays
        NotificationChannel notificationChannel  = new NotificationChannel(channelName,channelName,importance);
        notificationChannel.setDescription(description);
        notificationChannel.setVibrationPattern(null);
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(notificationChannel);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        if(!mediaPlayer.isPlaying()){
            stopForeground(true);
        }
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onDestroy() {
        mediaPlayer.stop();
        mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE|PlaybackStateCompat.ACTION_SKIP_TO_NEXT|PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
                .setState(PlaybackStateCompat.STATE_STOPPED,0,1.0f)
                .build()
        );
        stopForeground(true);
        super.onDestroy();
    }
}
