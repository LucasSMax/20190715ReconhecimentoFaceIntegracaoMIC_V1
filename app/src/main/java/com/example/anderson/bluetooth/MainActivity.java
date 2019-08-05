package com.example.anderson.bluetooth;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.AnimationDrawable;
import android.speech.RecognizerIntent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.bluetooth.BluetoothAdapter;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonElement;

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
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

import ai.api.android.AIConfiguration;
import ai.api.android.AIService;
import ai.api.AIListener;
import ai.api.model.AIError;
import ai.api.model.AIResponse;
import ai.api.model.Metadata;
import ai.api.model.Result;
import ai.api.model.Status;
import retrofit2.Call;



public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2, AIListener {


    private TextView vozTexto;
    private String resposta = "dialog";

    private static final int SOLICITA_ATIVACAO = 1;
    private static final int SOLICITA_CONEXAO = 2;
    private static final int SOLICITA_DESCOBERTA_BT = 3;
    private final int REQ_CODE_SPEECH_OUTPUT = 143;
    private final int BT_BLUETOOTH = 4;

    private static Mat mRgba, mGray, mCrop;
    private int absoluteFaceSize;
    private boolean recognize = false, falou = false;
    private static String txtclassifica;
    private File mCascadeFile;
    private Bitmap bmp = null;
    private MatOfRect faces = new MatOfRect();
    private CascadeClassifier cascadeFace;
    private JavaCameraView javaCameraView;
    private Pessoa pessoa = new Pessoa();

    private ImageView imgPalpebra;
    private Speech speech;

    private AIService aiService;
    private static final int REQUEST_INTERNET = 200;
    private static final int RECORD_AUDIO_PERMISSION = 1;


    //Eye Variables//
    //Screen Size
    DisplayMetrics displayMetrics = new DisplayMetrics();

    //Animation
    private AnimationDrawable palpebraAnim;

    //Images
    private ImageView ivPalpebra;
    private ImageView ivPupila;

    //Eye Position
    private float pupilaX;
    private float pupilaY;

    //Target Position
    private float targetX;
    private float targetY;

    //RAT
    private int x, y, centerx, centery;


    static
    {
        System.loadLibrary("opencv_java");
    }

    private BaseLoaderCallback baseLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case BaseLoaderCallback.SUCCESS: {
                    //Toast.makeText(getApplicationContext(), "Vision Ok", Toast.LENGTH_LONG).show();
                    javaCameraView.enableView();
                    break;
                }
                default: {
                    super.onManagerConnected(status);
                    break;
                }
            }
        }
    };

    

    BluetoothAdapter meuBluetoothAdapter = null;
    BluetoothDevice meuDevice = null;
    BluetoothSocket meuSocket = null;

    ConnectedThread connectedThread;
    
    boolean conexao = false, service;

    private static String MAC = null;

    UUID MEU_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    //voz


    String comando, comandoVoz;
    //voz

    int result;
    //textoVoz

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        ivPalpebra = findViewById(R.id.ivPalpebra);
        ivPupila = findViewById(R.id.ivPupila);
        vozTexto = (TextView)findViewById(R.id.textView);
        javaCameraView = (JavaCameraView) findViewById(R.id.java_camera_view);
        speech = new Speech(this);

        javaCameraView.setVisibility(SurfaceView.VISIBLE);
        javaCameraView.setCvCameraViewListener(this);

        meuBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        haarCascadeHandler();

        if (meuBluetoothAdapter.equals(null))
            Toast.makeText(getApplicationContext(),"Seu Dispositivo não possui Bluetooth",Toast.LENGTH_LONG).show();

        else if(!((BluetoothAdapter) meuBluetoothAdapter).isEnabled())
        {
            Intent ativaBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(ativaBluetooth, SOLICITA_ATIVACAO);
        }

        ivPalpebra.setOnTouchListener(new OnTouchSwipeListener(MainActivity.this)
        {
            @Override
            public void onSwipeRight() {
                startActivityForResult(new Intent(MainActivity.this, Menu.class), BT_BLUETOOTH);
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left);
            }
        });

        //Eye Initialization//
        ivPalpebra.setBackgroundResource(R.drawable.palpebra_anim);

        //Get Screen Size
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        //Start Blinking Animation
        BlinkRoutine();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        if (speech.getToSpeech()!=null)
        {
            speech.getToSpeech().stop();
            speech.getToSpeech().shutdown();
        }
        if(javaCameraView!=null)
            javaCameraView.disableView();
    }

//    private void btnVoz (){
//        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
//        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
//        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
//        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,"Hill Speak Now ...");
//
//        try {
//            startActivityForResult(intent, REQ_CODE_SPEECH_OUTPUT);
//        }
//        catch (ActivityNotFoundException tim) {
////just put an toast if Google mic is not opened
//        }
//    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        //super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case BT_BLUETOOTH:
                if (conexao)
                {
                    //desconectar
                    try
                    {
                        meuSocket.close();
                        conexao = false;
                        Toast.makeText(getApplicationContext(),"Bluetooth foi Desconectado!",Toast.LENGTH_LONG).show();
                    }
                    catch (IOException erro)
                    {
                        Toast.makeText(getApplicationContext(),"Ocorreu um erro: "+erro,Toast.LENGTH_LONG).show();
                    }
                }
                else
                {
                    //conectar
                    Intent abreLista=new Intent(MainActivity.this,ListaDispositivos.class);
                    startActivityForResult(abreLista,SOLICITA_CONEXAO);
                }
                break;

            case SOLICITA_ATIVACAO:
                if(resultCode== Activity.RESULT_OK){
                    Toast.makeText(getApplicationContext(),"O Bluetooth foi ativado!",Toast.LENGTH_LONG).show();
                } else{
                    Toast.makeText(getApplicationContext(),"O Bluetooth não foi ativado, o APP será encerrado!",Toast.LENGTH_LONG).show();
                    finish();
                }
                break;

            case SOLICITA_CONEXAO:
                if(resultCode== Activity.RESULT_OK){
                    MAC = data.getExtras().getString(ListaDispositivos.ENDERECO_MAC);
                    Toast.makeText(getApplicationContext(),"MAC Final: "+MAC,Toast.LENGTH_LONG).show();
                    meuDevice = meuBluetoothAdapter.getRemoteDevice(MAC);
                    try{
                        meuSocket = meuDevice.createRfcommSocketToServiceRecord(MEU_UUID);
                        meuSocket.connect();
                        Toast.makeText(getApplicationContext(),"Você foi conectado com: "+MAC,Toast.LENGTH_LONG).show();

                        conexao = true;
                        connectedThread = new ConnectedThread( meuSocket,MainActivity.this);
                        connectedThread.start();
                        connectedThread.enviar("r");

                    } catch (IOException erro){
                        conexao = false;
                        Toast.makeText(getApplicationContext(),"Ocorreu um erro: "+erro,Toast.LENGTH_LONG).show();
                    }
                } else{
                    Toast.makeText(getApplicationContext(),"Falha ao obter o MAC!",Toast.LENGTH_LONG).show();
                }
                break;

            case REQ_CODE_SPEECH_OUTPUT: {
                if ((resultCode == RESULT_OK) && (null!= data)){
                    ArrayList<String> voiceInText = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    comandoVoz = voiceInText.get(0);

                        final AIConfiguration config = new AIConfiguration("bd26714d7b1f4087aa0623da6018c8d9",
                                AIConfiguration.SupportedLanguages.English,
                                AIConfiguration.RecognitionEngine.System);
                        aiService = AIService.getService(this, config);
                        aiService.setListener(this);

                        if (comandoVoz.contains("register")) {
                            startActivity(new Intent(MainActivity.this, Register.class));
                            connectedThread.enviar("r");
                        }
                        else if(comandoVoz.contains("yes"))
                        {
                            falou = false;
                            int count = 0;
                            recognize = true;
                            pessoa.setName("");
                            //while(true)
                            //{
                                if(!pessoa.getName().isEmpty())
                                {
                                    if(!pessoa.getName().equals("unknow"))
                                    {
                                        speech.toSpeech("Hello " + pessoa.getName() + ", nice to see you.");
                                        pessoa.setName("");
                                        falou = true;
                                        break;
                                    }
                                }
                                else
                                    recognize = true;
                            //}

                            validateOs();
                        }

                        else
                            connectedThread.enviar("r");
                }
                ////////comandos manuais/////////
            }
        }
    }

    public void openCvHandler()
    {
        if(OpenCVLoader.initDebug())
            baseLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);

        else
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_11, this, baseLoaderCallback);

    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        openCvHandler();
    }

    @Override
    public void onResult(AIResponse result)
    {
        aiService.stopListening();
        final Result resultado = result.getResult();
        final Status status = result.getStatus();
        final String speech = resultado.getFulfillment().getSpeech();
        final Metadata metadata = resultado.getMetadata();

        String parameterString = "";
        if(resultado.getParameters() != null && !resultado.getParameters().isEmpty())
        {
            for(final Map.Entry<String, JsonElement> entry : resultado.getParameters().entrySet()){
                parameterString += "("+entry.getKey()+", "+entry.getValue()+")";
            }
        }

//        vozTexto.setText("Query: "+ resultado.getResolvedQuery()+
//                "\nActions: "+ resultado.getAction()+
//                "\nParameters: " + parameterString+
//                "\nIntent Name: " + metadata.getIntentName()+
//                "\nIntent Id: "+metadata.getIntentId()+
//                "\nResponse: "+resultado.getFulfillment().getSpeech());//"\n Results: " + resultado.getFulfillment().getDisplayText());

        resposta = resultado.getFulfillment().getSpeech();

        if(!pessoa.getName().isEmpty() && !falou) {
            if (!pessoa.getName().equals("unknow")) {
                MainActivity.this.speech.toSpeech(resposta + ", alright " + pessoa.getName() + "?");
                falou = true;
            }
        }
        else
        {
            MainActivity.this.speech.toSpeech(resposta);
            recognize = true;
            //resposta.replaceAll("@name", pessoa.getName());
        }

        if(!resposta.contains("soon"))
            validateOs();
        else
            connectedThread.enviar("r");
    }

    @Override
    public void onError(AIError error)
    {

    }

    @Override
    public void onAudioLevel(float level)
    {

    }

    @Override
    public void onListeningStarted()
    {

    }

    @Override
    public void onListeningCanceled()
    {

    }

    @Override
    public void onListeningFinished()
    {

    }

    @Override
    public void onCameraViewStarted(int width, int height)
    {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mGray = new Mat(height, width, CvType.CV_8UC1);
        mCrop = new Mat(height, width, CvType.CV_8UC4);
        absoluteFaceSize = (int) (height * 0.2);
    }

    @Override
    public void onCameraViewStopped()
    {
        mRgba.release();
        mGray.release();
        mCrop.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame)
    {
        mRgba = inputFrame.rgba();
        Imgproc.cvtColor(mRgba, mGray, Imgproc.COLOR_RGBA2GRAY);
        if(cascadeFace != null)
        {
            cascadeFace.detectMultiScale(mGray, faces, 1.1,2,2,
                    new Size(absoluteFaceSize, absoluteFaceSize), new Size());
        }

        Rect[] faceArray = faces.toArray();

        for(int i = 0; i < faceArray.length; i++)
        {
            Core.rectangle(mRgba, faceArray[i].tl(), faceArray[i].br(), new Scalar(0, 255, 0, 255), 3);
        }

        LookAtBiggest(faceArray);

        if(recognize)
        {
            String response = "";
            if(!faces.empty())
            {
                Rect rect = faces.toArray()[0];

                centerx = (rect.width)/2 + rect.x;
                centery = (rect.height)/2 + rect.y;
                x = mRgba.width()/3;
                y = mRgba.height()/3;

                if(centerx < x){
                    if(centery < y){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                connectedThread.enviar("0");
                            }
                        });
                    }
                    else if(centery > y*2){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                connectedThread.enviar("6");
                            }
                        });
                    }
                    else{
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                connectedThread.enviar("3");
                            }
                        });
                    }
                }
                else if(centerx > x*2){
                    if(centery < y){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                connectedThread.enviar("2");
                            }
                        });
                    }
                    else if(centery > 2*y){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                connectedThread.enviar("8");
                            }
                        });
                    }
                    else{
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                connectedThread.enviar("5");
                            }
                        });
                    }
                }
                else{
                    if(centery < y){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                connectedThread.enviar("1");
                            }
                        });
                    }
                    else if(centery > 2*y){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                connectedThread.enviar("7");
                            }
                        });
                    }
                    else{
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                connectedThread.enviar("4");
                            }
                        });
                    }
                }

                mCrop = new Mat(mRgba, rect);
                Imgproc.cvtColor(mCrop, mCrop, Imgproc.COLOR_RGBA2RGB);
                Imgproc.resize(mCrop,mCrop,new Size(96,96));
                bmp = Bitmap.createBitmap(mCrop.cols(), mCrop.rows(), Bitmap.Config.RGB_565);
                Utils.matToBitmap(mCrop, bmp);

                try {
                    response = updateImage(imageToString(bmp));
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if(response != null && !response.equals("null") && !response.equals("scanning"))
                {
                    pessoa.setName(response);
                    pessoa.setProfissao("profissao");
                    //toSpeech.speak("You are " + txtclassifica, TextToSpeech.QUEUE_FLUSH,null,null);
                }
                else
                    pessoa.setName("unknow");
                    //toSpeech.speak("I don't know you... do you want to tell me your name?",
                     //       TextToSpeech.QUEUE_FLUSH,null,null);
            }
            //else
                //toSpeech.speak("I am not seeing anyone...", TextToSpeech.QUEUE_FLUSH,null,null);

        }
        recognize = false;
        return mRgba;
    }

    private String updateImage(String image) throws IOException
    {
        ApiInterface apiInterface = ApiClient.getApiClient().create(ApiInterface.class);
        Call<ImageClass> call = apiInterface.uploadImage(image);
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

    private void validateOs()
    {
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO},
                    RECORD_AUDIO_PERMISSION);

        }
        else
        {
            aiService.startListening();
            //service = false;

        }
    }
    //@Override
    public void onRequestPermissionResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        if(requestCode == RECORD_AUDIO_PERMISSION)
        {
            if(grantResults.length == 1 && grantResults[0] == getPackageManager().PERMISSION_GRANTED){
                aiService.startListening();
            }
            else
                vozTexto.setText("Permissão negada");
        }
    }

    public void BlinkRoutine()
    {
        palpebraAnim = (AnimationDrawable) ivPalpebra.getBackground();
        palpebraAnim.start();
    }

    public void LookAtBiggest(Rect[] mFaces)
    {
        if(mFaces.length == 0)
        {
            return;
        }
        int target = 0;
        float area = 0;
        for(int i = 0; i < mFaces.length; i++)
        {
            if(mFaces[i].height * mFaces[i].width > area)
            {
                target = i;
                area = mFaces[i].height * mFaces[i].width;
            }
        }
        targetX = mFaces[target].x + mFaces[target].width;
        targetY = mFaces[target].y + mFaces[target].height;
        MoveEye(targetX, targetY);
    }

    public void MoveEye(float _x, float _y)
    {
        //Because of the eye image size, it should start positioned at the screen's 0, 0 point
        //It'll move 100 pixels max to either side depending on where it needs to look in screen coordinates

        //Local Variables
        float inMinX, inMaxX; //X coordinates for comparison
        float inMinY, inMaxY; //Y coordinates for comparison

        //X coordinates
        if(_x > displayMetrics.widthPixels / 2)
        {
            //Move right
            inMinX = displayMetrics.widthPixels / 2;
            inMaxX = displayMetrics.widthPixels;
            pupilaX = ((_x - inMinX) / (inMaxX - inMinX)) * 100;
        }
        else if(_x < displayMetrics.widthPixels / 2)
        {
            //Move left
            inMaxX = 0;
            inMinX = displayMetrics.widthPixels / 2;
            pupilaX = ((_x - inMinX) * -1 / (inMaxX - inMinX)) * 100;
        }

        //Y coordinates
        if(_y > displayMetrics.heightPixels / 2)
        {
            //Move Down
            inMinY = displayMetrics.heightPixels / 2;
            inMaxY = displayMetrics.heightPixels;
            pupilaY = ((_y - inMinY) / (inMaxY - inMinY)) * 100;
        }
        else if(_y < displayMetrics.heightPixels / 2)
        {
            //Move Up
            inMaxY = 0;
            inMinY = displayMetrics.heightPixels / 2;
            pupilaY = ((_y - inMinY) * -1 / (inMaxY - inMinY)) * 100;
        }

        ivPupila.setTranslationX(-pupilaX); //Moves eye in X
        ivPupila.setTranslationY(pupilaY); //Moves eye in Y
    }
}
