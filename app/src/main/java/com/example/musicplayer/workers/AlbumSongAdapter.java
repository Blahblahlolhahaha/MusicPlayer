package com.example.musicplayer.workers;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicplayer.MainActivity;
import com.example.musicplayer.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class AlbumSongAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{
    private ArrayList<HashMap<String,String>> songs;
    private String albumArtist;
    private Context context;
    int discNum;
    int currentDisk = 0;
    boolean gotDisc = false;
    ArrayList<Integer> disk_position = new ArrayList<>();
    public AlbumSongAdapter(ArrayList<HashMap<String,String>> songs, String albumArtist, Context context){
        this.albumArtist = albumArtist;
        this.context = context;
        if(songs.get(0).containsKey("disc")){
            discNum = 0;
            disk_position.add(0);
            Collections.sort(songs,new SortSongs("disc"));
            songs = splitByDisk(songs);
            gotDisc = true;
            currentDisk = 0;
        }
        else{
            Collections.sort(songs,new SortSongs("track"));
        }
        this.songs = songs;
    }

    @Override
    public int getItemViewType(int position) {
        return disk_position.contains(position) ? 0 : 1;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if(viewType == 1){
            CardView cardView = (CardView) LayoutInflater.from(parent.getContext()).inflate(R.layout.album_song_card,parent,false);
            SongViewHolder songViewHolder = new SongViewHolder(cardView);
            return songViewHolder;
        }
        else{
            TextView textView = (TextView) LayoutInflater.from(parent.getContext()).inflate(R.layout.disk_text,parent,false);
            DiskViewHolder diskViewHolder = new DiskViewHolder(textView);
            return diskViewHolder;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder.getItemViewType() == 1){
            HashMap<String,String> song = songs.get(position);
            TextView track = ((SongViewHolder) holder).cardView.findViewById(R.id.track);
            TextView artist = ((SongViewHolder) holder).cardView.findViewById(R.id.artist);
            TextView songName = (((SongViewHolder) holder)).cardView.findViewById(R.id.song_name);
            TextView duration = (((SongViewHolder) holder)).cardView.findViewById(R.id.duration);
            track.setText(song.get("track"));
            if(!song.get("artist").equals(albumArtist)){
                artist.setText(song.get("artist"));
                artist.setVisibility(View.VISIBLE);
            }
            songName.setText(song.get("title"));
            duration.setText(song.get("duration"));
            ((SongViewHolder) holder).bind(((MainActivity)context).getOnclickListener(position,songs));
        }
        else{
            TextView disk = ((DiskViewHolder)holder).textView;
            String diskText = "Disc " + songs.get(position + disk_position.indexOf(position) + 1).get("disc");
            disk.setText(diskText);
        }

    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    public static class SongViewHolder extends RecyclerView.ViewHolder{
        CardView cardView;
        public SongViewHolder(@NonNull CardView c) {
            super(c);
            cardView = c;
        }
        public void bind(View.OnClickListener callback){
            cardView.setOnClickListener(callback);
        }
    }
    public static class DiskViewHolder extends RecyclerView.ViewHolder{
        TextView textView;
        public DiskViewHolder(@NonNull TextView t) {
            super(t);
            textView = t;
        }
    }
    private ArrayList<HashMap<String,String>> splitByDisk(ArrayList<HashMap<String,String>> songs){
        ArrayList<HashMap<String,String>> diskTracks = new ArrayList<>();
        ArrayList<HashMap<String,String>> songsss = new ArrayList<>();
        for(int i = 0; i < songs.size(); i++){
            HashMap<String,String> song = songs.get(i);
            diskTracks.add(song);
            if(i + 1 != songs.size()){
                if(!song.get("disc").equals(songs.get(i+1).get("disc"))){
                    Collections.sort(diskTracks,new SortSongs("track"));

                    songsss.addAll(diskTracks);
                    diskTracks.clear();
                    disk_position.add(disk_position.size() + 1  + i);
                }
            }
            else if(i + 1 == songs.size()){
                Collections.sort(diskTracks,new SortSongs("track"));
                songsss.addAll(diskTracks);
            }
        }
        if(disk_position.size() == 1){
            gotDisc = false;
            currentDisk = 0;
            disk_position.clear();
        }
        else{
            for (int i:
                 disk_position) {
                songsss.add(i,null);
            }
        }
        return songsss;
    }

    private class SortSongs implements Comparator<Map<String, String>>
    {
        private final String key;


        public SortSongs(String key) {
            this.key = key;
        }

        public int compare(Map<String, String> first,
                           Map<String, String> second)
        {
            if(key.equals("track") || key.equals("disc")){
                int firstValue = Integer.parseInt(first.get(key));
                int secondValue = Integer.parseInt(second.get(key));
                return firstValue - secondValue;
            }
            else{
                String firstValue = first.get(key);
                String secondValue = second.get(key);
                return firstValue.compareTo(secondValue);
            }
        }
    }
}
