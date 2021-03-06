package com.example.musicplayer.adapters;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.support.v4.media.MediaBrowserCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.arch.core.executor.TaskExecutor;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.musicplayer.MainActivity;
import com.example.musicplayer.R;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder>{
    private final ArrayList<MediaBrowserCompat.MediaItem> songs;
    private final Context context;
    private final static Uri sArtworkUri = Uri
            .parse("content://media/external/audio/albumart");
    private final MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
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
        Glide.with(context).load(ContentUris.withAppendedId(sArtworkUri, Long.parseLong(song.getDescription().getExtras().getString("albumID")))).diskCacheStrategy(DiskCacheStrategy.ALL).into(albumArt);
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
