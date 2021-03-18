package com.example.musicplayer.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicplayer.MainActivity;
import com.example.musicplayer.R;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class AlbumSongAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{
    private final ArrayList<HashMap<String,String>> songs;
    private final String albumArtist;
    private final Context context;
    int discNum;
    int currentDisk = 0;
    boolean gotDisc = false;
    ArrayList<Integer> disc_position = new ArrayList<>();
    public AlbumSongAdapter(ArrayList<HashMap<String,String>> songs, String albumArtist, Context context){
        this.albumArtist = albumArtist;
        this.context = context;
        if(songs.get(0).containsKey("disc")){
            //checks whether if the album contains disc numbers
            discNum = 0;
            disc_position.add(0);
            songs.sort(new SortSongs("disc"));//sort songs by disc
            songs = splitByDisc(songs);//sort songs by track number in each disc
            gotDisc = true;
            currentDisk = 0;
        }
        else{
            songs.sort(new SortSongs("track"));//if no disc, sort tracks by track number
        }
        this.songs = songs;
    }

    @Override
    public int getItemViewType(int position) {
        return disc_position.contains(position) ? 0 : 1;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if(viewType == 1){
            CardView cardView = (CardView) LayoutInflater.from(parent.getContext()).inflate(R.layout.album_song_card,parent,false);
            return new SongViewHolder(cardView);
        }
        else{
            TextView textView = (TextView) LayoutInflater.from(parent.getContext()).inflate(R.layout.disk_text,parent,false);
            return new DiskViewHolder(textView);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder.getItemViewType() == 1){
            //a song
            //set song information on the CardView
            HashMap<String,String> song = songs.get(position);
            TextView track = ((SongViewHolder) holder).cardView.findViewById(R.id.track);
            TextView artist = ((SongViewHolder) holder).cardView.findViewById(R.id.artist);
            TextView songName = (((SongViewHolder) holder)).cardView.findViewById(R.id.song);
            TextView duration = (((SongViewHolder) holder)).cardView.findViewById(R.id.duration);
            LinearLayoutCompat linearLayout = ((SongViewHolder) holder).cardView.findViewById(R.id.background);
            track.setText(song.get("track"));
            if(!song.get("artist").equals(albumArtist)){
                artist.setText(song.get("artist"));
                artist.setVisibility(View.VISIBLE);
            }
            songName.setText(song.get("title"));
            duration.setText(song.get("duration"));
            //Checks whether the song is selected in a previous view and shows the current state of the CardView
            if(!((MainActivity) context).checkSelected(song)){
                ((SongViewHolder) holder).cardView.setSelected(false);
                linearLayout.setBackgroundColor(context.getResources().getColor(R.color.black,null));
            }
            else{
                ((SongViewHolder) holder).cardView.setSelected(true);
                linearLayout.setBackgroundColor(context.getResources().getColor(R.color.blue,null));
            }
            ((SongViewHolder) holder).bind(((MainActivity)context).getSongOnclickListener(position,songs));//gets onClickListener/onLongClickListener for the song
            ((SongViewHolder) holder).bindOnLongClick(((MainActivity) context).getSongOnLongClickListener(song));
        }
        else{
            //a new disc
            TextView disk = ((DiskViewHolder)holder).textView;
            String diskText = "Disc " + songs.get(position + disc_position.indexOf(position) + 1).get("disc");
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
        public void bindOnLongClick(View.OnLongClickListener callback){
            cardView.setOnLongClickListener(callback);
        }
    }
    public static class DiskViewHolder extends RecyclerView.ViewHolder{
        TextView textView;
        public DiskViewHolder(@NonNull TextView t) {
            super(t);
            textView = t;
        }
    }
    private ArrayList<HashMap<String,String>> splitByDisc(ArrayList<HashMap<String,String>> songs){
        ArrayList<HashMap<String,String>> discTracks = new ArrayList<>();
        ArrayList<HashMap<String,String>> songsss = new ArrayList<>();
        for(int i = 0; i < songs.size(); i++){
            HashMap<String,String> song = songs.get(i);
            discTracks.add(song);
            if(i + 1 != songs.size()){
                if(songs.get(i).get("disc") == null){
                    songs.get(i).put("disc","0");//if no disc just become disc 0 (prevent errors at the check ltr)
                }
                else if(songs.get(i+1).get("disc") == null){
                    songs.get(i+1).put("disc","0");//if no disc just become disc 0 (prevent errors at the check ltr)
                }
                if(!song.get("disc").equals(songs.get(i+1).get("disc"))){
                    //if next song disc number different from current disc, sort songs by track number and then add to main array
                    discTracks.sort(new SortSongs("track"));
                    songsss.addAll(discTracks);
                    discTracks.clear();//clear discTracks for next disc
                    disc_position.add(disc_position.size() + 1  + i);//add position to array to determine RecyclerView item
                }
            }
            else if(i + 1 == songs.size()){
                //last song on disc, just add it in to discTracks
                discTracks.sort(new SortSongs("track"));
                songsss.addAll(discTracks);
            }
        }
        if(disc_position.size() == 1){
            //If only got one disc, just proceed without being a disc
            gotDisc = false;
            currentDisk = 0;
            disc_position.clear();
        }
        else{
            for (int i:
                    disc_position) {
                //add null stuff into array to for coordination
                songsss.add(i,null);
            }
        }
        return songsss;
    }

    private static class SortSongs implements Comparator<Map<String, String>>
    {
        private final String key;


        public SortSongs(String key) {
            this.key = key;
        }

        public int compare(Map<String, String> first,
                           Map<String, String> second)
        {
            if(key.equals("track") || key.equals("disc")){
                //convert track/disc into integer for comparison
                int firstValue,secondValue;
                if(first.get(key)!=null){
                    firstValue = Integer.parseInt(first.get(key));
                }
                else{
                    firstValue = 0;
                }
                if(second.get(key)!=null){
                    secondValue = Integer.parseInt(second.get(key));
                }
                else{
                    secondValue = 0;
                }
                //if first value bigger, will move to the back
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
