package com.nmbb.vlc.ui;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import com.nmbb.vlc.R;

import org.videolan.libvlc.EventHandler;
import org.videolan.libvlc.IVideoPlayer;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.LibVlcException;
import org.videolan.vlc.util.VLCInstance;

public class VlcVideoActivity extends Activity implements SurfaceHolder.Callback, IVideoPlayer {

	private final static String TAG = "[VlcVideoActivity]";

	private SurfaceView mSurfaceView;
	private LibVLC mMediaPlayer;
	private SurfaceHolder mSurfaceHolder;
    
    private View mLoadingView;

	private int mVideoHeight;
	private int mVideoWidth;
	private int mVideoVisibleHeight;
	private int mVideoVisibleWidth;
	private int mSarNum;
	private int mSarDen;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_video_vlc);

		mSurfaceView = (SurfaceView) findViewById(R.id.video);
        mLoadingView = findViewById(R.id.video_loading);
		try {
			mMediaPlayer = VLCInstance.getLibVlcInstance();
		} catch (LibVlcException e) {
			e.printStackTrace();
		}

		mSurfaceHolder = mSurfaceView.getHolder();
		mSurfaceHolder.setFormat(PixelFormat.RGBX_8888);
		mSurfaceHolder.addCallback(this);

		mMediaPlayer.eventVideoPlayerActivityCreated(true);

		EventHandler em = EventHandler.getInstance();
		em.addHandler(mVlcHandler);

		this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
		mSurfaceView.setKeepScreenOn(true);
		//		mMediaPlayer.setMediaList();
		//		mMediaPlayer.getMediaList().add(new Media(mMediaPlayer, "http://live.3gv.ifeng.com/zixun.m3u8"), false);
		//		mMediaPlayer.playIndex(0);
		mMediaPlayer.playMRL("http://live.3gv.ifeng.com/zixun.m3u8");
	}

	@Override
	public void onPause() {
		super.onPause();

		if (mMediaPlayer != null) {
			mMediaPlayer.stop();
			mSurfaceView.setKeepScreenOn(false);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mMediaPlayer != null) {
			mMediaPlayer.eventVideoPlayerActivityCreated(false);

			EventHandler em = EventHandler.getInstance();
			em.removeHandler(mVlcHandler);
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		setSurfaceSize(mVideoWidth, mVideoHeight, mVideoVisibleWidth, mVideoVisibleHeight, mSarNum, mSarDen);
		super.onConfigurationChanged(newConfig);
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		if (mMediaPlayer != null) {
			mSurfaceHolder = holder;
			mMediaPlayer.attachSurface(holder.getSurface(), this);
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		mSurfaceHolder = holder;
		if (mMediaPlayer != null) {
			mMediaPlayer.attachSurface(holder.getSurface(), this);//, width, height
		}
		if (width > 0) {
			mVideoHeight = height;
			mVideoWidth = width;
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		if (mMediaPlayer != null) {
			mMediaPlayer.detachSurface();
		}
	}

	@Override
	public void setSurfaceSize(int width, int height, int visible_width, int visible_height, int sar_num, int sar_den) {
		mVideoHeight = height;
		mVideoWidth = width;
		mVideoVisibleHeight = visible_height;
		mVideoVisibleWidth = visible_width;
		mSarNum = sar_num;
		mSarDen = sar_den;
		mHandler.removeMessages(HANDLER_SURFACE_SIZE);
		mHandler.sendEmptyMessage(HANDLER_SURFACE_SIZE);
	}

	private static final int HANDLER_BUFFER_START = 1;
	private static final int HANDLER_BUFFER_END = 2;
	private static final int HANDLER_SURFACE_SIZE = 3;

	private static final int SURFACE_BEST_FIT = 0;
	private static final int SURFACE_FIT_HORIZONTAL = 1;
	private static final int SURFACE_FIT_VERTICAL = 2;
	private static final int SURFACE_FILL = 3;
	private static final int SURFACE_16_9 = 4;
	private static final int SURFACE_4_3 = 5;
	private static final int SURFACE_ORIGINAL = 6;
	private int mCurrentSize = SURFACE_BEST_FIT;

	private Handler mVlcHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (msg == null || msg.getData() == null)
				return;

			switch (msg.getData().getInt("event")) {
			case EventHandler.MediaPlayerTimeChanged:
				break;
			case EventHandler.MediaPlayerPositionChanged:
				break;
			case EventHandler.MediaPlayerPlaying:
				mHandler.removeMessages(HANDLER_BUFFER_END);
				mHandler.sendEmptyMessage(HANDLER_BUFFER_END);
				break;
			case EventHandler.MediaPlayerBuffering:
				break;
			case EventHandler.MediaPlayerLengthChanged:
				break;
			case EventHandler.MediaPlayerEndReached:
				//播放完成
				break;
			}

		}
	};

	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case HANDLER_BUFFER_START:
                showLoading();
				break;
			case HANDLER_BUFFER_END:
                hideLoading();
				break;
			case HANDLER_SURFACE_SIZE:
				changeSurfaceSize();
				break;
			}
		}
	};

	private void showLoading() {
        mLoadingView.setVisibility(View.VISIBLE);
	}

	private void hideLoading() {
        mLoadingView.setVisibility(View.GONE);
	}

	private void changeSurfaceSize() {
		// get screen size
		int dw = getWindowManager().getDefaultDisplay().getWidth();
		int dh = getWindowManager().getDefaultDisplay().getHeight();

		// calculate aspect ratio
		double ar = (double) mVideoWidth / (double) mVideoHeight;
		// calculate display aspect ratio
		double dar = (double) dw / (double) dh;

		switch (mCurrentSize) {
		case SURFACE_BEST_FIT:
			if (dar < ar)
				dh = (int) (dw / ar);
			else
				dw = (int) (dh * ar);
			break;
		case SURFACE_FIT_HORIZONTAL:
			dh = (int) (dw / ar);
			break;
		case SURFACE_FIT_VERTICAL:
			dw = (int) (dh * ar);
			break;
		case SURFACE_FILL:
			break;
		case SURFACE_16_9:
			ar = 16.0 / 9.0;
			if (dar < ar)
				dh = (int) (dw / ar);
			else
				dw = (int) (dh * ar);
			break;
		case SURFACE_4_3:
			ar = 4.0 / 3.0;
			if (dar < ar)
				dh = (int) (dw / ar);
			else
				dw = (int) (dh * ar);
			break;
		case SURFACE_ORIGINAL:
			dh = mVideoHeight;
			dw = mVideoWidth;
			break;
		}

		mSurfaceHolder.setFixedSize(mVideoWidth, mVideoHeight);
		ViewGroup.LayoutParams lp = mSurfaceView.getLayoutParams();
		lp.width = dw;
		lp.height = dh;
		mSurfaceView.setLayoutParams(lp);
		mSurfaceView.invalidate();
	}
}
