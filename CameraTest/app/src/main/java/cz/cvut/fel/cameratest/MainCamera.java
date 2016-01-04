package cz.cvut.fel.cameratest;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class MainCamera extends AppCompatActivity {

    private Camera kamera = null;
    private CameraView oknoKamery = null;
    private Camera.Parameters param;
    private int quality = 100;
    private int pocetSnimku = 1;
    private int snimek = 0;
    SensorManager sMgr;
    Sensor natoceni;
    float rotX;
    float rotY;
    float rotZ;
    String mediaDir;

    int camWidth;
    int camHeight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_camera);

        OpenCVLoader.initDebug();

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
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "Test");

        if (!mediaStorageDir.exists()) {
           if (!mediaStorageDir.mkdirs()) {

            }
        }
        mediaDir = mediaStorageDir.getPath();

        if (kamera != null) {
            param = kamera.getParameters();

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

        sMgr = (SensorManager)this.getSystemService(SENSOR_SERVICE);
        natoceni = sMgr.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        SensorEventListener eventNatoceni = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                rotX = event.values[0];
                rotY = event.values[1];
                rotZ = event.values[2];
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };

        sMgr.registerListener(eventNatoceni, natoceni, SensorManager.SENSOR_DELAY_NORMAL);
    }

    private Camera.PictureCallback keypoints = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            kamera.startPreview();
            Mat obrazek;
            Mat grayMat;
            MatOfKeyPoint keyPoints = new MatOfKeyPoint();
            FeatureDetector fDetector = FeatureDetector.create(FeatureDetector.FAST);

            Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
            obrazek = new Mat(bmp.getHeight(), bmp.getWidth(), CvType.CV_8UC3);
            Bitmap bmp32 = bmp.copy(Bitmap.Config.RGB_565, true);
            Utils.bitmapToMat(bmp32, obrazek);

            grayMat = new Mat(obrazek.height(), obrazek.width(), CvType.CV_8UC1);
            Imgproc.cvtColor(obrazek, grayMat, Imgproc.COLOR_RGB2GRAY);
            fDetector.detect(grayMat, keyPoints);
            Features2d.drawKeypoints(grayMat, keyPoints, obrazek);
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            Imgcodecs.imwrite(mediaDir + File.separator + "KEYPOINTS_GRAY_" + timeStamp + ".BMP", obrazek);

            fDetector.detect(obrazek, keyPoints);
            Features2d.drawKeypoints(obrazek, keyPoints, obrazek);
            timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            Imgcodecs.imwrite(mediaDir + File.separator + "KEYPOINTS_" + timeStamp + ".BMP", obrazek);

            Toast.makeText(getApplicationContext(), "Keypoints saved", Toast.LENGTH_LONG).show();
        }
    };

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
            kamera.startPreview();
            snimek++;
            try {
                File outFile = new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES) + File.separator + "Test" + File.separator + "dataNatoceni.txt");
                Writer output = new BufferedWriter(new FileWriter(outFile, true));
                output.append(pictureFile.toString() + "," + Float.toString(rotX) + "," + Float.toString(rotY) + "," + Float.toString(rotZ) + "\n");
                output.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (snimek < pocetSnimku){
                kamera.takePicture(null, null, mPicture);
            }
        }
    };

    /**
     * Used to return the camera File output.
     *
     * @return
     */
    private File ulozitObrazek() {



        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        mediaFile = new File(mediaDir + File.separator +
            "IMG_" + timeStamp + ".jpg");

        return mediaFile;
    }

    public void btnTakePictureClick(View view) {
        //kamera.takePicture(null, null, mPicture);
        kamera.takePicture(null, null, keypoints);
    }

}
