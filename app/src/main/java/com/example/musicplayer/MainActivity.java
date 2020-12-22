package com.example.musicplayer;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.os.Build;
import android.os.Bundle;

import com.example.musicplayer.fragments.AlbumSongsFragment;
import com.example.musicplayer.fragments.MainFragment;
import com.example.musicplayer.fragments.SongFragment;
import com.example.musicplayer.fragments.PlayingFragment;
import com.example.musicplayer.interfaces.Callback;
import com.example.musicplayer.workers.CacheWorker;
import com.example.musicplayer.workers.MusicPlayer;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.os.IBinder;
import android.util.Log;
import android.view.View;

import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity implements Callback {

    private final String LOG_TAG = "Music Player";
    private MusicPlayer musicPlayer;
    private MediaSession mediaSession;
    private boolean bound = false;
    private TextView artistTextView,songNameTextView;
    private ImageView albumArtView;
    private Intent intent;
    private ImageButton play,previous,next;
    private ServiceConnection serviceConnection;
    private LinearLayoutCompat playing;
    private boolean isPlaying,album;
    private PlayingFragment playingFragment;
    private CacheWorker cacheWorker;
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        playingFragment = new PlayingFragment();
        artistTextView = findViewById(R.id.artist);
        songNameTextView = findViewById(R.id.song_name);
        albumArtView = findViewById(R.id.album_art);
        play = findViewById(R.id.play);
        previous = findViewById(R.id.previous);
        next = findViewById(R.id.next);
        playing = findViewById(R.id.playing);
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                MusicPlayer.MusicPlayerBinder binder = (MusicPlayer.MusicPlayerBinder) iBinder;
                musicPlayer = binder.getService();
                musicPlayer.registerCallback(MainActivity.this);
                mediaSession = musicPlayer.getMediaSession();
                play.setOnClickListener(view -> {
                    if(musicPlayer.getPlayingStatus()){
                        musicPlayer.pause();
                    }
                    else{
                        musicPlayer.play();
                    }
                });
                previous.setOnClickListener(view -> {
                    musicPlayer.previous();
                });
                next.setOnClickListener(view -> {
                    musicPlayer.next();
                });
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                bound = false;
            }
        };
        isStoragePermissionGranted();
        cacheWorker = new CacheWorker(getApplicationContext(),getCacheDir()+File.separator+"albumArt");
        playing.setOnClickListener(view -> {
            if(!songNameTextView.getText().toString().equals("No music playing!")){
                FragmentManager fragmentManager = getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.add(R.id.fragment,playingFragment).addToBackStack("yes").commit();
                playing.setVisibility(View.GONE);
                isPlaying = true;
            }
        });
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment,new MainFragment(cacheWorker.getSongsMap(),cacheWorker.getAlbumMap(),cacheWorker.getArtistMap())).addToBackStack("original").commit();
    }

    public  boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v(LOG_TAG,"Permission is granted");
                return true;
            } else {

                Log.v(LOG_TAG,"Permission is revoked");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                return false;
            }
        }
        else {
            Log.v(LOG_TAG,"Permission is granted");
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            Log.v(LOG_TAG,"Permission: "+permissions[0]+ "was "+grantResults[0]);
        }
    }

    @Override
    public void onBackPressed() {
        if(isPlaying){
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.popBackStack("yes",FragmentManager.POP_BACK_STACK_INCLUSIVE);
            playing.setVisibility(View.VISIBLE);
            isPlaying = false;
        }
        else if(album){
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.popBackStack("album",FragmentManager.POP_BACK_STACK_INCLUSIVE);
            album = false;
        }
    }
    
    public View.OnClickListener getOnclickListener(int position,ArrayList<HashMap<String,String>> songs){
        return view -> {
            if(musicPlayer!=null){
                musicPlayer.reset(position,songs);
            }
            else{
                intent = new Intent(this, MusicPlayer.class);
                intent.putExtra("songs",songs);
                intent.putExtra("start",position);
                bindService();
                startService(intent);
                play.setBackground(ContextCompat.getDrawable(getApplicationContext(),R.drawable.pause));
            }
        };
    }

    public View.OnClickListener getAlbumOnClickListener(String id){
        return view -> {
            ArrayList<HashMap<String,String>> albumsSongs = cacheWorker.getAlbumSongs(id);
            String[] albumInfo = cacheWorker.getAlbumMap().get(id).split(",");
            AlbumSongsFragment albumSongsFragment = new AlbumSongsFragment(albumsSongs,getAlbumArt(id),albumInfo[0],albumInfo[1]);
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.fragment,albumSongsFragment).addToBackStack("album").commit();
            album = true;
        };
    }

    public void callback(String songName, String artist, String album){
        songNameTextView.setText(songName);
        artistTextView.setText(artist);
        Bitmap albumArt = getAlbumArt(album);
        albumArtView.setImageBitmap(albumArt);
        playingFragment.setSongInfo(songName,artist,albumArt);
        mediaSession.setMetadata(new MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE,songName)
                .putString(MediaMetadata.METADATA_KEY_ARTIST,artist)
                .putString(MediaMetadata.METADATA_KEY_ALBUM,album)
                .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART,albumArt)
                .build()
        );
        musicPlayer.createNotification();
    }

    public void setLogo(boolean playing){
        if(playing){
            play.setBackground(ContextCompat.getDrawable(getApplicationContext(),R.drawable.pause));
        }
        else{
            play.setBackground(ContextCompat.getDrawable(getApplicationContext(),R.drawable.play));
        }
    }
    public Bitmap getAlbumArt(String album){
        return cacheWorker.getAlbumArt(album);
    }

    public MusicPlayer getMusicPlayer(){
        return musicPlayer;
    }

    private void bindService(){
        bindService(intent,serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void unbindService(){
        if(bound){
            musicPlayer.registerCallback(null);
            unbindService(serviceConnection);
            bound = false;
        }
    }
}