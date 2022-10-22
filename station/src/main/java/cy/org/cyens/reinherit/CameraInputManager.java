package cy.org.cyens.reinherit;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.util.Size;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.camera.camera2.interop.Camera2CameraInfo;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.ar.core.ImageFormat;
import com.google.common.util.concurrent.ListenableFuture;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

public class CameraInputManager {
    //region Variables *****************************************************************************
    // Camera / Image data
    Activity mParentActivity;
    ImageCapture mImageCapture;

    // Analysis Data
    FrameAnalyzer mPageAnalyzer = new FrameAnalyzer();
    boolean mResetBaseFrame = true;

    // Metric Data
    public class MetricsData {
        public int mMaxCounterValue = 5;

        public double mMinMetricValue = 5, mMaxMetricValue = 40;
        public double mBaseMetricWeight = 0.5, mFlowMetricWeight = 0.5;
        public double mBaseMetric, mFlowMetric, mGrandMetric;

        public double computeGrandMetric() {
            mGrandMetric = mMetricsData.mBaseMetricWeight * mMetricsData.mBaseMetric + mMetricsData.mFlowMetricWeight * mMetricsData.mBaseMetric;
            return mGrandMetric;
        }

        public int getCounterMetric(){
            if (mGrandMetric < mMinMetricValue)
                return 0;
            else if (mGrandMetric >= mMaxMetricValue)
                return mMaxCounterValue;
            else{
                return (int) floor(mGrandMetric);
            }
        }

        private double floor(double sum){

            return ((sum - mMinMetricValue)/(mMaxMetricValue - mMinMetricValue)) * (mMaxCounterValue-1) + 1;
        }
    }
    private MetricsData mMetricsData;

    // Person Counter Data
    private int mMaxPersonHistory = 0;
    private int lastPersonCounter = 0;
    private int[] personCounterHistory;
    private int nextPersonCounterHistoryIndex = 0;
    private int personCounterHistorySum = 0;
    private int personCounterHistoryCount = 0;

    //endregion Variables **************************************************************************

    //region Camera Manager ************************************************************************
    public void initializeCamera(Activity parentActivity){
        mParentActivity = parentActivity;

        PreviewView previewView = parentActivity.findViewById(R.id.viewFinder);
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(parentActivity);
        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    startCamera(cameraProvider, previewView);
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, ContextCompat.getMainExecutor(parentActivity));
    }

    @SuppressLint("UnsafeOptInUsageError")
    private void startCamera(@NonNull ProcessCameraProvider cameraProvider, PreviewView previewView) {
        cameraProvider.unbindAll();

        // Select camera
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        // Bind selector to camera and retrieve camera resolution
        // Get camera information
        Camera inputCamera = cameraProvider.bindToLifecycle((LifecycleOwner) mParentActivity, cameraSelector);
        Camera2CameraInfo cameraInfo = Camera2CameraInfo.from(inputCamera.getCameraInfo());
        String cameraId = cameraInfo.getCameraId();
        Size targetSize = new Size(1280, 720);
        try {
            CameraManager cameraManager = (CameraManager) mParentActivity.getSystemService(Context.CAMERA_SERVICE);
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap streamConfigurationMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            // Get available output sizes
            Size[] outputSizes = streamConfigurationMap.getOutputSizes(ImageFormat.YUV_420_888);
            targetSize = outputSizes[0];
        } catch (CameraAccessException e) {
            e.printStackTrace();
            targetSize = new Size(1280, 720);
        }

        // Build the previewer
        Preview preview = new Preview.Builder()
                .build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        // Build the image capture
        mImageCapture = new ImageCapture.Builder()
                .setTargetResolution(targetSize)
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build();

        mMetricsData = new MetricsData();
        // Build the image analysis
        ImageAnalysis imageAnalyzer = new ImageAnalysis.Builder()
                .setTargetResolution(targetSize)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                .build();
        imageAnalyzer.setAnalyzer(Executors.newSingleThreadExecutor(), mPageAnalyzer);
        if (mMaxPersonHistory == 0)
            resetPersonCounterData(10);

        // Bind camera to activity
        cameraProvider.unbindAll();
        Camera boundCamera = cameraProvider.bindToLifecycle((LifecycleOwner) mParentActivity, cameraSelector, preview, mImageCapture, imageAnalyzer);

        // Disable auto-focus
        boundCamera.getCameraControl().cancelFocusAndMetering();
        resetBaseFrame();
    }

    //endregion Camera Manager *********************************************************************

    //region Screen capture ************************************************************************
    public void CaptureScreenshot(String fileName){

        File file = new File(mParentActivity.getExternalFilesDir(null),fileName);

        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(file).build();

        mImageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(mParentActivity), imListener);
    }


    private ImageCapture.OnImageSavedCallback imListener = new ImageCapture.OnImageSavedCallback() {
        @Override
        public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
            Toast.makeText(mParentActivity.getBaseContext(), "Camera capture saved", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onError(@NonNull ImageCaptureException exception) {
            Toast.makeText(mParentActivity.getBaseContext(), "Camera capture failed", Toast.LENGTH_SHORT).show();
        }
    };
    //endregion Screen capture *********************************************************************

    //region Analyze frames ************************************************************************
    public void resetPersonCounterData(){
        resetPersonCounterData(mMaxPersonHistory);
    }

    public void resetPersonCounterData(int maxPersonHistory){
        mMaxPersonHistory = maxPersonHistory;
        // Reset person counter array
        if (personCounterHistory == null || personCounterHistory.length != maxPersonHistory)
            personCounterHistory = new int[maxPersonHistory];

        for (int i = 0; i < maxPersonHistory; i++)
            personCounterHistory[i] = 0;

        // Reset counters
        personCounterHistoryCount = 0;
        personCounterHistorySum = 0;
        nextPersonCounterHistoryIndex = 0;
    }

    public void resetBaseFrame(){
        mResetBaseFrame = true;
    }

    /** Listener containing callbacks for image file I/O events. */
    public interface onPersonCounterChangeCallback {
        /** Called when an image has been successfully saved. */
        void OnPersonCounterChange(@NonNull int counter);
    }
    private onPersonCounterChangeCallback mPersonCounterCallbackListener;
    public void setPersonCounterChangeCallback(onPersonCounterChangeCallback listener)
    {
        mPersonCounterCallbackListener = listener;
    }

    public interface OnMetricsUpdateCallback {
        /** Called when an image has been successfully saved. */
        void onMetricsUpdated();
    }
    private OnMetricsUpdateCallback mMetricsUpdateCallbackListener;
    public void setMetricsUpdateCallback(OnMetricsUpdateCallback listener)
    {
        mMetricsUpdateCallbackListener = listener;
    }

     public MetricsData getMetricsData(){
        return mMetricsData;
    }

    //region Frame Analyzer ************************************************************************
    private class FrameAnalyzer implements ImageAnalysis.Analyzer {
        private Mat mBaseFrame;
        private Mat mLastFrame;

        private Mat imageToGrayscaleMat(ImageProxy image) {
            ImageProxy.PlaneProxy plane = image.getPlanes()[0];
            int height = image.getHeight();
            int width = image.getWidth();
            // Get Y channel - gray
            ByteBuffer yBuffer = plane.getBuffer();
            return new Mat(height, width, CvType.CV_8UC1, yBuffer);
        }

        @Override
        public void analyze(ImageProxy image) {
            // Check if a valid image has been retrieved
            if (image.getFormat() != ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888 || image.getPlanes()[0].getPixelStride() != 1) {
                image.close();
                return;
            }

            // Convert image to grayscale
            Mat matImage = imageToGrayscaleMat(image);
            if (mResetBaseFrame){
                mBaseFrame = matImage.clone();
                mLastFrame = mBaseFrame;
                mResetBaseFrame = false;
            }

            // If base frame has not been set yet, return
            if (mBaseFrame == null) {
                image.close();
                return;
            }

            // Process frame data and extract metrics
            ProcessFrameData(matImage);

            // archive current frame and close image
            mLastFrame = matImage.clone();
            image.close();
        }

        private void ProcessFrameData(Mat currentFrame){
            // Get metrics from frames
            Mat diffImage = new Mat();
            // Get absolute frame difference compared to base frame
            Core.absdiff(currentFrame, mBaseFrame, diffImage);
            mMetricsData.mBaseMetric = Core.mean(diffImage).val[0];

            // Get absolute frame difference compared to last frame
            Core.absdiff(currentFrame, mLastFrame, diffImage);
            mMetricsData.mFlowMetric = Core.mean(diffImage).val[0];

            mMetricsData.computeGrandMetric();

            int currentPersonCounter = mMetricsData.getCounterMetric();

            if (personCounterHistoryCount == mMaxPersonHistory)
                personCounterHistorySum -= personCounterHistory[nextPersonCounterHistoryIndex];
            else
                personCounterHistoryCount++;

            personCounterHistory[nextPersonCounterHistoryIndex] = currentPersonCounter;
            personCounterHistorySum += currentPersonCounter;
            nextPersonCounterHistoryIndex = (nextPersonCounterHistoryIndex + 1) % mMaxPersonHistory;

            int averageCurrentPersonCounter = (int)((float) personCounterHistorySum / personCounterHistoryCount);
            if(lastPersonCounter != averageCurrentPersonCounter) {
                mPersonCounterCallbackListener.OnPersonCounterChange(averageCurrentPersonCounter);
                lastPersonCounter = averageCurrentPersonCounter;
            }
            mMetricsUpdateCallbackListener.onMetricsUpdated();
        }
    }
    //endregion Frame Analyzer *********************************************************************
    //endregion Analyze frames *********************************************************************
}