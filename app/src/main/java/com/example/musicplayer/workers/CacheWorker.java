package com.example.musicplayer.workers;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class CacheWorker {
    private LruCache<String, Bitmap> albumArtCache;
    private DiskLruCache cache;
    private final Object cacheLock = new Object();
    private final int DISK_CACHE_SIZE = 1024*1024*100;
    private String directory;
    private boolean initialising = true;
    private int albumSize;
    private SongManager songManager;
    private Context context;
    public CacheWorker(Context context,String cacheDirectory){
        this.context = context;
        getSongs();
        this.albumSize = songManager.getAlbum().size();
        this.directory = cacheDirectory;
        File diskCache = new File(directory);
        new InitDiskCacheTask().execute(diskCache);
        albumArtCache = new LruCache<>(1024*1024*100);
        this.context = context;
        new BitmapWorkerTask().execute(songManager.getSongs());
    }

    public HashMap<String, Object> getAlbumMap(){
        return songManager.getAlbum();
    }

    public HashMap<String, String> getArtistMap(){
        return songManager.getArtist();
    }

    public ArrayList<HashMap<String,String>> getSongsMap(){
        return songManager.getSongs();
    }

    public Bitmap getAlbumArt(String albumID){
        return albumArtCache.get(albumID);
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
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            albumArt.compress(Bitmap.CompressFormat.PNG,100,baos);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(editor.newOutputStream(0));
            objectOutputStream.writeObject(baos.toByteArray());
            baos.close();
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
                    InputStream bitmapInput = snapshot.getInputStream(0);
                    Bitmap albumArt = BitmapFactory.decodeStream(bitmapInput);
                    bitmapInput.close();
                    return albumArt;
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
                    cache = DiskLruCache.open(cacheDir,1,1,DISK_CACHE_SIZE);
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
        @Override
        protected Void doInBackground(ArrayList<HashMap<String,String>>... songs) {
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
                    }
                    albumArtCache.put(album,albumArt);
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
                MediaStore.Audio.Media.YEAR
        };
        String[] projection1 = {
                MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ALBUM
        };
        String[] projection2 = {
                MediaStore.Audio.Artists._ID, MediaStore.Audio.Artists.ARTIST
        };
        Cursor cursor = context.getApplicationContext().getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                null
        );
        Cursor c = context.getApplicationContext().getContentResolver().query(
                MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                projection1,
                null,
                null,
                null
        );
        Cursor cc = context.getApplicationContext().getContentResolver().query(
                MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
                projection2,
                null,
                null,
                null
        );
        ArrayList<HashMap<String,String>> songs = new ArrayList<>();
        HashMap<String,String> artist = new HashMap<>();
        HashMap<String,Object> albums = new HashMap<>();
        while(cc.moveToNext()){
            artist.put(cc.getString(0),cc.getString(1));
        }
        while(c.moveToNext()){
            albums.put(c.getString(0),c.getString(1));
        }
        while(cursor.moveToNext()){
            HashMap<String,String> song = new HashMap<>();
            song.put("ID",cursor.getString(0));
            song.put("title",cursor.getString(1));
            song.put("data",cursor.getString(2));
            song.put("display_name",cursor.getString(3));
            song.put("artist",artist.get(cursor.getString(4)));
            song.put("album",cursor.getString(5));
            song.put("duration",cursor.getString(6));
            song.put("year",cursor.getString(7));
            songs.add(song);
        }
        Collections.sort(songs,new SortSongs("title"));
        songManager = new SongManager(songs,albums,artist);
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
}
