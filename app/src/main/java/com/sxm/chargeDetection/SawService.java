package com.sxm.chargeDetection;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

public class SawService extends Service {

    private int upX, upY;
    public static boolean saw = false;
    public static String s = "";
    private static String ss = null;
    private WindowManager wm;
    private TextView tv;
    private static int x = 0, y = 0;
    private static long time = 0;
    private Thread thread;
    private static int tempX = 0, tempY = 0;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        tv = new TextView(this);
        //监听字符串s是否有变化
        thread = new Thread() {
            @Override
            public void run() {
                try {
                    while (true) {
                        if (ss != s) {
                            ss = s;
                            tv.post(() -> tv.setText(s));
                        }
                        sleep(16);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        thread.start();
        tv.setText(s);
        tv.setTextColor(0xffffffff);
        tv.setBackgroundColor(0x66000000);

        //获取WindowManager
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);

        //创建布局参数
        WindowManager.LayoutParams params = new WindowManager.LayoutParams();

        //设置参数
        params.type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                WindowManager.LayoutParams.TYPE_PHONE;
        params.format = PixelFormat.RGBA_8888;

        //设置窗口的行为准则
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;

        //设置透明度
        params.alpha = 1.0f;

        //设置内部视图对齐方式，这边位置为左边靠上
        params.gravity = Gravity.LEFT | Gravity.TOP;

        //窗口的左上角坐标
        params.x = x;
        params.y = y;

        //设置窗口的宽高,这里为自动
        params.width = WindowManager.LayoutParams.WRAP_CONTENT;
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;

        // 视图移动处理
        ((View) tv).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int diffX = tempX - upX, diffY = tempY - upY;
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        upX = (int) event.getRawX();
                        upY = (int) event.getRawY();
                        DisplayMetrics dm = getResources().getDisplayMetrics();
                        tempX = Math.min(tempX, dm.widthPixels - tv.getWidth());
                        tempY = Math.min(tempY, dm.heightPixels - tv.getHeight());
                        Log.d("ACTION_DOWN", "downX:" + upX + " downY:" + upY);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        // 与上一次位置相差不到5则不移动
//                        if (event.getRawX() - upX > 5 || event.getRawY() - upY > 5) {
                        x = params.x = ((int) event.getRawX()) + diffX;
                        y = params.y = ((int) event.getRawY()) + diffY;
                        wm.updateViewLayout(tv, params);
//                        Log.d("ACTION_MOVE", x + "\t" + y);
//                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        // 相差不到5则代表点击
//                        if (event.getRawX() - upX < 5 && event.getRawY() - upY < 5) {
                        long time2 = System.currentTimeMillis();
                        if (time2 - time < 250) stopSelf();
                        time = time2;
//                        }
                        tempX = Math.max((int) event.getRawX() + diffX, 0);
                        tempY = Math.max((int) event.getRawY() + diffY, 0);
                        Log.d("ACTION_UP", "upX:" + tempX + " upY:" + tempY);
                        break;
                }
                return false;
            }
        });

        // 添加进WindowManager
        wm.addView(tv, params);

        return super.onStartCommand(intent, flags, startId);
    }

    @Override

    public void onDestroy() {
        thread.interrupt();
        wm.removeView(tv);
        saw = false;
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
