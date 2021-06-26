package com.example.musicplayer.adapters

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.musicplayer.MainActivity
import com.example.musicplayer.R
import com.example.musicplayer.workers.Artist

class ArtistsAdapter(val artists: ArrayList<Artist>, val context: Context): RecyclerView.Adapter<ArtistsAdapter.ArtistViewHolder>() {
    private val sArtworkUri = Uri
            .parse("content://media/external/audio/albumart")

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArtistViewHolder {
        val cardView: CardView = LayoutInflater.from(parent.context).inflate(R.layout.artist_card,parent,false) as CardView
        return ArtistViewHolder(cardView)
    }

    override fun onBindViewHolder(holder: ArtistViewHolder, position: Int) {
        val albumArtView: ImageView = holder.cardView.findViewById(R.id.album_art)
        val artistTextView: TextView = holder.cardView.findViewById(R.id.artist)
        val trackAlbumTextView: TextView = holder.cardView.findViewById(R.id.tracks)
        val artist: Artist = artists.get(position)
        artistTextView.setText(artist.name)
        val trackAlbum: String = String.format("%s tracks| %s albums",artist.tracks,artist.albums)
        trackAlbumTextView.setText(trackAlbum);
        Glide.with(context).load(ContentUris.withAppendedId(sArtworkUri, artist.firstSongAlbum.toLong())).diskCacheStrategy(DiskCacheStrategy.ALL).into(albumArtView)
        holder.bind((context as MainActivity).getArtistOnClickListener(artist))
    }

    override fun getItemCount(): Int {
        return artists.size
    }

    class ArtistViewHolder(val cardView: CardView): RecyclerView.ViewHolder(cardView){
        fun bind(callback: View.OnClickListener){
            cardView.setOnClickListener(callback)
        }
    }
}