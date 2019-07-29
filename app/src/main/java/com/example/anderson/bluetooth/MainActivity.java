package com.example.anderson.bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.AnimationDrawable;
import android.os.Handler;
import android.os.Message;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.bluetooth.BluetoothAdapter;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

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
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import retrofit2.Call;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    Button btnConexao, btnDescobrir;
    private TextView vozTexto;

    private static final int SOLICITA_ATIVACAO = 1;
    private static final int SOLICITA_CONEXAO = 2;
    private static final int SOLICITA_DESCOBERTA_BT = 3;
    private static final int MESSAGE_READ = 4;
    private static Mat mRgba, mGray, mCrop;
    private int absoluteFaceSize;
    private boolean recognize = false;
    private static String nome;
    private File mCascadeFile;
    private Bitmap bmp = null;
    private MatOfRect faces = new MatOfRect();
    private CascadeClassifier cascadeFace;
    private JavaCameraView javaCameraView;

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

    ConnectedThread connectedThread;

    Handler mHandler;
    StringBuilder dadosBluetooth = new StringBuilder();

    BluetoothAdapter meuBluetoothAdapter = null;
    BluetoothDevice meuDevice = null;
    BluetoothSocket meuSocket = null;

    boolean conexao = false;

    private static String MAC = null;

    UUID MEU_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    //voz
    private Button openMic;
    private final int REQ_CODE_SPEECH_OUTPUT = 143;
    String comando, comandoVoz;
    //voz
    public TextToSpeech toSpeech;
    int result;
    //textoVoz

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnConexao = (Button)findViewById(R.id.btnConexao);
        btnDescobrir = (Button)findViewById(R.id.btnDescobrir);
        openMic = (Button)findViewById(R.id.btnVoz);
        vozTexto = (TextView)findViewById(R.id.textView);


        haarCascadeHandler(); //Carrega o arquivo para haar cascade
 //       openCvHandler();

        javaCameraView = (JavaCameraView) findViewById(R.id.java_camera_view);
        javaCameraView.setVisibility(SurfaceView.VISIBLE);
        javaCameraView.setCvCameraViewListener(this);

        meuBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (meuBluetoothAdapter == null) {
            // Device does not support Bluetooth
            Toast.makeText(getApplicationContext(),"Seu Dispositivo não possui Bluetooth",Toast.LENGTH_LONG).show();
        } else if(!((BluetoothAdapter) meuBluetoothAdapter).isEnabled()){
                    Intent ativaBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(ativaBluetooth, SOLICITA_ATIVACAO);

                }

        btnConexao.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (conexao){
                    //desconectar
                    try{
                        meuSocket.close();
                        conexao = false;
                        btnConexao.setText("CONECTAR");
                        Toast.makeText(getApplicationContext(),"Bluetooth foi Desconectado!",Toast.LENGTH_LONG).show();
                    } catch (IOException erro){
                        Toast.makeText(getApplicationContext(),"Ocorreu um erro: "+erro,Toast.LENGTH_LONG).show();
                    }
                } else {
                    //conectar
                    Intent abreLista=new Intent(MainActivity.this,ListaDispositivos.class);
                    startActivityForResult(abreLista,SOLICITA_CONEXAO);
                }

            }
        });

        btnDescobrir.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                if (!meuBluetoothAdapter.isDiscovering()){
                    Toast.makeText(getApplicationContext(),"Descobrindo Dispositivos!",Toast.LENGTH_LONG).show();
                    Intent listarNovosDevices = new Intent(MainActivity.this, DescobrindoDispositivos.class);
                    startActivityForResult(listarNovosDevices,SOLICITA_DESCOBERTA_BT);
                }
            }
        });

        openMic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //if (conexao){
                if(true){
                    btnVoz();

                    //connectedThread.enviar("A\r\n");
                    //connectedThread.enviar("a\r\n");
                } else{

                    Toast.makeText(getApplicationContext(),"Bluetooth não está conectado!",Toast.LENGTH_LONG).show();
                }
            }
        });

        toSpeech=new TextToSpeech(MainActivity.this, new TextToSpeech.OnInitListener(){
            @Override
            public void onInit(int status) {
                if (status==TextToSpeech.SUCCESS)
                {
                    result=toSpeech.setLanguage(Locale.getDefault());
                    //toSpeech.setPitch(1f);
                    //toSpeech.setSpeechRate(2f);
                    /*Voice voiceobj = new Voice("it-it-x-kda#male_2-local",
                            Locale.getDefault(), 1, 1, false, null);

                    toSpeech.setVoice(voiceobj);*/

                }
                else
                {
                    Toast.makeText(getApplicationContext(),"Caracteristica não suportada", Toast.LENGTH_SHORT).show();
                }

                //Eye Initialization//
                ivPalpebra = findViewById(R.id.ivPalpebra);
                ivPupila = findViewById(R.id.ivPupila);
                ivPalpebra.setBackgroundResource(R.drawable.palpebra_anim);

                //Get Screen Size
                getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

                //Start Blinking Animation
                BlinkRoutine();
            }
        });

        mHandler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                if (msg.what==MESSAGE_READ){
                    String recebidos = (String) msg.obj;
                    dadosBluetooth.append(recebidos);
                    //int fimInformacao=dadosBluetooth.indexOf("\n");
                    int fimInformacao=dadosBluetooth.indexOf("\r");
                    if (fimInformacao>0){
                        String dadoNumeroMIC = dadosBluetooth.substring(0,fimInformacao);
                        int tamInformacao = dadoNumeroMIC.length();
                        if (dadoNumeroMIC.contains("r")){
                        //if (dadosBluetooth.charAt(0)!='A'){
                            String identMIC = dadosBluetooth.substring(0,tamInformacao);
                            Log.d("Recebidos", identMIC);
                            ///iniciar escuta do locutor///
                            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
                            //intent.putExtra(RecognizerIntent.EXTRA_PROMPT,"Hill Speak Now ...");
                            toSpeech.speak("Hello so I can recognize you, please stay at a distance of 50cm.", TextToSpeech.QUEUE_FLUSH,null);
                            toSpeech.speak("I am the vision of the future, would you like to dialogue?", TextToSpeech.QUEUE_FLUSH,null);
                            try {
                                startActivityForResult(intent, REQ_CODE_SPEECH_OUTPUT);
                            }
                            catch (ActivityNotFoundException tim) {
                            //just put an toast if Google mic is not opened
                            }
                            ///finaliza escuta locutor///


                            /*
                            if (dadosFinais.contains("L1on")){
                                btnLed1.setText("Led 1 LIGADO");
                                Log.d("Led1","Ligado");
                            }else if (dadosFinais.contains("L1of")){
                                btnLed1.setText("Led 1 DESLIGADO");
                                Log.d("Led1","Desligado");
                            }
                            */

                        }
                        dadosBluetooth.delete(0,dadosBluetooth.length());

                    }
                }

            }
        };

    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        if (toSpeech!=null)
        {
            toSpeech.stop();
            toSpeech.shutdown();
        }
        if(javaCameraView!=null)
            javaCameraView.disableView();
    }

    private void btnVoz (){
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,"Hill Speak Now ...");

        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_OUTPUT);
        }
        catch (ActivityNotFoundException tim) {
//just put an toast if Google mic is not opened
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        //super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
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
                        connectedThread = new ConnectedThread(meuSocket);
                        connectedThread.start();
                        btnConexao.setText("DESCONECTAR");

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
                    vozTexto.setText(comandoVoz);
                    //Toast.makeText(getApplicationContext(),"Comando = "+vozTexto.toString(),Toast.LENGTH_LONG).show();


                    ///Incluir aqui seu código para reconhecimento facial///

                    ///Finalização com variável string das informações do reconhecimento facial


                    ////////comandos manuais/////////

                    if(comandoVoz.contains("what is my name") || comandoVoz.equals("who am I") || comandoVoz.contains("what's my name")) // comando usado para reconhecimento
                        recognize = true;

                    else if(comandoVoz.contains("register"))
                        startActivity(new Intent(MainActivity.this, Register.class));

                    else if (comandoVoz.contains("foward right")) {
                        comando = "A\r\n";
                    }

                    else if (comandoVoz.contains("foward")) {
                        comando = "B\r\n";
                        toSpeech.speak("Beware the Robot is moving forward, you can use the Stop or Behind",
                                TextToSpeech.QUEUE_FLUSH,null,null);
                    }

                    else if (comandoVoz.contains("foward left")) {
                        comando = "C\r\n";
                    }

                    else if (comandoVoz.contains("left")) {
                        comando = "D\r\n";
                    }

                    else if (comandoVoz.contains("right")) {
                        comando = "E\r\n";
                    }

                    else if (comandoVoz.contains("behind left")) {
                        comando = "F\r\n";
                    }

                    else if (comandoVoz.contains("behind")) {
                        comando = "G\r\n";
                        toSpeech.speak("Be aware the robot is behind",
                                TextToSpeech.QUEUE_FLUSH,null,null);
                        toSpeech.speak("We have other modes of movement: clockwise, anticlockwise, in doubt " +
                                "send the Stop command", TextToSpeech.QUEUE_FLUSH,null);
                    }

                    else if (comandoVoz.contains("behind right")) {
                        comando = "H\r\n";
                    }

                    else if (comandoVoz.contains("clockwise")){//)&&(!(comandoVoz.contains("anti")))) {
                        comando = "I\r\n";
                        toSpeech.speak("If I do not stop, I'll go dizzy.",
                                TextToSpeech.QUEUE_FLUSH,null,null);
                    }

                    else if (comandoVoz.contains("anticlockwise")) {
                        comando = "J\r\n";
                        toSpeech.speak("I get dizzy very easily", TextToSpeech.QUEUE_FLUSH,null);
                    }

                    else if (comandoVoz.contains("run oracle protocol")) {
                        toSpeech.speak("Now it's party reason, get ready!",
                                TextToSpeech.QUEUE_FLUSH,null,null);
                        comando = "I\r\nJ\r\nJ\r\nI\r\nJ\r\nI\r\n";
                    }
                    else {
                        comando = "a\r\n";
                    }

                    //connectedThread.enviar(comando);
                    Toast.makeText(getApplicationContext(),"Comando = "+comando,Toast.LENGTH_LONG).show();
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
    protected void onResume() {
        super.onResume();
        openCvHandler();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mGray = new Mat(height, width, CvType.CV_8UC1);
        mCrop = new Mat(height, width, CvType.CV_8UC4);
        absoluteFaceSize = (int) (height * 0.2);
    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
        mGray.release();
        mCrop.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
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
                                connectedThread.enviar("0\r\n");
                            }
                        });
                    }
                    else if(centery > y*2){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                connectedThread.enviar("6\r\n");
                            }
                        });
                    }
                    else{
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                connectedThread.enviar("3\r\n");
                            }
                        });
                    }
                }
                else if(centerx > x*2){
                    if(centery < y){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                connectedThread.enviar("2\r\n");
                            }
                        });
                    }
                    else if(centery > 2*y){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                connectedThread.enviar("8\r\n");
                            }
                        });
                    }
                    else{
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                connectedThread.enviar("5\r\n");
                            }
                        });
                    }
                }
                else{
                    if(centery < y){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                connectedThread.enviar("1\r\n");
                            }
                        });
                    }
                    else if(centery > 2*y){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                connectedThread.enviar("7\r\n");
                            }
                        });
                    }
                    else{
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                connectedThread.enviar("4\r\n");
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
                    nome = updateImage(imageToString(bmp));
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if(nome != null && !nome.equals("null") && !nome.equals("scanning"))
                {
                    toSpeech.speak("You are " + nome, TextToSpeech.QUEUE_FLUSH,null,null);
                }
                else
                    toSpeech.speak("I don't know you... do you want to tell me your name?",
                            TextToSpeech.QUEUE_FLUSH,null,null);
            }
            else
                toSpeech.speak("I am not seeing anyone...", TextToSpeech.QUEUE_FLUSH,null,null);

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


    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);

                    String dadosBt = new String(buffer, 0, bytes);
                    // Send the obtained bytes to the UI activity
                    mHandler.obtainMessage(MESSAGE_READ, bytes, -1, dadosBt).sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }

        public void enviar(String dadosEnviar) {
            byte [] msgBuffer = dadosEnviar.getBytes();
            try {
                mmOutStream.write(msgBuffer);
            } catch (IOException e) { }
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
