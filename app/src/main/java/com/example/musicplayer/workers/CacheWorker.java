package com.example.musicplayer.workers;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;
import android.util.LruCache;

import com.example.musicplayer.R;
import com.jakewharton.disklrucache.DiskLruCache;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class CacheWorker {
    private final LruCache<String, Bitmap> albumArtCache;
    private DiskLruCache cache;
    private final Object cacheLock = new Object();
    private boolean initialising = true;
    private SongManager songManager;
    private Context context;
    public CacheWorker(Context context,String cacheDirectory){
        this.context = context;
        getSongs();
        File diskCache = new File(cacheDirectory);
        initDiskCache(diskCache);
        albumArtCache = new LruCache<>(1024*1024*100);
        this.context = context;
        new BitmapWorkerTask().execute(songManager.getAlbums());
    }

    public ArrayList<HashMap<String,String>> getAlbumMap(){
        return songManager.getAlbums();
    }

    public ArrayList<HashMap<String, String>> getArtistMap(){
        return songManager.getArtists();
    }

    public ArrayList<HashMap<String,String>> getSongsMap(){
        return songManager.getSongs();
    }

    public ArrayList<Genre> getGenreMap(){
        return songManager.getGenres();
    }

    public ArrayList<Playlist> getPlaylistMap() { return songManager.getPlaylists(); }


    public String getAlbumName(String ID){
        for (HashMap<String,String> album:
                getAlbumMap()) {
            if(album.get("ID").equals(ID)){
                return album.get("name");
            }
        }
        return "";
    }

    public String getAlbumID(String name){
        for (HashMap<String,String> album:
                getAlbumMap()) {
            if(album.get("name").equals(name)){
                return album.get("ID");
            }
        }
        return "";
    }

    public Bitmap getAlbumArt(String albumID){
        return albumArtCache.get(albumID);
    }

    public Bitmap getArtistAlbumArt(String artist){
        for (HashMap<String,String> song:
                getSongsMap()) {
            if(song.get("artist").equals(artist)){
                return albumArtCache.get(song.get("album"));
            }
        }
        return BitmapFactory.decodeResource(context.getResources(), R.drawable.placeholder);
    }

    public ArrayList<HashMap<String,String>> getArtistSongs(String artist){
        ArrayList<HashMap<String,String>> artistSongs = new ArrayList<>();
        for (HashMap<String,String> song:
                getSongsMap()) {
            if(song.get("artist").equals(artist)){
                artistSongs.add(song);
            }
        }
        return artistSongs;
    }

    public ArrayList<HashMap<String,String>> getArtistAlbums(String artist){
        ArrayList<HashMap<String,String>> artistAlbums = new ArrayList<>();
        for (HashMap<String,String> album:
                getAlbumMap()) {
            if(album.get("artist").equals(artist)){
                artistAlbums.add(album);
            }
        }
        return artistAlbums;
    }

    public ArrayList<HashMap<String,String>> getAlbumSongs(String albumID){
        ArrayList<HashMap<String,String>> albumSongs = new ArrayList<>();
        for (HashMap<String,String> song:
             getSongsMap()) {
            if(song.get("album").equals(albumID)){
                albumSongs.add(song);
            }
        }
        return albumSongs;
    }

    private void storeCache(Bitmap albumArt,String album){
        synchronized (cacheLock){
            while(initialising){
                try{
                    cacheLock.wait();
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
            }
        }
        try{
            DiskLruCache.Editor editor = cache.edit(album);
            albumArt.compress(Bitmap.CompressFormat.PNG,100,editor.newOutputStream(0));
            editor.commit();

        }catch(IOException e){
            e.printStackTrace();
        }
    }

    private Bitmap getCache(String album){
        synchronized (cacheLock){
            while(initialising){
                try{
                    cacheLock.wait();
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
            }
            if(cache != null){
                try{
                    DiskLruCache.Snapshot snapshot = cache.get(album);
                    if(snapshot == null){return null;}
                    byte[] yes = new byte[(int) snapshot.getLength(0)];
                    BufferedInputStream objectInputStream = new BufferedInputStream(snapshot.getInputStream(0));
                    objectInputStream.read(yes,0,yes.length);
                    objectInputStream.close();
                    return BitmapFactory.decodeByteArray(yes,0,yes.length);
                }catch(IOException e){
                    e.printStackTrace();
                }
            }
            return null;
        }
    }
    void initDiskCache(File cacheDir){
        class InitDiskCacheTask implements Runnable{
            final File cacheDir;
            InitDiskCacheTask(File cacheDir){
                this.cacheDir = cacheDir;
            }
            @Override
            public void run() {
                synchronized (cacheLock){
                    try {
                        int DISK_CACHE_SIZE = 1024 * 1024 * 100;
                        cache = DiskLruCache.open(cacheDir,1,1, DISK_CACHE_SIZE);
                        cacheLock.notifyAll();
                        initialising = false;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        Thread t = new Thread(new InitDiskCacheTask(cacheDir));
        t.start();
    }


    private class BitmapWorkerTask extends AsyncTask<ArrayList<HashMap<String,String>>,Void,Void>{
        @SafeVarargs
        @Override
        protected final Void doInBackground(ArrayList<HashMap<String, String>>... albums) {
            if(albums != null){
                for (HashMap<String,String> album:albums[0]
                ) {
                    String albumID = album.get("ID");
                    if(albumArtCache.get(albumID) == null){
                        Bitmap albumArt = getCache(albumID);
                        if(albumArt == null){
                            MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
                            try{
                                for (HashMap<String,String>song:
                                        songManager.getSongs()) {
                                    if(song.get("album").equals(albumID)){
                                        mediaMetadataRetriever.setDataSource(song.get("data"));
                                    }
                                }
                            }catch(RuntimeException e){
                                Log.d("sad", "doInBackground: sad");
                            }

                            byte[] albumBytes = mediaMetadataRetriever.getEmbeddedPicture();
                            if(albumBytes == null){
                                albumArt =  BitmapFactory.decodeResource(context.getResources(), R.drawable.placeholder);
                            }
                            else{
                                albumArt = BitmapFactory.decodeByteArray(albumBytes,0,albumBytes.length);
                                if(albumArt == null){
                                    albumArt =  BitmapFactory.decodeResource(context.getResources(), R.drawable.placeholder);
                                }
                            }
                            albumArtCache.put(albumID,albumArt);
                            storeCache(albumArt,albumID);
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                                mediaMetadataRetriever.close();
                            }

                        }

                        albumArtCache.put(albumID,albumArt);
                    }
                }
            }

            return null;
        }
    }

    private void getSongs(){
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";

        String[] projection;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            projection = new String[]{
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.DATA,
                    MediaStore.Audio.Media.DISPLAY_NAME,
                    MediaStore.Audio.Media.ARTIST_ID,
                    MediaStore.Audio.Media.ALBUM_ID,
                    MediaStore.Audio.Media.YEAR,
                    MediaStore.Audio.Media.TRACK,
                    MediaStore.Audio.Media.DURATION,

            };
        }
        else{
            projection = new String[]{
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.DATA,
                    MediaStore.Audio.Media.DISPLAY_NAME,
                    MediaStore.Audio.Media.ARTIST_ID,
                    MediaStore.Audio.Media.ALBUM_ID,
                    MediaStore.Audio.Media.YEAR,
                    MediaStore.Audio.Media.TRACK,
            };
        }
        String[] projection1 = {
                MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ALBUM,MediaStore.Audio.Albums.ARTIST
        };
        String[] projection2 = {
                MediaStore.Audio.Artists._ID, MediaStore.Audio.Artists.ARTIST,MediaStore.Audio.Artists.NUMBER_OF_TRACKS, MediaStore.Audio.Artists.NUMBER_OF_ALBUMS
        };
        String[] projection3 = {
                MediaStore.Audio.Playlists._ID,MediaStore.Audio.Playlists.NAME
        };
        String[] projection4 = {
                MediaStore.Audio.Genres._ID,MediaStore.Audio.Genres.NAME
        };
        Cursor cursor = context.getApplicationContext().getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                null
        );
        Cursor albumCursor = context.getApplicationContext().getContentResolver().query(
                MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                projection1,
                null,
                null,
                null
        );
        Cursor artistCursor = context.getApplicationContext().getContentResolver().query(
                MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
                projection2,
                null,
                null,
                null
        );
        Cursor playlistCursor = context.getApplicationContext().getContentResolver().query(
                MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                projection3,
                null,
                null,
                null
        );

        Cursor genreCursor = context.getApplicationContext().getContentResolver().query(
                MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI,
                projection4,
                null,
                null,
                null
        );

        ArrayList<HashMap<String,String>> songs = new ArrayList<>();
        ArrayList<HashMap<String,String>> artists = new ArrayList<>();
        ArrayList<HashMap<String,String>> albums = new ArrayList<>();
        ArrayList<Genre> genres = new ArrayList<>();
        ArrayList<Playlist> playlists = new ArrayList<>();
        while(artistCursor.moveToNext()){
            HashMap<String,String> artist = new HashMap<>();
            artist.put("ID",artistCursor.getString(0));
            artist.put("name",artistCursor.getString(1));
            artist.put("tracks",artistCursor.getString(2));
            artist.put("albums",artistCursor.getString(3));
            artists.add(artist);
        }
        while(albumCursor.moveToNext()){
            HashMap<String,String> album = new HashMap<>();
            album.put("ID",albumCursor.getString(0));
            album.put("name",albumCursor.getString(1));
            album.put("artist",albumCursor.getString(2));
            albums.add(album);
        }
        while(cursor.moveToNext()){
            HashMap<String,String> song = new HashMap<>();
            song.put("ID",cursor.getString(0));
            song.put("title",cursor.getString(1));
            song.put("data",cursor.getString(2));
            song.put("display_name",cursor.getString(3));
            if(cursor.getString(4) != null){
                String ID = cursor.getString(4);
                for(HashMap<String,String>artist
                        :artists){
                    if(artist.get("ID").equals(ID)){
                        song.put("artist",artist.get("name"));
                    }
                }
            }
            else{
                song.put("artist","Unknown");
            }
            song.put("album",cursor.getString(5)==null?"Unknown":cursor.getString(5));
            song.put("year",cursor.getString(6));
            String track = cursor.getString(7);
            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q){
                if(track.length() == 4){
                    String disc = track.substring(0,1);
                    String trackNum = String.valueOf(Integer.parseInt(track.substring(1)));
                    song.put("disc",disc);
                    song.put("track",trackNum);
                }
                else{
                    song.put("track",track);
                }
            }
            else{
                if(track != null){
                    song.put("track",track.split("/")[0]);
                }
                else{
                    song.put("track","1");
                }
            }
            int genreIndex;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                song.put("duration",formatDuration(Long.parseLong(cursor.getString(8))));
            }
            else{
                MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                mmr.setDataSource(song.get("data"));
                String duration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                song.put("duration",formatDuration(Long.parseLong(duration)));
            }
            songs.add(song);
        }
        songs.sort(new SortSongs("title"));
        while(playlistCursor.moveToNext()){
            String id = playlistCursor.getString(0);
            long idLong = Long.parseLong(id);
            String[] projection5 = {
                    MediaStore.Audio.Playlists.Members.AUDIO_ID,
            };
            Cursor playlistSongCursor = context.getContentResolver().query(
                    MediaStore.Audio.Playlists.Members.getContentUri("external",idLong),
                    projection5,
                    MediaStore.Audio.Media.IS_MUSIC +" != 0 ",
                    null,
                    null);
            ArrayList<HashMap<String,String>> playListSongs = new ArrayList<>();
            while(playlistSongCursor.moveToNext()){
                Log.d("yes",playlistSongCursor.getString(0));
                for(HashMap<String,String>song:
                songs){
                    if(song.get("ID").equals(playlistSongCursor.getString(0))){
                        playListSongs.add(song);
                    }
                }
            }
            playlists.add(new Playlist(id,playlistCursor.getString(1),playListSongs));
            playlistSongCursor.close();
        }
        while(genreCursor.moveToNext()){
            ArrayList<HashMap<String,String>> genreSongs = new ArrayList<>();
            String ID = genreCursor.getString(0);
            long idLong = Long.parseLong(ID);
            String name = genreCursor.getString(1);
            String[] projection5 = {
                    MediaStore.Audio.Genres.Members.AUDIO_ID,
            };

            Cursor genreSongCursor = context.getContentResolver().query(
                    MediaStore.Audio.Genres.Members.getContentUri("external",idLong),
                    projection5,
                    MediaStore.Audio.Media.IS_MUSIC +" != 0 ",
                    null,
                    null);
            while(genreSongCursor.moveToNext()){
                for(HashMap<String,String>song:
                        songs){
                    if(song.get("ID").equals(genreSongCursor.getString(0))){
                        genreSongs.add(song);
                    }
                }
            }
            genres.add(new Genre(ID,name,genreSongs));
        }
        albumCursor.close();
        artistCursor.close();
        cursor.close();
        playlistCursor.close();
        songManager = new SongManager(songs,albums,artists,genres,playlists);
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
            // TODO: Null checking, both for maps and values
            String firstValue = first.get(key);
            String secondValue = second.get(key);
            return firstValue.compareTo(secondValue);
        }
    }

    private String formatDuration(long duration) {
        long minutes = TimeUnit.MINUTES.convert(duration, TimeUnit.MILLISECONDS);
        long seconds = TimeUnit.SECONDS.convert(duration, TimeUnit.MILLISECONDS)
                - minutes * TimeUnit.SECONDS.convert(1, TimeUnit.MINUTES);

        return String.format(Locale.ENGLISH,"%02d:%02d", minutes, seconds);
    }
}
