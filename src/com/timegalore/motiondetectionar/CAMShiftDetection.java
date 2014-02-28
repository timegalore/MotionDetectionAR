package com.timegalore.motiondetectionar;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.core.TermCriteria;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.Video;

import android.util.Log;

public class CAMShiftDetection {

	private static final boolean DEBUG = true;
	private static final String TAG = "CamShiftDetection";

	private int g_erosion_level = 10;
	private int g_erosion_kernel_size = 4;
	private int g_termcrit_count = 10;
	private double g_termcrit_eps = 0.01;
	private Mat g_hist = new Mat();
	private Rect g_initialWindow = new Rect();
	private Rect g_firstWindow;

	public CAMShiftDetection(Mat targetImage, Rect initialWindow,
			int erosion_level, int erosion_kernel_size, int termcrit_count,
			double termcrit_eps) {
		g_erosion_level = erosion_level;
		g_erosion_kernel_size = erosion_kernel_size;
		g_termcrit_count = termcrit_count;
		g_termcrit_eps = termcrit_eps;
		g_initialWindow = initialWindow;
		g_firstWindow = new Rect(initialWindow.tl(), initialWindow.br());

		g_hist = getImageHistogram(getHue(targetImage), targetImage.size(), 10,
				0, 180);

	}

	public RotatedRect CAMShift(Mat in) {

		Mat backProjection = getBackProjection(getHue(in), g_hist, 0, 180, 1.0);

		Mat clarifiedBackProjection = clarifyDetectedAreas(backProjection,
				g_erosion_kernel_size, g_erosion_level);

		validateWindow();

		RotatedRect rr = doCamShift(clarifiedBackProjection, g_initialWindow,
				g_termcrit_count, g_termcrit_eps);

		g_initialWindow = rr.boundingRect();

		return rr;

	}

	private void validateWindow() {
		if ((g_initialWindow.width < 0) || (g_initialWindow.height < 0)
				|| (g_initialWindow.width * g_initialWindow.height < 10)) {
			if (DEBUG)
				Log.d(TAG, "detection window too small - resetting");

			if (DEBUG)
				Log.d(TAG, "g first wndow " + g_firstWindow.toString());
			g_initialWindow = new Rect(g_firstWindow.tl(), g_firstWindow.br());
		}
	}

	private Mat getHue(Mat in) {
		Mat out = new Mat(in.size(), CvType.CV_8UC1);
		Mat hueImage = new Mat(in.size(), in.type());

		Imgproc.cvtColor(in, hueImage, Imgproc.COLOR_RGB2HSV);
		Core.extractChannel(hueImage, out, 0);

		return out;
	}

	private Mat getImageHistogram(Mat huesImage, Size size, int buckets,
			float minRange, float maxRange) {

		Mat hist = new Mat();

		MatOfFloat ranges = new MatOfFloat(minRange, maxRange);

		List<Mat> planes = new ArrayList<Mat>();
		planes.add(huesImage);

		MatOfInt chans = new MatOfInt(0);
		MatOfInt histSize = new MatOfInt(buckets);

		Imgproc.calcHist(planes, chans, new Mat(), hist, histSize, ranges);

		return hist;

	}

	private Mat clarifyDetectedAreas(Mat in, int erosion_kernel_size,
			int erosion_level) {
		Mat out = new Mat(in.size(), in.type());

		Mat eroded_kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT,
				new Size(erosion_kernel_size, erosion_kernel_size), new Point(
						erosion_kernel_size / 2, erosion_kernel_size / 2));

		Imgproc.erode(in, out, eroded_kernel, new Point(-1, -1), erosion_level,
				Imgproc.BORDER_DEFAULT, new Scalar(0));

		return out;
	}

	private RotatedRect doCamShift(Mat in, Rect initialWindow,
			int termcrit_count, double termcrit_eps) {

		TermCriteria termcrit = new TermCriteria(TermCriteria.MAX_ITER
				| TermCriteria.EPS, termcrit_count, termcrit_eps);

		RotatedRect rr = Video.CamShift(in, initialWindow, termcrit);

		return rr;

	}

	private Mat getBackProjection(Mat in, Mat histogram, int minRange,
			int maxRange, double scale) {
		ArrayList<Mat> images = new ArrayList<Mat>();
		images.add(in);

		Mat backproject = new Mat(in.size(), CvType.CV_8UC1);

		Imgproc.calcBackProject(images, new MatOfInt(0), histogram,
				backproject, new MatOfFloat(minRange, maxRange), scale);

		return backproject;
	}

}
