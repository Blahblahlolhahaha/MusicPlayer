package com.example.musicplayer.fragments;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicplayer.R;
import com.example.musicplayer.workers.SongAdapter;
import com.example.musicplayer.workers.SongManager;

import java.util.ArrayList;
import java.util.HashMap;

public class MainFragment extends Fragment {
    private SongManager songManager;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.main_fragment,container,false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.ARTIST_ID,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.YEAR
        };
        String[] projection1 = {
                MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ALBUM_ART
        };
        String[] projection2 = {
                MediaStore.Audio.Artists._ID, MediaStore.Audio.Artists.ARTIST
        };
        Cursor cursor = getContext().getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                null
        );
        Cursor c = getContext().getContentResolver().query(
                MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                projection1,
                null,
                null,
                null
        );
        Cursor cc = getContext().getContentResolver().query(
                MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
                projection2,
                null,
                null,
                null
        );
        ArrayList<HashMap<String,String>> songs = new ArrayList<>();
        HashMap<String,Bitmap> album = new HashMap<>();
        HashMap<String,String> artist = new HashMap<>();
        while(cursor.moveToNext()){
            HashMap<String,String> song = new HashMap<>();
            song.put("ID",cursor.getString(0));
            song.put("title",cursor.getString(1));
            song.put("data",cursor.getString(2));
            song.put("display_name",cursor.getString(3));
            song.put("artist",cursor.getString(4));
            song.put("album",cursor.getString(5));
            song.put("duration",cursor.getString(6));
            song.put("year",cursor.getString(7));
            songs.add(song);
            if(album.get(cursor.getString(5)) == null){
                MediaMetadataRetriever mmr =  new MediaMetadataRetriever();
                mmr.setDataSource(cursor.getString(2));
                byte[] picArray = mmr.getEmbeddedPicture();
                if(picArray!=null){
                    Bitmap art = BitmapFactory.decodeByteArray(picArray,0,picArray.length);
                    album.put(cursor.getString(5),art);
                }
            }

        }
        while(cc.moveToNext()){
            artist.put(cc.getString(0),cc.getString(1));
        }
        songManager = new SongManager(songs,album,artist);
        RecyclerView songList = view.findViewById(R.id.songs);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        songList.setLayoutManager(linearLayoutManager);
        songList.setHasFixedSize(true);
        SongAdapter songAdapter = new SongAdapter(songManager,getContext());
        songList.setAdapter(songAdapter);
    }
}
