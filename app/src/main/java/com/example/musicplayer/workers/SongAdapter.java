package com.example.musicplayer.workers;

import android.content.Context;
import android.graphics.Bitmap;
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

import java.util.ArrayList;
import java.util.HashMap;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder>{
    private final ArrayList<HashMap<String,String>> songs;
    private final Context context;
    public SongAdapter(ArrayList<HashMap<String,String>> songs, Context context){
        this.songs =  songs;
        this.context = context;
    }
    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        CardView cardView = (CardView) LayoutInflater.from(parent.getContext()).inflate(R.layout.song_card,parent,false);
        return new SongViewHolder(cardView);
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        HashMap<String,String> song = songs.get(position);
        ImageView albumArt = holder.cardView.findViewById(R.id.album_art);
        TextView artist = holder.cardView.findViewById(R.id.artist);
        TextView songName = holder.cardView.findViewById(R.id.song);
        TextView duration = holder.cardView.findViewById(R.id.duration);
        Bitmap album_art = ((MainActivity)context).getAlbumArt(songs.get(position).get("album"));
        albumArt.setImageBitmap(album_art);
        artist.setText(song.get("artist"));
        songName.setText(song.get("title"));
        duration.setText(song.get("duration"));
        holder.bindOnClick(((MainActivity)context).getSongOnclickListener(position,songs));
        holder.bindOnLongClick(((MainActivity) context).getSongOnLongClickListener(song));
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
        public void bindOnClick(View.OnClickListener callback){
            cardView.setOnClickListener(callback);
        }

        public void bindOnLongClick(View.OnLongClickListener callback){
            cardView.setOnLongClickListener(callback);
        }
    }

}
