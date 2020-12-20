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

import java.util.HashMap;

public class AlbumAdapter extends RecyclerView.Adapter<AlbumAdapter.AlbumViewHolder> {
    private HashMap<String,String> albums;
    private String[] albumIDs;
    private Context context;

    public AlbumAdapter(HashMap<String,String> albums, Context context){
        this.albums = albums;
        this.context = context;
        albumIDs = albums.keySet().toArray(new String[0]);
    }

    @NonNull
    @Override
    public AlbumViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        CardView cardView = (CardView) LayoutInflater.from(parent.getContext()).inflate(R.layout.album_card,parent,false);
        AlbumViewHolder albumViewHolder = new AlbumViewHolder(cardView);
        return albumViewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull AlbumViewHolder holder, int position) {
        ImageView albumArt = holder.cardView.findViewById(R.id.album_art);
        TextView album = holder.cardView.findViewById(R.id.album_name);
        TextView artist = holder.cardView.findViewById(R.id.artist);
        String albumID = albumIDs[position];
        String[] albumInfo = albums.get(albumID).split(",");
        album.setText(albumInfo[0]);
        artist.setText(albumInfo[1]);
        Bitmap album_art = ((MainActivity) context).getAlbumArt(albumID);
        albumArt.setImageBitmap(album_art);
        holder.bind(((MainActivity)context).getAlbumOnClickListener(albumID));
    }

    @Override
    public int getItemCount() {
        return albumIDs.length;
    }

    public static class AlbumViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        public AlbumViewHolder(@NonNull CardView c) {
            super(c);
            cardView = c;
        }
        public void bind(View.OnClickListener callback){
            cardView.setOnClickListener(callback);
        }
    }
}
