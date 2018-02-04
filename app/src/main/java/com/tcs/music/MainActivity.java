package com.tcs.music;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatSeekBar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class MainActivity extends AppCompatActivity implements MediaPlayer.OnCompletionListener, SeekBar.OnSeekBarChangeListener {
    private static final int NOTIFY_ID = 1;
    private ArrayList<Songs> songList;
    private ListView songView;
    private View playerLayout;
    private int currentSongIndex = 0;
    private Handler mHandler = new Handler();
    private TextView stTime, endTime;
    private AppCompatSeekBar appCompatSeekBar;
    private ImageView play_prv;
    private ImageView play, pause;
    private ImageView next_song;
    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        songView = findViewById(R.id.song_list);
        playerLayout = findViewById(R.id.playerLayout);
        stTime = findViewById(R.id.stTime);
        appCompatSeekBar = findViewById(R.id.seekBar);
        endTime = findViewById(R.id.endTime);
        play_prv = findViewById(R.id.play_prv);
        play = findViewById(R.id.play);
        pause = findViewById(R.id.pause);
        next_song = findViewById(R.id.next_song);
        mediaPlayer = new MediaPlayer();
        songList = new ArrayList<>();
        appCompatSeekBar.setOnSeekBarChangeListener(this); // Important
        mediaPlayer.setOnCompletionListener(this); // Important
        getSongList();
        Collections.sort(songList, new Comparator<Songs>() {
            public int compare(Songs a, Songs b) {
                return a.getTitle().compareTo(b.getTitle());
            }
        });

        Adapter songAdt = new Adapter(this, songList);
        songView.setAdapter(songAdt);
        songView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                playerLayout.setVisibility(View.VISIBLE);
                currentSongIndex = position;
                playSong(songList.get(position).getUri());

            }
        });

        play_prv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentSongIndex > 0) {
                    currentSongIndex = currentSongIndex - 1;
                    playSong(songList.get(currentSongIndex - 1).getUri());

                } else {
                    // play last song
                    currentSongIndex = songList.size() - 1;
                    playSong(songList.get(songList.size() - 1).getUri());

                }

            }
        });

        next_song.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentSongIndex < (songList.size() - 1)) {
                    currentSongIndex = currentSongIndex + 1;
                    playSong(songList.get(currentSongIndex + 1).getUri());

                } else {
                    // play first song
                    currentSongIndex = 0;
                    playSong(songList.get(0).getUri());

                }
            }
        });

        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mediaPlayer.isPlaying()) {
                    play.setImageResource(R.drawable.play);
                    mediaPlayer.pause();
                    NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                    mNotifyMgr.cancel(NOTIFY_ID);
                } else {
                    play.setImageResource(R.drawable.pause);
                    mediaPlayer.start();
                    showNotification();
                }
            }
        });
    }


    private void playSong(String uri) {
        play.setImageResource(R.drawable.pause);
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        try {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(uri);
            mediaPlayer.prepare();
            mediaPlayer.start();
            showNotification();
            appCompatSeekBar.setProgress(0);
            appCompatSeekBar.setMax(100);
        } catch (IOException e) {
            e.printStackTrace();
        }
        updateProgressBar();

    }

    private void showNotification() {
        Intent notIntent = new Intent(this, MainActivity.class);
        notIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendInt = PendingIntent.getActivity(this, 0,
                notIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder builder = new Notification.Builder(this);

        builder.setContentIntent(pendInt)
                .setSmallIcon(getNotificationIcon(builder))
                .setTicker(songList.get(currentSongIndex).getTitle())
                .setOngoing(true)
                .setContentTitle("Playing").setContentText(songList.get(currentSongIndex).getTitle());
        Notification not = builder.build();
        NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
// Builds the notification and issues it.
        mNotifyMgr.notify(NOTIFY_ID, builder.build());
        // startForeground(NOTIFY_ID, not);
    }

    private void updateProgressBar() {
        mHandler.postDelayed(mUpdateTimeTask, 100);
    }

    private Runnable mUpdateTimeTask = new Runnable() {
        public void run() {
            long totalDuration = mediaPlayer.getDuration();
            long currentDuration = mediaPlayer.getCurrentPosition();

            // Displaying Total Duration time
            endTime.setText("" + Utilities.milliSecondsToTimer(totalDuration));
            // Displaying time completed playing
            stTime.setText("" + Utilities.milliSecondsToTimer(currentDuration));

            // Updating progress bar
            int progress = (int) (Utilities.getProgressPercentage(currentDuration, totalDuration));
            //Log.d("Progress", ""+progress);
            appCompatSeekBar.setProgress(progress);

            // Running this thread after 100 milliseconds
            mHandler.postDelayed(this, 100);
        }
    };

    private void getSongList() {
        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);
        if (musicCursor != null && musicCursor.moveToFirst()) {
            int uri = musicCursor.getColumnIndex
                    (MediaStore.Audio.Media.DATA);
            int titleColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.TITLE);
            int idColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media._ID);
            int artistColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.ARTIST);
            do {
                String mUri = musicCursor.getString(uri);
                long thisId = musicCursor.getLong(idColumn);
                String thisTitle = musicCursor.getString(titleColumn);
                String thisArtist = musicCursor.getString(artistColumn);
                songList.add(new Songs(thisId, thisTitle, thisArtist, mUri));
            }
            while (musicCursor.moveToNext());
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        mHandler.removeCallbacks(mUpdateTimeTask);
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        mHandler.removeCallbacks(mUpdateTimeTask);
        int totalDuration = mediaPlayer.getDuration();
        int currentPosition = Utilities.progressToTimer(seekBar.getProgress(), totalDuration);

        // forward or backward to certain seconds
        mediaPlayer.seekTo(currentPosition);

        // update timer progress again
        updateProgressBar();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        play.setImageResource(R.drawable.play);
        NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mNotifyMgr.cancel(NOTIFY_ID);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mNotifyMgr.cancel(NOTIFY_ID);
    }

    private int getNotificationIcon(Notification.Builder notificationBuilder) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            int color = 0x036085;
            notificationBuilder.setColor(color);
            return R.drawable.ic_stat_headphones;

        } else {
            return R.drawable.ic_stat_headphones;
        }
    }
}
