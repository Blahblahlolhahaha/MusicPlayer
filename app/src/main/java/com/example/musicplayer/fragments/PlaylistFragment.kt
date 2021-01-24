package com.example.musicplayer.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.R
import com.example.musicplayer.workers.Playlist
import com.example.musicplayer.workers.PlaylistsAdapter

class PlaylistFragment(val playlist: ArrayList<Playlist>):Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.playlist_fragment,container,false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val add_playlist: CardView =  view.findViewById(R.id.add_playlist)
        val playlists: RecyclerView = view.findViewById(R.id.playlist)
        val linearLayoutManager = LinearLayoutManager(context)
        val playlistsAdapter = PlaylistsAdapter(playlist,requireContext())
        add_playlist.setOnClickListener{
            PlaylistDialog().showDialog(requireContext())
        }
        playlists.layoutManager = linearLayoutManager
        playlists.setHasFixedSize(true)
        playlists.adapter = playlistsAdapter
    }
}