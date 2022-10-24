package cy.org.cyens.reinherit;

import android.app.Activity;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Locale;

public class SoundManager {
    private final int mMaxSounds;
    private String mSoundFilesNamePrefix;
    private final String mSoundFileExtension;
    private final Activity mParentActivity;

    //This is a dictionary to save every mac address as a key and the device name as a value
    public Hashtable<Integer, MusicThread> Threads = new Hashtable<Integer, MusicThread>();

    public SoundManager(int maximumSoundsNo, String folder, String soundFilePrefix, String soundFileExtension, Activity parentActivity) {
        mParentActivity = parentActivity;
        mMaxSounds = maximumSoundsNo;
        mSoundFileExtension = soundFileExtension;
        initializeSounds(folder, soundFilePrefix);
    }

    public void onDestroy() {
        Enumeration<Integer> threadsEnumeration = Threads.keys();

        while (threadsEnumeration.hasMoreElements()) {
            // Getting the key of a particular entry
            int key = threadsEnumeration.nextElement();
            Threads.get(key).stopPlayback();
        }
    }

    private void initializeSounds(String folder, String soundFilePrefix){
        mSoundFilesNamePrefix = Environment.getExternalStorageDirectory().getPath() + folder + soundFilePrefix;
    }

    public void playSound(int index){
        stopSound(index+1);
        for(int i = 1; i <= index; i++){
            if(Threads.containsKey(i)){
                if(Threads.get(i).isAlive()){
                    continue;
                }else{
                    Threads.get(i).start();
                }
            }else{
                MusicThread music_thread = new MusicThread(i, mSoundFilesNamePrefix, mSoundFileExtension, mParentActivity);
                music_thread.start();
                Threads.put(i,music_thread);
            }
        }
    }

    public void stopSound(int index){
        for(int i = index; i <= mMaxSounds; i++){
            if(Threads.containsKey(i)){
                if(Threads.get(i).isAlive()){  //if thread is aive then music is still playing so kill the thread
                    Threads.get(i).mIsRunning = false;
                    Threads.remove(i);
                }
            }
        }
    }

    public class MusicThread extends Thread{
        public MediaPlayer mSoundPlayer;
        public int mId;
        private boolean mIsRunning;

        public MusicThread(int id, String filePrefix, String fileExtension, Activity parentActivity){
            this.mId = id;
            this.mIsRunning = true;

            String soundPath = String.format(Locale.US, "%s%d.%s", filePrefix, this.mId, fileExtension);
            this.mSoundPlayer = MediaPlayer.create(parentActivity, Uri.parse(soundPath));
        }

        public void stopPlayback(){
            this.mIsRunning = false;
        }

        public void run(){
            mSoundPlayer.start();
            mSoundPlayer.setLooping(true);

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
