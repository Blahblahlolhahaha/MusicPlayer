package com.example.musicplayer.adapters

import android.content.ContentUris
import android.content.Context
import android.media.browse.MediaBrowser
import android.net.Uri
import android.support.v4.media.MediaBrowserCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.musicplayer.MainActivity
import com.example.musicplayer.R
import com.example.musicplayer.workers.Playlist

class PlaylistsAdapter(private var playlists: ArrayList<Playlist>, private var songs: ArrayList<MediaBrowserCompat.MediaItem>, private var context: Context) : RecyclerView.Adapter<PlaylistsAdapter.PlaylistViewHolder>() {
    private val sArtworkUri = Uri
            .parse("content://media/external/audio/albumart")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        val cardView: CardView = LayoutInflater.from(parent.context).inflate(R.layout.display_card,parent,false) as CardView
        return PlaylistViewHolder(cardView)
    }

    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        val imageView: ImageView = holder.cardView.findViewById(R.id.album_art)
        val textView: TextView = holder.cardView.findViewById(R.id.card_title)
        if(!playlists[position].firstSongAlbum.equals("")){
            Glide.with(context).load(ContentUris.withAppendedId(sArtworkUri,playlists[position].firstSongAlbum.toLong())).into(imageView)
        }
        textView.setText(playlists[position].name)
        if(songs.isNotEmpty()){
            holder.bind((context as MainActivity).getAddSongtoPlaylistOnClickListener(playlists[position],songs))
        }
        else{
            holder.bind((context as MainActivity).getPlaylistOnClickListener(playlists[position]))
        }

    }

    override fun getItemCount(): Int {
        return playlists.size
    }


    class PlaylistViewHolder(val cardView: CardView): RecyclerView.ViewHolder(cardView) {
        fun bind(callback: View.OnClickListener){
            cardView.setOnClickListener(callback)
        }
    }
}