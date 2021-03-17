package com.example.musicplayer.fragments

import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.example.musicplayer.MainActivity
import com.example.musicplayer.R
import com.example.musicplayer.workers.Playlist

class   SelectSongsFragment(val playlist:Playlist): Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.select_songs_fragment,container,false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val toolbar: Toolbar = view.findViewById(R.id.toolbar)
        val mainActivity = requireContext() as MainActivity
        toolbar.setTitle("Select Songs")
        mainActivity.setSupportActionBar(toolbar)
        mainActivity.supportActionBar?.setDisplayShowHomeEnabled(true)
        mainActivity.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener{
            mainActivity.setSelecting(false)
            mainActivity.onBackPressed()}
        setHasOptionsMenu(true)
        toolbar.setTitleTextColor(resources.getColor(R.color.white,null))
        mainActivity.setSelecting(true)
        val cacheWorker = mainActivity.cacheWorker
        childFragmentManager.beginTransaction().replace(R.id.inner_fragment,MainFragment(cacheWorker.songsMap,cacheWorker.albumMap,cacheWorker.artistMap,cacheWorker.genreMap,cacheWorker.playlistMap)).commit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val selectedSongs = (requireContext() as MainActivity).selectedSongs
        if(selectedSongs.size != 0){
            playlist.addSongs(selectedSongs,context)
        }
        (requireContext() as MainActivity).onBackPressed()
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.select_menu,menu)
    }
}