package com.example.musicplayer.fragments;

import android.content.ContentUris;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.musicplayer.MainActivity;
import com.example.musicplayer.R;
import com.example.musicplayer.adapters.AlbumSongAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class AlbumSongsFragment extends Fragment {
    private final ArrayList<MediaBrowserCompat.MediaItem> songs;
    private final String albumArt;
    private final String albumName,artist;
    private final static Uri sArtworkUri = Uri
            .parse("content://media/external/audio/albumart");

    public AlbumSongsFragment(ArrayList<MediaBrowserCompat.MediaItem> songs, String albumArt, String albumName, String artist){
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
        FloatingActionButton play = view.findViewById(R.id.play);
        RecyclerView recyclerView = view.findViewById(R.id.songs);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(linearLayoutManager);
        AlbumSongAdapter songAdapter = new AlbumSongAdapter(songs,artist,mainActivity);
        recyclerView.setAdapter(songAdapter);
        ImageView albumArtView = view.findViewById(R.id.album_art);
        Glide.with(requireContext()).load(ContentUris.withAppendedId(sArtworkUri,Long.parseLong(albumArt))).into(albumArtView);
        toolbar.setTitle(albumName);
        toolbar.setSubtitle(artist);
        assert mainActivity != null;
        mainActivity.setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener((view1) -> mainActivity.onBackPressed());
        Objects.requireNonNull(mainActivity.getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        mainActivity.getSupportActionBar().setDisplayShowHomeEnabled(true);
        play.setOnClickListener(mainActivity.getSongOnclickListener(0,songs));
    }
}
