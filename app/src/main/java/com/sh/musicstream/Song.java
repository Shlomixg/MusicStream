package com.sh.musicstream;

import java.io.Serializable;

public class Song implements Serializable {
    private String name;
    private String artist;
    private String albumArt;
    private String link;
    private int duration;

    /* Constructor */
    public Song(String name, String artist, String albumArt, String link) {
        this.name = name;
        this.artist = artist;
        this.albumArt = albumArt;
        this.link = link;
        this.duration = 10000;
    }

    /* Setters */
    public void setAlbumArt(String albumArt) {
        this.albumArt = albumArt;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    /* Getters */
    public String getName() {
        return name;
    }

    public String getArtist() {
        return artist;
    }

    public String getAlbumArt() {
        return albumArt;
    }

    public String getLink() {
        return link;
    }

    public int getDuration() {
        return duration;
    }

}
