package de.thu.madmusicplayer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements MediaPlayer.OnPreparedListener {
    private MediaPlayer mediaPlayer;
    SharedPreferences preferences;
    private static final int MY_PERMISSION_REQUEST = 1;

    private TextView songTitleTextView;
    private TextView songArtistTextView;
    private TextView songAlbumTextView;

    private Button prevSongButton;
    private Button playPauseSongButton;
    private Button nextSongButton;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        preferences = getPreferences(MODE_PRIVATE);

        songTitleTextView = (TextView) findViewById(R.id.titleTextView);
        songArtistTextView = (TextView) findViewById(R.id.artistTextView);
        songAlbumTextView = (TextView) findViewById(R.id.albumTextView);

        prevSongButton = (Button) findViewById(R.id.prevSongButton);
        playPauseSongButton = (Button) findViewById(R.id.playPauseButton);
        nextSongButton = (Button) findViewById(R.id.nextSongButton);

        //Disable the buttons before the song loads
        prevSongButton.setEnabled(false);
        playPauseSongButton.setEnabled(false);
        nextSongButton.setEnabled(false);

        //Ask for permission
        if (ContextCompat.checkSelfPermission(
                MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED) {

        } else if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)) {

        } else {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[] { Manifest.permission.READ_EXTERNAL_STORAGE },
                    MY_PERMISSION_REQUEST);
        }


        //Display the newly/previously loaded song
        displaySong();


        mediaPlayer.setOnPreparedListener(this);
        loadSong();


    }
    public static Uri getSongUri(int songId) {
        return ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songId);
    }


   synchronized private void getAndStoreRandomSong(){
        ContentResolver cr = getContentResolver();
        Uri SongUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        Cursor songCursor = cr.query(SongUri,null,null,null,null);
        int songsFoundCount = songCursor.getCount();


        if(songsFoundCount > 0){
            //Generate new random number within the found songs index
            Random random = new Random();
            int rndIndex = random.nextInt(songsFoundCount);

            //Move the cursor to the random generated position
            songCursor.moveToPosition(rndIndex);

            int songId = songCursor.getColumnIndex(MediaStore.Audio.Media._ID);
            int songTitle = songCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int songArtist = songCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            int songAlbum = songCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
            int songDuration = songCursor.getColumnIndex(MediaStore.Audio.Media.DURATION);

            int currentId = songCursor.getInt(songId);
            String currentTitle = songCursor.getString(songTitle);
            String currentArtist = songCursor.getString(songArtist);
            String currentAlbum = songCursor.getString(songAlbum);
            String currentDuration = songCursor.getString(songDuration);

            //Display song info into the front end of the app and save it to the shared preferences

            SharedPreferences.Editor e = preferences.edit();
            e.putInt("songId",currentId);
            e.putString("songTitle",currentTitle);
            e.putString("songArtist",currentArtist);
            e.putString("songAlbum",currentAlbum);
            e.putString("songDuration",currentDuration);
            e.apply();

            displaySong();

            if(mediaPlayer.isPlaying()){
                loadSong();
            }

        }
        else{
            //No songs were found
            Log.i("SONGS INFO:", "No songs were found!");
        }
    }

    synchronized private void loadSong(){
        int currentId = preferences.getInt("songId",0);

        if(currentId!=0){
            Uri currentSongUri = getSongUri(currentId);
            try{
                if(mediaPlayer == null){
                    mediaPlayer = new MediaPlayer();
                }
                else{
                    mediaPlayer.reset();
                }

                mediaPlayer.setDataSource(getApplicationContext(),currentSongUri);
                mediaPlayer.prepareAsync();

                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        loadSong();
                        //do anything else you desire
                    }
                });


            }catch (IOException e){
                Log.e("MADMusicPlayer","Failed to open stream!");
                return;
            }
        }

    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // This overrides default action
    }

    synchronized private void displaySong(){

        if((preferences.getString("songTitle","").equals(""))){
            getAndStoreRandomSong();
        }
        else{

            String currentTitle = preferences.getString("songTitle","");
            String currentArtist = preferences.getString("songArtist","");
            String currentAlbum = preferences.getString("songAlbum","");
            //String currentDuration = preferences.getString("songDuration","");

            currentAlbum = "Album: " + currentAlbum;


            songTitleTextView.setText(currentTitle);
            songArtistTextView.setText(currentArtist);
            songAlbumTextView.setText( currentAlbum );

            if(!currentArtist.equals("<unknown>")){
                songArtistTextView.setVisibility(View.VISIBLE);
            }
            else{
                songArtistTextView.setVisibility(View.INVISIBLE);
            }


            loadSong();


        }
    }

    //On button click methods
    public void onSelectRandomSongButton(View v){
        getAndStoreRandomSong();
    }

    public void onPlayPauseButtonClick(View v){
        if(!mediaPlayer.isPlaying()){
            mediaPlayer.start();
        }
        else{
            mediaPlayer.pause();

        }
    }

    public void onSelectPreviousSongButton(View v){
        getAndStoreRandomSong();

    }

    public void onSelectNextSongButton(View v){
        getAndStoreRandomSong();

    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        mediaPlayer.setLooping(true);
        prevSongButton.setEnabled(true);
        playPauseSongButton.setEnabled(true);
        nextSongButton.setEnabled(true);
    }
}