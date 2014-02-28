package com.timegalore.motiondetectionar;

import javax.microedition.khronos.opengles.GL10;

import rajawali.Object3D;
import rajawali.lights.DirectionalLight;
import rajawali.math.vector.Vector3;
import rajawali.parser.LoaderOBJ;
import rajawali.parser.ParsingException;
import rajawali.renderer.RajawaliRenderer;
import android.content.Context;
import android.util.Log;

public class OpenGLRenderer extends RajawaliRenderer {

	private static final boolean DEBUG = true;
	private static final String TAG = "OpenGLRenderer";

	private DirectionalLight mLight;

	private Object3D m3DObject;

	private static final double rtod = 180 / Math.PI;

	public OpenGLRenderer(Context context) {
		super(context);
		setFrameRate(60);
	}

	public void initScene() {
		mLight = new DirectionalLight(1f, 0.2f, -1.0f); // set the direction
		mLight.setColor(1.0f, 1.0f, 1.0f);
		mLight.setPower(2);

		LoaderOBJ objParser = new LoaderOBJ(mContext.getResources(),
				mTextureManager, R.raw.block10_obj);

		try {
			objParser.parse();
		} catch (ParsingException e) {

			e.printStackTrace();

		}

		m3DObject = objParser.getParsedObject();
		m3DObject.setPosition(0, 0, 0);

		addChild(m3DObject);

	}

	@Override
	public void onDrawFrame(GL10 glUnused) {
		super.onDrawFrame(glUnused);

	}

	public void set3DObjectPosition(double x, double y, double z) {

		if (m3DObject != null)
			m3DObject.setPosition(x, y, z);
	}

	public Vector3 get3DObjectPosition() {

		return m3DObject.getPosition();
	}

	public void setCameraPosition(double x, double y, double z) {

		getCurrentCamera().setX(x);
		getCurrentCamera().setY(y);
		getCurrentCamera().setZ(z);

	}

	public void setCamLRTilt(double lrTiltAngleInRadians) {
		getCurrentCamera().setRotZ(-lrTiltAngleInRadians * rtod);

	}

	public void setCamFBTilt(double fbTiltAngleInRadians) {
		getCurrentCamera().setRotX(-fbTiltAngleInRadians * rtod);

	}

	public void setCubeSize(double d) {
		m3DObject.setScale(d);
	}

	public Vector3 getCubeSize() {
		return m3DObject.getScale();
	}

	public void setLRTilt(double lrTiltAngleInRadians) {
		m3DObject.setRotZ(-lrTiltAngleInRadians * rtod);

	}

	public void setFBTilt(double fbTiltAngleInRadians) {
		m3DObject.setRotX(-fbTiltAngleInRadians * rtod);

	}

	public void setSpin(double spinAngleInDegrees) {

		m3DObject.setRotY(m3DObject.getRotY() + spinAngleInDegrees);

		if (DEBUG)
			Log.d(TAG, "getRotY: " + m3DObject.getRotY());

	}

	public boolean isReady() {
		return (m3DObject != null);
	}

}
