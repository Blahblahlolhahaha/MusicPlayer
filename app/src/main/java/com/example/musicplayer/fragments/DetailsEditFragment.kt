package com.example.musicplayer.fragments

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.os.Bundle
import android.provider.MediaStore
import android.text.InputType
import android.view.*
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.view.menu.MenuBuilder
import androidx.core.view.get
import androidx.fragment.app.Fragment
import com.example.musicplayer.MainActivity
import com.example.musicplayer.R
import com.example.musicplayer.workers.CacheWorker
import org.cmc.music.metadata.MusicMetadata
import org.cmc.music.myid3.MyID3
import java.io.File
import java.util.*
import java.util.zip.Inflater
import kotlin.collections.ArrayList

class DetailsEditFragment(val song: HashMap<String, String>): Fragment() {
    private val editTextArray: ArrayList<EditText> = ArrayList()
    private val data = song["data"]
    private lateinit var toolbar: androidx.appcompat.widget.Toolbar
    private lateinit var cancelButton: Button
    private lateinit var saveButton: Button
    private lateinit var songEdit: EditText
    private lateinit var artistEdit: EditText
    private lateinit var albumEdit: EditText
    private lateinit var discEdit: EditText
    private lateinit var trackEdit: EditText
    private lateinit var yearEdit: EditText
    private lateinit var path: TextView
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.edit_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cancelButton = view.findViewById(R.id.cancel)
        saveButton = view.findViewById(R.id.save)
        songEdit = view.findViewById(R.id.song)
        artistEdit = view.findViewById(R.id.artist)
        albumEdit = view.findViewById(R.id.album)
        discEdit = view.findViewById(R.id.disc)
        trackEdit = view.findViewById(R.id.track)
        yearEdit = view.findViewById(R.id.year)
        path = view.findViewById(R.id.path)
        editTextArray.addAll(listOf(songEdit, artistEdit, albumEdit, discEdit, trackEdit, yearEdit))
        songEdit.setText(song["title"])
        artistEdit.setText(song["artist"])
        val mainActivity = context as MainActivity
        albumEdit.setText(mainActivity.getAlbumName(song["album"]))
        val disc: String? = song["disc"]
        discEdit.setText(if (disc.equals(null)) "1" else disc)
        trackEdit.setText(song["track"])
        yearEdit.setText(song["year"])
        path.text = data
        toolbar = view.findViewById(R.id.toolbar)
        mainActivity.setSupportActionBar(toolbar)
        mainActivity.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        mainActivity.supportActionBar?.setDisplayShowHomeEnabled(true)
        toolbar.setNavigationOnClickListener { mainActivity.onBackPressed() }
        toolbar.setTitleTextColor(resources.getColor(R.color.white,null))
        setHasOptionsMenu(true)
        cancelButton.setOnClickListener{
            for(editText in editTextArray){
                editText.inputType = InputType.TYPE_NULL
            }
            toolbar.visibility = View.VISIBLE
            cancelButton.visibility = View.GONE
            saveButton.visibility = View.GONE
        }
        saveButton.setOnClickListener{
            var src: File? = null
            data?.let { src = File(it)}
            if(src != null){
                val srcSet = MyID3().read(src)
                val musicMetadata = MusicMetadata("sad")
                musicMetadata.songTitle = songEdit.text.toString()
                musicMetadata.artist = artistEdit.text.toString()
                musicMetadata.album = albumEdit.text.toString()
                if(song["disc"] != null){
                    musicMetadata.trackNumber = Integer.parseInt(discEdit.text.toString() + String.format("%03d",Integer.parseInt(trackEdit.text.toString())))
                }
                else{
                    musicMetadata.trackNumber = Integer.parseInt(trackEdit.text.toString())
                }
                musicMetadata.year = yearEdit.text.toString()
                MyID3().update(src,srcSet,musicMetadata)
                for(editText in editTextArray){
                    editText.inputType = InputType.TYPE_NULL
                }
                toolbar.visibility = View.VISIBLE
                cancelButton.visibility = View.GONE
                saveButton.visibility = View.GONE
                if(!mainActivity.getAlbumID(musicMetadata.album).equals("")){
                    val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, Integer.parseInt(song["ID"]!!).toLong())
                    val values = ContentValues()
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        values.put(MediaStore.Audio.Media.IS_PENDING, 1)
                    }
                    val contentResolver = mainActivity.contentResolver
                    contentResolver.update(uri, values, null, null)
                    values.clear()
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        values.put(MediaStore.Audio.Media.IS_PENDING, 0)
                    }
                    values.put(MediaStore.Audio.Media.TITLE, musicMetadata.songTitle)
                    values.put(MediaStore.Audio.Media.ARTIST, musicMetadata.artist)
                    values.put(MediaStore.Audio.Media.ALBUM, musicMetadata.album)
                    values.put(MediaStore.Audio.Media.TRACK, musicMetadata.trackNumber.toString())
                    values.put(MediaStore.Audio.Media.YEAR, musicMetadata.year)
                    contentResolver.update(uri, values, null, null)
                }
                else{
                    Toast.makeText(context,"Album does not exist yet in this device! It will take awhile for the device to pick up the changes!",Toast.LENGTH_SHORT).show()
                }
                mainActivity.onBackPressed()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.edit -> {
                for (editText in editTextArray) {
                    if (editText.id != R.id.disc || editText.id != R.id.track || editText.id != R.id.year) {
                        editText.inputType = InputType.TYPE_CLASS_TEXT
                    } else {
                        editText.inputType = InputType.TYPE_CLASS_NUMBER
                    }
                }
                toolbar.visibility = View.GONE
                cancelButton.visibility = View.VISIBLE
                saveButton.visibility = View.VISIBLE
            }
            R.id.delete -> {
                context?.let { DeleteDialog(song).showDialog(it) }
            }
        }
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.edit_menu,menu)
    }
}