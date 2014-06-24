package com.dev_training.android.camerapreviewexample;

import java.io.IOException;
import java.util.List;

import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.annotation.SuppressLint;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.RelativeLayout;
import android.os.Build;

public class MainActivity extends ActionBarActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction()
					.add(R.id.container, new PlaceholderFragment()).commit();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment implements
			Callback {

		private static final String TAG = "PlaceholderFragment";
		private SurfaceView mSurfaceView;
		private SurfaceHolder mSurfaceHolder;
		private Camera mCamera;

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main, container,
					false);

			// SurfaceViewインスタンスの取得と初期化
			mSurfaceView = (SurfaceView) rootView
					.findViewById(R.id.surfaceView1);

			// SurfaceHolderのイベントに対するコールバック対応化
			mSurfaceHolder = (SurfaceHolder) mSurfaceView.getHolder();
			mSurfaceHolder.setSizeFromLayout();

			mSurfaceHolder.addCallback(this);
			
			return rootView;
		}

		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int width,
				int height) {
			Log.d(TAG, "surfaceChanged to "+width+","+height);

		}

		// Camera.open(int cameraId)はAPI Level9からの利用
		@SuppressLint("NewApi")
		@Override
		public void surfaceCreated(SurfaceHolder holer) {
			// カメラデバイスをオープン
			
			// デバイスのカメラ数を取得
			int numOfCams = Camera.getNumberOfCameras();
			mCamera = null;
			Log.d(TAG, "numOfCams = "+ numOfCams);
			for (int i = 0; i < numOfCams; i++) {
				try {
					mCamera = Camera.open(i);
					// オープンに成功した場合それが対象のカメラとなる。
					Log.d(TAG, "Camera.open("+i+") was successed.");
					break;
				} catch (RuntimeException e) {
					// 成功するまで繰り返す 
					Log.d(TAG, e.getMessage());
					continue;
				}
			}
			if (mCamera == null) {
				Log.d(TAG, "No camera device for use");
				return;
			}
			
			// カメラのパラメータから妥当なプレビューサイズを選択する
			Parameters params = mCamera.getParameters();
			
			
			Size previewSize = getOptimalPreviewSize(mCamera, mSurfaceHolder);
			params.setPreviewSize(previewSize.width, previewSize.height);
			
			mCamera.setParameters(params);
			
			// aspectRatio
			float aspectRatio = previewSize.width / (float)previewSize.height;
			float holderAspectRatio = mSurfaceView.getWidth() / (float)mSurfaceView.getHeight();
			
			// SurfaceViewのアスペクト比と回転角度をカメラプレビューに合わせる			
			if (holderAspectRatio > 1.0f){
				// 短い方のheightを維持
				
				// Aspect比が異なる場合、同一比率になるように調整
				if (aspectRatio != holderAspectRatio) {
					LayoutParams layoutParams = mSurfaceView.getLayoutParams();
					layoutParams.height = mSurfaceHolder.getSurfaceFrame().height();
					layoutParams.width = (int)(mSurfaceHolder.getSurfaceFrame().height()*aspectRatio);
					Log.d(TAG, "change size = "+layoutParams.width+", "+layoutParams.height);
					mSurfaceView.setLayoutParams(layoutParams);
				}							
			} else {
				// 90°回転
				mCamera.setDisplayOrientation(90);
				// 短い方のwidthを維持
				
				// Aspect比が異なる場合、同一比率になるように調整
				if (aspectRatio != holderAspectRatio) {
					LayoutParams layoutParams = mSurfaceView.getLayoutParams();
					layoutParams.width = mSurfaceHolder.getSurfaceFrame().width();
					layoutParams.height = (int)(mSurfaceHolder.getSurfaceFrame().width()*aspectRatio);
					
					Log.d(TAG, "Change Size = "+layoutParams.width+", "+layoutParams.height);
					mSurfaceView.setLayoutParams(layoutParams);
					
				}
				
			}
			
			// mSurfaceHolderにプレビュー画面を表示
			try {
				mCamera.setPreviewDisplay(mSurfaceHolder);
				mCamera.startPreview();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}

		/**
		 * CameraのプレビューサイズとSurfaceHolderのサイズのマッチングを行う
		 * @param camera	対象のCameraインスタンス
		 * @param holder	対象のSurfaceHolderインスタンス
		 * @return 対象カメラが対応しているサイズをSize型で返す
		 */
		private Size getOptimalPreviewSize(Camera camera, SurfaceHolder holder) {
			//
			Parameters params = camera.getParameters();
			// プレビューサイズの一覧取得
			List<Size> sizes = params.getSupportedPictureSizes();
			int holderWidth = holder.getSurfaceFrame().width();
			int holderHeight = holder.getSurfaceFrame().height();
			
			boolean orientation = (holderWidth > holderHeight) ? true: false;
			Size resultSize = null;
			// 最適サイズとの差分値
			int diff = Integer.MAX_VALUE;
			// もっとも近いサイズを採用
			for (int i=0; i<sizes.size(); i++){
				Size size = sizes.get(i);
				// 長い辺の長さとsize.heightを比較、差分が最も小さければ更新
				int curDiff = (orientation ? holderHeight: holderWidth)- size.height;
				if ( curDiff > 0 && curDiff <= diff) {
					resultSize = size;
					diff = (orientation ? holderHeight: holderWidth)- size.height;
				}
				// それぞれのプレビュー幅を調整
				Log.d(TAG, "("+size.width+","+size.height+")");	
			}
			Log.d(TAG, "SurfaceView Size = ("+holderWidth+","+holderHeight+")");	
			Log.d(TAG, "Optimal Size = ("+resultSize.width+","+resultSize.height+")");	
			return resultSize;
		}

		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			mCamera.stopPreview();
			mCamera.release();
			 
		}
	}

}
