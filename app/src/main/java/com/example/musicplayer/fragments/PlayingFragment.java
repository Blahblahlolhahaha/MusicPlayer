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
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.musicplayer.MainActivity;
import com.example.musicplayer.R;
import com.example.musicplayer.workers.MusicPlayer;

import org.w3c.dom.Text;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class PlayingFragment extends Fragment {
    private TextView artistTextView,songNameTextView,end;
    private ImageView albumArtView;
    private ImageButton play;
    private String artist,song;
    private long max;
    private Bitmap albumArt;
    private SeekBar seekBar;
    private Thread thread;
    private boolean stop;
    private MusicPlayer musicPlayer;
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
        ImageButton previous = view.findViewById(R.id.playing_previous);
        ImageButton next = view.findViewById(R.id.playing_next);
        ImageButton repeat = view.findViewById(R.id.playing_repeat);
        ImageButton shuffle = view.findViewById(R.id.playing_shuffle);
        seekBar = view.findViewById(R.id.seek_bar);
        TextView start = view.findViewById(R.id.start);
        end = view.findViewById(R.id.end);
        artistTextView.setText(artist);
        songNameTextView.setText(song);
        albumArtView.setImageBitmap(albumArt);
        musicPlayer = ((MainActivity)getContext()).getMusicPlayer();
        thread = new PositionThread();
        if(musicPlayer.getPlayingStatus()){
            play.setBackground(ContextCompat.getDrawable(getContext().getApplicationContext(),R.drawable.pause));
            seekBar.setMax((int) max);
            end.setText(formatDuration(max));
            startThread();
        }
        else{
            play.setBackground(ContextCompat.getDrawable(getContext().getApplicationContext(),R.drawable.play));
        }
        play.setOnClickListener(view0 -> {
            if(musicPlayer.getPlayingStatus()){
                play.setBackground(ContextCompat.getDrawable(getContext().getApplicationContext(),R.drawable.play));
                musicPlayer.pause();
            }
            else{
                play.setBackground(ContextCompat.getDrawable(getContext().getApplicationContext(),R.drawable.pause));
                musicPlayer.play();
                startThread();
            }
        });
        previous.setOnClickListener(view0 -> musicPlayer.previous());
        next.setOnClickListener(view0 -> musicPlayer.next());
        shuffle.setOnClickListener(view1 -> {
            if(musicPlayer.setShuffle()){
                Toast.makeText(getContext(),"Shuffle enabled!",Toast.LENGTH_SHORT).show();
            }
            else{
                Toast.makeText(getContext(),"Shuffle disabled",Toast.LENGTH_SHORT).show();
            }
        });
        repeat.setOnClickListener(view1 -> {
            switch (musicPlayer.setRepeat()){
                case "no":
                    Toast.makeText(getContext(),"Repeat disabled",Toast.LENGTH_SHORT).show();
                    break;
                case "repeat":
                    Toast.makeText(getContext(),"Repeat enabled",Toast.LENGTH_SHORT).show();
                    break;
                case "track":
                    Toast.makeText(getContext(),"Repeating current track",Toast.LENGTH_SHORT).show();
                    break;
            }
        });
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                start.setText(formatDuration(seekBar.getProgress()));

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                musicPlayer.setPosition(seekBar.getProgress());
            }
        });
    }
    public void setSongInfo(String songName, String artist, Bitmap album,String duration){
        song = songName;
        this.artist = artist;
        this.albumArt = album;

        max = formatTime(duration);
        if(songNameTextView != null){
            songNameTextView.setText(songName);
            artistTextView.setText(artist);
            albumArtView.setImageBitmap(album);
            seekBar.setMax((int) max);
            end.setText(duration);
        }
    }
    public void startThread(){
        if(thread != null){
            thread.interrupt();
            thread = new PositionThread();
            thread.start();
        }
    }

    public void resetSeekBar(){
        if(seekBar != null){
            seekBar.setProgress(0);
        }
    }

    private String formatDuration(long duration) {
        long minutes = TimeUnit.MINUTES.convert(duration, TimeUnit.MILLISECONDS);
        long seconds = TimeUnit.SECONDS.convert(duration, TimeUnit.MILLISECONDS)
                - minutes * TimeUnit.SECONDS.convert(1, TimeUnit.MINUTES);

        return String.format(Locale.ENGLISH,"%02d:%02d", minutes, seconds);
    }

    private long formatTime(String duration){
        String[] minSec = duration.split(":");
        return (long)(Integer.parseInt(minSec[0]) * 60 * 1000 + Integer.parseInt(minSec[1]) * 1000);
    }

    private class PositionThread extends Thread {
        @Override
        public void run() {
            try{
                while(musicPlayer.getPlayingStatus()){
                    if(!stop){
                        int position = musicPlayer.getPosition();
                        seekBar.setProgress(position);
                    }
                    else{
                        break;
                    }
                }
            }catch(IllegalStateException e){
                try {
                    sleep(100);
                    run();
                } catch (InterruptedException interruptedException) {
                    interruptedException.printStackTrace();
                }

            }

        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(thread.isAlive()){
            thread.interrupt();
        }
    }

}
