package com.example.musicplayer.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.musicplayer.R;
import com.example.musicplayer.workers.Album;
import com.example.musicplayer.workers.Artist;
import com.example.musicplayer.workers.Category;
import com.example.musicplayer.workers.Playlist;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.HashMap;

public class MainFragment extends Fragment {
    private final ArrayList<MediaBrowserCompat.MediaItem> songs;
    private final ArrayList<Album> album;
    private final ArrayList<Artist> artist;
    private final ArrayList<Category> genres;
    private final ArrayList<Playlist> playlists;
    private final String[] tabNames = {"Songs","Albums","Artists","Genres","Playlists","Search"};
    private PlaylistFragment playlistFragment;
    private final SongFragment songFragment;

    public MainFragment(ArrayList<MediaBrowserCompat.MediaItem> songs, ArrayList<Album> album, ArrayList<Artist> artist, ArrayList<Category> genres, ArrayList<Playlist> playlists){
        this.songs = songs;
        this.album = album;
        this.artist = artist;
        this.genres = genres;
        this.playlists = playlists;
        playlistFragment = new PlaylistFragment(playlists);
        songFragment = new SongFragment(songs);
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
        tabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);
        viewPager2.setAdapter(adapter);
        new TabLayoutMediator(tabLayout, viewPager2,((tab, position) -> {tab.setText(tabNames[position]);})).attach();
    }

    public void addPlaylist(Playlist playlist){
        playlists.add(playlist);
        refreshPlaylist();
    }

    public void removePlaylist(Playlist playlist){
        playlists.remove(playlist);
        refreshPlaylist();
    }

    public void removeSong(MediaBrowserCompat.MediaItem song){
        songs.remove(song);
        songFragment.resetSongs(songs);
    }

    private void refreshPlaylist(){
        FragmentTransaction fragmentTransaction = getChildFragmentManager().beginTransaction();
        fragmentTransaction.detach(playlistFragment);
        playlistFragment = new PlaylistFragment(playlists);
        fragmentTransaction.attach(playlistFragment);
        fragmentTransaction.commitNowAllowingStateLoss();
    }

    private class Adapter extends FragmentStateAdapter{
        public Adapter(Fragment fragment){
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch(position){
                case 1:
                    return new AlbumFragment(album);
                case 2:
                    return new ArtistsFragment(artist);
                case 3:
                    return new GenreFragment(genres);
                case 4:
                    return playlistFragment;
                case 5:
                    return new SearchFragment(songs,album,artist);
                default:
                    return songFragment;
            }
        }
        @Override
        public int getItemCount() {
            return 6;
        }
    }
}