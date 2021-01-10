package com.example.musicplayer;

import android.Manifest;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.musicplayer.fragments.AlbumSongsFragment;
import com.example.musicplayer.fragments.ArtistFragment;
import com.example.musicplayer.fragments.DeleteDialog;
import com.example.musicplayer.fragments.DetailsEditFragment;
import com.example.musicplayer.fragments.MainFragment;
import com.example.musicplayer.fragments.PlayingFragment;
import com.example.musicplayer.interfaces.Callback;
import com.example.musicplayer.workers.CacheWorker;
import com.example.musicplayer.workers.MusicPlayer;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity implements Callback {

    private final String LOG_TAG = "Music Player";
    private MusicPlayer musicPlayer;
    private MediaSessionCompat mediaSession;
    private boolean bound = false;
    private TextView artistTextView,songNameTextView;
    private ImageView albumArtView;
    private Intent intent;
    private ImageButton play,previous,next;
    private Button details,add,delete;
    private ServiceConnection serviceConnection;
    private LinearLayoutCompat playing,select;
    private boolean isPlaying,album,artist,selecting,viewing;
    private PlayingFragment playingFragment;
    private CacheWorker cacheWorker;
    private ArrayList<HashMap<String,String>> selectedSongs =  new ArrayList<>();
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        playingFragment = new PlayingFragment();
        artistTextView = findViewById(R.id.artist);
        songNameTextView = findViewById(R.id.song);
        albumArtView = findViewById(R.id.album_art);
        play = findViewById(R.id.play);
        previous = findViewById(R.id.previous);
        next = findViewById(R.id.next);
        playing = findViewById(R.id.playing);
        select = findViewById(R.id.selecting);
        add = findViewById(R.id.addd);
        details = findViewById(R.id.details);
        delete = findViewById(R.id.delete);
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
                previous.setOnClickListener(view -> musicPlayer.previous());
                next.setOnClickListener(view ->musicPlayer.next());
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
                fragmentTransaction(playingFragment,"yes");
                playing.setVisibility(View.GONE);
                isPlaying = true;
            }
        });
        details.setOnClickListener(view->{
            DetailsEditFragment detailsEditFragment = new DetailsEditFragment(selectedSongs.get(0));
            fragmentTransaction(detailsEditFragment,"details");
            viewing = true;
            selecting = false;
            select.setVisibility(View.GONE);
        });
        delete.setOnClickListener(view->{
            new DeleteDialog(selectedSongs).showDialog(MainActivity.this);
            selectedSongs.clear();
            select();
        });
        fragmentTransaction(new MainFragment(cacheWorker.getSongsMap(),cacheWorker.getAlbumMap(),cacheWorker.getArtistMap()),"original");
    }

    public void isStoragePermissionGranted() {
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            Log.v(LOG_TAG,"Permission is granted");
        } else {

            Log.v(LOG_TAG,"Permission is revoked");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            Log.v(LOG_TAG,"Permission is granted");
        } else {

            Log.v(LOG_TAG,"Permission is revoked");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
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
        else if(artist){
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.popBackStack("artist",FragmentManager.POP_BACK_STACK_INCLUSIVE);
            artist = false;
        }
        else if(selecting){
            selecting = false;
            select();
            selectedSongs.clear();
        }
        else if(viewing){
           Intent i = new Intent(this,MainActivity.class);
           startActivity(i);
        }
    }
    
    public View.OnClickListener getSongOnclickListener(int position, ArrayList<HashMap<String,String>> songs){
        return view -> {
            if(!selecting){
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
            }
            else{
                LinearLayoutCompat linearLayout = view.findViewById(R.id.background);
                boolean selected = view.isSelected();
                view.setSelected(!selected);
                if(!selected){
                    selectedSongs.add(songs.get(position));
                    if(selectedSongs.size() > 1 && details.getVisibility() != View.GONE){
                        details.setVisibility(View.GONE);
                    }
                   linearLayout.setBackgroundColor(getResources().getColor(R.color.blue,null));
                }
                else{
                    selectedSongs.remove(songs.get(position));
                    if(selectedSongs.size() == 1 && details.getVisibility() == View.GONE){
                        details.setVisibility(View.VISIBLE);
                    }
                    else if(selectedSongs.size() == 0){
                        selecting = false;
                        select();
                    }
                    linearLayout.setBackgroundColor(getResources().getColor(R.color.black,null));
                }
            }
        };
    }

    public View.OnLongClickListener getSongOnLongClickListener(HashMap<String,String> song){
        return view -> {
            if(!selecting){
                LinearLayoutCompat linearLayout = view.findViewById(R.id.background);
                view.setSelected(true);
                selectedSongs.add(song);
                selecting = true;
                linearLayout.setBackgroundColor(getColor(R.color.blue));
                select();
            }
            return true;
        };
    }

    public View.OnClickListener getArtistOnClickListener(String artistName){
        return view -> {
            ArrayList<HashMap<String,String>> songs = cacheWorker.getArtistSongs(artistName);
            ArrayList<HashMap<String,String>> albums = cacheWorker.getArtistAlbums(artistName);
            ArtistFragment artistFragment = new ArtistFragment(artistName,songs,albums);
            fragmentTransaction(artistFragment,"artist");
            artist = true;
        };
    }

    public View.OnClickListener getAlbumOnClickListener(final HashMap<String,String> albumMap){
        return view -> {
            String id = albumMap.get("ID");
            ArrayList<HashMap<String,String>> albumsSongs = cacheWorker.getAlbumSongs(id);
            AlbumSongsFragment albumSongsFragment = new AlbumSongsFragment(albumsSongs,getAlbumArt(id),albumMap.get("name"),albumMap.get("artist"));
            fragmentTransaction(albumSongsFragment,"album");
            album = true;
        };
    }



    public void callback(String songName, String artist, String album){
        songNameTextView.setText(songName);
        artistTextView.setText(artist);
        Bitmap albumArt = getAlbumArt(album);
        albumArtView.setImageBitmap(albumArt);
        playingFragment.setSongInfo(songName,artist,albumArt);
        mediaSession.setMetadata(new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE,songName)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST,artist)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM,album)
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART,albumArt)
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

    public Bitmap getArtistAlbumArt(String artist){
        return cacheWorker.getArtistAlbumArt(artist);
    }

    public String getAlbumName(String ID){return cacheWorker.getAlbumName(ID);}

    public String getAlbumID(String name){return cacheWorker.getAlbumID(name);}

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

    private void select(){
        select.setVisibility(selecting?View.VISIBLE:View.GONE);
        playing.setVisibility(selecting?View.GONE:View.VISIBLE);
    }

    private void fragmentTransaction(Fragment fragment,String name){
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction  fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment,fragment).addToBackStack(name).commit();
    }
}