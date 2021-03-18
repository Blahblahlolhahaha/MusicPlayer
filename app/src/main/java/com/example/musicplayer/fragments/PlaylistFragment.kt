package com.example.musicplayer.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.dialogs.PlaylistDialog
import com.example.musicplayer.R
import com.example.musicplayer.workers.Playlist
import com.example.musicplayer.adapters.PlaylistsAdapter

class PlaylistFragment:Fragment{
    private var playlists: ArrayList<Playlist>
    private var songs: ArrayList<HashMap<String,String>> = ArrayList()
    private lateinit var playlistsAdapter: PlaylistsAdapter
    constructor(playlists: ArrayList<Playlist>){
        this.playlists = playlists
    }

    constructor(playlists:ArrayList<Playlist>, songs: ArrayList<HashMap<String,String>>){
        this.playlists = playlists
        this.songs = songs
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.playlist_fragment,container,false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val add_playlist: CardView =  view.findViewById(R.id.add_playlist)
        val playlistsView: RecyclerView = view.findViewById(R.id.card_title)
        val linearLayoutManager = LinearLayoutManager(context)
        playlistsAdapter = PlaylistsAdapter(playlists,songs,requireContext())
        add_playlist.setOnClickListener{
            PlaylistDialog(songs).showDialog(requireContext())
        }
        playlistsView.layoutManager = linearLayoutManager
        playlistsView.setHasFixedSize(true)
        playlistsView.adapter = playlistsAdapter

    }
}