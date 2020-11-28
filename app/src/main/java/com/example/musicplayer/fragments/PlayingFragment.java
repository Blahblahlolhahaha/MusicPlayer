package com.example.musicplayer.fragments;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.musicplayer.MainActivity;
import com.example.musicplayer.R;
import com.example.musicplayer.workers.MusicPlayer;

public class PlayingFragment extends Fragment {
    private TextView artistTextView,songNameTextView;
    private ImageView albumArtView;
    private ImageButton play,previous,next,repeat,shuffle;
    private String artist,song;
    private Bitmap albumArt;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.playing,container,false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        artistTextView = view.findViewById(R.id.playing_artist);
        songNameTextView = view.findViewById(R.id.playing_song_name);
        albumArtView = view.findViewById(R.id.playing_album_art);
        play = view.findViewById(R.id.playing_play);
        previous = view.findViewById(R.id.playing_previous);
        next = view.findViewById(R.id.playing_next);
        repeat = view.findViewById(R.id.playing_repeat);
        shuffle = view.findViewById(R.id.playing_shuffle);
        artistTextView.setText(artist);
        songNameTextView.setText(song);
        albumArtView.setImageBitmap(albumArt);
        MusicPlayer musicPlayer = ((MainActivity)getContext()).getMusicPlayer();
        play.setOnClickListener(view0 -> {
            if(musicPlayer.getPlayingStatus()){
                play.setBackground(ContextCompat.getDrawable(getContext().getApplicationContext(),R.drawable.play));
                musicPlayer.pause();
            }
            else{
                play.setBackground(ContextCompat.getDrawable(getContext().getApplicationContext(),R.drawable.pause));
                musicPlayer.play();
            }
        });
        previous.setOnClickListener(view0 -> {
            musicPlayer.previous();
        });
        next.setOnClickListener(view0 -> {
            musicPlayer.next();
        });
        shuffle.setOnClickListener(view1 -> {
            if(musicPlayer.setShuffle()){
                Toast.makeText(getContext(),"Shuffle enabled!",Toast.LENGTH_SHORT).show();
            }
            else{
                Toast.makeText(getContext(),"Shuffle disabled",Toast.LENGTH_SHORT).show();
            }
        });
    }
    public void setSongInfo(String songName, String artist, Bitmap album){
        song = songName;
        this.artist = artist;
        this.albumArt = album;
        if(songNameTextView != null){
            songNameTextView.setText(songName);
            artistTextView.setText(artist);
            albumArtView.setImageBitmap(album);
        }

    }
}
