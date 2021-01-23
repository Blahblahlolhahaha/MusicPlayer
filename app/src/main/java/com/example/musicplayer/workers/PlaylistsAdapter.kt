package com.example.musicplayer.workers

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.MainActivity
import com.example.musicplayer.R

class PlaylistsAdapter(val playlist:ArrayList<Playlist>, val context: Context): RecyclerView.Adapter<PlaylistsAdapter.playlistViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): playlistViewHolder{
        val cardView: CardView = LayoutInflater.from(parent.context).inflate(R.layout.playlist_card,parent,false) as CardView
        return playlistViewHolder(cardView)
    }

    override fun onBindViewHolder(holder: playlistViewHolder, position: Int) {
        val imageView: ImageView = holder.cardView.findViewById(R.id.album_art)
        val textView: TextView = holder.cardView.findViewById(R.id.playlist)
        if(playlist[position].firstSongAlbum.equals("")){
            imageView.setImageBitmap((context as MainActivity).getAlbumArt(playlist[position].firstSongAlbum))
        }
        textView.setText(playlist[position].name)
        holder.bind((context as MainActivity).getPlaylistOnClickListener(playlist[position]))
    }

    override fun getItemCount(): Int {
        return playlist.size
    }


    class playlistViewHolder(val cardView: CardView): RecyclerView.ViewHolder(cardView) {
        fun bind(callback: View.OnClickListener){
            cardView.setOnClickListener(callback)
        }
    }
}