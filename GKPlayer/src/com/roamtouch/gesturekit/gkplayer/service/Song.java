package com.roamtouch.gesturekit.gkplayer.service;

import android.content.ContentUris;
import android.net.Uri;

public class Song {
    private long id;
    private String title;
    private String artist;
    private int albumId;
    private String album;
    private String songOrder;

    public Song(long id, String title, String artist, int albumId, String album, String songOrder) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.albumId = albumId;
        this.album = album;
        this.songOrder = songOrder;
    }

    public long getId() { return id; }
    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public int getAlbumId() { return albumId;}
    public String getAlbum() { return album;}
    public String getSongOrder() { return songOrder;}
    public Uri getURI() {
        return ContentUris.withAppendedId(
                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
    }
}
