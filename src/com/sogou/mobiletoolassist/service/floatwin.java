package com.sogou.mobiletoolassist.service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.sogou.mobiletoolassist.AssistActivity;
import com.sogou.mobiletoolassist.R;
import com.sogou.mobiletoolassist.assistApplication;
import com.sogou.mobiletoolassist.util.MailSender;
import com.sogou.mobiletoolassist.util.ScreenshotforGINGERBREAD_MR1;
import com.sogou.mobiletoolassist.util.ScreenshotforJELLY_BEAN;
import com.sogou.mobiletoolassist.util.StateValue;
import com.sogou.mobiletoolassist.util.UsefulClass;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

public class floatwin extends Service {
	private static WindowManager wm = null;
	private static WindowManager.LayoutParams params = null;
	private View btn_floatView = null;
	private Button clearBtn = null;
	private Button screenshotBtn = null;
	private ImageView smallview = null;
	private final IBinder binder = new MyBinder();
	private final static int hide = 0x0000001;
	private final static int screenshot = 0x0000002;
	private final static int visiable = 0x0000003;

	private static boolean isShooting = false;
	private static boolean isUninstalling = false;
	private Handler fltwinhandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case floatwin.hide:
				wm.removeView(btn_floatView);

				break;
			case floatwin.screenshot:
				floatwin.ScreenShot();
				Message message = new Message();
				message.what = floatwin.visiable;
				fltwinhandler.sendMessage(message);
				break;
			case floatwin.visiable:
				wm.addView(btn_floatView, params);
				break;
			}
			super.handleMessage(msg);
		}
	};

	@Override
	public IBinder onBind(Intent arg0) {

		return binder;
	}

	public class MyBinder extends Binder {
		public floatwin getService() {
			return floatwin.this;
		}
	}

	public native String memcreate(int mem);

	public native String memfree();

	static {
		System.loadLibrary("memCtrl");
	}

	@Override
	public void onCreate() {
		// createFloatView();
	}

	@Override
	public void onStart(Intent intent, int startId) {
		Log.d("study", "onstart");
	}

	/**
	 * 创建悬浮窗
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d("study", "onStartCommand");
		createFloatView();
		return Service.START_STICKY;// 表示被系统杀掉后需要重启
	}

	@Override
	public void onDestroy() {
		wm.removeView(btn_floatView);
		super.onDestroy();
	}

	@SuppressLint("InflateParams")
	private void createFloatView() {
		if (btn_floatView != null) {
			wm.addView(btn_floatView, params);
			return;
		}
		btn_floatView = LayoutInflater.from(this).inflate(R.layout.floatwin,
				null);
		smallview = (ImageView) btn_floatView.findViewById(R.id.fwinsmallview);
		smallview.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				if(isShooting)
					return;
				if (!clearBtn.isShown() || !screenshotBtn.isShown()) {
					clearBtn.setVisibility(Button.VISIBLE);
					screenshotBtn.setVisibility(Button.VISIBLE);
				} else {
					clearBtn.setVisibility(Button.GONE);
					screenshotBtn.setVisibility(Button.GONE);
				}
			}

		});
		clearBtn = (Button) btn_floatView.findViewById(R.id.cleardatabtn);
		clearBtn.setVisibility(Button.GONE);
		clearBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {

				floatwin.onClearBtn();

			}

		});
		screenshotBtn = (Button) btn_floatView.findViewById(R.id.screenshotbtn);
		screenshotBtn.setVisibility(Button.GONE);
		screenshotBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						Message message = new Message();
						message.what = floatwin.hide;
						Message message1 = new Message();
						message1.what = floatwin.screenshot;
						fltwinhandler.sendMessage(message);
						try {
							Thread.sleep(500);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						fltwinhandler.sendMessage(message1);
						// floatwin.ScreenShot();

					}

				}).start();
			}

		});
		wm = (WindowManager) getApplicationContext().getSystemService(
				Context.WINDOW_SERVICE);

		params = new WindowManager.LayoutParams();

		// 设置window type
		params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
		/*
		 * 如果设置为params.type = WindowManager.LayoutParams.TYPE_PHONE; 那么优先级会降低一些,
		 * 即拉下通知栏不可见
		 */

		params.format = PixelFormat.RGBA_8888;

		// 设置Window flag
		params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
				| WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
		/*
		 * 下面的flags属性的效果形同“锁定”。 悬浮窗不可触摸，不接受任何事件,同时不影响后面的事件响应。
		 * wmParams.flags=LayoutParams.FLAG_NOT_TOUCH_MODAL |
		 * LayoutParams.FLAG_NOT_FOCUSABLE | LayoutParams.FLAG_NOT_TOUCHABLE;
		 */

		// 设置悬浮窗的长得宽

		params.width = WindowManager.LayoutParams.WRAP_CONTENT;
		params.height = WindowManager.LayoutParams.WRAP_CONTENT;
		params.gravity = Gravity.LEFT; // 调整悬浮窗口至左上角
		params.x = 0;
		params.y = 0;
		// 需要增加增加system.alert_window权限
		wm.addView(btn_floatView, params);
		smallview.setOnTouchListener(new OnTouchListener() {
			int lastX, lastY;
			int paramX, paramY;

			@SuppressLint("ClickableViewAccessibility")
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					lastX = (int) event.getRawX();
					lastY = (int) event.getRawY();
					paramX = params.x;
					paramY = params.y;

					break;
				case MotionEvent.ACTION_MOVE:
					int dx = (int) event.getRawX() - lastX;
					int dy = (int) event.getRawY() - lastY;
					if (Math.abs(dx) < 5 && Math.abs(dy) < 5) {
						// 不这么做太灵敏了，明明我是在点击你移动个毛线啊

						break;
					}
					params.x = paramX + dx;
					params.y = paramY + dy;

					// 更新悬浮窗位置
					wm.updateViewLayout(btn_floatView, params);

					break;
				case MotionEvent.ACTION_UP:
					int dx1 = (int) event.getRawX() - lastX;
					int dy1 = (int) event.getRawY() - lastY;
					if (Math.abs(dx1) < 5 && Math.abs(dy1) < 5) {
						smallview.performClick();
					}

				}
				return true;
			}
		});
		// 设置悬浮窗的Touch监听
		screenshotBtn.setOnTouchListener(new OnTouchListener() {
			int lastX, lastY;
			int paramX, paramY;

			@SuppressLint("ClickableViewAccessibility")
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					lastX = (int) event.getRawX();
					lastY = (int) event.getRawY();
					paramX = params.x;
					paramY = params.y;

					break;
				case MotionEvent.ACTION_MOVE:
					int dx = (int) event.getRawX() - lastX;
					int dy = (int) event.getRawY() - lastY;
					if (Math.abs(dx) < 5 && Math.abs(dy) < 5) {
						// 不这么做太灵敏了，明明我是在点击你移动个毛线啊

						break;
					}
					params.x = paramX + dx;
					params.y = paramY + dy;

					// 更新悬浮窗位置
					wm.updateViewLayout(btn_floatView, params);

					break;
				case MotionEvent.ACTION_UP:
					int dx1 = (int) event.getRawX() - lastX;
					int dy1 = (int) event.getRawY() - lastY;
					if (Math.abs(dx1) < 5 && Math.abs(dy1) < 5) {
						

						Uri uri = RingtoneManager
								.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);// 系统自带提示音
						Ringtone rt = RingtoneManager.getRingtone(
								getApplicationContext(), uri);
						if (rt != null)
							rt.play();
						screenshotBtn.performClick();

					}

				}
				return true;
			}
		});

		clearBtn.setOnTouchListener(new OnTouchListener() {
			int lastX, lastY;
			int paramX, paramY;

			@SuppressLint("ClickableViewAccessibility")
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					lastX = (int) event.getRawX();
					lastY = (int) event.getRawY();
					paramX = params.x;
					paramY = params.y;

					break;
				case MotionEvent.ACTION_MOVE:
					int dx = (int) event.getRawX() - lastX;
					int dy = (int) event.getRawY() - lastY;
					if (Math.abs(dx) < 5 && Math.abs(dy) < 5) {
						// 不这么做太灵敏了，明明我是在点击你移动个毛线啊

						break;
					}
					params.x = paramX + dx;
					params.y = paramY + dy;

					// 更新悬浮窗位置
					wm.updateViewLayout(btn_floatView, params);

					break;
				case MotionEvent.ACTION_UP:
					int dx1 = (int) event.getRawX() - lastX;
					int dy1 = (int) event.getRawY() - lastY;
					if (Math.abs(dx1) < 5 && Math.abs(dy1) < 5) {

						clearBtn.performClick();

					}

				}
				return true;
			}
		});

	}

	public static void onClearBtn() {
		Context ctx = assistApplication.getContext();
		if (ctx == null) {
			Log.e(AssistActivity.myTag, "ctx 是空的");
			return;
		}
		if (!UsefulClass.hasappnamedxxx(ctx, "com.sogou.androidtool")) {
			Toast.makeText(ctx, "没有安装助手", Toast.LENGTH_SHORT).show();
			return;
		}
		String cmd = "pm clear com.sogou.androidtool";
		Toast.makeText(ctx, "准备清理数据~", Toast.LENGTH_SHORT).show();
		if (UsefulClass.processCmd(cmd) == StateValue.success) {
			Toast.makeText(ctx, "清理数据完毕~", Toast.LENGTH_SHORT).show();
		}
	}

	public static boolean ScreenShot() {
		floatwin.isShooting = true;
		String path = "";
//		Toast.makeText(assistApplication.getContext(), "开始截图",
//				Toast.LENGTH_LONG).show();
		if (Build.VERSION.SDK_INT > 13) {
			path = ScreenshotforJELLY_BEAN.shoot();
		} else {
			if (ScreenshotforGINGERBREAD_MR1.isInitialized())
				path = ScreenshotforGINGERBREAD_MR1.shoot();
		}

		File testpath = new File(path);
		if (!testpath.exists()) {
			Toast.makeText(assistApplication.getContext(), "截图文件不在，请检查sd卡是否正常",
					Toast.LENGTH_LONG).show();
			floatwin.isShooting = false;
			return false;
		}
		String info = UsefulClass.getDeviceInfo();
		String title = info + "【截图】";
		info += "</br>";
		info += UsefulClass.getZSPkgInfo();
		SharedPreferences appdata = assistApplication.getContext()
				.getSharedPreferences("AppData", MODE_PRIVATE);
		String emailReceiver = appdata.getString("mailReceiver",
				"pdatest@sogou-inc.com");
		if (assistApplication.getContext() != null) {
			ConnectivityManager mConnectivityManager = (ConnectivityManager) assistApplication
					.getContext()
					.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo mNetworkInfo = mConnectivityManager
					.getActiveNetworkInfo();
			if (mNetworkInfo == null || !mNetworkInfo.isConnected()) {
				Toast.makeText(assistApplication.getContext(),
						"网络貌似有问题哦，邮件发不出去", Toast.LENGTH_LONG).show();
				floatwin.isShooting = false;
				return false;
			}
		}
		if (MailSender.sendTextMail(title, info, path,
				new String[] { emailReceiver })) {
			String emailReceivername = appdata.getString("name", "pdatest");
			Toast.makeText(assistApplication.getContext(),
					"截图完毕，" + emailReceivername + "同学请静候邮件~", Toast.LENGTH_LONG)
					.show();
			File tmp = new File(path);
			if (tmp.exists())
				tmp.delete();
		} else {
			Toast.makeText(assistApplication.getContext(), "发送邮件异常，可能是读截图失败了",
					Toast.LENGTH_LONG).show();
		}
		floatwin.isShooting = false;
		return true;
	}
	private Runnable uinstallrun = new Runnable() {

		@Override
		public void run() {
			floatwin.isUninstalling = true;
			Context ctx = assistApplication.getContext();
			PackageManager pkgmgr = (PackageManager) ctx
					.getPackageManager();
			List<PackageInfo> allapps = pkgmgr.getInstalledPackages(0);
			List<PackageInfo> alluserapps = new ArrayList<PackageInfo>();
			for (PackageInfo app : allapps) {
				if ((app.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
					alluserapps.add(app);// 过滤所有非系统应用
				}
			}
			if (alluserapps.size() > 30) {
				Toast.makeText(ctx, "app太多，清理较慢，先去喝杯水吧~",
						Toast.LENGTH_SHORT).show();
			}
			allapps = null;
			if (alluserapps == null || alluserapps.isEmpty()) {
				Toast.makeText(ctx, "你的手机没有已安装应用~", Toast.LENGTH_SHORT)
						.show();
				floatwin.isUninstalling = false;
				return;
			}
			Toast.makeText(ctx, "开始清理app", Toast.LENGTH_SHORT).show();

			String cmd = "pm uninstall ";
			for (PackageInfo app : alluserapps) {
				if (app.packageName != null
						&& app.packageName.length() != 0
						&& !app.packageName.equals("com.sogou.androidtool")
						&& !app.packageName
								.equals("com.sogou.mobiletoolassist")
						&& !app.packageName
								.equals("com.sohu.inputmethod.sogou")
						&& !app.packageName
								.equals("com.speedsoftware.rootexplorer")) {

					if (StateValue.unroot == UsefulClass.processCmd(cmd
							+ app.packageName)) {
						Toast.makeText(ctx, "获取root权限失败！！！",
								Toast.LENGTH_SHORT).show();
						break;
					}
				}
			}
			Toast.makeText(ctx, "已卸载所有app", Toast.LENGTH_LONG).show();
			floatwin.isUninstalling = false;
		}
	};

	public void uninstallAPPs() {
		if(isUninstalling)
			return;
		fltwinhandler.post(uinstallrun);
	}

	public void floatwinswitch(boolean set) {
		if (wm == null || btn_floatView == null || params == null) {
			Log.e(AssistActivity.myTag, "some error");
		}
		if (set) {
			wm.addView(btn_floatView, params);
		} else {
			wm.removeView(btn_floatView);
		}
	}
}
