package com.tcs.music;

/**
 * Created by prabhat on 28/1/18.
 */

public class Songs {
    private long id;
    private String title;
    private String artist;
    private String uri;

    public String getUri() {
        return uri;
    }

    public Songs(long songID, String songTitle, String songArtist, String uri) {
        id=songID;
        this.uri = uri;
        title=songTitle;
        artist=songArtist;
    }

    public long getID(){return id;}
    public String getTitle(){return title;}
    public String getArtist(){return artist;}
}
