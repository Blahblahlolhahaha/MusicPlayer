package com.example.musicplayer.workers

import android.content.Context
import android.text.Layout
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.MainActivity
import com.example.musicplayer.R

class GenreAdapter(val genres:ArrayList<Genre>,val context: Context): RecyclerView.Adapter<GenreAdapter.GenreViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GenreViewHolder {
        val cardView: CardView = LayoutInflater.from(context).inflate(R.layout.display_card,parent,false) as CardView
        return GenreViewHolder(cardView)
    }

    override fun onBindViewHolder(holder: GenreViewHolder, position: Int) {
        val genre = genres[position]
        val textView = holder.cardView.findViewById<TextView>(R.id.card_title)
        val albumArt = holder.cardView.findViewById<ImageView>(R.id.album_art)
        textView.text = genre.name
        albumArt.setImageBitmap((context as MainActivity).getGenreAlbumArt(genre.firstSongAlbum))
        holder.bind(context.getGenreOnClickListener(genre))
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