package com.mqtt;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

public class MyServiceConnection implements ServiceConnection {

    public final   String             TAG            = MyServiceConnection.class.getSimpleName();
    private MyMqttService mqttService;
    private IGetMessageCallBack iGetMessageCallBack;

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mqttService = ((MyMqttService.CustomBinder)service).getService();
        mqttService.setIGetMessageCallBack(iGetMessageCallBack);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        Log.i(TAG, "Service Disconnected");
    }

    public MyMqttService getMqttService() {
        return mqttService;
    }

    public void publishMessage(String str) {
        mqttService.publish(str);
    }

    public void setIGetMessageCallBack(IGetMessageCallBack iGetMessageCallBack) {
        this.iGetMessageCallBack = iGetMessageCallBack;
    }
}
