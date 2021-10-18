package com.example.simpleuvccamera;

import androidx.appcompat.app.AppCompatActivity;

import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;
import android.widget.TextView;

import com.serenegiant.usb.DeviceFilter;
import com.serenegiant.usb.IFrameCallback;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.UVCCamera;

import java.nio.ByteBuffer;
import java.util.List;

public class MainActivity extends AppCompatActivity implements IFrameCallback {

    private USBMonitor mUSBMonitor;
    private UVCCamera mUVCCamera;
    public static final String TAG = "MainActivity";
    private TextureView textureView;
    private TextView frameNumbView;
    int frameNB = 0;
    private boolean mStart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textureView = findViewById(R.id.texture);
        frameNumbView = findViewById(R.id.tv_video_frames_nb);
        mUSBMonitor = new USBMonitor(this, new SimpleUSBMonitorListener(){
            @Override
            public void onAttach(UsbDevice device) {
                Log.i(TAG,"onAttach:" + device.getDeviceName());
                List<UsbDevice> devices = mUSBMonitor.getDeviceList();
                if (devices.contains(device)) {
                    Log.i(TAG,"device list contains device "+device+". requestPermission.");
                    mUSBMonitor.requestPermission(device);
                } else {
                    Log.i(TAG,"device list not contains device :"+device);
                }
            }

            @Override
            public void onDettach(UsbDevice device) {
                if (mUVCCamera != null) mUVCCamera.destroy();
                mUVCCamera = null;
            }

            @Override
            public void onConnect(UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock, boolean createNew) {
                try {
                    mStart = true;
                    runOnUiThread(() -> frameNumbView.setText("preview start..."));

                    List<UsbDevice> devices = mUSBMonitor.getDeviceList();
                    if (devices.contains(device)) {
                    } else {
                        Log.i(TAG,"device list not contains device " + device);
                        return;
                    }
                    final UVCCamera camera = new UVCCamera();
                    camera.open(ctrlBlock);
					mUVCCamera = camera;
					if (textureView.isAvailable()){
					    mUVCCamera.setPreviewTexture(textureView.getSurfaceTexture());
                    }
					mUVCCamera.setPreviewSize(1280,720,UVCCamera.PIXEL_FORMAT_YUV420SP);
                    mUVCCamera.setFrameCallback(MainActivity.this, UVCCamera.PIXEL_FORMAT_YUV420SP);
                    mUVCCamera.startPreview();
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onDisconnect(UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock) {
                super.onDisconnect(device, ctrlBlock);
                mStart = false;
                runOnUiThread(() -> frameNumbView.setText("preview stop"));
            }
        });

        mUSBMonitor.setDeviceFilter(DeviceFilter.getDeviceFilters(this, R.xml.device_filter));
        mUSBMonitor.register();
    }


    @Override
    protected void onDestroy() {
        mUSBMonitor.unregister();
        mUSBMonitor.destroy();
        super.onDestroy();
    }

    @Override
    public void onFrame(ByteBuffer frame) {
        frameNB++;
        if (mStart)
            runOnUiThread(() -> frameNumbView.setText("recv " + frameNB + " frames"));
    }
}