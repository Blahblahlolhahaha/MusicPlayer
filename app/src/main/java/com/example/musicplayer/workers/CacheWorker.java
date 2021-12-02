package com.example.musicplayer.workers;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.util.Log;
import android.util.LruCache;

import com.example.musicplayer.R;
import com.jakewharton.disklrucache.DiskLruCache;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class CacheWorker {
//    private final LruCache<String, Bitmap> albumArtCache;
    private DiskLruCache cache;
    private final Object cacheLock = new Object();
    private final Object loadingAlbum = new Object();
    private boolean initialising,loading = true;
    private SongManager songManager;
    private Context context;
    public CacheWorker(Context context,String cacheDirectory){
        this.context = context;
        getSongs();
//        File diskCache = new File(cacheDirectory);
//        initDiskCache(diskCache);
//        albumArtCache = new LruCache<>(1024*1024*100);
//        new BitmapWorkerTask().execute(songManager.getAlbums());
    }

    public ArrayList<Album> getAlbumMap(){
        return songManager.getAlbums();
    }

    public ArrayList<Artist> getArtistMap(){
        return songManager.getArtists();
    }

    public ArrayList<MediaBrowserCompat.MediaItem> getSongsMap(){
        return songManager.getSongs();
    }

    public ArrayList<Category> getGenreMap(){
        return songManager.getGenres();
    }

    public ArrayList<Playlist> getPlaylistMap() { return songManager.getPlaylists(); }

    public String getAlbumID(String name){
        for (Album album:
                getAlbumMap()) {
            if(album.getName().equals(name)){
                return album.getID();
            }
        }
        return "";
    }
//
//    public Bitmap getAlbumArt(String albumID) {
//        synchronized (loadingAlbum) {
//            while (loading) {
//                try {
//                    loadingAlbum.wait();
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//        return albumArtCache.get(albumID);
//    }

    public ArrayList<Album> getArtistAlbums(String artist){
        ArrayList<Album> artistAlbums = new ArrayList<>();
        for (Album album:
                getAlbumMap()) {
            if(album.getArtist().equals(artist)){
                artistAlbums.add(album);
            }
        }
        return artistAlbums;
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



//    private class BitmapWorkerTask extends AsyncTask<ArrayList<Album>,Void,Void>{
//        @SafeVarargs
//        @Override
//        protected final Void doInBackground(ArrayList<Album>... albums) {
//            if(albums != null){
//                synchronized (loadingAlbum){
//                    for (Category album:albums[0]
//                    ) {
//                        String albumID = album.getID();
//                        if(albumArtCache.get(albumID) == null){
//                            Bitmap albumArt = getCache(albumID);
//                            if(albumArt == null){
//                                MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
//                                try{
//                                    for (MediaBrowserCompat.MediaItem song:
//                                            songManager.getSongs()) {
//                                        if(song.getDescription().getExtras().getString("albumID").equals(albumID)){
//                                            mediaMetadataRetriever.setDataSource(song.getDescription().getMediaUri().toString());
//                                        }
//                                    }
//                                }catch(RuntimeException e){
//                                    Log.d("sad", "doInBackground: sad");
//                                }
//
//                                byte[] albumBytes = mediaMetadataRetriever.getEmbeddedPicture();
//                                if(albumBytes == null){
//                                    albumArt =  BitmapFactory.decodeResource(context.getResources(), R.drawable.placeholder);
//                                }
//                                else{
//                                    albumArt = BitmapFactory.decodeByteArray(albumBytes,0,albumBytes.length);
//                                    if(albumArt == null){
//                                        albumArt =  BitmapFactory.decodeResource(context.getResources(), R.drawable.placeholder);
//                                    }
//                                }
//                                albumArtCache.put(albumID,albumArt);
//                                storeCache(albumArt,albumID);
//                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
//                                    mediaMetadataRetriever.close();
//                                }
//
//                            }
//
//                            albumArtCache.put(albumID,albumArt);
//                        }
//                    }
//                    loading = false;
//                    loadingAlbum.notifyAll();
//
//                }
//
//            }
//
//            return null;
//        }
//    }

    private void getSongs(){
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";

        String[] projection;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            projection = new String[]{
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.DATA,
                    MediaStore.Audio.Media.DISPLAY_NAME,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.ALBUM,
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
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.ALBUM,
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

        ArrayList<MediaBrowserCompat.MediaItem> songs = new ArrayList<>();
        ArrayList<Artist> artists = new ArrayList<>();
        ArrayList<Album> albums = new ArrayList<>();
        ArrayList<Category> genres = new ArrayList<>();
        ArrayList<Playlist> playlists = new ArrayList<>();

        while(cursor.moveToNext()){
            Bundle otherDetails = new Bundle();
            if(cursor.getString(4) != null){
                otherDetails.putString("artist",cursor.getString(4));
            }
            else{
                otherDetails.putString("artist","Unknown");
            }
            otherDetails.putString("album",cursor.getString(5)==null?"Unknown":cursor.getString(5));
            otherDetails.putString("albumID",cursor.getString(6)==null?"Unknown":cursor.getString(6));
            otherDetails.putString("year",cursor.getString(7));
            String track = cursor.getString(8);
            if(track == null){
                otherDetails.putString("track","0");
            }
            else{
                if(track.length() == 4){
                    String disc = track.substring(0,1);
                    String trackNum = String.valueOf(Integer.parseInt(track.substring(1)));
                    otherDetails.putString("disc",disc);
                    otherDetails.putString("track",trackNum);
                }
                else{
                    otherDetails.putString("track",track);
                }
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                try{
                    otherDetails.putString("duration",formatDuration(Long.parseLong(cursor.getString(9))));
                }catch(NumberFormatException e){
//                    String path = cursor.getString(2);
//                    MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
//                    mediaMetadataRetriever.setDataSource(path);
//                    String duration = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
//                    otherDetails.putString("duration",formatDuration(Long.parseLong(duration)));
                    System.out.println(cursor.getString(1));
                    continue;

                }

            }
            else{
                MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                mmr.setDataSource(cursor.getString(2));
                String duration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                otherDetails.putString("duration",formatDuration(Long.parseLong(duration)));
            }
            MediaDescriptionCompat songDetails = new MediaDescriptionCompat.Builder()
                    .setMediaId(cursor.getString(0))
                    .setTitle(cursor.getString(1))
                    .setMediaUri(Uri.parse(cursor.getString(2)))
                    .setExtras(otherDetails)
                    .build();
            MediaBrowserCompat.MediaItem song =
                    new MediaBrowserCompat.MediaItem(songDetails,
                            MediaBrowserCompat.MediaItem.FLAG_PLAYABLE);
            songs.add(song);
        }

        songs.sort(new SortSongs());

        while(artistCursor.moveToNext()){
            ArrayList<MediaBrowserCompat.MediaItem> artistSongs = new ArrayList<>();
            String name  = artistCursor.getString(1);
            for(MediaBrowserCompat.MediaItem song : songs){
                if(song.getDescription().getExtras().getString("artist").equals(name)){
                    artistSongs.add(song);
                }
            }
            Artist artist = new Artist(artistCursor.getString(0),name,artistCursor.getString(2),artistCursor.getString(3),artistSongs);
            artists.add(artist);
        }

        while(albumCursor.moveToNext()){
            ArrayList<MediaBrowserCompat.MediaItem> albumSongs = new ArrayList<>();
            String albumID = albumCursor.getString(0);
            for(MediaBrowserCompat.MediaItem song : songs){
                if(song.getDescription().getExtras().getString("albumID").equals(albumID)){
                    albumSongs.add(song);
                }
            }
            Album album = new Album(albumID,albumCursor.getString(1),albumCursor.getString(2),albumSongs);
            albums.add(album);
        }


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
            ArrayList<MediaBrowserCompat.MediaItem> playListSongs = new ArrayList<>();
            while(playlistSongCursor.moveToNext()){
                Log.d("yes",playlistSongCursor.getString(0));
                for(MediaBrowserCompat.MediaItem song:
                songs){
                    if(song.getMediaId().equals(playlistSongCursor.getString(0))){
                        playListSongs.add(song);
                    }
                }
            }
            playlists.add(new Playlist(id,playlistCursor.getString(1),playListSongs));
            playlistSongCursor.close();
        }

        while(genreCursor.moveToNext()){
            ArrayList<MediaBrowserCompat.MediaItem> genreSongs = new ArrayList<>();
            String ID = genreCursor.getString(0);
            if(ID == null){
                break;
            }
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
                for(MediaBrowserCompat.MediaItem song:
                        songs){
                    if(song.getMediaId().equals(genreSongCursor.getString(0))){
                        genreSongs.add(song);
                    }
                }
            }
            genres.add(new Category(ID,name,genreSongs));
        }
        albums.sort(new SortAlbum());
        artists.sort(new SortArtist());
        albumCursor.close();
        artistCursor.close();
        cursor.close();
        playlistCursor.close();
        songManager = new SongManager(songs,albums,artists,genres,playlists);
    }

    private static class SortSongs implements Comparator<MediaBrowserCompat.MediaItem>
    {

        @Override
        public int compare(MediaBrowserCompat.MediaItem o1, MediaBrowserCompat.MediaItem o2) {
            String firstValue = (String) o1.getDescription().getTitle();
            String secondValue = (String) o2.getDescription().getTitle();
            assert firstValue != null;
            assert secondValue != null;
            return firstValue.compareTo(secondValue);
        }
    }

    private String formatDuration(long duration) {
        long minutes = TimeUnit.MINUTES.convert(duration, TimeUnit.MILLISECONDS);
        long seconds = TimeUnit.SECONDS.convert(duration, TimeUnit.MILLISECONDS)
                - minutes * TimeUnit.SECONDS.convert(1, TimeUnit.MINUTES);

        return String.format(Locale.ENGLISH,"%02d:%02d", minutes, seconds);
    }

    private static class SortArtist implements Comparator<Artist>
    {

        @Override
        public int compare(Artist o1, Artist o2) {
            String firstValue = (String) o1.getName();
            String secondValue = (String) o2.getName();
            assert firstValue != null;
            assert secondValue != null;
            return firstValue.compareTo(secondValue);
        }
    }

    private static class SortAlbum implements Comparator<Album>
    {
        @Override
        public int compare(Album o1, Album o2) {
            String firstValue = (String) o1.getName();
            String secondValue = (String) o2.getName();
            assert firstValue != null;
            assert secondValue != null;
            return firstValue.compareTo(secondValue);
        }
    }

}
