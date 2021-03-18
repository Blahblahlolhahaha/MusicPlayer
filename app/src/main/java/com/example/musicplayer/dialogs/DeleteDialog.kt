package com.example.musicplayer.dialogs

import android.app.Dialog
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import android.widget.Button
import android.widget.Toast
import com.example.musicplayer.MainActivity
import com.example.musicplayer.R
import java.io.File

class DeleteDialog {
    fun showDialog(songList: ArrayList<HashMap<String,String>>, song:HashMap<String,String>, context: Context){
        val dialog = Dialog(context)
        dialog.setContentView(R.layout.delete_dialog)
        val cancel: Button = dialog.findViewById(R.id.cancel)
        val delete: Button = dialog.findViewById(R.id.delete)
        cancel.setOnClickListener{
            dialog.dismiss()
        }
        delete.setOnClickListener{
            if(song.isEmpty()){
                for(song in songList){
                    val src = File(song["data"]!!)
                    val success = src.delete()
                    if(!success){
                        Toast.makeText(context, "Something went wrong while deleting " + song["name"], Toast.LENGTH_SHORT).show()
                    }
                    else{
                        val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song["ID"]!!.toLong())
                        val contentResolver: ContentResolver = context.contentResolver
                        contentResolver.delete(uri, null, null)
                        (context as MainActivity).removeSong(song)
                    }
                }
                (context as MainActivity).selectedSongs.clear()
            }
            else{
                val src = File(song?.get("data")!!)
                val success = src.delete()
                if(success){
                    Toast.makeText(context, "Success!", Toast.LENGTH_SHORT).show()
                    (context as MainActivity).onBackPressed()
                    val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, Integer.parseInt(song?.get("ID")!!).toLong())
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