package com.example.anderson.bluetooth;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.inputmethodservice.InputMethodService;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AndroidException;
import android.util.Base64;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import retrofit2.Call;

public class Register extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2{

    private JavaCameraView javaCameraView;
    private Mat mRgba, mGray, mCrop, mFace;
    private MatOfRect faces = new MatOfRect();
    private int absoluteFaceSize;
    private CascadeClassifier cascadeFace;
    private File mCascadeFile;
    private boolean upload = false;
    private Bitmap bmp;
    private String name, email, phone, response;
    private EditText inputName, inputEmail, inputPhone;
    private Button btCancel, btSend;
    private ProgressBar progressBar;
    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        javaCameraView = (JavaCameraView) findViewById(R.id.reg_camera_viewer);
        javaCameraView.setVisibility(SurfaceView.VISIBLE);
        javaCameraView.setCvCameraViewListener(this);
        haarCascadeHandler();
        btHandler();
        editTxtHandler();
    }

    private void editTxtHandler()
    {
        inputName = (EditText) findViewById(R.id.editTextName);
        inputEmail = (EditText) findViewById(R.id.editTextEmail);
        inputPhone = (EditText) findViewById(R.id.editTextPhone);

        inputName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if(s.length() >= 3 && s.length() <=10)
                {
                    btSend.setTextColor(Color.parseColor("#000000"));
                    btSend.setEnabled(true);
                }
                else
                {
                    btSend.setTextColor(Color.parseColor("#A9A9A9"));
                    btSend.setEnabled(false);
                }

            }
        });
    }

    private void progressBarHandler()
    {
        progressBar = (ProgressBar)findViewById(R.id.Load_bar);
        progressBar.setVisibility(View.VISIBLE);
    }

    private void textHandler()
    {
        textView = (TextView) findViewById(R.id.Server_log);
        textView.setVisibility(View.VISIBLE);
    }

    private void haarCascadeHandler()
    {
        try
        {
            InputStream is2 = getResources().openRawResource(R.raw.haarcascade_frontalface_alt);
            File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
            mCascadeFile = new File(cascadeDir, "haarcascade_frontalface_alt.xml");
            FileOutputStream os2 = new FileOutputStream(mCascadeFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is2.read(buffer)) != -1)
            {
                os2.write(buffer, 0, bytesRead);
            }
            is2.close();
            os2.close();

            cascadeFace = new CascadeClassifier(mCascadeFile.getAbsolutePath());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private BaseLoaderCallback baseLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status)
            {
                case BaseLoaderCallback.SUCCESS:{
                    javaCameraView.enableView();
                    break;
                }
                default:{
                    super.onManagerConnected(status);
                    break;
                }
            }

        }
    };

    private void btHandler()
    {
        btCancel = (Button) findViewById(R.id.bt_Cancel);
        btSend = (Button) findViewById(R.id.bt_Send);
        btSend.setEnabled(false);


        btCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);

                name = inputName.getText().toString();
                email = inputEmail.getText().toString();
                phone = inputPhone.getText().toString();

                btSend.setTextColor(Color.parseColor("#A9A9A9"));
                btSend.setEnabled(false);

                progressBarHandler();
                textHandler();

                upload = true;
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(javaCameraView!=null)
            javaCameraView.disableView();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        if(javaCameraView!=null)
            javaCameraView.disableView();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        if(OpenCVLoader.initDebug())
        {
            baseLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);

        }
        else
        {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_11, this, baseLoaderCallback);
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height)
    {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mGray = new Mat(height, width, CvType.CV_8UC1);
        absoluteFaceSize = (int) (height * 0.2);

    }

    @Override
    public void onCameraViewStopped()
    {
        mRgba.release();
        mGray.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame)
    {
        mCrop = null;
        mFace = null;
        mRgba = inputFrame.rgba();
        Imgproc.cvtColor(mRgba, mGray, Imgproc.COLOR_RGBA2GRAY);

        if(cascadeFace != null)
        {
            cascadeFace.detectMultiScale(mGray, faces, 1.1,2,2,
                    new Size(absoluteFaceSize, absoluteFaceSize), new Size());
        }

        if(faces.toArray().length > 0)
        {
            Rect [] rects = faces.toArray();
            Rect rect = rects[0];

            mCrop = new Mat(mRgba, rect);
            mFace = new Mat(mGray, rect);

            Imgproc.cvtColor(mCrop, mCrop, Imgproc.COLOR_RGBA2RGB);
            Imgproc.resize(mFace, mFace, new Size(96,96));
            Imgproc.resize(mCrop,mCrop,new Size(mRgba.width(),mRgba.height()));

            if(upload)
            {
                bmp = Bitmap.createBitmap(mFace.cols(), mFace.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(mFace, bmp);
                try {
                    response = updateImage(imageToString(bmp), name, phone, email);
                    if(response.equals("FINALIZADO"))
                    {
                        upload = false;
                        progressBar.setVisibility(View.INVISIBLE);
                        textView.setVisibility(View.INVISIBLE);
                        finish();
                    }
                    else
                    {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                textView.setText(response);
                            }
                        });

                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        if(mCrop != null)
            return mCrop;
        return mRgba;
    }

    private String updateImage(String image) throws IOException
    {
        ApiInterface apiInterface = ApiClient.getApiClient().create(ApiInterface.class);
        Call<ImageClass> call = apiInterface.uploadImage(image);
        ImageClass Im = call.execute().body();
        return Im.getResponse();
    }

    private String updateImage(String image, String name, String phone, String email) throws IOException
    {
        ApiInterface apiInterface = ApiClient.getApiClient().create(ApiInterface.class);
        Call<ImageClass> call = apiInterface.uploadImageReg(image, name, phone, email);
        ImageClass Im = call.execute().body();
        return Im.getResponse();
    }

    private String imageToString(Bitmap bitmap)
    {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG,100,byteArrayOutputStream);
        byte[] imgByte = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(imgByte, Base64.DEFAULT);
    }

}
