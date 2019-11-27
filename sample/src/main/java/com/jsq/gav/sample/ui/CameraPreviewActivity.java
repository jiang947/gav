//package com.jsq.video.simple.ui;
//
//import android.content.Context;
//import android.content.res.AssetManager;
//import android.hardware.Camera;
//import android.os.Bundle;
//import android.util.Log;
//
//import androidx.appcompat.app.AppCompatActivity;
//
//import com.jsq.pfd.Accelerometer;
//import com.jsq.video.simple.R;
//import com.jsq.video.simple.filter.MultiLightGiftFilter;
//import com.jsq.video.widget.CameraPreviewView;
//import com.lidx.pet.DetAlinNet;
//import com.lightgift.giftrenderer.LightGiftRenderer;
//import com.lightgift.giftrenderer.render.IEventListener;
//
//import java.io.File;
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.OutputStream;
//
//public class CameraPreviewActivity extends AppCompatActivity {
//
//    private CameraPreviewView mCameraPreviewView;
//    private DetAlinNet mPredictor;
//    private Accelerometer mAccelerometer;
//    private float[] mFaceInfos = null;
//    private int mImageWidth = 720;
//    private int mImageHeight = 1280;
//    //"640x480", "1280x720"
//
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_camera_preview);
//        mCameraPreviewView = findViewById(R.id.camera_preview_view);
//        mCameraPreviewView.setAspectRatio(720, 1280);
//        copyAssets(this, "cat_models");
//        mAccelerometer = new Accelerometer(this);
//        mAccelerometer.start();
//        mPredictor = new DetAlinNet();
//        String modelPath = getFilesDir().getAbsolutePath();
//
//        mPredictor.modelInit(modelPath);
//        mPredictor.setMaxFaces(0);
////        mPredictor.setDetType(2);
//        LightGiftRenderer.setAuthEventListener(
//                new IEventListener() {
//                    @Override
//                    public int onEvent(int i, int i1, String s) {
//                        Log.e("", "MSG(type/ret/info):" + i + "/" + i1 + "/" + s);
//                        return 0;
//                    }
//                });
//
//        int id = LightGiftRenderer.auth(
//                this,
//                "UNt+gaVMRxqqOFqMKM1B1u1FwLCDP3u7Rs6KJsoBE7T1C9RVriQ10a4MJA1ov0Tb",
//                48);
//        MultiLightGiftFilter filter = new MultiLightGiftFilter(this);
//        mCameraPreviewView.addFilter(filter);
//        mCameraPreviewView.setPreviewCallback(new Camera.PreviewCallback() {
//            @Override
//            public void onPreviewFrame(byte[] data, Camera camera) {
//                int direction = Accelerometer.getDirection();
//                byte[] imageData = new byte[mImageWidth * mImageHeight];
//                System.arraycopy(data, 0, imageData, 0, data.length * 2 / 3);
//                mFaceInfos = mPredictor.process(imageData, mImageHeight, mImageWidth, direction);
//                filter.setFaceInfos(mFaceInfos);
//            }
//        });
//
//        findViewById(R.id.toggle_camera).setOnClickListener(v -> mCameraPreviewView.toggleCamera());
//    }
//
//    @Override
//    protected void onPause() {
//        super.onPause();
//        mCameraPreviewView.onPause();
//    }
//
//    @Override
//    protected void onResume() {
//        super.onResume();
//        mCameraPreviewView.onResume();
//    }
//
//    private void copyAssets(Context context, String folder) {
//        AssetManager assetManager = context.getAssets();
//        String[] files = null;
//        try {
//            files = assetManager.list(folder);
//        } catch (IOException e) {
//            Log.e("tag", "Failed to get asset file list.", e);
//        }
//        File resourceDirectory = new File(context.getFilesDir().getAbsolutePath() + '/' + folder);
//        resourceDirectory.mkdirs();
//        if (files != null) for (String filename : files) {
//            InputStream in = null;
//            OutputStream out = null;
//            try {
//                in = assetManager.open(folder + '/' + filename);
//                File outFile = new File(resourceDirectory, filename);
//                out = new FileOutputStream(outFile);
//                copyFile(in, out);
//            } catch (IOException e) {
//                Log.e("tag", "Failed to copy asset file: " + filename, e);
//            } finally {
//                if (in != null) {
//                    try {
//                        in.close();
//                    } catch (IOException e) {
//                        // NOOP
//                    }
//                }
//                if (out != null) {
//                    try {
//                        out.close();
//                    } catch (IOException e) {
//                        // NOOP
//                    }
//                }
//            }
//        }
//    }
//
//    private void copyFile(InputStream in, OutputStream out) throws IOException {
//        byte[] buffer = new byte[1024];
//        int read;
//        while ((read = in.read(buffer)) != -1) {
//            out.write(buffer, 0, read);
//        }
//    }
//
//
//}
