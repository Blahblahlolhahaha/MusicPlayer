package com.example.musicplayer.workers;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadata;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.text.Layout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicplayer.MainActivity;
import com.example.musicplayer.R;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {
    private ArrayList<HashMap<String,String>> songs;
    private HashMap<String,Bitmap> albums;
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
        Bitmap album_art = albums.get(songs.get(position).get("album"));
        albumArt.setImageBitmap(album_art);
        artist.setText(songs.get(position).get("artist"));
        songName.setText(songs.get(position).get("title"));
        holder.bind(((MainActivity)context).getOnclickListener(position,songs));
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
        public void bind(View.OnClickListener callback){
            cardView.setOnClickListener(callback);
        }
    }
}