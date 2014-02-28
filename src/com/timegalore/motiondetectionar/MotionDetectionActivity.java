package com.timegalore.motiondetectionar;

import java.io.File;
import java.text.DecimalFormat;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import rajawali.RajawaliActivity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;

public class MotionDetectionActivity extends RajawaliActivity implements
		CvCameraViewListener2, OnTouchListener, SensorEventListener {

	private OpenGLRenderer mRenderer;

	private static final boolean DEBUG = true;
	private static final String TAG = "ImageManipulationsActivity";

	public static final int VIEW_MODE_CAPTUREIMAGE = 2;
	public static final int VIEW_MODE_SHOWIMAGE = 3;
	public static final int VIEW_MODE_CAMSHIFT = 8;

	private MenuItem mItemPreviewCaptureImage;
	private MenuItem mItemPreviewSampleImage;
	private MenuItem mItemCamShift;
	private CameraBridgeViewBase mOpenCvCameraView;

	private Mat loadedImage = null;

	private Mat mRgba;

	private boolean showThumbs = true;
	private boolean showEllipse = true;

	public static int viewMode = VIEW_MODE_CAMSHIFT;

	private CAMShiftDetection csd;
	private MotionFlowDetection mfd;

	// Accelerometer
	SensorManager mSensor = null;
	private static int mSensorX;
	private static int mSensorY;
	private static int mSensorZ;
	// Accelerometer

	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS: {
				Log.i(TAG, "OpenCV loaded successfully");
				mOpenCvCameraView.enableView();

				Log.d(TAG, "loading file");

				loadedImage = loadImageFromFile("red.jpg");

				Rect initialWindow = new Rect(loadedImage.width() / 3,
						loadedImage.height() / 3, loadedImage.width() * 2 / 3,
						loadedImage.height() * 2 / 3);

				csd = new CAMShiftDetection(loadedImage, initialWindow, 10, 4,
						10, 0.01);

			}
				break;
			default: {
				super.onManagerConnected(status);
			}
				break;
			}
		}
	};

	public MotionDetectionActivity() {
		Log.i(TAG, "Instantiated new " + this.getClass());
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "called onCreate");
		super.onCreate(savedInstanceState);

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		mOpenCvCameraView = (CameraBridgeViewBase) new JavaCameraView(this, -1);
		mOpenCvCameraView.setCvCameraViewListener(this);

		mLayout.addView(mOpenCvCameraView);

		mSurfaceView.setZOrderMediaOverlay(true);
		setGLBackgroundTransparent(true);
		mRenderer = new OpenGLRenderer(this);
		mRenderer.setSurfaceView(mSurfaceView);
		super.setRenderer(mRenderer);

		mRenderer.setCameraPosition(0, 0, 20);

	}

	@Override
	public void onPause() {
		super.onPause();
		if (mOpenCvCameraView != null)
			mOpenCvCameraView.disableView();
	}

	@Override
	public void onResume() {
		super.onResume();
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this,
				mLoaderCallback);

		mOpenCvCameraView.setOnTouchListener(this);

		initialiseSensor();

	}

	public void onDestroy() {
		super.onDestroy();
		if (mOpenCvCameraView != null)
			mOpenCvCameraView.disableView();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		Log.i(TAG, "called onCreateOptionsMenu");

		mItemPreviewCaptureImage = menu.add("Capture Image");
		mItemPreviewSampleImage = menu.add("Sample Image");
		mItemCamShift = menu.add("Cam Shift");

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (DEBUG)
			Log.d(TAG, "called onOptionsItemSelected; selected item: " + item);

		if (item == mItemPreviewCaptureImage)
			viewMode = VIEW_MODE_CAPTUREIMAGE;
		else if (item == mItemPreviewSampleImage)
			viewMode = VIEW_MODE_SHOWIMAGE;
		else if (item == mItemCamShift)
			viewMode = VIEW_MODE_CAMSHIFT;
		return true;
	}

	public void onCameraViewStarted(int width, int height) {

		mRgba = new Mat();

	}

	public void onCameraViewStopped() {
		// Explicitly deallocate Mats

		if (mRgba != null)
			mRgba.release();

		mRgba = null;

	}

	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {

		DecimalFormat df = new DecimalFormat("#.##");

		mRgba = inputFrame.rgba();

		switch (MotionDetectionActivity.viewMode) {

		case MotionDetectionActivity.VIEW_MODE_CAPTUREIMAGE:

			int w = mRgba.width();
			int h = mRgba.height();

			Core.rectangle(mRgba, new Point(w * 1 / 3, h * 1 / 3), new Point(
					w * 2 / 3, h * 2 / 3), new Scalar(255, 0, 0, 255));

			break;

		case MotionDetectionActivity.VIEW_MODE_SHOWIMAGE:

			Imgproc.resize(loadedImage, mRgba, mRgba.size());

			break;

		case MotionDetectionActivity.VIEW_MODE_CAMSHIFT:

			RotatedRect rr = csd.CAMShift(mRgba);

			if (showEllipse)
				Core.ellipse(mRgba, rr, new Scalar(255, 255, 0), 5);

			if (mfd == null)
				mfd = new MotionFlowDetection(mRgba.size());

			int leftRightRot = mfd.motionFlowDetection(mRgba);

			Core.putText(mRgba, "x: " + (int) rr.center.x + " x: " + mSensorX
					+ " y: " + mSensorY + " z: " + mSensorZ + " r: "
					+ leftRightRot, new Point(0, 30),
					Core.FONT_HERSHEY_COMPLEX, 1, new Scalar(255, 0, 0, 255), 2);

			if (mRenderer.isReady())
				augmentImage(mRgba, rr, mSensorX, mSensorY, mSensorZ,
						leftRightRot);

			break;

		}

		return mRgba;
	}

	private void augmentImage(Mat mRgba, RotatedRect rr, int mSensorX,
			int mSensorY, int mSensorZ, int leftRightRot) {

		// X is to right of device when facing it
		// Y is though top of device when facing it
		// Z is coming straight out of the screen when facing it

		// draw line through the centre of the object along the z axis
		// with phone vertical the in line would be straight up and down
		// and rotation left/right would cause line angle to change

		// Front/Back rotate is simply Z
		// rotation clock/anticlockwise is slight harder
		// but doesn't involve Z
		// in landscape: X is 10 and Y is 0
		// in portrait X is 0 and Y is 10
		// in upside down landscape: X is -10 and Y is 0
		// in upside down portrait: X is 0 and Y is -10
		// so angle of rotation where normal portrait is say 0 degrees
		// is: ATAN2(Y,-X)+PI/2
		// left/right movement is Y but depends on the clock
		// rotation so need to factor this in as Y * Cos (angle)

		Point centre = rr.center;

		double lrTiltAngleInRadians = Math.atan2(mSensorY, mSensorX);

		double fbTiltAngleInRadians = Math.PI / 2 * Math.sin(mSensorZ / 10.0);

		// due to limitations on sensor information, the phone cannot
		// distinguish between, say, a landscape view from the top with
		// an inverted landscape view from the bottom - ie. the sensors
		// will show all the same readings in both case
		// the trick therefore is to use sign of the Z setting to flip
		// the object when X becomes negative.
		if ((mSensorX < 0) && (mSensorZ > 0)) {
			// fbTiltAngleInRadians += Math.PI;
			// lrTiltAngleInRadians = -lrTiltAngleInRadians;
			fbTiltAngleInRadians += Math.PI;
			mRenderer.setSpin(0);

		}

		DecimalFormat df = new DecimalFormat("#.##");

		if (DEBUG)
			Log.d(TAG,
					"x:" + mSensorX + " y:" + mSensorY + " z:" + mSensorZ
							+ " rot:" + df.format(lrTiltAngleInRadians)
							+ " fb:" + df.format(fbTiltAngleInRadians) + " lr:"
							+ df.format(leftRightRot));

		setPosition(centre.x, centre.y);

		mRenderer.setCamLRTilt(-lrTiltAngleInRadians);
		mRenderer.setCamFBTilt(-fbTiltAngleInRadians);

		double cs = rr.size.width > rr.size.height ? rr.size.width
				: rr.size.height;

		cs = Math.sqrt(rr.boundingRect().area());

		mRenderer.setCubeSize(2 * cs / 480); // 0.6 for pegasus

	}

	public void printMatDetails(String name, Mat m) {

		Log.d(TAG,
				name + " - " + "c:" + m.channels() + ",cols:" + m.cols()
						+ ",dep:" + m.depth() + ",rows:" + m.rows() + ",type:"
						+ m.type() + ",w:" + m.width() + ",h:" + m.height());

	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		if (DEBUG)
			Log.d(TAG, "got touch " + event.getAction());

		float x = event.getX();
		float y = event.getY();

		if (DEBUG)
			Log.d(TAG, "x=" + x + ",y=" + y);

		// setPosition(x, y);

		if (DEBUG)
			Log.d(TAG, "object pos: "
					+ mRenderer.get3DObjectPosition().toString());

		if (viewMode == VIEW_MODE_CAPTUREIMAGE) {

			int w = mRgba.width();
			int h = mRgba.height();

			// +1 to x,y to avoid cutting the red line of the viewfinder box
			Rect roi = new Rect(new Point(w * 1 / 3 + 1, h * 1 / 3 + 1),
					new Point(w * 2 / 3, h * 2 / 3));

			Mat viewFinder = mRgba.submat(roi);

			Imgproc.resize(viewFinder, loadedImage, loadedImage.size());

			Rect initialWindow = new Rect(loadedImage.width() / 3,
					loadedImage.height() / 3, loadedImage.width() * 2 / 3,
					loadedImage.height() * 2 / 3);

			csd = new CAMShiftDetection(loadedImage, initialWindow, 10, 4, 10,
					0.01);

		}

		if (viewMode == VIEW_MODE_CAMSHIFT) {
			showEllipse = !showEllipse;
		}

		return false;
	}

	private void setPosition(double x, double y) {
		double cD = mRenderer.getCurrentCamera().getZ();

		float yVP = mRenderer.getViewportHeight();
		float xVP = mRenderer.getViewportWidth();

		// =(K16-xVP/2)* (cD/xVP)
		// =(K17-yVP/2)* (cD/2/yVP)

		double sx = 0.7;
		double sy = 1.3;

		double obx = (x - xVP / 2) * (cD / sx / xVP);
		double oby = (yVP / 2 - y) * (cD / sy / yVP);

		mRenderer.set3DObjectPosition(obx, oby, 0);

	}

	public Mat loadImageFromFile(String fileName) {

		Mat rgbLoadedImage = null;

		File root = Environment.getExternalStorageDirectory();
		File file = new File(root, fileName);

		// this should be in BGR format according to the
		// documentation.
		Mat image = Highgui.imread(file.getAbsolutePath());

		if (image.width() > 0) {

			rgbLoadedImage = new Mat(image.size(), image.type());

			Imgproc.cvtColor(image, rgbLoadedImage, Imgproc.COLOR_BGR2RGB);

			if (DEBUG)
				Log.d(TAG, "loadedImage: " + "chans: " + image.channels()
						+ ", (" + image.width() + ", " + image.height() + ")");

			image.release();
			image = null;
		}

		return rgbLoadedImage;

	}

	public void writeImageToFile(Mat image, String filename) {

		File root = Environment.getExternalStorageDirectory();
		File file = new File(root, filename);

		Highgui.imwrite(file.getAbsolutePath(), image);

		if (DEBUG)
			Log.d(TAG,
					"writing: " + file.getAbsolutePath() + " (" + image.width()
							+ ", " + image.height() + ")");
	}

	private void initialiseSensor() {
		if (mSensor == null)
			mSensor = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

		mSensor.registerListener(this,
				mSensor.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				SensorManager.SENSOR_DELAY_GAME);

	}

	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
	}

	@Override
	public void onSensorChanged(SensorEvent event) {

		float vals[] = event.values;

		mSensorX = (int) vals[0];
		mSensorY = (int) vals[1];
		mSensorZ = (int) vals[2];

	}
}
