package com.baidu.track.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Looper;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.baidu.track.R;
import com.baidu.track.TrackApplication;
import com.baidu.track.utils.BitmapUtil;
import com.baidu.track.utils.CommonUtil;
import com.baidu.track.utils.Constants;
import com.baidu.track.utils.SharedPreferenceUtil;

/**
 * 启动页
 */
public class SplashActivity extends BaseActivity {

    private static final String TAG = SplashActivity.class.getSimpleName();

    private TrackApplication trackApp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
        init();
        //初始化地图上使用的点的坐标显示
        BitmapUtil.init();

        new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(Constants.SPLASH_TIME);
                } catch (InterruptedException e) {
                    Log.e(TAG, "thread sleep failed");
                } finally {
                    turnToMain();
                }
            }
        }.start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        finish();
    }

    private void turnToMain() {
        Intent intent = new Intent(this, TrackQueryActivity.class);
        startActivity(intent);
        //从Splash到MainActivity的切换方式，第一个参数表示MainActivity的进入方式，从右进入，keep
        overridePendingTransition(R.anim.in_from_right, R.anim.keep);
        //        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    @Override
    protected int getContentViewId() {
        return R.layout.activity_splash;
    }

    private void init() {
        trackApp = (TrackApplication) getApplicationContext();
        boolean isContainsPermissionKey = SharedPreferenceUtil.contains(this, Constants.PERMISSIONS_DESC_KEY);
        if (!isContainsPermissionKey) {
            showDialog();
        } else {
            boolean isAccessPermission = SharedPreferenceUtil
                    .getBoolean(this, Constants.PERMISSIONS_DESC_KEY, false);
            if (!isAccessPermission) {
                showDialog();
            }
        }
    }

    /**
     * 显示提示信息
     */
    private void showDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("提示: ");
        TextView textView = new TextView(this);
        textView.setText(R.string.privacy_permission_desc);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        textView.setPadding( 50 , 30 , 50 , 10 );
        builder.setView(textView);

        builder.setPositiveButton("同意", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SharedPreferenceUtil
                        .putBoolean(SplashActivity.this, Constants.PERMISSIONS_DESC_KEY, true);
                dialog.cancel();
            }
        });

        builder.setNegativeButton("不同意", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SharedPreferenceUtil
                        .putBoolean(SplashActivity.this, Constants.PERMISSIONS_DESC_KEY, false);
                dialog.cancel();
            }
        });
        builder.setCancelable(false);
        builder.show();
    }
}
