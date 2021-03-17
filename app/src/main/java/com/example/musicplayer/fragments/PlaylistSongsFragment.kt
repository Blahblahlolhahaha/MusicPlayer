package com.example.musicplayer.fragments

import android.app.Dialog
import android.content.ContentUris
import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.MainActivity
import com.example.musicplayer.R
import com.example.musicplayer.workers.Playlist
import com.example.musicplayer.workers.SongAdapter

class PlaylistSongsFragment(val playlist:Playlist):Fragment() {
    lateinit var recyclerView: RecyclerView
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.playlist_songs_fragment,container,false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val toolbar = view.findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        recyclerView = view.findViewById(R.id.songs)
        val linearLayoutManager = LinearLayoutManager(context)
        recyclerView.layoutManager = linearLayoutManager
        recyclerView.setHasFixedSize(true)
        recyclerView.adapter = SongAdapter(playlist.songs,context)
        val mainActivity = (context as MainActivity)
        mainActivity.setSupportActionBar(toolbar)
        mainActivity.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        mainActivity.supportActionBar?.setDisplayShowHomeEnabled(true)
        toolbar.setNavigationOnClickListener { mainActivity.onBackPressed() }
        toolbar.setTitleTextColor(resources.getColor(R.color.white,null))
        toolbar.setTitle(playlist.name)
        setHasOptionsMenu(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.addd ->{
                (context as MainActivity).fragmentTransaction(SelectSongsFragment(playlist),"select")
                (context as MainActivity).setPlaylistSongs(true)
            }
            R.id.delete->{
                val dialog = Dialog(requireContext())
                dialog.setContentView(R.layout.delete_dialog)
                val cancel: Button = dialog.findViewById(R.id.cancel)
                val delete: Button = dialog.findViewById(R.id.delete)
                val text: TextView = dialog.findViewById(R.id.track_label)
                text.setText(requireContext().getString(R.string.delete_playlist))
                cancel.setOnClickListener{
                    dialog.dismiss()
                }
                delete.setOnClickListener{
                    val uri = ContentUris.withAppendedId(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,playlist.id.toLong())
                    requireContext().contentResolver.delete(uri,null,null)
                    (requireContext() as MainActivity).removePlaylist(playlist)
                    dialog.dismiss()
                }
                dialog.show()
            }
        }
        return true
    }
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.playlist_menu,menu)
    }
}