package com.example.musicplayer.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.musicplayer.R;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.HashMap;

public class MainFragment extends Fragment {
    private final ArrayList<HashMap<String,String>> songs;
    private final HashMap<String,String> album;
    private HashMap<String,String> artist;
    private final String[] tabNames = {"Songs","Albums","Artists"};
    public MainFragment(ArrayList<HashMap<String,String>> songs, HashMap<String,String> album, HashMap<String,String> artist){
        this.songs = songs;
        this.album = album;
        this.artist = artist;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.main_fragment,container,false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Adapter adapter = new Adapter(this);
        ViewPager2 viewPager2 = view.findViewById(R.id.pager);
        TabLayout tabLayout = view.findViewById(R.id.tabs);
        tabLayout.setTabTextColors(Color.parseColor("#FFFFFF"),Color.parseColor("#FFFFFF"));
        viewPager2.setAdapter(adapter);
        new TabLayoutMediator(tabLayout, viewPager2,((tab, position) -> {tab.setText(tabNames[position]);})).attach();
    }

    private class Adapter extends FragmentStateAdapter{
        public Adapter(Fragment fragment){
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch(position){
                case 0:
                    return new SongFragment(songs);
                case 1:
                    return new AlbumFragment(album);
                default:
                    return new SongFragment(songs);
            }
        }

        @Override
        public int getItemCount() {
            return 2;
        }
    }
}