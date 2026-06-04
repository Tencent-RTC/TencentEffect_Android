package rtc.volcengine.apiexample.examples.ThirdBeauty.xmagic.adapter.impl;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;



import org.light.utils.LightLogUtil;

import rtc.volcengine.apiexample.examples.ThirdBeauty.xmagic.adapter.api.DeviceDirection;

public class TESensorManager implements SensorEventListener {

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Context mApplicationContext = null;
    private EventListener mEventListener = null;


    public TESensorManager(Context context) {
        this.mApplicationContext = context.getApplicationContext();
        this.mSensorManager = (SensorManager) mApplicationContext.getSystemService(Context.SENSOR_SERVICE);
        this.mAccelerometer = this.mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    public void setEventListener(EventListener listener){
        this.mEventListener = listener;
    }

    public void start() {
        this.mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void stop() {
        this.mSensorManager.unregisterListener(this);
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        this.dispatchOrientation(event, mAccelerometer);
    }


    protected void dispatchOrientation(SensorEvent event, Sensor accelerometer) {
        int orientation = this.mApplicationContext.getResources().getConfiguration().orientation;
        DeviceDirection deviceDirection = DeviceDirection.PORTRAIT_UP;
        if (event.sensor == accelerometer) {
            float currentXAxis = event.values[0];
            float currentYAxis = event.values[1];
            float currentZAxis = event.values[2];
            // 检测手机是否水平朝上
            if (Math.abs(currentXAxis) < 1 && Math.abs(currentYAxis) < 1 && currentZAxis > 9) { // 手机水平放置朝上
                deviceDirection = DeviceDirection.HORIZONTAL_UP;
                LightLogUtil.d("dispatchOrientation", "HORIZONTAL_UP");
            } else if (Math.abs(currentXAxis) < 1 && Math.abs(currentYAxis) < 1 && currentZAxis < -9) { // 手机水平放置朝下
                deviceDirection = DeviceDirection.HORIZONTAL_DOWN;
                LightLogUtil.d("dispatchOrientation", "HORIZONTAL_DOWN");
            } else {  // 手机非水平放置
                LightLogUtil.d("dispatchOrientation", "not HORIZONTAL");
                if (Math.abs(currentYAxis) > Math.abs(currentXAxis)) {
                    if (currentYAxis > 1) {
                        deviceDirection = DeviceDirection.PORTRAIT_UP;
                    } else if (currentYAxis < -1) {
                        deviceDirection = DeviceDirection.PORTRAIT_DOWN;
                    }
                } else {
                    if (currentXAxis > 1) {
                        deviceDirection = DeviceDirection.LANDSCAPE_LEFT;
                    } else if (currentXAxis < -1) {
                        deviceDirection = DeviceDirection.LANDSCAPE_RIGHT;
                    }
                }
            }
        }
        if (this.mEventListener != null) {
            this.mEventListener.onDeviceDirectionChanged(orientation, deviceDirection);
        }
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


    public interface EventListener {

        /**
         * @param orientation     Overall orientation of the screen.  May be one of
         *                        * {@link # Configuration.ORIENTATION_LANDSCAPE}, {@link # Configuration.ORIENTATION_PORTRAIT}.
         * @param deviceDirection
         */
        void onDeviceDirectionChanged(int orientation, DeviceDirection deviceDirection);
    }
}
