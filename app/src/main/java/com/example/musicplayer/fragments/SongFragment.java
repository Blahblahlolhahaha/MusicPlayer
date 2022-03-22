package com.example.musicplayer.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicplayer.R;
import com.example.musicplayer.adapters.SongAdapter;
import com.example.musicplayer.workers.CacheWorker;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Objects;

public class SongFragment extends Fragment {
    private ArrayList<MediaBrowserCompat.MediaItem> songs;
    private SongAdapter songAdapter;
    public SongFragment(ArrayList<MediaBrowserCompat.MediaItem> songs){
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
        Spinner spinner = view.findViewById(R.id.sort);
        ArrayAdapter<CharSequence> menu = ArrayAdapter.createFromResource(requireContext(),R.array.sort,R.layout.spinner_item);
        spinner.setAdapter(menu);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        songList.setLayoutManager(linearLayoutManager);
        songList.setHasFixedSize(true);
        songAdapter = new SongAdapter(songs,getContext());
        songList.setAdapter(songAdapter);
        spinner.setSelection(menu.getPosition(requireContext().getSharedPreferences("sort_order",Context.MODE_PRIVATE).getString("order","name")));
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String sortChoice = (String) parent.getItemAtPosition(position);
                SharedPreferences sharedPreferences = requireContext().getSharedPreferences("sort_order", Context.MODE_PRIVATE);
                sharedPreferences.edit().putString("order",sortChoice).apply();
                songs.sort(new CacheWorker.SortSongs(sortChoice));
                songAdapter.notifyDataSetChanged();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    public void resetSongs(ArrayList<MediaBrowserCompat.MediaItem> songs){
        this.songs = songs;
        songAdapter.notifyDataSetChanged();
    }

}
