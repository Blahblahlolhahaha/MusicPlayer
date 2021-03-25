package com.example.musicplayer.dialogs

import android.app.Dialog
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import android.support.v4.media.MediaBrowserCompat
import android.widget.Button
import android.widget.Toast
import com.example.musicplayer.MainActivity
import com.example.musicplayer.R
import java.io.File
import java.util.ArrayList

class DeleteDialog {
    fun showDialog(songList: ArrayList<MediaBrowserCompat.MediaItem>, song: MediaBrowserCompat.MediaItem?, context: Context){
        val dialog = Dialog(context)
        dialog.setContentView(R.layout.delete_dialog)
        val cancel: Button = dialog.findViewById(R.id.cancel)
        val delete: Button = dialog.findViewById(R.id.delete)
        cancel.setOnClickListener{
            dialog.dismiss()
        }
        delete.setOnClickListener{
            if(song == null){
                for(song in songList){
                    val src = File(song.description.mediaUri.toString())
                    val success = src.delete()
                    if(!success){
                        Toast.makeText(context, "Something went wrong while deleting " + song.description.title, Toast.LENGTH_SHORT).show()
                    }
                    else{
                        val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.mediaId!!.toLong())
                        val contentResolver: ContentResolver = context.contentResolver
                        contentResolver.delete(uri, null, null)
                        (context as MainActivity).removeSong(song)
                    }
                }
                (context as MainActivity).selectedSongs.clear()
            }
            else{
                val src =  File(song.description.mediaUri.toString())
                val success = src.delete()
                if(success){
                    Toast.makeText(context, "Success!", Toast.LENGTH_SHORT).show()
                    (context as MainActivity).onBackPressed()
                    val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.mediaId!!.toLong())
                    val contentResolver = context.contentResolver
                    contentResolver.delete(uri, null, null)
                    context.removeSong(song);
                }

            }
            dialog.dismiss()
            (context as MainActivity).onBackPressed()

        }
        dialog.show()
    }
}