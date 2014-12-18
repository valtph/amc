package com.phvalt.accelerometergraphrendring;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.achartengine.ChartFactory;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import org.jtransforms.fft.DoubleFFT_1D;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.text.Layout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

public class MainActivity extends Activity implements OnClickListener {

	private static final String TAG = "Accelerometer Graph"; // Logging purpose
	/**Sensor Variables**/
	private float[] mCurrents = new float[3];
	private ConcurrentLinkedQueue<float[]> mHistory = new ConcurrentLinkedQueue<float[]>();
	private SensorManager mSensorManager;
	private Sensor mAccelerometer;
	private int mSensorDelay=5000;
	//private int mSensorDelay = SensorManager.SENSOR_DELAY_FASTEST;
	private int mMaxHistorySize;
	/**Saving variables**/
	private ArrayList<AccelData> sensorData;
	private boolean saving = false;
	private long index;
	private String path = "/mnt/sdcard/download/";
	private String fileName;
	/**Layout elements**/
	private TextView xCoor; // declare X axis
	private TextView yCoor; // declare Y axis
	private TextView zCoor; // declare Z axis
	private Button saveButton;
	//private ProgressBar cPB;
	private TextView timeInfo;
	/**Graph variables**/
	private boolean[] mGraphs = { true, true, true, };
	private int[] mAxisColors = new int[3];
	private GraphView mGraphView;
	private int mBGColor;
	private int mZeroLineColor;
	private int mStringColor;
	private boolean mDrawLoop = true;
	private int mDrawDelay = 100;
	private int mLineWidth = 2;
	private int mGraphScale = 6;
	private int mZeroLineY = 200;
	private int mZeroLineYOffset = 0;
	/**Sampling variables**/
	private SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd-HH_mm_ss");
	private long beginTimeStamp;
	private long finalTimeStamp;
	private long elapsedTime;
	private long offsetTimeStamp;
	private double frequency;
	/**Spectrum variable**/
	double[] fS;
	double[] magX;
	/**EventListenner setup**/
	private SensorEventListener mSensorEventListener = new SensorEventListener() {
		@Override
		public void onSensorChanged(SensorEvent event) {
				/**main routine, executed each time a variation is stored**/
				Log.i(TAG, "mSensorEventListener.onSensorChanged()");
				for (int axis = 0; axis < 3; axis++) {
					float value = event.values[axis];
					mCurrents[axis] = value;
				}
				if (saving) {
					/**Store the data for later**/
					AccelData data = new AccelData(index, mCurrents[0],
							mCurrents[1], mCurrents[2]);
					sensorData.add(data);
					index=index+1;
				}
				xCoor.setText("X : " + event.values[0]);
				yCoor.setText("Y : " + event.values[1]);
				zCoor.setText("Z : " + event.values[2]);
				synchronized (this) {
					/**Synchronized thread if queue size is greater than
					 * maxHistory poll() and remove, else, add to queue
					 **/
					if (mHistory.size() >= mMaxHistorySize) {
						mHistory.poll();
					}
					mHistory.add(mCurrents.clone());
				}
			}
		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			// Unused for now
			Log.i(TAG, "mSensorEventListener.onAccuracyChanged()");
		}
	};

	private void startGraph() {
		if (mAccelerometer != null) {
			mSensorManager.registerListener(mSensorEventListener,
					mAccelerometer, mSensorDelay);
		}
		if (!mDrawLoop) {
			// stop painting
			mSensorManager.unregisterListener(mSensorEventListener);
			mDrawLoop = false;
		}
	}
	private void stopGraph() {
		mSensorManager.unregisterListener(mSensorEventListener, mAccelerometer);
		mDrawLoop = false;
	}
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(TAG, "MainActivity.onCreate()");
		Window window = getWindow();
		window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		
		setContentView(R.layout.activity_main);

		xCoor = (TextView) findViewById(R.id.x_label); // create object
		yCoor = (TextView) findViewById(R.id.y_label);
		zCoor = (TextView) findViewById(R.id.z_label);
		saveButton = (Button) findViewById(R.id.saveButton);
		saveButton.setOnClickListener(this);
		//cPB = (ProgressBar) findViewById(R.id.barTimer);//unused cause slow down the UI
		timeInfo = (TextView) findViewById(R.id.timeInfo);
		timeInfo.setText("OFF");

		FrameLayout frame = (FrameLayout) findViewById(R.id.frame);
		Resources resources = getResources();
		mStringColor = resources.getColor(R.color.string);
		mBGColor = resources.getColor(R.color.background);
		mZeroLineColor = resources.getColor(R.color.zero_line);
		mAxisColors[0] = resources.getColor(R.color.accele_x);
		mAxisColors[1] = resources.getColor(R.color.accele_y);
		mAxisColors[2] = resources.getColor(R.color.accele_z);

		mGraphView = new GraphView(this);
		frame.addView(mGraphView, 0);
		
	}
	@Override
	protected void onStart() {
		Log.i(TAG, "MainActivity.OnStart()");
		// initialization
		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		List<Sensor> sensor = mSensorManager
				.getSensorList(Sensor.TYPE_LINEAR_ACCELERATION);
		if (sensor.size() > 0) {
			mAccelerometer = sensor.get(0);
		} else {
			Log.i(TAG, "No accelerometer found");
		}
		super.onStart();
	}
	@Override
	protected void onResume() {
		Log.i(TAG, "MainActivity.onResume()");
		startGraph();
		super.onResume();
	}
	@Override
	protected void onPause() {
		Log.i(TAG, "MainActivity.onPause()");
		stopGraph();
		super.onPause();
	}
	@Override
	protected void onStop() {
		Log.i(TAG, "MainActivity.onStop(");
		mSensorManager = null;
		mAccelerometer = null;
		super.onStop();
	}
	@Override
	protected void onDestroy() {
		Log.i(TAG, "MainActivity.onDestroy()");
		super.onDestroy();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	}
	@Override
	final public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
	}

	/**
	 * Menu Stubs
	 * 
	 * @Override public boolean onCreateOptionsMenu(Menu menu) { // Inflate the
	 *           menu; this adds items to the action bar if it is present.
	 *           getMenuInflater().inflate(R.menu.main, menu); return true; }
	 * @Override public boolean onOptionsItemSelected(MenuItem item) { // Handle
	 *           action bar item clicks here. The action bar will //
	 *           automatically handle clicks on the Home/Up button, so long //
	 *           as you specify a parent activity in AndroidManifest.xml. int id
	 *           = item.getItemId(); if (id == R.id.action_settings) { return
	 *           true; } return super.onOptionsItemSelected(item); }
	 **/

	private class GraphView extends SurfaceView implements
			SurfaceHolder.Callback, Runnable {

		private Thread mThread;
		private SurfaceHolder mHolder;

		public GraphView(Context context) {
			super(context);
			Log.i(TAG, "GraphView.GraphView()");
			mHolder = getHolder();
			mHolder.addCallback(this);
			setFocusable(true);
			requestFocus();
		}
		@Override
		public void run() {
			Log.i(TAG, "()");
			int width = getWidth();
			int height = getHeight();
			mMaxHistorySize = (int) ((width - 20) / mLineWidth);
			Paint textPaint = new Paint();
			textPaint.setColor(mStringColor);
			textPaint.setAntiAlias(true);
			textPaint.setTextSize(11);
			
			Paint zeroLinePaint = new Paint();
			zeroLinePaint.setColor(mZeroLineColor);
			zeroLinePaint.setAntiAlias(true);
			Paint[] linePaints = new Paint[3];
			for (int i = 0; i < 3; i++) {
				linePaints[i] = new Paint();
				linePaints[i].setColor(mAxisColors[i]);
				linePaints[i].setAntiAlias(true);
				linePaints[i].setStrokeWidth(2);
			}
			while (mDrawLoop) {
				Canvas canvas = mHolder.lockCanvas();
				if (canvas == null) {
					break;
				}
				canvas.drawColor(mBGColor);
				float zeroLineY = mZeroLineY + mZeroLineYOffset;

				synchronized (mHolder) {
					float twoLineY = zeroLineY - (20 * mGraphScale);
					float oneLineY = zeroLineY - (10 * mGraphScale);
					float halfLineY = zeroLineY - (5 * mGraphScale);
					float minasOneLineY = zeroLineY + (10 * mGraphScale);
					float minasTwoLineY = zeroLineY + (20 * mGraphScale);
					float minasHalfLineY = zeroLineY + (5 * mGraphScale);

					zeroLinePaint.setTextSize(20);
					canvas.drawText("2", 5, twoLineY + 5, zeroLinePaint);
					canvas.drawLine(20, twoLineY, width, twoLineY,
							zeroLinePaint);
					canvas.drawText("1", 5, oneLineY + 5, zeroLinePaint);
					canvas.drawLine(20, oneLineY, width, oneLineY,
							zeroLinePaint);
					canvas.drawText("0.5", 5, halfLineY + 5, zeroLinePaint);
					canvas.drawLine(20, halfLineY, width, halfLineY,
							zeroLinePaint);
					canvas.drawText("0", 5, zeroLineY + 5, zeroLinePaint);
					canvas.drawLine(20, zeroLineY, width, zeroLineY,
							zeroLinePaint);
					canvas.drawText("-2", 5, minasTwoLineY + 5, zeroLinePaint);
					canvas.drawLine(20, minasTwoLineY, width, minasTwoLineY,
							zeroLinePaint);
					canvas.drawText("-1", 5, minasOneLineY + 5, zeroLinePaint);
					canvas.drawLine(20, minasOneLineY, width, minasOneLineY,
							zeroLinePaint);
					canvas.drawText("-0.5", 5, minasHalfLineY + 5,
							zeroLinePaint);
					canvas.drawLine(20, minasHalfLineY, width, minasHalfLineY,
							zeroLinePaint);
					canvas.setDensity(2);

					if (mHistory.size() > 1) {
						Iterator<float[]> iterator = mHistory.iterator();
						float[] before = new float[3];
						int x = width - mHistory.size() * mLineWidth;
						int beforeX = x;
						x += mLineWidth;
						if (iterator.hasNext()) {
							float[] history = iterator.next();
							for (int axis = 0; axis < 3; axis++) {
								before[axis] = zeroLineY
										- (history[axis] * mGraphScale);
							}
							while (iterator.hasNext()) {
								history = iterator.next();
								for (int axis = 0; axis < 3; axis++) {
									float startY = zeroLineY
											- (history[axis] * mGraphScale);
									float stopY = before[axis];
									if (mGraphs[axis]) {
										canvas.drawLine(x, startY, beforeX,
												stopY, linePaints[axis]);
									}
									before[axis] = startY;
								}
								beforeX = x;
								x += mLineWidth;
							}
						}
					}
				}
				mHolder.unlockCanvasAndPost(canvas);
				try {
					Thread.sleep(mDrawDelay);
				} catch (InterruptedException e) {
					Log.e(TAG, e.getMessage());
				}
			}
		}
		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			Log.i(TAG, "GraphView.surfaceCreated()");
			mDrawLoop = true;
			mThread = new Thread(this);
			mThread.start();
		}
		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int width,
				int height) {
			Log.i(TAG, "GraphView.surfaceChanged()");
		}
		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			Log.i(TAG, "GraphView.surfaceDestroyed()");

			mDrawLoop = false;
			boolean roop = true;
			while (roop) {
				try {
					mThread.join();
					roop = false;
				} catch (InterruptedException e) {
					Log.e(TAG, e.getMessage());
				}
			}
			mThread = null;
		}
	}
	
	/**External functions **/
	private void computeFFTX(ArrayList<AccelData>sensorData) throws IOException {
			/**Extract xAccel**/
			double[] xAccel = new double[sensorData.size()];
			for(AccelData a:sensorData){
				xAccel[sensorData.indexOf(a)]=a.getX();
			}
			DoubleFFT_1D fftDo = new DoubleFFT_1D(xAccel.length);
			double[] fft = new double[xAccel.length * 2];
			System.arraycopy(xAccel, 0, fft, 0, xAccel.length);
			fftDo.realForwardFull(fft);
			magX = new double[xAccel.length];
			magX[0] = (2.0 / xAccel.length) * Math.sqrt(fft[0] * fft[0]);
			for (int i = 1; i < xAccel.length; i++) {
				magX[i] = (2.0 / xAccel.length)
						* Math.sqrt(fft[2 * i] * fft[2 * i] + fft[2 * i + 1]
								* fft[2 * i + 1]);
			}
			/**Frequency calculus**/
			
			elapsedTime=finalTimeStamp-beginTimeStamp;
			
			frequency=(double)(magX.length)/(double)(elapsedTime/1000);
			timeInfo.setText("F= "+Double.toString(frequency)+"Hz");
			/**Fill frequency index**/
			fS=new double[magX.length];
			for(int i=0;i<magX.length;i++){
				double freq = (double)(i)*frequency/(double)magX.length;
				fS[i]=(freq);
			}
			
			BufferedWriter wr = null;
			try {
				wr = new BufferedWriter(new FileWriter(path + "fftX"+fileName));
				for (int fftIndex=0;fftIndex<fS.length;fftIndex++) {
					wr.write(fftIndex+", "+Double.toString(fS[fftIndex])+ ", "+magX[fftIndex]);
					wr.newLine();
					wr.flush();
				}
			} 
			catch (IOException ex) {
				ex.printStackTrace();
			} finally {
				if (wr != null) {
					wr.close();
				}
			}
	}
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.saveButton:
			if (saving) {
				/** create a file store the data**/
				finalTimeStamp=SystemClock.elapsedRealtime();
				Date now = new Date();
				fileName = formatter.format(now) + ".txt";
				saveButton.setText("start");// change button tag
				
				try {
					saveTo(fileName);
					
				} catch (IOException e) {
					e.printStackTrace();
				}
				saving = false;
				
			    try {
					computeFFTX(sensorData);
				} catch (IOException e) {
					e.printStackTrace();
				}
			    Bundle b=new Bundle();
			    Intent i = new Intent(MainActivity.this, ShowSpectrumActivity.class);
			    b.putDoubleArray("frequences", fS);
			    b.putDoubleArray("amplitudes", magX);
			    i.putExtras(b);
			    startActivity(i);
			    this.onPause();
			} else {
				/** create an array to store data**/
				sensorData = new ArrayList<AccelData>();
				saving = true;
				index=0;
				//startTimer(5);
				beginTimeStamp = SystemClock.elapsedRealtime();
				saveButton.setText("press to stop");
				//beginTimeStamp = System.nanoTime();
			}
			break;
		default:
			break;
		}
	}
	/**	private void startTimer(final int seconds) {
		CountDownTimer countDownTimer = new CountDownTimer(seconds * 1000, 1) {
			@Override
			public void onTick(long millisUntilFinished) {
				long remaining = millisUntilFinished / 1000;
				timeInfo.setText(Integer.toString((int) remaining));
				cPB.setProgress((int) (seconds - remaining));

			}
			@Override
			public void onFinish() {
				cPB.setProgress(0);
				timeInfo.setText("");
			}
		}.start();
	}
**/
	private void saveTo(String fileName) throws IOException {
		BufferedWriter wr = null;

		try {
			wr = new BufferedWriter(new FileWriter(path + fileName));
			for (AccelData data : sensorData) {
				wr.write(data.toString());
				// wr.newLine();
				wr.flush();
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (wr != null) {
				wr.close();
			}
		}
	}
}
