package com.example.musicplayer.workers;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.icu.text.Edits;
import android.media.MediaMetadataRetriever;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.util.LruCache;

import com.example.musicplayer.R;
import com.jakewharton.disklrucache.DiskLruCache;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
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
        new InitDiskCacheTask().execute(diskCache);
        albumArtCache = new LruCache<>(1024*1024*100);
        this.context = context;
        new BitmapWorkerTask().execute(songManager.getSongs());
    }

    public ArrayList<HashMap<String,String>> getAlbumMap(){
        return songManager.getAlbum();
    }

    public ArrayList<HashMap<String, String>> getArtistMap(){
        return songManager.getArtist();
    }

    public ArrayList<HashMap<String,String>> getSongsMap(){
        return songManager.getSongs();
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
        ArrayList<HashMap<String,String>> artistAlbums = new ArrayList<>();
        ArrayList<HashMap<String,String>> albums = getAlbumMap();
        for (HashMap<String,String> song:
                getSongsMap()) {
            if(song.get("artist").equals(artist)){
                artistSongs.add(song);
                for (HashMap<String,String> album:
                        albums) {
                    if(album.get("ID").equals(song.get("album")) && !artistAlbums.contains(album)){
                        artistAlbums.add(album);
                    }
                }
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

            ByteBuffer byteBuffer =  ByteBuffer.allocate(albumArt.getByteCount());
            albumArt.copyPixelsToBuffer(byteBuffer);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(editor.newOutputStream(0));
            byte[] yes = byteBuffer.array();
            objectOutputStream.writeObject(yes);
            editor.commit();
            byteBuffer.clear();

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
                    ObjectInputStream objectInputStream = new ObjectInputStream(snapshot.getInputStream(0));
                    objectInputStream.read(yes);
                    objectInputStream.close();
                    return BitmapFactory.decodeByteArray(yes,0,yes.length);
                }catch(IOException e){
                    e.printStackTrace();
                }
            }
            return null;
        }
    }

    private class InitDiskCacheTask extends AsyncTask<File,Void,Void> {

        @Override
        protected Void doInBackground(File... files) {
            synchronized (cacheLock){
                File cacheDir = files[0];
                try {
                    int DISK_CACHE_SIZE = 1024 * 1024 * 100;
                    cache = DiskLruCache.open(cacheDir,1,1, DISK_CACHE_SIZE);
                    cacheLock.notifyAll();
                    initialising = false;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }
    }

    private class BitmapWorkerTask extends AsyncTask<ArrayList<HashMap<String,String>>,Void,Void>{
        @SafeVarargs
        @Override
        protected final Void doInBackground(ArrayList<HashMap<String, String>>... songs) {
            if(songs != null){
                for (HashMap<String,String> song:songs[0]
                ) {
                    String album = song.get("album");
                    if(albumArtCache.get(album) == null){
                        Bitmap albumArt = getCache(album);
                        if(albumArt == null){
                            MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
                            mediaMetadataRetriever.setDataSource(song.get("data"));
                            byte[] albumBytes = mediaMetadataRetriever.getEmbeddedPicture();
                            if(albumBytes == null){
                                albumArt =  BitmapFactory.decodeResource(context.getResources(), R.drawable.placeholder);
                                albumArtCache.put(album,albumArt);
                                storeCache(albumArt,song.get("album"));
                            }
                            else{
                                albumArt = BitmapFactory.decodeByteArray(albumBytes,0,albumBytes.length);
                                albumArtCache.put(album,albumArt);
                                storeCache(albumArt,song.get("album"));
                            }
                            mediaMetadataRetriever.close();
                        }

                        albumArtCache.put(album,albumArt);
                    }
                }
            }

            return null;
        }
    }
    private void getSongs(){
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.ARTIST_ID,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.YEAR,
                MediaStore.Audio.Media.TRACK
        };
        String[] projection1 = {
                MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ALBUM,MediaStore.Audio.Albums.ARTIST
        };
        String[] projection2 = {
                MediaStore.Audio.Artists._ID, MediaStore.Audio.Artists.ARTIST,MediaStore.Audio.Artists.NUMBER_OF_TRACKS, MediaStore.Audio.Artists.NUMBER_OF_ALBUMS
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
        ArrayList<HashMap<String,String>> songs = new ArrayList<>();
        ArrayList<HashMap<String,String>> artists = new ArrayList<>();
        ArrayList<HashMap<String,String>> albums = new ArrayList<>();
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
            String ID = cursor.getString(4);
            for(HashMap<String,String>artist
                    :artists){
                if(artist.get("ID").equals(ID)){
                    song.put("artist",artist.get("name"));
                }
            }
            song.put("album",cursor.getString(5));
            song.put("duration",formatDuration(Long.parseLong(cursor.getString(6))));
            song.put("year",cursor.getString(7));
            String track = cursor.getString(8);
            if(track.length() == 4){
                String disc = track.substring(0,1);
                String trackNum = String.valueOf(Integer.parseInt(track.substring(1)));
                song.put("disc",disc);
                song.put("track",trackNum);
            }
            else{
                song.put("track",track);
            }
            songs.add(song);
        }
        Collections.sort(songs,new SortSongs("title"));
        albumCursor.close();
        artistCursor.close();
        cursor.close();
        songManager = new SongManager(songs,albums,artists);
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
