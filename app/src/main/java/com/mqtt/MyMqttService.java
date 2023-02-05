package com.mqtt;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

//import androidx.annotation.IntDef;
//import androidx.annotation.WorkerThread;
//import android.support.annotation.Nullable;
import androidx.annotation.Nullable;
import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * Author       wildma
 * Github       https://github.com/wildma
 * CreateDate   2018/11/08
 * Desc	        ${MQTT服务}
 */

public class MyMqttService extends Service {

    public final   String             TAG            = MyMqttService.class.getSimpleName();
    private        MqttAndroidClient  mqttAndroidClient;
    private        MqttConnectOptions mMqttConnectOptions;
    private        IGetMessageCallBack mIGetMessageCallBack;
//    public         String             HOST           = "tcp://47.117.80.32:1883";//服务器地址（协议+地址+端口号）
//    public         String             USERNAME       = "admin";//用户名
//    public         String             PASSWORD       = "password";//密码

    public         String             HOST           = "tcp://192.168.1.6:1883";//服务器地址（协议+地址+端口号）
    public         String             USERNAME       = "admin";//用户名
    public         String             PASSWORD       = "******";//密码

    public static  String             PUBLISH_TOPIC  = "uav_tag/867387060317453/pub";//发布主题，上行
    public static  String             RESPONSE_TOPIC = "uav_tag/867387060317453/sub";//响应主题，下行
    @SuppressLint("MissingPermission")
    //客户端ID，一般以客户端唯一标识符表示，这里用设备序列号表示
    public         String             CLIENT_ID       = "SN123456"; //Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? Build.getSerial() : Build.SERIAL;


    @Override
    public void onCreate() {
        super.onCreate();
        init();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new CustomBinder();
    }

    public class CustomBinder extends Binder {
        public MyMqttService getService(){
            return MyMqttService.this;
        }
    }

    public void setIGetMessageCallBack(IGetMessageCallBack iGetMessageCallBack) {
        this.mIGetMessageCallBack = iGetMessageCallBack;
    }

    /**
     * 发布 （模拟其他客户端发布消息）
     *
     * @param message 消息
     */
    public void publish(String message) {
        String topic = PUBLISH_TOPIC;
        int qos = 2;
        boolean retained = false;
        try {
            //参数分别为：主题、消息的字节数组、服务质量、是否在服务器保留断开连接后的最后一条消息
            this.mqttAndroidClient.publish(topic, message.getBytes(), qos, retained);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    /**
     * 响应 （收到其他客户端的消息后，响应给对方告知消息已到达或者消息有问题等）
     *
     * @param message 消息
     */
    public void response(String message) {
        String topic = RESPONSE_TOPIC;
        int qos = 2;
        boolean retained = false;
        try {
            //参数分别为：主题、消息的字节数组、服务质量、是否在服务器保留断开连接后的最后一条消息
            mqttAndroidClient.publish(topic, message.getBytes(), qos, retained);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    /**
     * 初始化
     */
    private void init() {
        String serverURI = HOST; //服务器地址（协议+地址+端口号）
        mqttAndroidClient = new MqttAndroidClient(this, serverURI, CLIENT_ID);
        mqttAndroidClient.setCallback(mqttCallback); //设置监听订阅消息的回调
        mMqttConnectOptions = new MqttConnectOptions();
        mMqttConnectOptions.setCleanSession(true); //设置是否清除缓存
        mMqttConnectOptions.setConnectionTimeout(10); //设置超时时间，单位：秒
        mMqttConnectOptions.setKeepAliveInterval(20); //设置心跳包发送间隔，单位：秒
        mMqttConnectOptions.setUserName(USERNAME); //设置用户名
        mMqttConnectOptions.setPassword(PASSWORD.toCharArray()); //设置密码

        // last will message
        boolean doConnect = true;
        String message = "{\"terminal_uid\":\"" + CLIENT_ID + "\"}";
        String topic = PUBLISH_TOPIC;
        int qos = 2;
        boolean retained = false;
        if ((!message.equals("")) || (!topic.equals(""))) {
            // 最后的遗嘱
            try {
                mMqttConnectOptions.setWill(topic, message.getBytes(), qos, retained);
            } catch (Exception e) {
                Log.i(TAG, "Exception Occured", e);
                doConnect = false;
                iMqttActionListener.onFailure(null, e);
            }
        }
        if (doConnect) {
            doClientConnection();
        }
        Log.i(TAG, "MQTT init finished");
    }

    /**
     * 连接MQTT服务器
     */
    private void doClientConnection() {
        if (!mqttAndroidClient.isConnected() && isConnectIsNomarl()) {
            try {
                mqttAndroidClient.connect(mMqttConnectOptions, null, iMqttActionListener);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 判断网络是否连接
     */
    private boolean isConnectIsNomarl() {
        ConnectivityManager connectivityManager = (ConnectivityManager) this.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = connectivityManager.getActiveNetworkInfo();
        if (info != null && info.isAvailable()) {
            String name = info.getTypeName();
            Log.i(TAG, "当前网络名称：" + name);
            return true;
        } else {
            Log.i(TAG, "没有可用网络");
            /*没有可用网络的时候，延迟3秒再尝试重连*/
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    doClientConnection();
                }
            }, 3000);
            return false;
        }
    }

    //MQTT是否连接成功的监听
    private IMqttActionListener iMqttActionListener = new IMqttActionListener() {

        @Override
        public void onSuccess(IMqttToken arg0) {
            Log.i(TAG, "连接成功 ");
            try {
                mqttAndroidClient.subscribe(PUBLISH_TOPIC, 2);//订阅主题，参数：主题、服务质量
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onFailure(IMqttToken arg0, Throwable arg1) {
            arg1.printStackTrace();
            Log.i(TAG, "连接失败 ");
            doClientConnection();//连接失败，重连（可关闭服务器进行模拟）
        }
    };

    //订阅主题的回调
    private MqttCallback mqttCallback = new MqttCallback() {

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            String str = new String(message.getPayload());
            Log.i(TAG, "收到消息： " + str);
            //收到消息，这里弹出Toast表示。如果需要更新UI，可以使用广播或者EventBus进行发送
            Toast.makeText(getApplicationContext(), "messageArrived: " + new String(message.getPayload()), Toast.LENGTH_LONG).show();
            //收到其他客户端的消息后，响应给对方告知消息已到达或者消息有问题等，
            //response("message arrived");

            if (mIGetMessageCallBack != null){
                mIGetMessageCallBack.setMessage(str);
            }
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken arg0) {
            Log.i(TAG, "deliveryComplete： ");
        }

        @Override
        public void connectionLost(Throwable arg0) {
            Log.i(TAG, "连接断开 ");
            doClientConnection();//连接断开，重连
        }
    };

    @Override
    public void onDestroy() {
        try {
            mqttAndroidClient.disconnect(); //断开连接
        } catch (MqttException e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }
}
