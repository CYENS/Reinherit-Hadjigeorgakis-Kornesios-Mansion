package cy.org.cyens.reinherit;

import static org.opencv.calib3d.Calib3d.Rodrigues;
import static org.opencv.core.Core.subtract;
import static org.opencv.imgproc.Imgproc.cornerSubPix;

import static cy.org.cyens.reinherit.ImageManager.bitmapToMat;

import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point;
import org.opencv.core.Point3;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.core.TermCriteria;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class Calibrator {
    private static final String TAG = Calibrator.class.getName();
    private static final Size PATTERN_SIZE = new Size(7, 9); //interior number of corners
    private static final Point3 ORIGIN = new Point3();
    private static final String CAMERA_POSE_FILENAME = "camera_pose.png";
    // At least 10 test patterns needed for camera calibration
    // https://docs.opencv.org/4.x/dc/dbb/tutorial_py_calibration.html
    private static final int REQUIRED_TEST_PATTERNS = 10;

    private static final int CORNERS_SIZE = ((int) (PATTERN_SIZE.width * PATTERN_SIZE.height));
    private static final int CALIB_FLAGS = Calib3d.CALIB_FIX_PRINCIPAL_POINT +
            Calib3d.CALIB_ZERO_TANGENT_DIST + Calib3d.CALIB_FIX_ASPECT_RATIO +
            Calib3d.CALIB_FIX_K4 + Calib3d.CALIB_FIX_K5;
    private static final TermCriteria TERM_CRITERIA = new TermCriteria(TermCriteria.EPS +
            TermCriteria.MAX_ITER, 30, 0.1);

    private final Point3 xAxis;
    private final Point3 yAxis;
    private final Point3 zAxis;

    private final Mat mRVecPnP = new Mat();
    private final Mat mTVecPnP = new Mat();
    private final Mat mCameraMatrix = new Mat();
    private final MatOfPoint2f mCorners2D = new MatOfPoint2f();
    private final MatOfPoint3f mCorners3D = new MatOfPoint3f();
    private final MatOfDouble mDistortionCoefficients = new MatOfDouble();
    private final List<Mat> mCornersBuffer = new ArrayList<>();
    private final Size mImageSize;

    private final double mSquareSize;
    private boolean mCameraPoseFound = false;
    private boolean mIsCalibrated = false;
    private final ImageManager mImageManagerCalib;
    private final ImageManager mImageManagerCamPose;
    private Point3 mRefPoint = ORIGIN.clone();
    private Point mRefPointProjected = null;

    private ArrayList<MatOfPoint2f> axisProjected = null;

    public Calibrator(int imageSize, double squareSize, ImageManager imageManagerCalib, ImageManager imageManagerCamPose) {
        mSquareSize = squareSize;
        mImageSize = new Size(imageSize, imageSize);

        // Initialize mCornerCoordsPnP: coordinates for each corner
        List<Point3> cornerCoordsList = new ArrayList<>();
        for (int i = 0; i < PATTERN_SIZE.height; i++) {
            for (int j = 0; j < PATTERN_SIZE.width; j++) {
                float x = (float) (i * mSquareSize);
                float y = (float) (j * mSquareSize);
                float z = 0;
                Point3 cornerCoord = new Point3(x, y, z);
                cornerCoordsList.add(cornerCoord);
            }
        }
        mCorners3D.fromList(cornerCoordsList);

        mImageManagerCalib = imageManagerCalib;

        // Load saved images for calibration
        ArrayList<Bitmap> images = mImageManagerCalib.getImagesFromDir();
        if (images != null) {
            for (int i = 0; i < images.size() && i < REQUIRED_TEST_PATTERNS; i++) {
                findChessboardCorners(bitmapToMat(images.get(i)), true);
            }
        }

        if (hasRequiredTestPatters()) {
            calibrateCamera();
        }

        xAxis = new Point3(mSquareSize, 0.0, 0.0);
        yAxis = new Point3(0.0, mSquareSize, 0.0);
        zAxis = new Point3(0.0, 0.0, mSquareSize);

        mImageManagerCamPose = imageManagerCamPose;
        Bitmap img = mImageManagerCamPose.load(CAMERA_POSE_FILENAME);
        if (img != null && findChessboardCorners(bitmapToMat(img))) {
            solvePnP();
        }
    }

    public void resetCalibration() {
        mCornersBuffer.clear();
        mCameraPoseFound = false;
        mIsCalibrated = false;
        mImageManagerCalib.clearDir();
    }

    public int getBufferSize() {
        return mCornersBuffer.size();
    }

    public int getRequiredTestPatterns() {
        return REQUIRED_TEST_PATTERNS;
    }

    public boolean isCalibrated() {
        return mIsCalibrated;
    }

    // Estimates camera pose (camera rotation & translation matrices)
    public boolean solvePnP() {
        // https://docs.opencv.org/4.x/d5/d1f/calib3d_solvePnP.html
        if (!mIsCalibrated) {
            return false;
        }
        mCameraPoseFound = Calib3d.solvePnP(mCorners3D, mCorners2D, mCameraMatrix,
                mDistortionCoefficients, mRVecPnP, mTVecPnP, false,
                Calib3d.CV_ITERATIVE);
        Log.i(TAG, "Rotation matrix: " + mRVecPnP.dump());
        Log.i(TAG, "Translation matrix: " + mTVecPnP.dump());

        // Reset reference point at origin
        mRefPoint = ORIGIN.clone();
        projectRefPoint();
        axisProjected = null;

        return mCameraPoseFound;
    }

    public boolean hasRequiredTestPatters() {
        return mCornersBuffer.size() >= REQUIRED_TEST_PATTERNS;
    }

    public boolean cameraPoseFound() {
        return mCameraPoseFound;
    }

    public boolean findChessboardCorners(Mat image, boolean addToCalibBuffer) {
        // CALIB_CB_FAST_CHECK saves a lot of time on images that do not contain any chessboard corners
        boolean result = Calib3d.findChessboardCorners(image, PATTERN_SIZE, mCorners2D,
                Calib3d.CALIB_CB_ADAPTIVE_THRESH + Calib3d.CALIB_CB_NORMALIZE_IMAGE
                        + Calib3d.CALIB_CB_FAST_CHECK);
        if (result) {
            cornerSubPix(image, mCorners2D, new Size(11, 11),
                    new Size(-1, -1), TERM_CRITERIA);

            if (addToCalibBuffer) {
                mCornersBuffer.add(mCorners2D.clone());
                mImageManagerCalib.save(image, String.valueOf(mCornersBuffer.size()).concat(".png"));
            } else {
                mImageManagerCamPose.save(image, CAMERA_POSE_FILENAME);
            }

            Calib3d.drawChessboardCorners(image, PATTERN_SIZE, mCorners2D, true);
        }
        return result;
    }

    public boolean findChessboardCorners(Mat image) {
        return findChessboardCorners(image, false);
    }

    // Estimates camera matrix and distortion coeff. considering all the images of chessboard in the buffer
    public void calibrateCamera() {
        if (mCornersBuffer.size() == 0) {
            return;
        }

        ArrayList<Mat> rVecs = new ArrayList<>();
        ArrayList<Mat> tVecs = new ArrayList<>();
        ArrayList<Mat> cornerCoords = new ArrayList<>();

        Mat.eye(3, 3, CvType.CV_64FC1).copyTo(mCameraMatrix); // returns an identity matrix of the specified size and type.
        //mCameraMatrix.put(0, 0, 1.0);
        Mat.zeros(5, 1, CvType.CV_64FC1).copyTo(mDistortionCoefficients);

        //Mat reprojectionErrors = new Mat();
        cornerCoords.add(Mat.zeros(CORNERS_SIZE, 1, CvType.CV_32F));
        calcBoardCornerPositions(cornerCoords.get(0));

        // For each chessboard corners entry in the buffer, add the 3d world coordinates in the list cornerCoords
        // Assume for all the images the coordinates are the same
        for (int i = 1; i < mCornersBuffer.size(); i++) {
            cornerCoords.add(cornerCoords.get(0));
        }

        Calib3d.calibrateCamera(cornerCoords, mCornersBuffer, mImageSize,
                mCameraMatrix, mDistortionCoefficients, rVecs, tVecs, CALIB_FLAGS);

        mIsCalibrated = true;
        //mRms = computeReprojectionErrors(objectPoints, rvecs, tvecs, reprojectionErrors);
        //Log.i(TAG, String.format("Average re-projection error: %f", mRms));
        Log.i(TAG, "Camera matrix: " + mCameraMatrix.dump());
        Log.i(TAG, "Distortion coefficients: " + mDistortionCoefficients.dump());
    }

    public void drawAxis(Mat image) {
        if (!mCameraPoseFound) {
            return;
        }

        if (axisProjected == null) {
            projectAxis();
        }

        Imgproc.line(image, axisProjected.get(0).toList().get(0), axisProjected.get(0).toList().get(1), new Scalar(255, 0, 0), 4);
        Imgproc.line(image, axisProjected.get(1).toList().get(0), axisProjected.get(1).toList().get(1), new Scalar(0, 255, 0), 4);
        Imgproc.line(image, axisProjected.get(2).toList().get(0), axisProjected.get(2).toList().get(1), new Scalar(0, 0, 255), 4);
    }

    public void drawRefPoint(Mat image) {
        if (mRefPointProjected == null) {
            projectRefPoint();
        }
        Imgproc.drawMarker(image, mRefPointProjected, new Scalar(255, 0, 0), 1, 5, 4, 0);
    }

    public void resetCameraPose() {
        mCameraPoseFound = false;
        mImageManagerCamPose.clearDir();
    }

    public void setReferencePoint(Point p) {
        this.mRefPointProjected = p.clone();
        this.mRefPoint = transform2DTo3D(p);
    }

    private void projectRefPoint() {
        if (!mCameraPoseFound) {
            return;
        }

        MatOfPoint2f tmp = new MatOfPoint2f();
        Calib3d.projectPoints(new MatOfPoint3f(mRefPoint), mRVecPnP, mTVecPnP, mCameraMatrix, mDistortionCoefficients, tmp);
        mRefPointProjected = tmp.toList().get(0).clone();
    }

    private void projectAxis() {
        axisProjected = new ArrayList<>(3);
        axisProjected.add(new MatOfPoint2f());
        axisProjected.add(new MatOfPoint2f());
        axisProjected.add(new MatOfPoint2f());

        Calib3d.projectPoints(new MatOfPoint3f(ORIGIN, xAxis), mRVecPnP, mTVecPnP, mCameraMatrix, mDistortionCoefficients, axisProjected.get(0));
        Calib3d.projectPoints(new MatOfPoint3f(ORIGIN, yAxis), mRVecPnP, mTVecPnP, mCameraMatrix, mDistortionCoefficients, axisProjected.get(1));
        Calib3d.projectPoints(new MatOfPoint3f(ORIGIN, zAxis), mRVecPnP, mTVecPnP, mCameraMatrix, mDistortionCoefficients, axisProjected.get(2));
    }

    // Calculates euclidean distance from reference to the given point
    public double calculateDistance(Point3 p) {
        return Math.abs(Math.sqrt(Math.pow(mRefPoint.x - p.x, 2) + Math.pow(mRefPoint.y - p.y, 2) +
                Math.pow(mRefPoint.z - p.z, 2)));
    }

    public Point3 getRefPoint() {
        return mRefPoint;
    }

    public Point getRefPointProjected() {
        return mRefPointProjected;
    }

    // Draws a line from reference to given point
    public void drawLineFromRefPoint(Mat image, Point p) {
        if (!mCameraPoseFound) {
            return;
        }

        Imgproc.line(image, mRefPointProjected, new Point(p.x, p.y), new Scalar(0, 255, 0), 2);
    }

    public Point transform3dTo2D() { //TODO
        return new Point();
    }

    public Point3 transform2DTo3D(Point p) {
        // based on
        // https://stackoverflow.com/questions/12299870/computing-x-y-coordinate-3d-from-image-point
        double Z = 0; // assume point on the ground level

        Mat rMat = new Mat();
        Rodrigues(mRVecPnP, rMat); // convert rotation vector to rotation matrix

        Mat pMat = new Mat(3, 1, CvType.CV_64F);
        pMat.put(0, 0, p.x, p.y, 1);

        Mat leftSideMat = new Mat();
        Core.gemm(rMat.inv(), mCameraMatrix.inv(), 1.0, new Mat(), 0.0, leftSideMat); // matrix multiplication
        Core.gemm(leftSideMat, pMat, 1.0, new Mat(), 0.0, leftSideMat);

        Mat rightSideMat = new Mat();
        Core.gemm(rMat.inv(), mTVecPnP, 1.0, new Mat(), 0.0, rightSideMat);

        double s = (Z + rightSideMat.get(2, 0)[0] / leftSideMat.get(2, 0)[0]);

        Mat tmp = new Mat();
        Core.gemm(mCameraMatrix.inv(), pMat, s, new Mat(), 0.0, tmp);
        subtract(tmp, mTVecPnP, tmp);
        Core.gemm(rMat.inv(), tmp, 1.0, new Mat(), 0.0, tmp);

        return new Point3(tmp.get(0, 0)[0], tmp.get(1, 0)[0], 0);
    }

    // Calculates world coordinates of chessboard corners
    // Assumes first corner at xyz (0,0,0)
    private void calcBoardCornerPositions(Mat corners) {
        // Multiply total number of corners with 3 (axis)
        float[] positions = new float[CORNERS_SIZE * 3];

        for (int i = 0; i < PATTERN_SIZE.height; i++) {
            for (int j = 0; j < PATTERN_SIZE.width; j++) {
                positions[(int) ((i * PATTERN_SIZE.width + j) * 3 + 0)] = j * (float) mSquareSize; // x axis
                positions[(int) ((i * PATTERN_SIZE.width + j) * 3 + 1)] = i * (float) mSquareSize; // y axis
                positions[(int) ((i * PATTERN_SIZE.width + j) * 3 + 2)] = 0; // z axis
            }
        }
        corners.create(CORNERS_SIZE, 1, CvType.CV_32FC3);
        corners.put(0, 0, positions);
    }
}
