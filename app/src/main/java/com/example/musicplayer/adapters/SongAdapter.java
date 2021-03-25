package com.example.musicplayer.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.browse.MediaBrowser;
import android.support.v4.media.MediaBrowserCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicplayer.MainActivity;
import com.example.musicplayer.R;

import java.util.ArrayList;
import java.util.HashMap;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder>{
    private final ArrayList<MediaBrowserCompat.MediaItem> songs;
    private final Context context;
    public SongAdapter(ArrayList<MediaBrowserCompat.MediaItem> songs, Context context){
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
        MediaBrowserCompat.MediaItem song = songs.get(position);
        ImageView albumArt = holder.cardView.findViewById(R.id.album_art);
        TextView artist = holder.cardView.findViewById(R.id.artist);
        TextView songName = holder.cardView.findViewById(R.id.song);
        TextView duration = holder.cardView.findViewById(R.id.duration);
        LinearLayoutCompat linearLayout = holder.cardView.findViewById(R.id.background);
        Bitmap album_art = ((MainActivity)context).getAlbumArt(songs.get(position).getDescription().getExtras().getString("album"));
        albumArt.setImageBitmap(album_art);
        artist.setText(song.getDescription().getExtras().getString("artist"));
        songName.setText(song.getDescription().getTitle());
        duration.setText(song.getDescription().getExtras().getString("duration"));
        if(!((MainActivity) context).checkSelected(song)){
            holder.cardView.setSelected(false);
            linearLayout.setBackgroundColor(context.getResources().getColor(R.color.black,null));
        }
        else{
            holder.cardView.setSelected(true);
            linearLayout.setBackgroundColor(context.getResources().getColor(R.color.blue,null));
        }
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
