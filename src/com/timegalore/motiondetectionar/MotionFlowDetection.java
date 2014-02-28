package com.timegalore.motiondetectionar;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.Video;

public class MotionFlowDetection {

	private Mat mGray1 = null;
	private Mat mGray2 = null;
	MatOfPoint initial = null;
	MatOfByte status = null;
	MatOfFloat err = null;
	MatOfPoint2f prevPts = null;
	MatOfPoint2f nextPts = null;
	int maxCorners;
	Size imageSize;

	public MotionFlowDetection(Size s) {
		imageSize = s;
	}

	public Point motionFlowDetection(Mat prevImage, Mat nextImage) {

		Point direction = null;

		setOpticalFlowParameters(prevImage);

		Mat resultImage = prevImage.clone();

		Imgproc.cvtColor(prevImage, mGray1, Imgproc.COLOR_RGBA2GRAY);
		Imgproc.cvtColor(nextImage, mGray2, Imgproc.COLOR_RGBA2GRAY);

		Imgproc.goodFeaturesToTrack(mGray1, initial, 1000, 0.01, 5);

		initial.convertTo(prevPts, CvType.CV_32FC2);

		Video.calcOpticalFlowPyrLK(mGray1, mGray2, prevPts, nextPts, status,
				err);

		Point[] pointp = prevPts.toArray();
		Point[] pointn = nextPts.toArray();

		markPointsOnImage(resultImage, pointp, pointn);

		direction = getAverageDirection(pointp, pointn);

		return direction;

	}

	private Point getAverageDirection(Point[] pointp, Point[] pointn) {

		Point p = new Point();

		int nosOfPoints = pointp.length;

		for (int i = 0; i < nosOfPoints; i++) {
			p.x += pointp[i].x - pointn[i].x;
			p.y += pointp[i].y - pointn[i].y;
		}

		p.x = p.x / nosOfPoints;
		p.y = p.y / nosOfPoints;

		return p;

	}

	private void markPointsOnImage(Mat resultImage, Point[] pointp,
			Point[] pointn) {

		for (int i = 0; i < pointp.length; i++) {

			int distanceX = (int) Math.abs(pointn[i].x - pointp[i].x);
			int distanceY = (int) Math.abs(pointn[i].y - pointp[i].y);

			Core.circle(resultImage, pointn[i], 10, new Scalar(255, 0, 0, 255));
		}

	}

	public int motionFlowDetection(Mat image) {

		setOpticalFlowParameters(image);

		mGray1 = mGray2.clone();

		Imgproc.cvtColor(image, mGray2, Imgproc.COLOR_RGBA2GRAY);

		Imgproc.goodFeaturesToTrack(mGray1, initial, maxCorners, 0.01, 5);

		initial.convertTo(prevPts, CvType.CV_32FC2);

		Video.calcOpticalFlowPyrLK(mGray1, mGray2, prevPts, nextPts, status,
				err);

		Point[] pointp = prevPts.toArray();
		Point[] pointn = nextPts.toArray();

		int dir = calculateVelocityFromMotionFlow(pointn, pointp);

		return removeOutliersAndNoise(dir);
	}

	private int removeOutliersAndNoise(int v) {

		if ((v < 0) && (v > -10))
			v = 0;

		if ((v > 0) && (v < 10))
			v = 0;

		if ((v < 0) && (v < -80))
			v = -80;
		if ((v > 0) && (v > 80))
			v = 80;

		return v;
	}

	private int calculateVelocityFromMotionFlow(Point[] pointn, Point[] pointp) {

		// find the average difference from all the analysed points. The sign of
		// the
		// average gives you the direction

		int points = pointn.length;

		int total = 0;

		for (int i = 0; i < points; i++) {

			total += pointn[i].x - pointp[i].x;

		}

		return (int) total / points;
	}

	private void setOpticalFlowParameters(Mat image) {

		if (mGray1 == null)
			mGray1 = new Mat(imageSize, CvType.CV_8UC1);
		if (mGray2 == null) {
			mGray2 = new Mat(imageSize, CvType.CV_8UC1);
			Imgproc.cvtColor(image, mGray2, Imgproc.COLOR_RGBA2GRAY);

		}

		initial = new MatOfPoint();
		status = new MatOfByte();
		err = new MatOfFloat();
		prevPts = new MatOfPoint2f();
		nextPts = new MatOfPoint2f();

		maxCorners = 10;

	}

}
