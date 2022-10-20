package cy.org.cyens.reinherit;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Adopted with modifications from Ilya Gazman (3/6/2016)
 * https://stackoverflow.com/questions/17674634/saving-and-reading-bitmaps-images-from-internal-memory-in-android
 */
public class ImageManager {

    private static final String TAG = ImageManager.class.getName();
    private final File mDirectory;
    private final String mDirectoryName;
    private final Context mContext;

    public ImageManager(Context context, String directoryName) {
        this.mContext = context;
        mDirectoryName = directoryName;
        mDirectory = mContext.getDir(mDirectoryName, Context.MODE_PRIVATE);
        if (!mDirectory.exists() && !mDirectory.mkdirs()) {
            Log.e(TAG, "Error creating directory " + mDirectory);
        }
    }

    public void save(Mat matImage, String fileName) {
        Bitmap bitmapImage = Bitmap.createBitmap(matImage.width(), matImage.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(matImage, bitmapImage);
        save(bitmapImage, fileName);
    }

    public static Mat bitmapToMat(Bitmap bitmapImage) {
        Mat matImage = new Mat();
        // https://stackoverflow.com/questions/17390289/convert-bitmap-to-mat-after-capture-image-using-android-camera
        Bitmap bitmapImage32 = bitmapImage.copy(Bitmap.Config.ARGB_8888, true); // ensures it is ARGB_8888
        Utils.bitmapToMat(bitmapImage32, matImage);
        Imgproc.cvtColor(matImage, matImage, Imgproc.COLOR_BGR2GRAY);

        return matImage;
    }

    public void save(Bitmap bitmapImage, String fileName) {
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(createFile(fileName));
            bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean directoryExists() {
        return mContext.getDir(mDirectoryName, Context.MODE_PRIVATE).exists();
    }

    public void clearDir() {
        if (!mDirectory.exists()) {
            Log.e(TAG, "Directory does not exists" + mDirectory);
            return;
        }
        String[] fileNames;

        fileNames = mDirectory.list();
        if (fileNames == null) {
            return;
        }

        for (String fileName : fileNames) {
            File file = new File(mDirectory, fileName);
            file.delete();
        }
    }

    public boolean deleteFile(String fileName) {
        File file = new File(mDirectory, fileName);
        return file.delete();
    }

    public ArrayList<Bitmap> getImagesFromDir() {
        if (!mDirectory.exists()) {
            Log.e(TAG, "Directory does not exists" + mDirectory);
            return null;
        }
        ArrayList<Bitmap> images = new ArrayList<>();
        File lister = mDirectory.getAbsoluteFile();

        for (String list : lister.list()) {
            images.add(load(list));
        }
        return images;
    }

    private File createFile(String fileName) {
        if (!mDirectory.exists()) {
            Log.e(TAG, "Directory does not exists" + mDirectory);
            return null;
        }

        return new File(mDirectory, fileName);
    }

    public Bitmap load(String fileName) {
        if (fileName == null) {
            return null;
        }
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(createFile(fileName));
            return BitmapFactory.decodeStream(inputStream);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}