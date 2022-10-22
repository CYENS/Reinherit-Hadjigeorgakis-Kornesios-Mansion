package cy.org.cyens.reinherit;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.view.PixelCopy;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.ar.core.ImageFormat;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Date;

public class ImageUtilities {

    public static void saveMatToBitmap(Mat mat, String label){
        Bitmap resultBitmap = null;
        try {
            Mat temp = new Mat(mat.cols(), mat.rows(), CvType.CV_8UC1);
            mat.copyTo(temp);
            Imgproc.cvtColor(temp, temp, Imgproc.COLOR_BGR2RGBA);
            resultBitmap = Bitmap.createBitmap(temp.cols(),temp.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(temp, resultBitmap, true);
            saveBitmap(resultBitmap, label);
        }
        catch (CvException e){
            Log.d("Exception",e.getMessage());
        }
    }
    public static Bitmap saveBitmap(Bitmap b){
        return saveBitmap(b, "screenshot_");
    }

    public static Bitmap saveBitmap(Bitmap b, String label){
        Date date = new Date();

        // Here we are initialising the format of our image name
        CharSequence format = android.text.format.DateFormat.format("yyyy-MM-dd_HH-mm-ss", date);
        try {
            try (FileOutputStream out = new FileOutputStream(Environment.getExternalStorageDirectory() +"/ReinheritImages/" + label + format +".png")) {
                b.compress(Bitmap.CompressFormat.PNG, 100, out);
            }
        } catch (Exception e) {
        }

        return b;

    }

    // load file to storage and return a path.
    @NonNull
    private static String getPath(String file, @NonNull Context context, String tag) {
        AssetManager assetManager = context.getAssets();
        BufferedInputStream inputStream;
        try {
            // Read data from assets.
            inputStream = new BufferedInputStream(assetManager.open(file));
            byte[] data = new byte[inputStream.available()];
            inputStream.read(data);
            inputStream.close();

            // Create copy file in storage.
            File outFile = new File(context.getFilesDir(), file);
            FileOutputStream os = new FileOutputStream(outFile);
            os.write(data);
            os.close();

            // Return a path to file
            return outFile.getAbsolutePath();
        } catch (IOException ex) {
            Log.e(tag, "Failed to get file path");
        }
        return "";
    }

    protected static File takeScreenshot(View view, String filename){
        Date date = new Date();

        // Here we are initialising the format of our image name
        CharSequence format = android.text.format.DateFormat.format("yyyy-MM-dd_hh.mm.ss", date);
        try {
            // Initialising the directory of storage
            String dirpath = Environment.getExternalStorageDirectory() + "";
            File file = new File(dirpath);
            if (!file.exists()) {
                boolean mkdir = file.mkdir();
            }

            // File name
            String path = dirpath + "/ReinheritImages/" + filename + "-" + format + ".jpeg";
            view.setDrawingCacheEnabled(true);
            Bitmap bitmap = Bitmap.createBitmap(view.getDrawingCache());
            //view.setDrawingCacheEnabled(false);
            File imageurl = new File(path);
            FileOutputStream outputStream = new FileOutputStream(imageurl);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream);
            outputStream.flush();
            outputStream.close();
            return imageurl;

        } catch (FileNotFoundException io) {
            io.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Bitmap takeSc(Activity parentActivity, View view){
        final Bitmap b = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);

        Date date = new Date();

        // Here we are initialising the format of our image name
        CharSequence format = android.text.format.DateFormat.format("yyyy-MM-dd_HH-mm-ss", date);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            PixelCopy.request((SurfaceView) view, b, (int result) -> {
                if (result != PixelCopy.SUCCESS) {
                    Toast.makeText(parentActivity, "Failed to copy", Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    try (FileOutputStream out = new FileOutputStream(Environment.getExternalStorageDirectory() +"/ReinheritImages/" + "/surfaceview-"+ format +".png")) {
                        b.compress(Bitmap.CompressFormat.PNG, 100, out);
                    }
                } catch (Exception e) {
                }
            }, view.getHandler());
        }

        return b;

    }

    public static Bitmap toBitmap(Image image) {
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];
        //U and V are swapped
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.YUV_420_888, image.getWidth(), image.getHeight(), null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 75, out);

        byte[] imageBytes = out.toByteArray();
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
    }

    public static  Bitmap convertImageProxyToBitmap(Image image) {
        ByteBuffer byteBuffer = image.getPlanes()[0].getBuffer();
        byteBuffer.rewind();
        byte[] bytes = new byte[byteBuffer.capacity()];
        byteBuffer.get(bytes);
        byte[] clonedBytes = bytes.clone();
        return BitmapFactory.decodeByteArray(clonedBytes, 0, clonedBytes.length);
    }
    public static Mat convertYUVtoMat(@NonNull Image img) {
        byte[] nv21;

        ByteBuffer yBuffer = img.getPlanes()[0].getBuffer();
        ByteBuffer uBuffer = img.getPlanes()[1].getBuffer();
        ByteBuffer vBuffer = img.getPlanes()[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        nv21 = new byte[ySize + uSize + vSize];

        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        Mat yuv = new Mat(img.getHeight() + img.getHeight()/2, img.getWidth(), CvType.CV_8UC1);
        yuv.put(0, 0, nv21);
        Mat rgb = new Mat();
        Imgproc.cvtColor(yuv, rgb, Imgproc.COLOR_YUV2RGB_NV21, 3);
        Core.rotate(rgb, rgb, Core.ROTATE_90_CLOCKWISE);
        return  rgb;
    }
}
