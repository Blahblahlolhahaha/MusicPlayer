package com.example.musicplayer.fragments

import android.app.Dialog
import android.content.Context
import android.widget.Button
import android.widget.Toast
import com.example.musicplayer.R
import java.io.File

class DeleteDialog {
    var songPath: String? = null
    var songPathList: List<String>? = null
    constructor(pathList: List<String>){
        songPathList = pathList
    }
    constructor(path: String){
        songPath  = path
    }

    fun showDialog(context: Context){
        val dialog: Dialog = Dialog(context)
        dialog.setContentView(R.layout.delete_dialog)
        val cancel: Button = dialog.findViewById(R.id.cancel)
        val delete: Button = dialog.findViewById(R.id.delete)
        cancel.setOnClickListener{
            dialog.dismiss()
        }
        delete.setOnClickListener{
            if(songPath == null){
                for(path in songPathList!!){
                    val file = File(path)
                    val success: Boolean = file.delete()
                    if(!success){
                        Toast.makeText(context,"Something went wrong",Toast.LENGTH_SHORT).show()
                    }
                }
            }
            else{
                val file = File(songPath!!)
                val success: Boolean = file.delete()
                if(!success){
                    Toast.makeText(context,"Something went wrong",Toast.LENGTH_SHORT).show()
                }
            }
            dialog.dismiss()
        }
        dialog.show()
    }
}