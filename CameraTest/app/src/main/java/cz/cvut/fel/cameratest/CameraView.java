package cz.cvut.fel.cameratest;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.util.Log;
import android.util.Size;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import java.io.IOException;

public class CameraView extends SurfaceView implements SurfaceHolder.Callback {
    private SurfaceHolder surface;
    private Camera kamera;
    private boolean previewRunning = false;

    public void snimek() {
        previewRunning = false;
    }

    public boolean beziPreview() {
        return previewRunning;
    }

    public void start() {
        try {
            kamera.setPreviewDisplay(surface);
            kamera.startPreview();
            previewRunning = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public CameraView(Context context, Camera camera){
        super(context);
        kamera = camera;
        kamera.setDisplayOrientation(90);
        surface = getHolder();
        surface.addCallback(this);
        surface.setType(SurfaceHolder.SURFACE_TYPE_NORMAL);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        try{
            kamera.setPreviewDisplay(surfaceHolder); //kam se má obraz z kamery vykreslovat
            kamera.startPreview(); //začít vykreslovat obraz z kamery
            previewRunning = true;
        } catch (IOException e) {
            Log.d("ERROR", "Camera error on surfaceCreated " + e.getMessage());
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i2, int i3) {
        //při otočení orientace obnovit obraz
        if (surface.getSurface() == null)//je surface připraven přijmout data?
            return;

        try {
            kamera.stopPreview();
        } catch (Exception e) {

        }
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            kamera.setDisplayOrientation(90); // Portrait Mode
        } else
        kamera.setDisplayOrientation(0);

        try{
            kamera.setPreviewDisplay(surface);
            kamera.startPreview();
            previewRunning = true;
        } catch (IOException e) {
            Log.d("ERROR", "Chyba kamery - surfaceChanged " + e.getMessage());
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        //pokud se zruší okno surface, vypnout kameru
        kamera.stopPreview();
        kamera.release();
    }

}
