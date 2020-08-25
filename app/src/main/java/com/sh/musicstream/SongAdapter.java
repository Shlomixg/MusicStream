package com.sh.musicstream;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.songCardHolder> {

    private List<Song> songList;
    private SongClickListener listener;

    interface SongClickListener {
        void onClickListener(int pos, View v);

        void onLongClickListener(int pos, View v);
    }

    public void setListener(SongClickListener listener) {
        this.listener = listener;
    }

    public SongAdapter(List<Song> songList) {
        this.songList = songList;
    }

    public class songCardHolder extends RecyclerView.ViewHolder {

        TextView songName;
        TextView artistName;
        ImageView albumArt;

        public songCardHolder(@NonNull View itemView) {
            super(itemView);
            songName = itemView.findViewById(R.id.song_name_holder);
            artistName = itemView.findViewById(R.id.artist_name_holder);
            albumArt = itemView.findViewById(R.id.album_art_iv_holder);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null)
                        listener.onClickListener(getAdapterPosition(), v);
                }
            });

            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (listener != null)
                        listener.onLongClickListener(getAdapterPosition(), v);
                    return false;
                }
            });
        }
    }

    @NonNull
    @Override
    public SongAdapter.songCardHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.song_layout, parent, false);
        return new songCardHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull SongAdapter.songCardHolder holder, int position) {
        Song song = songList.get(position);
        holder.songName.setText(song.getName());
        holder.artistName.setText(song.getArtist());

        String path = song.getAlbumArt();
        if (path == null) {
            path = "android.resource://com.sh.musicstream/drawable/ic_launcher_foreground";
        }
        Glide.with(holder.itemView.getContext()).load(path).into(holder.albumArt);
    }

    @Override
    public int getItemCount() {
        return songList.size();
    }

    public void onItemMove(ArrayList<Song> list, int fromPosition, int toPosition) {
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(list, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(list, i, i - 1);
            }
        }
        notifyItemMoved(fromPosition, toPosition);
    }
}