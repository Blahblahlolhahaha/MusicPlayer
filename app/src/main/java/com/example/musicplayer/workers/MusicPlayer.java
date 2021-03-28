package com.example.musicplayer.workers;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaMetadata;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Base64;
import android.view.KeyEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationManagerCompat;
import androidx.media.MediaBrowserServiceCompat;
import androidx.media.app.NotificationCompat;
import androidx.media.session.MediaButtonReceiver;

import com.example.musicplayer.R;
import com.example.musicplayer.interfaces.Callback;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MusicPlayer extends MediaBrowserServiceCompat implements MediaPlayer.OnPreparedListener {
    private MediaPlayer mediaPlayer = null;
    private ArrayList<MediaBrowserCompat.MediaItem> original;
    private ArrayList<MediaBrowserCompat.MediaItem> songs;
    private int current;
    private boolean shuffle;
    private String repeat;
    private final IBinder musicPlayerBinder = new MusicPlayerBinder();
    private Callback callback;
    private MediaSessionCompat mediaSession;
    private NotificationManagerCompat notificationManager;

    @Override
    public void onCreate() {
        super.onCreate();
        mediaSession = new MediaSessionCompat(this,"mediaSession");
        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        mediaButtonIntent.setClass(getApplicationContext(), MediaButtonReceiver.class);
        PendingIntent mediaButtonPendingIntent = PendingIntent.getBroadcast(getApplicationContext(),0,mediaButtonIntent,0);
        mediaSession.setMediaButtonReceiver(mediaButtonPendingIntent);
        configureMediaSession();
        setSessionToken(mediaSession.getSessionToken());
    }

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

            Bundle bundle = intent.getExtras();
            original = (ArrayList<MediaBrowserCompat.MediaItem>) bundle.get("songs");//get song list
            songs = (ArrayList<MediaBrowserCompat.MediaItem>) original.clone();//clone cos pass-by-value is kinda sad
            int start = bundle.getInt("start");//check starting position for song
            shuffle = getSharedPreferences("settings",0).getBoolean("shuffle",false);
            repeat = getSharedPreferences("settings",0).getString("repeat","no");
            current = start;
            if(shuffle){
                // if shuffle, shuffle the queue
                shuffle();
            }
            if (android.os.Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1) {
                createNotificationChannel();
            }
            createMusicPlayer();
            MediaButtonReceiver.handleIntent(mediaSession,intent);
            encodeRecent();
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

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {
        return new BrowserRoot("Music Player", null);
    }

    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        result.sendResult(getRecent());
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        //starts the song
        mediaPlayer.start();
        callback.callback((String) songs.get(current).getDescription().getTitle(),songs.get(current).getDescription().getExtras().getString("artist"),songs.get(current).getDescription().getExtras().getString("albumID"),songs.get(current).getDescription().getExtras().getString("duration"));
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
        int position  = mediaPlayer.getCurrentPosition();
        mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE|PlaybackStateCompat.ACTION_SKIP_TO_NEXT|PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS|PlaybackStateCompat.ACTION_SEEK_TO )
                .setState(PlaybackStateCompat.STATE_PLAYING,position,1.0f)
                .build());
        return position;
    }

    public void setPosition(int position){
        mediaPlayer.seekTo(position);
        mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE|PlaybackStateCompat.ACTION_SKIP_TO_NEXT|PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS|PlaybackStateCompat.ACTION_SEEK_TO )
                .setState(PlaybackStateCompat.STATE_PLAYING,getPosition(),1.0f)
                .build()
        );
    }

    public void pause(){
        mediaPlayer.pause();
        callback.setLogo(false);
        createNotification();
        mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE|PlaybackStateCompat.ACTION_SKIP_TO_NEXT|PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS|PlaybackStateCompat.ACTION_SEEK_TO )
                .setState(PlaybackStateCompat.STATE_PAUSED,getPosition(),1.0f)
                .build()
        );
    }
    public void play(){
        mediaPlayer.start();
        callback.setLogo(true);
        createNotification();
        mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE|PlaybackStateCompat.ACTION_SKIP_TO_NEXT|PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS|PlaybackStateCompat.ACTION_SEEK_TO )
                .setState(PlaybackStateCompat.STATE_PLAYING,getPosition(),1.0f)
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
            MediaBrowserCompat.MediaItem current_song = songs.get(current);
            songs = (ArrayList<MediaBrowserCompat.MediaItem>) original.clone();
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
        MediaBrowserCompat.MediaItem current_song = songs.get(current);
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
                .setStyle(mediaStyle.setShowActionsInCompactView(0,1,2).setMediaSession(mediaSession.getSessionToken()))
                .addAction(new androidx.core.app.NotificationCompat.Action.Builder(R.drawable.previous,"previous",PendingIntent.getService(getApplicationContext(),intent.getIntExtra("previous",0),intent.setAction("action_previous"),PendingIntent.FLAG_UPDATE_CURRENT)).build())
                .addAction(playAction)
                .addAction(new androidx.core.app.NotificationCompat.Action.Builder(R.drawable.next,"next",PendingIntent.getService(getApplicationContext(),intent.getIntExtra("next",0),intent.setAction("action_next"),PendingIntent.FLAG_UPDATE_CURRENT)).build())
                .addAction(new androidx.core.app.NotificationCompat.Action.Builder(R.drawable.close,"next",PendingIntent.getService(getApplicationContext(),intent.getIntExtra("close",0),intent.setAction("action_close"),PendingIntent.FLAG_UPDATE_CURRENT)).build())
                .build();
        startForeground(10000,notification);
//        notificationManager.notify(10000,notification);
    }

    public MediaBrowserCompat.MediaItem getCurrentSong(){
        return songs.get(current);
    }

    private void createMusicPlayer(){
        //Creates musicPlayer for playing songs
        MediaBrowserCompat.MediaItem currentSong = songs.get(current);//get current song
        mediaPlayer = MediaPlayer.create(getApplicationContext(), Uri.parse(currentSong.getDescription().getMediaUri().toString()));
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
                .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE|PlaybackStateCompat.ACTION_SKIP_TO_NEXT|PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS|PlaybackStateCompat.ACTION_SEEK_TO )
                .setState(PlaybackStateCompat.STATE_PLAYING,getPosition(),1.0f)
                .build()
        );
    }
    @SuppressWarnings("unchecked")
    public void reset(int position, ArrayList<MediaBrowserCompat.MediaItem> newSongs){
        //plays new song list at a position
        original = (ArrayList<MediaBrowserCompat.MediaItem>) newSongs.clone();
        songs = (ArrayList<MediaBrowserCompat.MediaItem>) original.clone();
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
            public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
                KeyEvent keyEvent = (KeyEvent) mediaButtonEvent.getExtras().get(Intent.EXTRA_KEY_EVENT);
                if(keyEvent.getAction() == KeyEvent.ACTION_DOWN){
                    switch(keyEvent.getKeyCode()){
                        case KeyEvent.KEYCODE_MEDIA_PLAY:
                            play();
                            break;
                        case KeyEvent.KEYCODE_MEDIA_PAUSE:
                            pause();
                            break;
                        case KeyEvent.KEYCODE_MEDIA_NEXT:
                            next();
                            break;
                        case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                            previous();
                            break;
                        case KeyEvent.KEYCODE_MEDIA_STOP:
                            mediaPlayer.stop();
                            mediaPlayer.release();
                            break;
                    }
                }
                return super.onMediaButtonEvent(mediaButtonEvent);
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

            @Override
            public void onFastForward() {
                mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                        .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE|PlaybackStateCompat.ACTION_SKIP_TO_NEXT|PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS|PlaybackStateCompat.ACTION_SEEK_TO )
                        .setState(PlaybackStateCompat.STATE_PLAYING,getPosition(),1.0f)
                        .build()
                );
            }

            @Override
            public void onRewind() {
                mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                        .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE|PlaybackStateCompat.ACTION_SKIP_TO_NEXT|PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS|PlaybackStateCompat.ACTION_SEEK_TO )
                        .setState(PlaybackStateCompat.STATE_PLAYING,getPosition(),1.0f)
                        .build()
                );
            }
        });
        mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE|PlaybackStateCompat.ACTION_SKIP_TO_NEXT|PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS|PlaybackStateCompat.ACTION_SEEK_TO )
                .setState(PlaybackStateCompat.STATE_STOPPED,0,1.0f)
                .build()
        );

        mediaSession.setActive(true);
    }
    @RequiresApi(api = Build.VERSION_CODES.O)
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

    private void encodeRecent(){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(songs);
            String encoded = new String(Base64.encode(baos.toByteArray(),Base64.DEFAULT));
            SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("recent",MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("recent",encoded);
            editor.apply();
            baos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private ArrayList<MediaBrowserCompat.MediaItem> getRecent(){
        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("recent",MODE_PRIVATE);
        byte[] decoded = Base64.decode(sharedPreferences.getString("recent",""),Base64.DEFAULT);
        ByteArrayInputStream bais = new ByteArrayInputStream(decoded);
        try {
            ObjectInputStream oos = new ObjectInputStream(bais);
            ArrayList<MediaBrowserCompat.MediaItem> recent = (ArrayList<MediaBrowserCompat.MediaItem>) oos.readObject();
            bais.close();
            return recent;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
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
