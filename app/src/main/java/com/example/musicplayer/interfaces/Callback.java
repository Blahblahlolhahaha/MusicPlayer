package com.example.musicplayer.interfaces;

import android.graphics.Bitmap;

public interface Callback {
    void callback(String songName, String artist, String albumArt, String duration);
    void setLogo(boolean playOrStop);
}
