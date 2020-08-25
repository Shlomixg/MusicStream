package com.sh.musicstream;

public class ServiceMessageEvent {

    public final String message;
    public final int duration;

    public ServiceMessageEvent(String message) {
        this.message = message;
        this.duration = 0;
    }

    public ServiceMessageEvent(String message, int duration) {
        this.message = message;
        this.duration = duration;
    }
}