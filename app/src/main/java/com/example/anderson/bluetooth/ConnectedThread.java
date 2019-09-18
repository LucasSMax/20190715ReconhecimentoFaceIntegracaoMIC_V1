package com.example.anderson.bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothSocket;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;

public class ConnectedThread extends Thread implements RecognitionListener {
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

    }

    @Override
    public void onResults(Bundle results) {

    }

    @Override
    public void onPartialResults(Bundle partialResults) {

    }

    @Override
    public void onEvent(int eventType, Bundle params) {

    }

    private final InputStream mmInStream;
    private final OutputStream mmOutStream;
    private Handler mHandler;
    private static final int MESSAGE_READ = 4;
    private final int REQ_CODE_SPEECH_OUTPUT = 143;
    private int result;
    private StringBuilder dadosBluetooth = new StringBuilder();
    private Speech speech;
    private SpeechRecognizer mSpeechRec;
    private Intent mSpeechRecIntent;

    public ConnectedThread(BluetoothSocket socket, Activity ac)
    {
        final Activity activity = ac;
        speech = new Speech(ac);
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



        mHandler = new Handler(){
            @Override
            public void handleMessage(Message msg) {

                if (msg.what==MESSAGE_READ){
                    int recebidos = (int) msg.obj;


                    dadosBluetooth.append((char) recebidos);

                    mSpeechRec = SpeechRecognizer.createSpeechRecognizer(activity);
                    mSpeechRecIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                    mSpeechRecIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                    mSpeechRecIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
                    //mSpeechRecIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    mSpeechRecIntent.putExtra("valor", recebidos);
                    //mSpeechRec.setRecognitionListener(activity.);

                    //speech.toSpeech("I am the vision of the future, do you like to dialog?");

                    try {
                        activity.startActivityForResult(new Intent(activity, getVoice.class), REQ_CODE_SPEECH_OUTPUT);

                    }
                    catch (ActivityNotFoundException tim) {
                        //just put an toast if Google mic is not opened
                    }
                    ///finaliza escuta locutor///

                    dadosBluetooth.delete(0,dadosBluetooth.length());

                }

            }
        };
    }

    public void run() {
        // buffer store for the stream
        int bytes, availableBytes = 0, teste; // bytes returned from read()

        // Keep listening to the InputStream until an exception occurs
        while (true)
        {
            try
            {
                availableBytes = mmInStream.available();
                if(availableBytes > 0)
                {
                    byte[] buffer = new byte[availableBytes];
                    bytes = mmInStream.read(buffer);

                    if(bytes > 0)
                    {
                        teste = buffer[0];
                        mHandler.obtainMessage(MESSAGE_READ, bytes, -1, teste).sendToTarget();
                    }
                }

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
