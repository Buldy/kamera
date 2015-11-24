package cz.cvut.fel.cameratest;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainCamera extends AppCompatActivity {

    private Camera kamera = null;
    private CameraView oknoKamery = null;
    private Camera.Parameters param;
    private int quality = 100;
    private int pocetSnimku = 1;
    private int snimek = 0;
    float rotX;
    float rotY;
    float rotZ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_camera);

        try {
            kamera = Camera.open();//inicializace kamery
        } catch (Exception e) {
            Log.d("ERROR", "Failed to get camera: " + e.getMessage());
        }

        List<Camera.Size> size;
        param = kamera.getParameters();
        size = param.getSupportedPictureSizes();
        String[] sizes = new String[size.size()];
        for (int i = 0; i < size.size(); i++) {
            sizes[i] = Integer.toString(size.get(i).width) + "x" + Integer.toString(size.get(i).height);
        }
        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, sizes);
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Spinner lstResolution = (Spinner) findViewById(R.id.lstResolutions);
        lstResolution.setAdapter(spinnerArrayAdapter);

        final TextView txtQuality = (TextView) findViewById(R.id.txtQuality);
        SeekBar seekBar = (SeekBar) findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                txtQuality.setText(Integer.toString(progress + 1));
                quality = progress + 1;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    public void btnStartClick(View view) {
        if (kamera != null) {
            param = kamera.getParameters();

            int camWidth;
            int camHeight;
            Spinner lstResolution = (Spinner) findViewById(R.id.lstResolutions);
            String[] widthHeight = lstResolution.getSelectedItem().toString().split("x");
            camWidth = Integer.parseInt(widthHeight[0]);
            camHeight = Integer.parseInt(widthHeight[1]);
            param.setPictureSize(camWidth, camHeight);

            param.setRotation(90);
            param.setJpegQuality(quality);
            kamera.setParameters(param);

            oknoKamery = new CameraView(this, kamera);//vytvořit okno s obrazem z kamery
            FrameLayout camera_view = (FrameLayout) findViewById(R.id.camera_view);//najít základní okno programu
            camera_view.addView(oknoKamery);//přidat okno do programu
        }
        Button btnStart = (Button) findViewById(R.id.btnStart);
        btnStart.setEnabled(false);
        Spinner lstResolution = (Spinner) findViewById(R.id.lstResolutions);
        lstResolution.setEnabled(false);
        Button btnTakePicture = (Button) findViewById(R.id.btnTakePicture);
        btnTakePicture.setEnabled(true);
        EditText txtPocetSnimku = (EditText) findViewById(R.id.txtPocetSnimku);
        pocetSnimku = Integer.parseInt(txtPocetSnimku.getText().toString());
    }

    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            File pictureFile = ulozitObrazek();
            if (pictureFile == null) {
                return;
            }

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            oknoKamery.start();
        }
    };

    /**
     * Used to return the camera File output.
     *
     * @return
     */
    private File ulozitObrazek() {

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "Test");

        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                "IMG_" + timeStamp + ".jpg");

        return mediaFile;
    }

    public void btnTakePictureClick(View view) {
        while (snimek < pocetSnimku) {
            if (oknoKamery.beziPreview()) {
                kamera.takePicture(null, null, mPicture);
                oknoKamery.snimek();
                snimek++;
            }
        }
    }

}
