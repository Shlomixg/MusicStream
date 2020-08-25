package com.sh.musicstream;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.widget.RemoteViews;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;

public class MusicPlayerService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener {

    private MediaPlayer mPlayer = new MediaPlayer();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mPlayer.setOnPreparedListener(this);
        mPlayer.setOnCompletionListener(this);
        mPlayer.reset();
        EventBus.getDefault().register(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent.hasExtra("command_notif")) {
            onMessageEvent(new MessageEvent(intent.getStringExtra("command_notif")));
        } else {
            if (mPlayer.isPlaying()) {
                mPlayer.reset();
            }
            if (!mPlayer.isPlaying()) {
                try {
                    mPlayer.setDataSource(MainActivity.playlist.get(MainActivity.currSongIdx).getLink());
                    mPlayer.prepareAsync();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mPlayer.start();
        MainActivity.playlist.get(MainActivity.currSongIdx).setDuration(mPlayer.getDuration());
        EventBus.getDefault().post(new ServiceMessageEvent("updateNewSong", mPlayer.getDuration()));
        final int NOTIFICATION_ID = 1;
        updateProgress();
        EventBus.getDefault().post(new ServiceMessageEvent("resumePlay"));
        startForeground(NOTIFICATION_ID, buildNotification());
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (MainActivity.currSongIdx >= MainActivity.playlist.size() - 1) {
            mHandler.removeCallbacks(runnable);
        }
        playSong(true);
    }

    @Override
    public void onDestroy() {
        mHandler.removeCallbacks(runnable);
        stopForeground(true);
        EventBus.getDefault().unregister(this);
        super.onDestroy();
        if (mPlayer != null) {
            if (mPlayer.isPlaying()) mPlayer.stop();
            mPlayer.reset();
            mPlayer.release();
            mPlayer = null;
        }
    }

    // This method will be called when a MessageEvent is posted (in the UI thread for Toast)
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent event) {
        switch (event.message) {
            case "prev":
                if (mPlayer.isPlaying()) {
                    mHandler.removeCallbacks(runnable);
                }
                playSong(false);
                break;
            case "next":
                if (mPlayer.isPlaying()) {
                    mHandler.removeCallbacks(runnable);
                }
                playSong(true);
                break;
            case "stop":
                stopForeground(true);
                stopSelf();
                MainActivity.isPlaying = false;
                MainActivity.isPaused = false;
                MainActivity.currSongIdx = 0;
                EventBus.getDefault().post(new ServiceMessageEvent("reset"));
                break;
            case "pause":
                if (mPlayer != null) {
                    mPlayer.pause();
                    mHandler.removeCallbacks(runnable);
                    MainActivity.isPlaying = false;
                    MainActivity.isPaused = true;
                    EventBus.getDefault().post(new ServiceMessageEvent("pausePlay"));
                }
                break;
            case "play":
                if (mPlayer != null) {
                    mPlayer.start();
                    updateProgress();
                    MainActivity.isPlaying = true;
                    MainActivity.isPaused = false;
                    EventBus.getDefault().post(new ServiceMessageEvent("resumePlay"));
                }
                break;
            case "updateTime":
                mPlayer.seekTo(event.duration);
                break;
        }
    }

    public void playSong(boolean isNext) {
        boolean continuePlay = false;
        if (isNext) {
            if (MainActivity.currSongIdx < MainActivity.playlist.size() - 1) {
                MainActivity.currSongIdx++;
                continuePlay = true;
            }
        } else {
            if (MainActivity.currSongIdx > 0) {
                MainActivity.currSongIdx--;
                continuePlay = true;
            }
        }
        if (continuePlay) {
            mPlayer.reset();
            try {
                MainActivity.isPlaying = true;
                MainActivity.isPaused = false;
                mPlayer.setDataSource(MainActivity.playlist.get(MainActivity.currSongIdx).getLink());
                mPlayer.prepareAsync();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            mHandler.removeCallbacks(runnable);
            mPlayer.pause();
            mPlayer.seekTo(0);
            MainActivity.isPlaying = false;
            MainActivity.isPaused = true;
            EventBus.getDefault().post(new ServiceMessageEvent("reset"));
        }
    }

    private Handler mHandler = new Handler();

    private void updateProgress() {
        EventBus.getDefault().post(new ServiceMessageEvent("progress", mPlayer.getCurrentPosition()));
        mHandler.postDelayed(runnable, 1000);
    }

    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            updateProgress();
        }
    };

    public Notification buildNotification() {
        final String CHANNEL_ID = "music_notification_id";

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= 26) {
            CharSequence channelName = "MusicStream Channel";
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID, channelName, importance);
            notificationChannel.enableLights(true);

            manager.createNotificationChannel(notificationChannel);
        }

        // Get the layout to use in the custom notification
        RemoteViews notificationLayout = new RemoteViews(getPackageName(), R.layout.notificaion_layout);
        RemoteViews notificationExpandedLayout = new RemoteViews(getPackageName(), R.layout.notification_expanded_layout);
        notificationLayout.setTextViewText(R.id.notif_song_name_tv, MainActivity.playlist.get(MainActivity.currSongIdx).getName());
        notificationLayout.setTextViewText(R.id.notif_artist_name_tv, " - " + MainActivity.playlist.get(MainActivity.currSongIdx).getArtist());
        notificationExpandedLayout.setTextViewText(R.id.notif_song_name_tv, MainActivity.playlist.get(MainActivity.currSongIdx).getName());
        notificationExpandedLayout.setTextViewText(R.id.notif_artist_name_tv, " - " + MainActivity.playlist.get(MainActivity.currSongIdx).getArtist());

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID);

        Intent skipPrevIntent = new Intent(this, MusicPlayerService.class);
        skipPrevIntent.putExtra("command_notif", "prev");
        PendingIntent prevPendingIntent = PendingIntent.getService(this, 0, skipPrevIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        notificationLayout.setOnClickPendingIntent(R.id.notif_skip_prev_btn, prevPendingIntent);
        notificationExpandedLayout.setOnClickPendingIntent(R.id.notif_skip_prev_btn, prevPendingIntent);

        Intent stopIntent = new Intent(this, MusicPlayerService.class);
        stopIntent.putExtra("command_notif", "stop");
        PendingIntent stopPendingIntent = PendingIntent.getService(this, 1, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        notificationLayout.setOnClickPendingIntent(R.id.notif_stop_btn, stopPendingIntent);
        notificationExpandedLayout.setOnClickPendingIntent(R.id.notif_stop_btn, stopPendingIntent);

        Intent pauseIntent = new Intent(this, MusicPlayerService.class);
        pauseIntent.putExtra("command_notif", "pause");
        PendingIntent pausePendingIntent = PendingIntent.getService(this, 2, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        notificationLayout.setOnClickPendingIntent(R.id.notif_pause_btn, pausePendingIntent);
        notificationExpandedLayout.setOnClickPendingIntent(R.id.notif_pause_btn, pausePendingIntent);

        Intent playIntent = new Intent(this, MusicPlayerService.class);
        playIntent.putExtra("command_notif", "play");
        PendingIntent playPendingIntent = PendingIntent.getService(this, 3, playIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        notificationLayout.setOnClickPendingIntent(R.id.notif_play_btn, playPendingIntent);
        notificationExpandedLayout.setOnClickPendingIntent(R.id.notif_play_btn, playPendingIntent);

        Intent skipNextIntent = new Intent(this, MusicPlayerService.class);
        skipNextIntent.putExtra("command_notif", "next");
        PendingIntent nextPendingIntent = PendingIntent.getService(this, 4, skipNextIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        notificationLayout.setOnClickPendingIntent(R.id.notif_skip_next_btn, nextPendingIntent);
        notificationExpandedLayout.setOnClickPendingIntent(R.id.notif_skip_next_btn, nextPendingIntent);

        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSmallIcon(R.drawable.ic_notificaion)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setCustomContentView(notificationLayout)
                .setCustomBigContentView(notificationExpandedLayout);

        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 5, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingIntent);

        return builder.build();
    }

}