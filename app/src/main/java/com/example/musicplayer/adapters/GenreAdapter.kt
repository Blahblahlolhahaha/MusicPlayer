package com.example.musicplayer.adapters

import android.content.ContentUris
import android.content.Context
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
import com.example.musicplayer.MainActivity
import com.example.musicplayer.R
import com.example.musicplayer.workers.Category

class GenreAdapter(val genres:ArrayList<Category>, val context: Context): RecyclerView.Adapter<GenreAdapter.GenreViewHolder>() {
    private val sArtworkUri = Uri
            .parse("content://media/external/audio/albumart")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GenreViewHolder {
        val cardView: CardView = LayoutInflater.from(context).inflate(R.layout.display_card,parent,false) as CardView
        return GenreViewHolder(cardView)
    }

    override fun onBindViewHolder(holder: GenreViewHolder, position: Int) {
        val genre = genres[position]
        val textView = holder.cardView.findViewById<TextView>(R.id.card_title)
        val albumArt = holder.cardView.findViewById<ImageView>(R.id.album_art)
        textView.text = genre.name
        Glide.with(context).load(ContentUris.withAppendedId(sArtworkUri,genre.firstSongAlbum.toLong())).into(albumArt)
        holder.bind((context as MainActivity).getGenreOnClickListener(genre))
    }

    override fun getItemCount(): Int {
        return genres.size;
    }

    class GenreViewHolder(val cardView: CardView):RecyclerView.ViewHolder(cardView){
        fun bind(onClickListener: View.OnClickListener){
            cardView.setOnClickListener(onClickListener)
        }
    }
}