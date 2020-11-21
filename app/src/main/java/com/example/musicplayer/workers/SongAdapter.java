package com.example.musicplayer.workers;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadata;
import android.media.MediaMetadataRetriever;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicplayer.R;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {
    private ArrayList<HashMap<String,String>> songs;
    private HashMap<String,String> albums;
    private HashMap<String,String> artists;
    private Context context;
    public SongAdapter(SongManager s, Context context){
        songs = s.getSongs();
        albums = s.getAlbum();
        artists = s.getArtist();
        this.context = context;
    }
    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        CardView cardView = (CardView) LayoutInflater.from(parent.getContext()).inflate(R.layout.song_card,parent,false);
        SongViewHolder songViewHolder = new SongViewHolder(cardView);
        return songViewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        ImageView albumArt = holder.cardView.findViewById(R.id.album_art);
        TextView artist = holder.cardView.findViewById(R.id.artist);
        TextView songName = holder.cardView.findViewById(R.id.song_name);
        String albumArtPath = albums.get(songs.get(position).get("album"));
        if(!albumArtPath.equals("")){
            Drawable album_art = Drawable.createFromPath(albumArtPath);
            albumArt.setBackground(album_art);
        }
        artist.setText(artists.get(songs.get(position).get("artist")));
        songName.setText(songs.get(position).get("title"));
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    public static class SongViewHolder extends RecyclerView.ViewHolder{
        CardView cardView;
        public SongViewHolder(@NonNull CardView c) {
            super(c);
            cardView = c;
        }
    }
}
