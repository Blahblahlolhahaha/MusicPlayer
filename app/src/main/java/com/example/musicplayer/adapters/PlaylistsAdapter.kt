package com.example.musicplayer.adapters

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
import com.example.musicplayer.workers.Playlist

class PlaylistsAdapter(val playlist:ArrayList<Playlist>, val context: Context): RecyclerView.Adapter<PlaylistsAdapter.PlaylistViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        val cardView: CardView = LayoutInflater.from(parent.context).inflate(R.layout.display_card,parent,false) as CardView
        return PlaylistViewHolder(cardView)
    }

    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        val imageView: ImageView = holder.cardView.findViewById(R.id.album_art)
        val textView: TextView = holder.cardView.findViewById(R.id.card_title)
        if(!playlist[position].firstSongAlbum.equals("")){
            imageView.setImageBitmap((context as MainActivity).getAlbumArt(playlist[position].firstSongAlbum))
        }
        textView.setText(playlist[position].name)
        holder.bind((context as MainActivity).getPlaylistOnClickListener(playlist[position]))
    }

    override fun getItemCount(): Int {
        return playlist.size
    }


    class PlaylistViewHolder(val cardView: CardView): RecyclerView.ViewHolder(cardView) {
        fun bind(callback: View.OnClickListener){
            cardView.setOnClickListener(callback)
        }
    }
}