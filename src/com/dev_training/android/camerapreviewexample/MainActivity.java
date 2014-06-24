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

			// SurfaceView�C���X�^���X�̎擾�Ə�����
			mSurfaceView = (SurfaceView) rootView
					.findViewById(R.id.surfaceView1);

			// SurfaceHolder�̃C�x���g�ɑ΂���R�[���o�b�N�Ή���
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

		// Camera.open(int cameraId)��API Level9����̗��p
		@SuppressLint("NewApi")
		@Override
		public void surfaceCreated(SurfaceHolder holer) {
			// �J�����f�o�C�X���I�[�v��
			
			// �f�o�C�X�̃J���������擾
			int numOfCams = Camera.getNumberOfCameras();
			mCamera = null;
			Log.d(TAG, "numOfCams = "+ numOfCams);
			for (int i = 0; i < numOfCams; i++) {
				try {
					mCamera = Camera.open(i);
					// �I�[�v���ɐ��������ꍇ���ꂪ�Ώۂ̃J�����ƂȂ�B
					Log.d(TAG, "Camera.open("+i+") was successed.");
					break;
				} catch (RuntimeException e) {
					// ��������܂ŌJ��Ԃ� 
					Log.d(TAG, e.getMessage());
					continue;
				}
			}
			if (mCamera == null) {
				Log.d(TAG, "No camera device for use");
				return;
			}
			
			// �J�����̃p�����[�^����Ó��ȃv���r���[�T�C�Y��I������
			Parameters params = mCamera.getParameters();
			
			
			Size previewSize = getOptimalPreviewSize(mCamera, mSurfaceHolder);
			params.setPreviewSize(previewSize.width, previewSize.height);
			
			mCamera.setParameters(params);
			
			// aspectRatio
			float aspectRatio = previewSize.width / (float)previewSize.height;
			float holderAspectRatio = mSurfaceView.getWidth() / (float)mSurfaceView.getHeight();
			
			// SurfaceView�̃A�X�y�N�g��Ɖ�]�p�x���J�����v���r���[�ɍ��킹��			
			if (holderAspectRatio > 1.0f){
				// �Z������height���ێ�
				
				// Aspect�䂪�قȂ�ꍇ�A����䗦�ɂȂ�悤�ɒ���
				if (aspectRatio != holderAspectRatio) {
					LayoutParams layoutParams = mSurfaceView.getLayoutParams();
					layoutParams.height = mSurfaceHolder.getSurfaceFrame().height();
					layoutParams.width = (int)(mSurfaceHolder.getSurfaceFrame().height()*aspectRatio);
					Log.d(TAG, "change size = "+layoutParams.width+", "+layoutParams.height);
					mSurfaceView.setLayoutParams(layoutParams);
				}							
			} else {
				// 90����]
				mCamera.setDisplayOrientation(90);
				// �Z������width���ێ�
				
				// Aspect�䂪�قȂ�ꍇ�A����䗦�ɂȂ�悤�ɒ���
				if (aspectRatio != holderAspectRatio) {
					LayoutParams layoutParams = mSurfaceView.getLayoutParams();
					layoutParams.width = mSurfaceHolder.getSurfaceFrame().width();
					layoutParams.height = (int)(mSurfaceHolder.getSurfaceFrame().width()*aspectRatio);
					
					Log.d(TAG, "Change Size = "+layoutParams.width+", "+layoutParams.height);
					mSurfaceView.setLayoutParams(layoutParams);
					
				}
				
			}
			
			// mSurfaceHolder�Ƀv���r���[��ʂ�\��
			try {
				mCamera.setPreviewDisplay(mSurfaceHolder);
				mCamera.startPreview();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}

		/**
		 * Camera�̃v���r���[�T�C�Y��SurfaceHolder�̃T�C�Y�̃}�b�`���O���s��
		 * @param camera	�Ώۂ�Camera�C���X�^���X
		 * @param holder	�Ώۂ�SurfaceHolder�C���X�^���X
		 * @return �ΏۃJ�������Ή����Ă���T�C�Y��Size�^�ŕԂ�
		 */
		private Size getOptimalPreviewSize(Camera camera, SurfaceHolder holder) {
			//
			Parameters params = camera.getParameters();
			// �v���r���[�T�C�Y�̈ꗗ�擾
			List<Size> sizes = params.getSupportedPictureSizes();
			int holderWidth = holder.getSurfaceFrame().width();
			int holderHeight = holder.getSurfaceFrame().height();
			
			boolean orientation = (holderWidth > holderHeight) ? true: false;
			Size resultSize = null;
			// �œK�T�C�Y�Ƃ̍����l
			int diff = Integer.MAX_VALUE;
			// �����Ƃ��߂��T�C�Y���̗p
			for (int i=0; i<sizes.size(); i++){
				Size size = sizes.get(i);
				// �����ӂ̒�����size.height���r�A�������ł���������΍X�V
				int curDiff = (orientation ? holderHeight: holderWidth)- size.height;
				if ( curDiff > 0 && curDiff <= diff) {
					resultSize = size;
					diff = (orientation ? holderHeight: holderWidth)- size.height;
				}
				// ���ꂼ��̃v���r���[���𒲐�
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
