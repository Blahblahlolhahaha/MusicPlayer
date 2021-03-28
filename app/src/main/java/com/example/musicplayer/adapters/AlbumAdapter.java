package com.example.musicplayer.adapters;

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
import com.example.musicplayer.workers.Album;
import com.example.musicplayer.workers.Category;

import java.util.ArrayList;
import java.util.HashMap;

public class AlbumAdapter extends RecyclerView.Adapter<AlbumAdapter.AlbumViewHolder> {
    private final ArrayList<Album> albums;
    private final Context context;

    public AlbumAdapter(ArrayList<Album> albums, Context context){
        this.albums = albums;
        this.context = context;
    }

    @NonNull
    @Override
    public AlbumViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //creates ViewHolder for each album
        CardView cardView = (CardView) LayoutInflater.from(parent.getContext()).inflate(R.layout.album_card,parent,false);
        return new AlbumViewHolder(cardView);
    }

    @Override
    public void onBindViewHolder(@NonNull AlbumViewHolder holder, int position) {
        //Sets album information on the CardView
        Album currentAlbum = albums.get(position);
        ImageView albumArt = holder.cardView.findViewById(R.id.album_art);
        TextView album = holder.cardView.findViewById(R.id.album);
        TextView artist = holder.cardView.findViewById(R.id.artist);
        String albumID = currentAlbum.getID();
        album.setText(currentAlbum.getName());
        artist.setText(currentAlbum.getArtist());
        Bitmap album_art = ((MainActivity) context).getAlbumArt(albumID);
        albumArt.setImageBitmap(album_art);
        holder.bind(((MainActivity)context).getAlbumOnClickListener(currentAlbum)); //gets the listener for each album
    }

    @Override
    public int getItemCount() {
        return albums.size();//tells adapter how large is the dataset
    }

    public static class AlbumViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        public AlbumViewHolder(@NonNull CardView c) {
            super(c);
            cardView = c;
        }
        public void bind(View.OnClickListener callback){
            cardView.setOnClickListener(callback); //binds listener to the CardView
        }
    }
}
