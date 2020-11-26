package com.example.musicplayer;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.os.Bundle;

import com.example.musicplayer.fragments.MainFragment;
import com.example.musicplayer.interfaces.Callback;
import com.example.musicplayer.workers.MusicPlayer;
import com.example.musicplayer.workers.SongManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.os.IBinder;
import android.os.PersistableBundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements Callback {

    private final String LOG_TAG = "Music Player";
    private MusicPlayer musicPlayer;
    private boolean bound = false;
    private SongManager songManager;
    private TextView artistTextView,songNameTextView;
    private ImageView albumArtView;
    private Intent intent;
    private ImageButton play,previous,next;
    private ServiceConnection serviceConnection;
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        artistTextView = findViewById(R.id.artist);
        songNameTextView = findViewById(R.id.song_name);
        albumArtView = findViewById(R.id.album_art);
        play = findViewById(R.id.play);
        previous = findViewById(R.id.previous);
        next = findViewById(R.id.next);
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                MusicPlayer.MusicPlayerBinder binder = (MusicPlayer.MusicPlayerBinder) iBinder;
                musicPlayer = binder.getService();
                musicPlayer.registerCallback(MainActivity.this);
                play.setOnClickListener(view -> {
                    if(musicPlayer.getPlayingStatus()){
                        play.setBackground(ContextCompat.getDrawable(getApplicationContext(),R.drawable.play));
                        musicPlayer.pause();
                    }
                    else{
                        play.setBackground(ContextCompat.getDrawable(getApplicationContext(),R.drawable.pause));
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
        getSongs();
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment,new MainFragment(songManager)).commit();
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

    public View.OnClickListener getOnclickListener(int position,ArrayList<HashMap<String,String>> songs){
        return view -> {
            if(musicPlayer!=null){
                musicPlayer.playAnotherSong(position);
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

    public void callback(String songName, String artist, String album){
        songNameTextView.setText(songName);
        artistTextView.setText(artist);
        albumArtView.setImageBitmap(getAlbumArt(album));

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

    public Bitmap getAlbumArt(String album){
        return songManager.getAlbumArt(album);
    }

    private void getSongs(){
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.ARTIST_ID,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.YEAR
        };
        String[] projection1 = {
                MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ALBUM
        };
        String[] projection2 = {
                MediaStore.Audio.Artists._ID, MediaStore.Audio.Artists.ARTIST
        };
        Cursor cursor = getApplicationContext().getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                null
        );
        Cursor c = getApplicationContext().getContentResolver().query(
                MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                projection1,
                null,
                null,
                null
        );
        Cursor cc = getApplicationContext().getContentResolver().query(
                MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
                projection2,
                null,
                null,
                null
        );
        ArrayList<HashMap<String,String>> songs = new ArrayList<>();
        HashMap<String,Bitmap> albumArt = new HashMap<>();
        HashMap<String,String> artist = new HashMap<>();
        HashMap<String,String> album = new HashMap<>();
        while(cc.moveToNext()){
            artist.put(cc.getString(0),cc.getString(1));
        }
        while(c.moveToNext()){
            album.put(c.getString(0),c.getString(1));
        }
        while(cursor.moveToNext()){
            HashMap<String,String> song = new HashMap<>();
            song.put("ID",cursor.getString(0));
            song.put("title",cursor.getString(1));
            song.put("data",cursor.getString(2));
            song.put("display_name",cursor.getString(3));
            song.put("artist",artist.get(cursor.getString(4)));
            song.put("album",album.get(cursor.getString(5)));
            song.put("duration",cursor.getString(6));
            song.put("year",cursor.getString(7));
            songs.add(song);
            if(albumArt.get(cursor.getString(5)) == null){
                MediaMetadataRetriever mmr =  new MediaMetadataRetriever();
                mmr.setDataSource(cursor.getString(2));
                byte[] picArray = mmr.getEmbeddedPicture();
                if(picArray!=null){
                    Bitmap art = BitmapFactory.decodeByteArray(picArray,0,picArray.length);
                    albumArt.put(album.get(cursor.getString(5)),art);
                }
            }

        }
        songManager = new SongManager(songs,albumArt,artist);
    }
}