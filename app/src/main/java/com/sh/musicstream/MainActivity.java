package com.sh.musicstream;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.slider.LabelFormatter;
import com.google.android.material.slider.Slider;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    TextView song_tv, artist_tv;
    ImageView album_art_iv;
    ImageButton playBtn;
    RecyclerView recycler;
    Slider slider;
    Song tempSong;

    SongAdapter adapter;
    static ArrayList<Song> playlist = new ArrayList<>();
    static int currSongIdx = 0;
    static boolean isPlaying = false, isPaused = false;
    boolean isSliderActive = false;

    SharedPreferences sharedPrefs;

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        EventBus.getDefault().unregister(this);
        savePlaylist(playlist);
        super.onStop();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        adapter = new SongAdapter(playlist);
        adapter.notifyDataSetChanged();

        recycler = findViewById(R.id.recycler);
        song_tv = findViewById(R.id.song_name_tv);
        artist_tv = findViewById(R.id.artist_name_tv);
        album_art_iv = findViewById(R.id.album_art_iv);
        playBtn = findViewById(R.id.play_btn);
        slider = findViewById(R.id.slider);

        if (playlist.size() == 0) {
            sharedPrefs = getSharedPreferences("com.sh.musicstream", MODE_PRIVATE);
            if (sharedPrefs.getBoolean("first_run", true)) {
                playlist.add(new Song("Happy", "Pharrell Williams", "android.resource://com.sh.musicstream/drawable/pharrell_williams_girl_cover", "https://static.wixstatic.com/mp3/6c9f37_1ceae9ae5a664d929162b02a7ce0022b.mp3"));
                playlist.add(new Song("God Save The Queen", "Queen", "android.resource://com.sh.musicstream/drawable/queen_a_night_at_the_opera_cover", "https://static.wixstatic.com/mp3/6c9f37_e8f9143687004b91ab5fac79ab90bf01.mp3"));
                playlist.add(new Song("Riders On The Storm ", "Infected Mushroom", "android.resource://com.sh.musicstream/drawable/infected_mushroom_legend_of_the_black_shawarma_cover", "https://static.wixstatic.com/mp3/6c9f37_b81c7a20f99547c0ba7c20a6e3cc9f0d.mp3"));
                playlist.add(new Song("Bohemian Rhapsody", "Queen", "android.resource://com.sh.musicstream/drawable/queen_a_night_at_the_opera_cover", "https://static.wixstatic.com/mp3/6c9f37_ec2cdcbf3ab441ff955cfbcc4d0dfd79.mp3"));
                playlist.add(new Song("Karma Police", "Radiohead", "android.resource://com.sh.musicstream/drawable/radiohead_ok_computer_cover", "https://static.wixstatic.com/mp3/6c9f37_8b44fd713b5249d996a5c137138de39d.mp3"));
                playlist.add(new Song("Send Me an Angel", "Infected Mushroom", "android.resource://com.sh.musicstream/drawable/infected_mushroom_army_of_mushrooms_cover", "https://static.wixstatic.com/mp3/6c9f37_f0502caeef714d51bb1b33d9a97d3405.mp3"));
                savePlaylist(playlist);
                sharedPrefs.edit().putBoolean("first_run", false).apply();
            } else {
                loadPlaylist(playlist);
            }
        }

        updateSongUI(currSongIdx);

        if (getIntent().hasExtra("newSong")) {
            tempSong = (Song) getIntent().getSerializableExtra("newSong");
            playlist.add(tempSong);
            savePlaylist(playlist);
            adapter.notifyDataSetChanged();
        } else if (getIntent().hasExtra("editedSong") && getIntent().hasExtra("index")) {
            tempSong = (Song) getIntent().getSerializableExtra("editedSong");
            int tempIdx = getIntent().getIntExtra("index", 0);
            playlist.set(tempIdx, tempSong);
            savePlaylist(playlist);
            adapter.notifyDataSetChanged();
        }

        recycler.setHasFixedSize(true);
        recycler.setLayoutManager(new LinearLayoutManager(this));

        adapter.setListener(new SongAdapter.SongClickListener() {
            @Override
            public void onClickListener(int pos, View v) {
                Intent intent = new Intent(MainActivity.this, SongDetails.class);
                intent.putExtra("songDetails", playlist.get(pos));
                intent.putExtra("index", pos);
                startActivity(intent);
            }

            @Override
            public void onLongClickListener(int pos, View v) {

            }
        });

        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                Song temp = playlist.get(currSongIdx);
                adapter.onItemMove(playlist, viewHolder.getAdapterPosition(), target.getAdapterPosition());
                savePlaylist(playlist);
                currSongIdx = playlist.indexOf(temp); // Update index of current song
                return true;
            }

            @Override
            public void onSwiped(@NonNull final RecyclerView.ViewHolder viewHolder, int direction) {
                if (playlist.size() == 1) {
                    final MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(viewHolder.itemView.getContext());
                    dialog.setMessage("You can't leave the list empty.\n\nIf you want to remove this song, edit it or add another song and than remove.")
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    adapter.notifyItemChanged(viewHolder.getAdapterPosition());
                                }
                            }).setCancelable(false).create().show();
                } else if (viewHolder.getAdapterPosition() == currSongIdx) {
                    final MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(viewHolder.itemView.getContext());
                    dialog.setMessage("You can't delete the song that is currently playing.\n\nPlease skip back or forward.")
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    adapter.notifyItemChanged(viewHolder.getAdapterPosition());
                                }
                            }).setCancelable(false).create().show();
                } else {
                    final MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(viewHolder.itemView.getContext());
                    dialog.setMessage("Delete this from the list?")
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Song temp = playlist.get(currSongIdx);
                                    playlist.remove(viewHolder.getAdapterPosition());
                                    adapter.notifyItemRemoved(viewHolder.getAdapterPosition());
                                    savePlaylist(playlist);
                                    currSongIdx = playlist.indexOf(temp); // Update index of current song
                                }
                            })
                            .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    adapter.notifyItemChanged(viewHolder.getAdapterPosition());
                                }
                            }).setCancelable(false).create().show();
                }
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(recycler);
        recycler.setAdapter(adapter);

        slider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {
                isSliderActive = true;
            }

            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {
                EventBus.getDefault().post(new MessageEvent("updateTime", (int) slider.getValue()));
                isSliderActive = false;
            }
        });
        slider.addOnChangeListener(new Slider.OnChangeListener() {
            @Override
            public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {

            }
        });
        slider.setLabelFormatter(new LabelFormatter() {
            @NonNull
            @Override
            public String getFormattedValue(float value) {
                int duration = (int) value;
                int mns = (duration / 60000) % 60000;
                int scs = duration % 60000 / 1000;

                return String.format("%02d:%02d", mns, scs);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateSongUI(currSongIdx);
    }

    /**
     * Creating menu
     *
     * @param menu menu to create
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    /**
     * Handling selection of items in menu
     *
     * @param item selected item
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add_song_item:
                Intent intent = new Intent(MainActivity.this, AddSong.class);
                startActivity(intent);
                return true;
            case android.R.id.home:
                Toast.makeText(this, "home", Toast.LENGTH_SHORT).show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onPlayPauseBtn(View view) {
        if (isPlaying) {
            EventBus.getDefault().post(new MessageEvent("pause"));
            playBtn.setBackgroundResource(R.drawable.ic_baseline_play_arrow_24);
        } else {
            if (isPaused) {
                EventBus.getDefault().post(new MessageEvent("play"));
            } else {
                Intent intent = new Intent(this, MusicPlayerService.class);
                startService(intent);
                isPlaying = true;
                isPaused = false;
            }
            playBtn.setBackgroundResource(R.drawable.ic_baseline_pause_24);
        }
    }

    public void onStopBtn(View view) {
        Intent intent = new Intent(this, MusicPlayerService.class);
        stopService(intent);
        isPlaying = false;
        isPaused = false;
        currSongIdx = 0;
        updateSongUI(0);
    }

    public void onSkipPrevBtn(View view) {
        EventBus.getDefault().post(new MessageEvent("prev"));
    }

    public void onSkipNextBtn(View view) {
        EventBus.getDefault().post(new MessageEvent("next"));
    }

    /**
     * Updating the ui elements and display to the user the correct details of the song
     *
     * @param idx Index of the song
     */
    public void updateSongUI(int idx) {
        song_tv.setText(playlist.get(idx).getName());
        artist_tv.setText(playlist.get(idx).getArtist());
        String path = (playlist.get(idx).getAlbumArt());
        if (path == null) {
            path = "android.resource://com.sh.musicstream/drawable/ic_launcher_foreground";
        }
        Glide.with(this).load(path).into(album_art_iv);

        slider.setValue(0);
        slider.setValueTo(playlist.get(idx).getDuration());

        if (isPlaying) {
            playBtn.setBackgroundResource(R.drawable.ic_baseline_pause_24);
        } else {
            playBtn.setBackgroundResource(R.drawable.ic_baseline_play_arrow_24);
        }
    }

    public void loadPlaylist(ArrayList<Song> list) {
        try {
            FileInputStream fis = openFileInput("songs");
            ObjectInputStream isr = new ObjectInputStream(fis);
            int size = isr.readInt();
            for (int i = 0; i < size; i++) {
                list.add((Song) isr.readObject());
            }
            isr.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void savePlaylist(ArrayList<Song> list) {
        FileOutputStream fos;
        try {
            fos = openFileOutput("songs", MODE_PRIVATE);
            ObjectOutputStream os = new ObjectOutputStream(fos);
            os.writeInt(list.size());
            for (Song v : list) {
                os.writeObject(v);
            }
            os.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // This method will be called when a ServiceMessageEvent is posted
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onServiceMessageEvent(ServiceMessageEvent event) {
        switch (event.message) {
            case "updateNewSong":
                slider.setValue(0);
                slider.setValueTo((float) event.duration);
                updateSongUI(currSongIdx);
                break;
            case "progress":
                if (!isSliderActive) slider.setValue((float) event.duration);
                break;
            case "pausePlay":
                playBtn.setBackgroundResource(R.drawable.ic_baseline_play_arrow_24);
                break;
            case "resumePlay":
                playBtn.setBackgroundResource(R.drawable.ic_baseline_pause_24);
                break;
            case "reset":
                updateSongUI(currSongIdx);
                break;
        }
    }
}