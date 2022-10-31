package cy.org.cyens.reinherit;

import android.app.Activity;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;

import java.io.File;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Objects;

public class SoundManager {
    private final int mMaxSounds;
    private String mSoundFilesNamePrefix;
    private final String mSoundFileExtension;
    private final Activity mParentActivity;
    private boolean mLoopSounds;

    //This is a dictionary to save every mac address as a key and the device name as a value
    public Hashtable<Integer, MusicThread> mMusicThreads = new Hashtable<Integer, MusicThread>();

    //Create a thread to constantly play a default background noise for each station
    private MusicThread backgroundAudioMediaThread;
    private boolean isBackgroundAudioPlaying = false;

    //Create a media player object for each of the five sounds that needs to be played
    protected static MediaPlayer createMediaPlayer(int id, String filePrefix, String fileExtension, Activity parentActivity){
        String soundPath = String.format(Locale.US, "%s%d.%s", filePrefix, id, fileExtension);
        File soundFile = new File(soundPath);
        return soundFile.exists() ? MediaPlayer.create(parentActivity, Uri.parse(soundPath)) : null;
    }

    public SoundManager(int maximumSoundsNo, String folder, String soundFilePrefix, String soundFileExtension, Activity parentActivity, boolean loopSounds, boolean includeBackgroundAudio) {
        mParentActivity = parentActivity;
        mMaxSounds = maximumSoundsNo;
        mSoundFileExtension = soundFileExtension;
        setLoopSounds(loopSounds);
        initializeSounds(folder, soundFilePrefix);

        startBackgroundAudio(includeBackgroundAudio);
    }

    //Start the background audio
    private void startBackgroundAudio(boolean includeBackgroundAudio) {
        if (includeBackgroundAudio) {
            if (backgroundAudioMediaThread == null)// if no audio background is playling create a thread for it
                backgroundAudioMediaThread = new MusicThread(0, mSoundFilesNamePrefix, mSoundFileExtension, mParentActivity, mLoopSounds);

            backgroundAudioMediaThread.start();// start the thread
            isBackgroundAudioPlaying = true;
        }
        else
            backgroundAudioMediaThread = null;
    }

    public void onDestroy() {
        Enumeration<Integer> threadsEnumeration = mMusicThreads.keys();

        while (threadsEnumeration.hasMoreElements()) {
            // Getting the key of a particular entry
            int key = threadsEnumeration.nextElement();
            Objects.requireNonNull(mMusicThreads.get(key)).stopPlayback();
        }
    }

    private void initializeSounds(String folder, String soundFilePrefix){
        mSoundFilesNamePrefix = Environment.getExternalStorageDirectory().getPath() + folder + soundFilePrefix;
    }

    //make the sounds loop
    public void setLoopSounds(boolean loopSounds){
        mLoopSounds = loopSounds;
    }

    //play sounds based on how many people are detected at any given time
    public void playSound(int index){
        if (backgroundAudioMediaThread != null && !isBackgroundAudioPlaying) {
            startBackgroundAudio(true);
        }

        stopSound(index+1); //stop sounds that were playing when more people were present in the camera view
        for(int i = 1; i <= index; i++){
            if(mMusicThreads.containsKey(i)){
                MusicThread thread = Objects.requireNonNull(mMusicThreads.get(i));
                if(!thread.isAlive()){//check if the sound is already playing
                    thread.start();
                }
            }else{
                MusicThread music_thread = new MusicThread(i, mSoundFilesNamePrefix, mSoundFileExtension, mParentActivity, mLoopSounds);
                music_thread.start();
                mMusicThreads.put(i,music_thread);
            }
        }
    }

    //stop sounds
    public void stopSound(int index){
        if (index == 0 && backgroundAudioMediaThread != null) {
            isBackgroundAudioPlaying = false;
            backgroundAudioMediaThread.stopPlayback();
        }

        for(int i = index; i <= mMaxSounds; i++){
            if(mMusicThreads.containsKey(i)){
                MusicThread thread = Objects.requireNonNull(mMusicThreads.get(i));
                if(thread.isAlive()){  //if thread is aive then music is still playing so kill the thread
                    thread.stopPlayback();
                    mMusicThreads.remove(i);
                }
            }
        }
    }

    //MusicThread class that extends java Thread. Run multiple instances of this class to play simultaneously multiple sources of audio
    public static class MusicThread extends Thread{
        public MediaPlayer mSoundPlayer;
        private boolean mIsRunning;
        private final boolean mLoopSound;

        public MusicThread(int id, String filePrefix, String fileExtension, Activity parentActivity, boolean loop){
            this.mSoundPlayer = createMediaPlayer(id, filePrefix, fileExtension, parentActivity);
            mLoopSound = loop;
        }

        public void stopPlayback(){
            this.mIsRunning = false;
        }

        //Main function that runs everytime a new thread is created
        public void run(){
            if (mSoundPlayer == null) return;

            this.mIsRunning = true;

            mSoundPlayer.start();
            mSoundPlayer.setLooping(mLoopSound);

            try {
                while(mSoundPlayer.isPlaying() && mIsRunning){
                    Thread.sleep(100); // wait
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mSoundPlayer.release();
        }
    }
}
