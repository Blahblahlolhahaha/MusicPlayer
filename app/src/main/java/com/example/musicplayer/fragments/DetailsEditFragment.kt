package com.example.musicplayer.fragments

import android.app.Activity
import android.app.PendingIntent
import android.content.ContentUris
import android.content.ContentValues
import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.provider.MediaStore.createWriteRequest
import android.support.v4.media.MediaBrowserCompat
import android.text.InputType
import android.util.Log
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.musicplayer.dialogs.DeleteDialog
import com.example.musicplayer.MainActivity
import com.example.musicplayer.R
import org.cmc.music.metadata.MusicMetadata
import org.cmc.music.myid3.MyID3
import java.io.File
import java.io.FileOutputStream
import kotlin.collections.ArrayList

class DetailsEditFragment(val song: MediaBrowserCompat.MediaItem) : Fragment() {
    private val editTextArray: ArrayList<EditText> = ArrayList()
    private val data = song.description.mediaUri.toString()
    private lateinit var toolbar: androidx.appcompat.widget.Toolbar
    private lateinit var cancelButton: Button
    private lateinit var saveButton: Button
    private lateinit var songEdit: EditText
    private lateinit var artistEdit: EditText
    private lateinit var albumEdit: EditText
    private lateinit var discEdit: EditText
    private lateinit var trackEdit: EditText
    private lateinit var yearEdit: EditText
    private lateinit var albumArt: ImageView
    private lateinit var path: TextView
    private val sArtworkUri = Uri
        .parse("content://media/external/audio/albumart")
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
        albumArt = view.findViewById(R.id.album_art)
        val album: String? = song.description.extras?.getString("albumID")
        if (album != null) {
            Glide.with(requireContext())
                .load(ContentUris.withAppendedId(sArtworkUri, album.toLong()))
                .diskCacheStrategy(DiskCacheStrategy.ALL).into(albumArt)
        }
        editTextArray.addAll(listOf(songEdit, artistEdit, albumEdit, discEdit, trackEdit, yearEdit))
        songEdit.setText(song.description.title)
        artistEdit.setText(song.description.extras?.getString("artist"))
        val mainActivity = context as MainActivity
        albumEdit.setText(song.description.extras?.getString("album"))
        val disc: String? = song.description.extras?.getString("disc")
        discEdit.setText(if (disc.equals(null)) "1" else disc)
        trackEdit.setText(song.description.extras?.getString("track"))
        yearEdit.setText(song.description.extras?.getString("year"))
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, Integer.parseInt(song.mediaId!!).toLong())
                val uris : MutableCollection<Uri> = mutableListOf(uri)
                val contentResolver = mainActivity.contentResolver
                val intent =  createWriteRequest(contentResolver,uris)
                startIntentSenderForResult(intent.intentSender,404,null,0,0,0,null)
            }
            else {
                save()
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
                context?.let { DeleteDialog().showDialog(ArrayList(),song,it) }
            }
        }
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.edit_menu,menu)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == Activity.RESULT_OK && requestCode == 404){
            save()
        }
    }

    fun save(){
        val mainActivity = context as MainActivity
        val src: File = File(data)
        if(src != null){
            val srcSet = MyID3().read(src)
            val musicMetadata = MusicMetadata("sad")
            musicMetadata.songTitle = songEdit.text.toString()
            musicMetadata.artist = artistEdit.text.toString()
            musicMetadata.album = albumEdit.text.toString()
            if(song.description.extras?.getString("disc") != null){
                musicMetadata.trackNumber = Integer.parseInt(discEdit.text.toString() + String.format("%03d",Integer.parseInt(trackEdit.text.toString())))
            }
            else{
                musicMetadata.trackNumber = Integer.parseInt(trackEdit.text.toString())
            }
            musicMetadata.year = yearEdit.text.toString();
            for(editText in editTextArray){
                editText.inputType = InputType.TYPE_NULL
            }
            toolbar.visibility = View.VISIBLE
            cancelButton.visibility = View.GONE
            saveButton.visibility = View.GONE
            val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, Integer.parseInt(song.mediaId!!).toLong())
            val values = ContentValues()
            val contentResolver = mainActivity.contentResolver
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.put(MediaStore.Audio.Media.IS_PENDING, 0)
            }
            val tmp = File((context as MainActivity).filesDir.toString() ,"tmp.mp3")
            MyID3().write(src,tmp, srcSet, musicMetadata)
            values.put(MediaStore.Audio.Media.TITLE, musicMetadata.songTitle)
            values.put(MediaStore.Audio.Media.ARTIST, musicMetadata.artist)
            values.put(MediaStore.Audio.Media.ALBUM, musicMetadata.album)
            values.put(MediaStore.Audio.Media.TRACK, musicMetadata.trackNumber.toString())
            values.put(MediaStore.Audio.Media.YEAR, musicMetadata.year)
            contentResolver.update(uri, values, null, null)

            contentResolver.openFileDescriptor(uri,"w").use{ it ->
                if (it != null) {
                    FileOutputStream(it.fileDescriptor).use {
                        it.write(
                            tmp.readBytes()
                        )
                    }
                }

            }
            tmp.delete()
            if(mainActivity.getAlbumID(musicMetadata.album).equals("")){
                Toast.makeText(context,"Album does not exist yet in this device! It will take awhile for the device to pick up the changes!",Toast.LENGTH_SHORT).show()
            }
            mainActivity.onBackPressed()
        }
    }
}