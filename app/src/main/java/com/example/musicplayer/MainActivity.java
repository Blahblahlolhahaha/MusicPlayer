package com.example.musicplayer;

import android.Manifest;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

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
import com.example.musicplayer.dialogs.DeleteDialog;
import com.example.musicplayer.fragments.DetailsEditFragment;
import com.example.musicplayer.fragments.GenreSongsFragment;
import com.example.musicplayer.fragments.MainFragment;
import com.example.musicplayer.fragments.PlayingFragment;
import com.example.musicplayer.fragments.PlaylistSongsFragment;
import com.example.musicplayer.interfaces.Callback;
import com.example.musicplayer.workers.CacheWorker;
import com.example.musicplayer.workers.Genre;
import com.example.musicplayer.workers.MusicPlayer;
import com.example.musicplayer.workers.Playlist;

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
    private Button details,add,delete,playSelected,remove;
    private ServiceConnection serviceConnection;
    private LinearLayoutCompat playing,select;
    private boolean isPlaying,album,artist,selecting,viewing,playlist,playlistSongs,genre;
    private PlayingFragment playingFragment;
    private CacheWorker cacheWorker;
    private ArrayList<HashMap<String,String>> selectedSongs =  new ArrayList<>();
    private ArrayList<View> selectedViews = new ArrayList<>();
    private Playlist currentPlaylist;
    private MainFragment main;
    private BroadcastReceiver broadcastReceiver;
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
        playSelected = findViewById(R.id.play_selected);
        remove = findViewById(R.id.remove);
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                // get music player from the service to control from main activity through the buttons
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
                if(musicPlayer.getPlayingStatus()){
                    HashMap<String,String> currentSong = musicPlayer.getCurrentSong();
                    String songName = currentSong.get("title");
                    String artist = currentSong.get("artist");
                    String album = currentSong.get("album");
                    songNameTextView.setText(songName);
                    artistTextView.setText(artist);
                    Bitmap albumArt = getAlbumArt(album);
                    albumArtView.setImageBitmap(albumArt);
                    playingFragment.setSongInfo(songName,artist,albumArt,currentSong.get("duration"));
                    setLogo(true);
                }
            }

            @Override
            public void onNullBinding(ComponentName name) {
                Log.d("sada","dead");
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
                // go to playing fragment if there is no music playing
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
            DeleteDialog deleteDialog = new DeleteDialog();
            deleteDialog.showDialog(selectedSongs,new HashMap(),MainActivity.this); //show dialog for confirmation to delete
//            selectedSongs.clear(); // clear the selected array after deleting/cancelling
            select(); // revert back to original view
        });
        playSelected.setOnClickListener(view->{
            //play selected songs
            if(musicPlayer!=null){
                musicPlayer.reset(0,selectedSongs);
            }
            else{
                intent = new Intent(this, MusicPlayer.class);
                intent.putExtra("songs",selectedSongs);
                intent.putExtra("start",0);
                bindService(intent,serviceConnection, Context.BIND_AUTO_CREATE);;
                startForegroundService(intent);
                play.setBackground(ContextCompat.getDrawable(getApplicationContext(),R.drawable.pause));

            }
            this.onBackPressed();

        });
        remove.setOnClickListener(view -> {
            //remove songs from playlist based on ID
            String[]IDs = new String[selectedSongs.size()];
            for(int i = 0; i<IDs.length;i++){
                IDs[i] = selectedSongs.get(i).get("ID");
            }
            this.currentPlaylist.removeSongs(IDs,getApplicationContext()); // songs are removed here
            onBackPressed(); //reset state back to normal
            fragmentTransaction(new PlaylistSongsFragment(currentPlaylist),"playlist"); //refresh the playlist to reflect changes
        });
        if(isMusicPlayerRunning()){
            intent = new Intent(this, MusicPlayer.class);
            intent.setAction("restart_app");
            bindService(intent,serviceConnection, Context.BIND_AUTO_CREATE);
            startForegroundService(intent);

        }
        main = new MainFragment(cacheWorker.getSongsMap(),cacheWorker.getAlbumMap(),cacheWorker.getArtistMap(),cacheWorker.getGenreMap(),cacheWorker.getPlaylistMap());
        fragmentTransaction(main,"original"); // instantiate first view
    }

    public void isStoragePermissionGranted() {
        //check for read/write permissions
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            Log.v(LOG_TAG,"Permission is granted");
        } else {
            //if not granted, request permission
            Log.v(LOG_TAG,"Permission is revoked");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            Log.v(LOG_TAG,"Permission is granted");
        } else {
            //if not granted, request permission
            Log.v(LOG_TAG,"Permission is revoked");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        //request for required permission
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            Log.v(LOG_TAG,"Permission: "+permissions[0]+ "was "+grantResults[0]);
        }
    }

    @Override
    public void onBackPressed() {
        //checks for fragment app is in now and execute the required actions
        if(isPlaying){ //if at playingFragment
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.popBackStack("yes",FragmentManager.POP_BACK_STACK_INCLUSIVE);
            playing.setVisibility(View.VISIBLE);
            isPlaying = false;
        }
        else if(album){ //if at albumSongsFragment
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.popBackStack("album",FragmentManager.POP_BACK_STACK_INCLUSIVE);
            album = false;
        }
        else if(artist){ //if at artistFragment
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.popBackStack("artist",FragmentManager.POP_BACK_STACK_INCLUSIVE);
            artist = false;
        }
        else if(playlistSongs){//if at selectSongsFragment
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.popBackStack("select",FragmentManager.POP_BACK_STACK_INCLUSIVE);
            fragmentManager.popBackStack("playlist",FragmentManager.POP_BACK_STACK_INCLUSIVE);
            fragmentTransaction(new PlaylistSongsFragment(currentPlaylist),"playlist");
            playlistSongs = false;
            selecting = false;
            select();
            unSelect();
        }
        else if(selecting){// if selecting songs
            selecting = false;
            select();
            unSelect();
        }
        else if(viewing){//if at DetailsEditFragment
           Intent i = new Intent(this,MainActivity.class);
           startActivity(i);
        }
        else if(playlist){//if at playlistSongsFragment
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.popBackStack("playlist",FragmentManager.POP_BACK_STACK_INCLUSIVE);
            playlist = false;
            currentPlaylist = null;
        }
        else if(genre){
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.popBackStack("genre",FragmentManager.POP_BACK_STACK_INCLUSIVE);
            genre = false;
        }
    }
    
    public View.OnClickListener getSongOnclickListener(int position, ArrayList<HashMap<String,String>> songs){
        return view -> {
            //checks whether if user is selecting songs or not. If selecting, selects/unselects the song based on whether it has already be selected or not
            // else, play chosen song
            if(selecting){
                LinearLayoutCompat linearLayout = view.findViewById(R.id.background);
                boolean selected = view.isSelected();
                view.setSelected(!selected);
                if(!selected){
                    selectedSongs.add(songs.get(position));
                    selectedViews.add(view);
                    if(selectedSongs.size() > 1 && details.getVisibility() != View.GONE){
                        //removes details button when more than one song selected
                        details.setVisibility(View.GONE);
                    }
                    linearLayout.setBackgroundColor(getResources().getColor(R.color.blue,null)); //change background to blue to indicate song is selected
                }
                else{
                    selectedSongs.remove(songs.get(position));
                    selectedViews.remove(view);
                    if(!playlistSongs){
                        if(selectedSongs.size() == 1 && details.getVisibility() == View.GONE){
                            //shows editing choice if only one song is selected
                            details.setVisibility(View.VISIBLE);
                        }
                        else if(selectedSongs.size() == 0){
                            //exits selecting mode when all songs are deselected
                            selecting = false;
                            select();
                        }
                    }
                    linearLayout.setBackgroundColor(getResources().getColor(R.color.black,null)); //change background to black to indicate song is unselected
                }
            }
            else{
                if(musicPlayer!=null){
                    //resets music player if there is a song playing already
                    musicPlayer.reset(position,songs);
                }
                else{
                    //creates a new music player if not playing any song yet
                    intent = new Intent(this, MusicPlayer.class);
                    intent.putExtra("songs",songs);
                    intent.putExtra("start",position);
                    bindService(intent,serviceConnection, Context.BIND_AUTO_CREATE);;
                    if (android.os.Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1) {
                        startForegroundService(intent);
                    }
                    else{
                        startService(intent);
                    }
                    play.setBackground(ContextCompat.getDrawable(getApplicationContext(),R.drawable.pause));
                }
            }
        };
    }

    public View.OnLongClickListener getSongOnLongClickListener(HashMap<String,String> song){
        return view -> {
            //if user long clicks, enters selecting mode if not in it already else will have same effect as clicking
            if(!selecting){
                LinearLayoutCompat linearLayout = view.findViewById(R.id.background);
                view.setSelected(true);
                selectedSongs.add(song);
                selectedViews.add(view);
                selecting = true;
                linearLayout.setBackgroundColor(getColor(R.color.blue));
                select();
                if(playlist){
                    remove.setVisibility(View.VISIBLE);
                    delete.setVisibility(View.GONE);
                }
            }
            else{
                view.performClick();
            }
            return true;
        };
    }

    public View.OnClickListener getArtistOnClickListener(String artistName){
        return view -> {
            //goes into artistFragment with selected artist
            ArrayList<HashMap<String,String>> songs = cacheWorker.getArtistSongs(artistName);
            ArrayList<HashMap<String,String>> albums = cacheWorker.getArtistAlbums(artistName);
            ArtistFragment artistFragment = new ArtistFragment(artistName,songs,albums);
            fragmentTransaction(artistFragment,"artist");
            artist = true;
        };
    }

    public View.OnClickListener getAlbumOnClickListener(final HashMap<String,String> albumMap){
        return view -> {
            //goes into AlbumSongsFragment with selected album
            String id = albumMap.get("ID");
            ArrayList<HashMap<String,String>> albumsSongs = cacheWorker.getAlbumSongs(id);
            AlbumSongsFragment albumSongsFragment = new AlbumSongsFragment(albumsSongs,getAlbumArt(id),albumMap.get("name"),albumMap.get("artist"));
            fragmentTransaction(albumSongsFragment,"album");
            album = true;
        };
    }

    public View.OnClickListener getPlaylistOnClickListener(final Playlist playlist){
        return view->{
            //goes into PlaylistSongsFragment with selected playlist
            PlaylistSongsFragment playlistSongsFragment = new PlaylistSongsFragment(playlist);
            currentPlaylist = playlist;
            fragmentTransaction(playlistSongsFragment,"playlist");
            this.playlist = true;
        };
    }

    public View.OnClickListener getGenreOnClickListener(final Genre genre){
        return view->{
            GenreSongsFragment genreSongsFragment = new GenreSongsFragment(genre.getSongs(),genre.getName());
            fragmentTransaction(genreSongsFragment,"genre");
            this.genre = true;
        };
    }

    public void callback(String songName, String artist, String album, String duration){
        // callback for MusicPlayer to load song information for notification
        songNameTextView.setText(songName);
        artistTextView.setText(artist);
        Bitmap albumArt = getAlbumArt(album);
        albumArtView.setImageBitmap(albumArt);
        playingFragment.setSongInfo(songName,artist,albumArt,duration);
        playingFragment.resetSeekBar();
        playingFragment.startThread();
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

    public Bitmap getGenreAlbumArt(String album){
        return cacheWorker.getAlbumArt(album);
    }

    public String getAlbumName(String ID){return cacheWorker.getAlbumName(ID);}

    public String getAlbumID(String name){return cacheWorker.getAlbumID(name);}

    public MusicPlayer getMusicPlayer(){
        return musicPlayer;
    }

    public ArrayList<HashMap<String, String>> getSelectedSongs() {
        return selectedSongs;
    }

    public CacheWorker getCacheWorker() {
        return cacheWorker;
    }

    public void setCurrentPlaylist(Playlist currentPlaylist) {
        this.currentPlaylist = currentPlaylist;
    }

    public void setSelecting(boolean selecting) {
        this.selecting = selecting;
    }

    public void setPlaylistSongs(boolean playlistSongs) {
        this.playlistSongs = playlistSongs;
    }

    public void fragmentTransaction(Fragment fragment,String name){
        //does transaction of fragments
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction  fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment,fragment).addToBackStack(name).commit();
    }

    public boolean checkSelected(HashMap<String,String> song){
        return selectedSongs.contains(song);
    }

    public void addToSelectedViews(View view){

    }

    public void addPlaylist(Playlist playlist){
        currentPlaylist = playlist;
        this.playlist = true;;
        fragmentTransaction(new PlaylistSongsFragment(playlist),"playlist");
        main.addPlaylist(playlist);
    }

    public void removePlaylist(Playlist playlist){
        onBackPressed();
        main.removePlaylist(playlist);
    }

    public void removeSong(HashMap<String,String> song){
        main.removeSong(song);
    }

    private void unbindService(){
        if(bound){
            musicPlayer.registerCallback(null);
            unbindService(serviceConnection);
            bound = false;
        }
    }

    private void select(){
        //goes into/exits selecting mode
        select.setVisibility(selecting?View.VISIBLE:View.GONE);
        playing.setVisibility(selecting?View.GONE:View.VISIBLE);
    }

    private void unSelect(){
        //after exiting selecting mode, revert all the views back to normal
        for(View view : selectedViews){
            LinearLayoutCompat linearLayout = view.findViewById(R.id.background);
            view.setSelected(false);
            linearLayout.setBackgroundColor(getResources().getColor(R.color.black,null));
        }
        selectedSongs.clear();
        selectedViews.clear();
    }

    private boolean isMusicPlayerRunning(){
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (MusicPlayer.class.getName().equals(service.service.getClassName())) {
                Log.i ("isMyServiceRunning?", true+"");
                return true;
            }
        }
        Log.i ("isMyServiceRunning?", false+"");
        return false;
    }

}