package com.example.musicplayer.fragments

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.musicplayer.MainActivity
import com.example.musicplayer.R

class DetailsEditFragment(val song:HashMap<String,String>): Fragment() {
    private val editTextArray: ArrayList<EditText> = ArrayList()
    private val data = song["data"]
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
        return inflater.inflate(R.layout.edit_fragment,container,false)
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
        editTextArray.addAll(listOf(songEdit,artistEdit,albumEdit,discEdit,trackEdit,yearEdit))
        songEdit.setText(song["title"])
        artistEdit.setText(song["artist"])
        albumEdit.setText((context as MainActivity).getAlbumName(song["album"]))
        val disc: String? = song["disc"]
        discEdit.setText(if (disc.equals(null)) "1" else disc)
        trackEdit.setText(song["track"])
        yearEdit.setText(song["year"])
        path.text = data
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.edit -> {
                for (editText in editTextArray) {
                    if(editText.id != R.id.disc || editText.id != R.id.track || editText.id != R.id.year){
                        editText.inputType = InputType.TYPE_CLASS_TEXT
                    }
                    else{
                        editText.inputType = InputType.TYPE_CLASS_NUMBER
                    }
                }
            }
            R.id.delete->{

            }
        }
        return true;
    }
}