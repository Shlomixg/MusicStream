package com.sh.musicstream;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class SongDetails extends AppCompatActivity {

    Song song;
    TextView song_name_tv, artist_tv, link_tv;
    ImageView album_art_iv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_song_details);

        song = (Song) getIntent().getSerializableExtra("songDetails");

        song_name_tv = findViewById(R.id.song_name_det_tv);
        artist_tv = findViewById(R.id.artist_det_tv);
        link_tv = findViewById(R.id.link_det_tv);
        album_art_iv = findViewById(R.id.album_art_det_iv);

        song_name_tv.setText(song.getName());
        artist_tv.setText(song.getArtist());
        link_tv.setText(song.getLink());
        album_art_iv.setImageURI(Uri.parse(song.getAlbumArt()));

    }

    public void onEditSong(View view) {
        Intent intent = new Intent(this, AddSong.class);
        intent.putExtra("editSong", song);
        intent.putExtra("index", getIntent().getIntExtra("index", 0));
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    public void onReturn(View view) {
        onBackPressed();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
}