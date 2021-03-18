package com.example.musicplayer.dialogs

import android.app.Dialog
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteException
import android.net.Uri
import android.provider.MediaStore
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.example.musicplayer.MainActivity
import com.example.musicplayer.R
import com.example.musicplayer.workers.Playlist
import kotlin.collections.ArrayList

class PlaylistDialog {

    fun showDialog(context: Context){
        val dialog = Dialog(context)
        dialog.setContentView(R.layout.playlist_name_dialog)
        val cancel: Button = dialog.findViewById(R.id.cancel)
        val editText: EditText = dialog.findViewById(R.id.name)
        val create: Button = dialog.findViewById(R.id.create)
        cancel.setOnClickListener{
            dialog.dismiss()
        }
        create.setOnClickListener{
            val name = editText.text.toString()
            if(name.isEmpty()){
                Toast.makeText(context, "Name cannot be empty!", Toast.LENGTH_SHORT).show()
            }
            else{
                val contentResolver = context.contentResolver
                val playlistUri: Uri = MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI
                val projection = arrayOf(
                        MediaStore.Audio.Playlists._ID, MediaStore.Audio.Playlists.NAME
                )
                try{
                    val cursor: Cursor =  contentResolver.query(playlistUri, projection, MediaStore.Audio.Playlists.NAME + "=" + name + "", null, null)!!
                    if(cursor.moveToNext()){
                        Toast.makeText(context, "Playlist already exists!", Toast.LENGTH_SHORT).show()
                    }
                }catch(error: SQLiteException){
                    val contentValues = ContentValues()
                    contentValues.put(MediaStore.Audio.Playlists.NAME, name)
                    val uri = context.contentResolver.insert(playlistUri, contentValues)
                    dialog.dismiss()
                    val cursor: Cursor =  contentResolver.query(uri!!, projection, null, null, null)!!
                    if(cursor.moveToNext()){
                        val playlist = Playlist(cursor.getString(0),cursor.getString(1), ArrayList())
                        (context as MainActivity).addPlaylist(playlist)
                    }
                }
            }
        }

        dialog.show()
    }
}