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
import android.graphics.drawable.Drawable;
import android.media.MediaMetadata;
import android.media.MediaMetadataRetriever;
import android.media.session.MediaSession;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;

import com.example.musicplayer.fragments.MainFragment;
import com.example.musicplayer.fragments.PlayingFragment;
import com.example.musicplayer.interfaces.Callback;
import com.example.musicplayer.workers.MusicPlayer;
import com.example.musicplayer.workers.SongManager;
import com.jakewharton.disklrucache.DiskLruCache;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;
import android.util.LruCache;
import android.view.View;

import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.Result;

public class MainActivity extends AppCompatActivity implements Callback {

    private final String LOG_TAG = "Music Player";
    private MusicPlayer musicPlayer;
    private MediaSession mediaSession;
    private boolean bound = false;
    private SongManager songManager;
    private TextView artistTextView,songNameTextView;
    private ImageView albumArtView;
    private Intent intent;
    private ImageButton play,previous,next;
    private ServiceConnection serviceConnection;
    private LinearLayoutCompat playing;
    private boolean isPlaying;
    private PlayingFragment playingFragment;
    private LruCache<String,Bitmap> albumArtCache;
    private DiskLruCache cache;
    private final Object cacheLock = new Object();
    private final int DISK_CACHE_SIZE = 1024*1024*100;
    private final String directory = "Album Art";
    private boolean initialising = true;
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
        getSongs();
        playing.setOnClickListener(view -> {
            if(!songNameTextView.getText().toString().equals("No music playing!")){
                FragmentManager fragmentManager = getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.add(R.id.fragment,playingFragment).addToBackStack("yes").commit();
                playing.setVisibility(View.GONE);
                isPlaying = true;
            }
        });
        File diskCache = new File(getCacheDir().getPath() + File.separator + directory);
        new InitDiskCacheTask().execute(diskCache);
        albumArtCache = new LruCache<>(1024*1024*100);
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment,new MainFragment(songManager)).addToBackStack("original").commit();
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
    }

    public View.OnClickListener getOnclickListener(int position,ArrayList<HashMap<String,String>> songs){
        return view -> {
            if(musicPlayer!=null){
                musicPlayer.reset(position);
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
        return songManager.getAlbumArt(album);
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
        }
        Collections.sort(songs,new SortSongs("title"));
        songManager = new SongManager(songs,albumArt,artist);
    }

    private class SortSongs implements Comparator<Map<String, String>>
    {
        private final String key;


        public SortSongs(String key) {
            this.key = key;
        }

        public int compare(Map<String, String> first,
                           Map<String, String> second)
        {
            // TODO: Null checking, both for maps and values
            String firstValue = first.get(key);
            String secondValue = second.get(key);
            return firstValue.compareTo(secondValue);
        }
    }

    private Bitmap getCache(String album){
        synchronized (cacheLock){
            while(initialising){
                try{
                    cacheLock.wait();
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
            }
            if(cache != null){
                try{
                    DiskLruCache.Snapshot snapshot = cache.get(album);
                    if(snapshot == null){return null;}
                    InputStream bitmapInput = snapshot.getInputStream(0);
                    Bitmap albumArt = BitmapFactory.decodeStream(bitmapInput);
                    return albumArt;
                }catch(IOException e){
                    e.printStackTrace();
                }
            }
            return null;
        }
    }

    private void storeCache(Bitmap albumArt,String album){
        synchronized (cacheLock){
            while(initialising){
                try{
                    cacheLock.wait();
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
            }
        }
        try{
            DiskLruCache.Editor editor = cache.edit(album);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            albumArt.compress(Bitmap.CompressFormat.PNG,100,baos);
            editor.newOutputStream(0).write(baos.toByteArray());
            baos.close();
            editor.commit();
        }catch(IOException e){
            e.printStackTrace();
        }

    }

    class InitDiskCacheTask extends AsyncTask<File,Void,Void>{

        @Override
        protected Void doInBackground(File... files) {
            synchronized (cacheLock){
                File cacheDir = files[0];
                try {
                    cache = DiskLruCache.open(cacheDir,1,songManager.getAlbum().size(),DISK_CACHE_SIZE);
                    cacheLock.notifyAll();
                    initialising = false;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }
    }

    public class BitmapWorkerTask extends AsyncTask<HashMap<String,String>,Void,Void>{
        @Override
        protected Void doInBackground(HashMap<String,String>... songs) {
            for (HashMap<String,String> song:songs
                 ) {
                String album = song.get("album");
                Bitmap albumArt = getCache(album);
                if(albumArt == null){
                    MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
                    mediaMetadataRetriever.setDataSource(song.get("data"));
                    byte[] albumBytes = mediaMetadataRetriever.getEmbeddedPicture();
                    if(albumBytes == null){
                        albumArt =  BitmapFactory.decodeResource(getResources(),R.drawable.placeholder);
                        albumArtCache.put(album,albumArt);
                        storeCache(albumArt,song.get("album"));
                    }
                    albumArt = BitmapFactory.decodeByteArray(albumBytes,0,albumBytes.length);
                    albumArtCache.put(album,albumArt);
                    storeCache(albumArt,song.get("album"));
                }
                albumArtCache.put(album,albumArt);
            }
            return null;
        }
    }
}