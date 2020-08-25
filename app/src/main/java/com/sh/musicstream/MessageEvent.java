package com.sh.musicstream;

public class MessageEvent {

    public final String message;
    public final int duration;

    public MessageEvent(String message) {
        this.message = message;
        this.duration = 0;
    }

    public MessageEvent(String message, int duration) {
        this.message = message;
        this.duration = duration;
    }
}