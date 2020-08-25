package com.sh.musicstream;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;

import java.io.File;
import java.util.Date;
import java.util.List;

public class AddSong extends AppCompatActivity {

    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int SELECT_IMAGE = 2;

    Song songToEdit;
    boolean isEditing = false;

    EditText song_et, artist_et, link_et;
    ImageView art_iv;
    String imagePath, currDateTimeString;
    File file;
    Uri imageURI = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_song);

        song_et = findViewById(R.id.song_name_et);
        artist_et = findViewById(R.id.artist_name_et);
        link_et = findViewById(R.id.link_et);
        art_iv = findViewById(R.id.album_art_iv_edit);

        if (getIntent().hasExtra("editSong")) {
            songToEdit = (Song) getIntent().getSerializableExtra("editSong");
            if (songToEdit != null) {
                song_et.setText(songToEdit.getName());
                artist_et.setText(songToEdit.getArtist());
                link_et.setText(songToEdit.getLink());
                art_iv.setImageURI(Uri.parse(songToEdit.getAlbumArt()));
                isEditing = true;
            }
        }
    }

    public void onTakePic(View view) {
        TedPermission.with(this)
                .setPermissionListener(writePermissionListener)
                .setDeniedMessage("If you reject this permission, you won't be able to shot pictures for album art.\n\nPlease consider to turn on permissions at [Settings] > [Permissions]")
                .setPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .check();
    }

    PermissionListener writePermissionListener = new PermissionListener() {
        @Override
        public void onPermissionGranted() {
            currDateTimeString = java.text.DateFormat.getDateTimeInstance().format(new Date());
            file = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), currDateTimeString + ".png");
            imageURI = FileProvider.getUriForFile(AddSong.this, "com.sh.musicstream.provider", file);
            Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, imageURI);
            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
        }

        @Override
        public void onPermissionDenied(List<String> deniedPermissions) {
            Toast.makeText(AddSong.this, "Permission Denied.", Toast.LENGTH_SHORT).show();
        }
    };

    public void onSelectPic(View view) {
        TedPermission.with(this)
                .setPermissionListener(readPermissionListener)
                .setDeniedMessage("If you reject this permission, you won't be able to select pictures for album art.\n\nPlease consider to turn on permissions at [Settings] > [Permissions].")
                .setPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .check();
    }

    PermissionListener readPermissionListener = new PermissionListener() {
        @Override
        public void onPermissionGranted() {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), SELECT_IMAGE);
        }

        @Override
        public void onPermissionDenied(List<String> deniedPermissions) {
            Toast.makeText(AddSong.this, "Permission Denied.", Toast.LENGTH_SHORT).show();
        }
    };

    public void onSave(View view) {
        int index = getIntent().getIntExtra("index", 0);
        if (isEditing && index == MainActivity.currSongIdx) {
            final MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this);
            dialog.setMessage("You can't edit the song that is currently playing.\n\nPlease skip back or forward trough the notification and try save again.")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    }).setCancelable(false).create().show();
        } else if ((song_et.getText().toString().equals("")) || (artist_et.getText().toString().equals("")) || (link_et.getText().toString().equals(""))) {
            Toast.makeText(this, "All Song details must be provided", Toast.LENGTH_SHORT).show();
        } else {
            if (imageURI == null) {
                if (isEditing) imageURI = Uri.parse(songToEdit.getAlbumArt());
                else
                    imageURI = Uri.parse("android.resource://com.sh.musicstream/drawable/music512");
            }
            Song song = new Song(song_et.getText().toString(), artist_et.getText().toString(), imageURI.toString(), link_et.getText().toString());
            Intent intent = new Intent(AddSong.this, MainActivity.class);
            if (isEditing) {
                intent.putExtra("editedSong", song);
                intent.putExtra("index", index);
            } else intent.putExtra("newSong", song);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        }
    }

    public void onCancel(View view) {
        onBackPressed();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(AddSong.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // From Camera
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            art_iv.setImageDrawable(Drawable.createFromPath(file.getAbsolutePath()));
            imagePath = file.getAbsolutePath();
        }
        // From Gallery
        if (requestCode == SELECT_IMAGE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                imageURI = data.getData();
                art_iv.setImageURI(imageURI);
            } else {
                Toast.makeText(this, "Error. Try again.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}