package com.example.musicplayer.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicplayer.R;
import com.example.musicplayer.adapters.SongAdapter;

import java.util.ArrayList;
import java.util.HashMap;

public class SongFragment extends Fragment {
    private ArrayList<HashMap<String,String>> songs;
    private SongAdapter songAdapter;
    public SongFragment(ArrayList<HashMap<String,String>> songs){
        this.songs = songs;
    }
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.songs_fragment,container,false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        RecyclerView songList = view.findViewById(R.id.songs);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        songList.setLayoutManager(linearLayoutManager);
        songList.setHasFixedSize(true);
        songAdapter = new SongAdapter(songs,getContext());
        songList.setAdapter(songAdapter);
    }

    public void resetSongs(ArrayList<HashMap<String,String>> songs){
        this.songs = songs;
        songAdapter.notifyDataSetChanged();
    }
}
