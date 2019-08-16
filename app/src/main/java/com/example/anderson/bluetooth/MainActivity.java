package com.example.anderson.bluetooth;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.AnimationDrawable;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.annotation.MainThread;
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
import java.util.Locale;
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



public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2, AIListener, RecognitionListener{

    private String resposta = "dialog";

    private static final int SOLICITA_ATIVACAO = 1;
    private static final int SOLICITA_CONEXAO = 2;
    private static final int SOLICITA_DESCOBERTA_BT = 3;
    private final int REQ_CODE_SPEECH_OUTPUT = 143;
    private final int BT_BLUETOOTH = 4;
a
    private Pessoa pessoa;

    private static Mat mRgba, mGray, mCrop;
    private int absoluteFaceSize, count = 0, try_recognize = 0, recebido = 2;
    private boolean recognize = false, falou = false, rosto = false;
    private File mCascadeFile;
    private Bitmap bmp = null;
    private MatOfRect faces = new MatOfRect();
    private CascadeClassifier cascadeFace;
    private JavaCameraView javaCameraView;

    private Intent intent;

    private Speech speech;
    private SpeechRecognizer mSpeechRecognizer;

    private AIService aiService;
    private static final int REQUEST_INTERNET = 200;
    private static final int RECORD_AUDIO_PERMISSION = 1;

    private Button bTestar, bSimulate;

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

    private AIConfiguration config;

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

        config = new AIConfiguration("bd26714d7b1f4087aa0623da6018c8d9",
                AIConfiguration.SupportedLanguages.English,
                AIConfiguration.RecognitionEngine.System);


        mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(MainActivity.this);
        mSpeechRecognizer.setRecognitionListener(this);

        ivPalpebra = findViewById(R.id.ivPalpebra);
        ivPupila = findViewById(R.id.ivPupila);
        javaCameraView = (JavaCameraView) findViewById(R.id.java_camera_view);
        speech = new Speech(this);

        bTestar = (Button) findViewById(R.id.testar);
        //bSimulate = (Button) findViewById(R.id.simulate);

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

        intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        bTestar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, Register.class));
//                speech.toSpeech("I am the vision of the future, do you like to dialog?");
//
//                mSpeechRecognizer.startListening(intent);
            }
        });

        bSimulate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSpeechRecognizer.startListening(intent);
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
        if(mSpeechRecognizer != null)
            mSpeechRecognizer.destroy();
        if(aiService != null)
            aiService = null;
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
                        //Toast.makeText(getApplicationContext(),"Você foi conectado com: "+MAC,Toast.LENGTH_LONG).show();

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
//                   ArrayList<String> voiceInText = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
//                   comandoVoz = voiceInText.get(0);

                    int cmd_bt = data.getIntExtra("valor", 0);
                    while (speech.getToSpeech().isSpeaking());
                    if(true)
                    {
                       //mSpeechRecognizer.startListening(intent);
                        pessoa = new Pessoa();
                        recognize = true;
                        rosto = false;
                    }
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
        //aiService.stopListening();
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
//                "\nIntent Id: "+metadata.getIntentId()+m
//                "\nResponse: "+resultado.getFulfillment().getSpeech());//"\n Results: " + resultado.getFulfillment().getDisplayText());

        resposta = resultado.getFulfillment().getSpeech();
        //intent = metadata




        if(!resposta.contains("soon")) {
            MainActivity.this.speech.toSpeech(resposta);
            while (MainActivity.this.speech.getToSpeech().isSpeaking());
            validateOs();
        }else
        {
            connectedThread.enviar("r");
            MainActivity.this.speech.toSpeech("Good bye.");
            while (MainActivity.this.speech.getToSpeech().isSpeaking());

        }

    }

    @Override
    public void onError(AIError error)
    {
        if(error.getMessage().contains("busy"))
        {
            aiService.stopListening();
            speech.toSpeech("I didn't catch that. Sorry.");
            while(speech.getToSpeech().isSpeaking());
            connectedThread.enviar("r");
        }
        else{
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    aiService = AIService.getService(MainActivity.this, config);
                    aiService.setListener(MainActivity.this);
                    aiService.startListening();
                }
            });
        }


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
        Toast.makeText(getApplicationContext(), "Canceled", Toast.LENGTH_LONG).show();

        aiService = AIService.getService(this, config);
        aiService.setListener(this);

        aiService.startListening();
    }

    @Override
    public void onListeningFinished()
    {
//        aiService.stopListening();
//        if(!finish_dialog)
//        {
//            speech.toSpeech("I don't understand you");
//        }
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


        if(recognize) //trocar por recognize
        {
            String response = "";
            if(!faces.empty())
            {
                Rect rect = faces.toArray()[0];

                centerx = (rect.width)/2 + rect.x;
                centery = (rect.height)/2 + rect.y;
                x = mRgba.width()/3;
                y = mRgba.height()/3;

//                if(!rosto)
//                {
                    if(centerx < x){
                        if(centery < y){
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    connectedThread.enviar("0");
                                    //rosto = true;
                                }
                            });
                        }
                        else if(centery > y*2){
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    connectedThread.enviar("6");
                                    //rosto = true;
                                }
                            });
                        }
                        else{
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    connectedThread.enviar("3");
                                    //rosto = true;
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
                                    //rosto = true;
                                }
                            });
                        }
                        else if(centery > 2*y){
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    connectedThread.enviar("8");
                                    //rosto = true;
                                }
                            });
                        }
                        else{
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    connectedThread.enviar("5");
                                    //rosto = true;
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
                                    //rosto = true;
                                }
                            });
                        }
                        else if(centery > 2*y){
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    connectedThread.enviar("7");
                                    //rosto = true;
                                }
                            });
                        }
                        else{
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    connectedThread.enviar("4");
                                    //rosto = true;
                                }
                            });
                        }
                    }
                //}


                mCrop = new Mat(mRgba, rect);
                Imgproc.cvtColor(mCrop, mCrop, Imgproc.COLOR_RGBA2RGB);
                Imgproc.resize(mCrop,mCrop,new Size(96,96));
                bmp = Bitmap.createBitmap(mCrop.cols(), mCrop.rows(), Bitmap.Config.RGB_565);
                Utils.matToBitmap(mCrop, bmp);
                rosto = true;
                try {
                    response = updateImage(imageToString(bmp));
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if(response != null && !response.equals("null")
                        && !response.equals("scanning") && !response.equals("unknow"))
                {
                    count = 0;
                    pessoa.setName(response);
                    pessoa.setProfissao("profissao");
                    dialogFlow_nome();
                    recognize = false;
                    count = 0;

                    return mRgba;
                }
                else {
                    pessoa.setName("");

                }
                //toSpeech.speak("I don't know you... do you want to tell me your name?",
                     //       TextToSpeech.QUEUE_FLUSH,null,null);
            }

            if(!rosto) {
                if (pessoa.getName().equals("")) {
                    count += 1;
                    if (count <= 70) {
                        if (count % 10 == 0) {
                            switch (count / 10) {
                                case 1:
                                    connectedThread.enviar("0");
                                    break;
                                case 2:
                                    connectedThread.enviar("5");
                                    break;
                                case 3:
                                    connectedThread.enviar("5");
                                    break;
                                case 4:
                                    connectedThread.enviar("7");
                                    break;
                                case 5:
                                    connectedThread.enviar("3");
                                    break;
                                case 6:
                                    connectedThread.enviar("3");
                                    break;
                                default:
                                    break;
                            }
                        }
                    } else {
                        speech.toSpeech("I didn't find anyone.");
                        while (speech.getToSpeech().isSpeaking());
                        recognize = false;
                        connectedThread.enviar("r");
                        count = 0;
                    }
                }
            }
            //else
                //toSpeech.speak("I am not seeing anyone...", TextToSpeech.QUEUE_FLUSH,null,null);

        }
        //recognize = false;
        if(rosto && recognize)
        {
            try_recognize+=1;
            if(try_recognize > 15)
            {

                connectedThread.enviar("r");
                speech.toSpeech("I'm not seeing anyone");
                while (speech.getToSpeech().isSpeaking());
                recognize = false;
                try_recognize = 0;
                count = 0;
            }
        }
        return mRgba;
    }

    private String updateImage(String image) throws IOException
    {
        try
        {

            ApiInterface apiInterface = ApiClient.getApiClient().create(ApiInterface.class);
            Call<ImageClass> call = apiInterface.uploadImage(image);
            ImageClass Im = call.execute().body();
            return Im.getResponse();
        }
        catch (Exception e)
        {

        }
        return "failed";
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
            aiService = AIService.getService(MainActivity.this, config);
            aiService.setListener(MainActivity.this);
            aiService.startListening();
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

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    @Override
    public void onReadyForSpeech(Bundle params) {

    }

    @Override
    public void onBeginningOfSpeech() {

    }

    @Override
    public void onRmsChanged(float rmsdB) {

    }

    @Override
    public void onBufferReceived(byte[] buffer) {

    }

    @Override
    public void onEndOfSpeech() {

    }

    @Override
    public void onError(int error) {
        speech.toSpeech("Sorry, I didn't understand. Can you repeat please?");
        mSpeechRecognizer.startListening(intent);
    }

    @Override
    public void onResults(Bundle results) {
        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        comandoVoz = matches.get(0);
        if (comandoVoz.contains("hello actor")) {
            //dialogFlow();
        }
    }

    public void dialogFlow()
    {
        aiService = AIService.getService(this, config);
        aiService.setListener(this);

        if (comandoVoz.contains("register")) {
            startActivity(new Intent(MainActivity.this, Register.class));
        }
        else if(comandoVoz.contains("yes")) {

            pessoa = new Pessoa();
            recognize = true;
        }
        else
            connectedThread.enviar("r");
    }

    private void dialogFlow_nome()
    {
        speech.toSpeech("Hello " + pessoa.getName() + ", nice to see you.");
        while(speech.getToSpeech().isSpeaking());
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mSpeechRecognizer.stopListening();
                validateOs();
            }
        });

    }

    @Override
    public void onPartialResults(Bundle partialResults) {

    }

    @Override
    public void onEvent(int eventType, Bundle params) {

    }
}
