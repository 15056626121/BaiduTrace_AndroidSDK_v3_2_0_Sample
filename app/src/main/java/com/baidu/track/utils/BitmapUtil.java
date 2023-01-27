package com.baidu.track.utils;

import android.content.Context;
import android.content.res.Resources;

import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.track.R;

/**
 * Created by baidu on 17/3/8.
 */

public class BitmapUtil {

    public static BitmapDescriptor bmArrowPoint = null;

    public static BitmapDescriptor bmStart = null;

    public static BitmapDescriptor bmEnd = null;

    public static BitmapDescriptor bmGeo = null;

    public static BitmapDescriptor bmGcoding = null;

    public static BitmapDescriptor bmCs = null;

    public static BitmapDescriptor bmJsc = null;

    public static BitmapDescriptor bmJzw = null;

    public static BitmapDescriptor bmStay = null;

    /**
     * 创建bitmap，在MainActivity onCreate()中调用
     */
    public static void init() {
        bmArrowPoint = BitmapDescriptorFactory.fromResource(R.mipmap.icon_point);
        bmStart = BitmapDescriptorFactory.fromResource(R.mipmap.icon_start);
        bmEnd = BitmapDescriptorFactory.fromResource(R.mipmap.icon_end);
        bmGeo = BitmapDescriptorFactory.fromResource(R.mipmap.icon_geo);
        bmGcoding = BitmapDescriptorFactory.fromResource(R.mipmap.icon_gcoding);
        bmCs = BitmapDescriptorFactory.fromResource(R.drawable.icon_sc);
        bmJsc = BitmapDescriptorFactory.fromResource(R.drawable.icon_jsc);
        bmJzw = BitmapDescriptorFactory.fromResource(R.drawable.icon_jzw);
        bmStay = BitmapDescriptorFactory.fromResource(R.drawable.icon_stay);
    }

    /**
     * 回收bitmap，在MainActivity onDestroy()中调用
     */
    public static void clear() {
        bmArrowPoint.recycle();
        bmStart.recycle();
        bmEnd.recycle();
        bmGeo.recycle();
        bmCs.recycle();
        bmJsc.recycle();
        bmJzw.recycle();
        bmStay.recycle();
    }

    public static BitmapDescriptor getMark(Context context, int index) {
        Resources res = context.getResources();
        int resourceId;
        if (index <= 10) {
            resourceId = res.getIdentifier("icon_mark" + index, "mipmap", context.getPackageName());
        } else {
            resourceId = res.getIdentifier("icon_markx", "mipmap", context.getPackageName());
        }
        return BitmapDescriptorFactory.fromResource(resourceId);
    }
}
