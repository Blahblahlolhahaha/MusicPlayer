package com.example.musicplayer.fragments;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicplayer.MainActivity;
import com.example.musicplayer.R;
import com.example.musicplayer.workers.AlbumSongAdapter;
import com.example.musicplayer.workers.SongAdapter;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.HashMap;

public class AlbumSongsFragment extends Fragment {
    private ArrayList<HashMap<String,String>> songs;
    private Bitmap albumArt;
    private String albumName,artist;
    private ImageView albumArtView;
    private FloatingActionButton play;
    private CoordinatorLayout coordinatorLayout;

    public AlbumSongsFragment(ArrayList<HashMap<String,String>> songs, Bitmap albumArt, String albumName, String artist){
        this.songs = songs;
        this.albumArt = albumArt;
        this.albumName  = albumName;
        this.artist = artist;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.album_songs_fragment,container,false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        MainActivity mainActivity = (MainActivity) getContext();
        Toolbar toolbar = view.findViewById(R.id.toolbar);
        play = view.findViewById(R.id.play);
        RecyclerView recyclerView = view.findViewById(R.id.songs);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(linearLayoutManager);
        AlbumSongAdapter songAdapter = new AlbumSongAdapter(songs,artist,mainActivity);
        recyclerView.setAdapter(songAdapter);
        albumArtView = view.findViewById(R.id.album_art);
        albumArtView.setImageBitmap(albumArt);
        toolbar.setTitle(albumName);
        toolbar.setSubtitle(artist);
        mainActivity.setSupportActionBar(toolbar);
        mainActivity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mainActivity.getSupportActionBar().setDisplayShowHomeEnabled(true);
        play.setOnClickListener(mainActivity.getOnclickListener(0,songs));
    }
}