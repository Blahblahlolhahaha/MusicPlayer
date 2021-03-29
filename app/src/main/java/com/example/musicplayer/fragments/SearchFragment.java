package com.example.musicplayer.fragments;

import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicplayer.R;
import com.example.musicplayer.adapters.AlbumAdapter;
import com.example.musicplayer.adapters.ArtistsAdapter;
import com.example.musicplayer.adapters.SongAdapter;
import com.example.musicplayer.workers.Album;
import com.example.musicplayer.workers.Artist;
import com.example.musicplayer.workers.Category;
import com.example.musicplayer.workers.Playlist;

import java.util.ArrayList;

public class SearchFragment extends Fragment {
    private final ArrayList<MediaBrowserCompat.MediaItem> songs;
    private final ArrayList<Album> albums;
    private final ArrayList<Artist> artists;
    private ArrayList<MediaBrowserCompat.MediaItem> searchSongs = new ArrayList<>();
    private ArrayList<Album> searchAlbum = new ArrayList<>();
    private ArrayList<Artist> searchArtist = new ArrayList<>();
    public SearchFragment(ArrayList<MediaBrowserCompat.MediaItem> songs, ArrayList<Album> album, ArrayList<Artist> artist){
        this.songs = songs;
        this.albums = album;
        this.artists = artist;
    }
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.search_fragment,container,false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        TextView songTitle = view.findViewById(R.id.song_title);
        TextView albumTitle = view.findViewById(R.id.album_title);
        TextView artistTitle = view.findViewById(R.id.artist_title);
        RecyclerView songsView = view.findViewById(R.id.songs);
        RecyclerView albumsView = view.findViewById(R.id.albums);
        RecyclerView artistsView = view.findViewById(R.id.artists);

        songsView.setLayoutManager(new LinearLayoutManager(getContext()));
        albumsView.setLayoutManager(new LinearLayoutManager(getContext()));
        artistsView.setLayoutManager(new LinearLayoutManager(getContext()));

        songsView.setAdapter(new SongAdapter(searchSongs,requireContext()));
        albumsView.setAdapter(new AlbumAdapter(searchAlbum,requireContext(),true));
        artistsView.setAdapter(new ArtistsAdapter(searchArtist,requireContext()));

        EditText editText = view.findViewById(R.id.search);
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String search = s.toString().toLowerCase();
                searchAlbum.clear();
                searchArtist.clear();
                searchSongs.clear();
                for(MediaBrowserCompat.MediaItem song:songs){
                    boolean title = song.getDescription().getTitle().toString().toLowerCase().contains(search);
                    boolean artist = song.getDescription().getExtras().getString("artist").toLowerCase().contains(search);
                    boolean album = song.getDescription().getExtras().getString("artist").toLowerCase().contains(search);
                    if(title || artist || album){
                        searchSongs.add(song);
                    }
                }
                songsView.getAdapter().notifyDataSetChanged();
                if(songsView.getVisibility() == View.GONE && searchSongs.size() > 0){
                    songTitle.setVisibility(View.VISIBLE);
                    songsView.setVisibility(View.VISIBLE);
                }
                else if(songsView.getVisibility() == View.VISIBLE && searchSongs.size()<1){
                    songTitle.setVisibility(View.GONE);
                    songsView.setVisibility(View.GONE);
                }

                for(Artist artist:artists){
                    if(artist.getName().toLowerCase().contains(search)){
                        searchArtist.add(artist);
                    }
                }
                artistsView.getAdapter().notifyDataSetChanged();
                if(artistsView.getVisibility() == View.GONE && searchArtist.size() > 0){
                    artistTitle.setVisibility(View.VISIBLE);
                    artistsView.setVisibility(View.VISIBLE);
                }
                else if(artistsView.getVisibility() == View.VISIBLE && searchArtist.size()<1){
                    artistTitle.setVisibility(View.GONE);
                    artistsView.setVisibility(View.GONE);
                }


                for(Album album:albums){
                    if(album.getName().toLowerCase().contains(search)){
                        searchAlbum.add(album);
                    }
                }
                albumsView.getAdapter().notifyDataSetChanged();
                if(albumsView.getVisibility() == View.GONE && searchAlbum.size() > 0){
                    albumTitle.setVisibility(View.VISIBLE);
                    albumsView.setVisibility(View.VISIBLE);
                }
                else if(albumsView.getVisibility() == View.VISIBLE && albums.size()<1){
                    albumTitle.setVisibility(View.GONE);
                    albumsView.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }
}
