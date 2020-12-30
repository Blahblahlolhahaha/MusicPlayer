package com.example.musicplayer.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.musicplayer.MainActivity
import com.example.musicplayer.R
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.tabs.TabLayoutMediator.TabConfigurationStrategy

class ArtistFragment(val name: String,val songs: ArrayList<HashMap<String, String>>, val albums: ArrayList<HashMap<String,String>>) : Fragment(){
    val tabNames: Array<String> = arrayOf("Songs","Albums")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.artist_fragment,container,false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val toolbar = view.findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        val adapter =  Adapter(this)
        val viewPager2 = view.findViewById<ViewPager2>(R.id.pager)
        val tabLayout = view.findViewById<TabLayout>(R.id.tabs)
        val mainActivity = context as MainActivity
        toolbar.setTitle(name)
        mainActivity.setSupportActionBar(toolbar)
        mainActivity.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        mainActivity.supportActionBar?.setDisplayShowHomeEnabled(true)
        toolbar.setNavigationOnClickListener { view -> mainActivity.onBackPressed() }
        tabLayout.setTabTextColors(Color.parseColor("#FFFFFF"), Color.parseColor("#FFFFFF"))
        viewPager2.adapter = adapter
        TabLayoutMediator(tabLayout,viewPager2, TabConfigurationStrategy { tab: TabLayout.Tab, position: Int -> tab.text = tabNames[position] }).attach()
    }

    inner class Adapter(val fragment: Fragment):FragmentStateAdapter(fragment){

        override fun createFragment(position: Int): Fragment {
            when(position){
                0-> return SongFragment(songs)
                1-> return AlbumFragment(albums)
                else-> return SongFragment(songs)
            }
        }

        override fun getItemCount(): Int {
            return 2
        }

    }
}