package com.example.anderson.bluetooth;

import android.content.Intent;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.Locale;

public class getVoice extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int recebido;
        if (savedInstanceState == null) {
            recebido =  getIntent().getIntExtra("valor", 0);
//            if(extras == null) {
//                recebido = 88;
//            } else {
//                recebido = extras.getInt("valor");
//            }
        } else {
            recebido = (Integer) savedInstanceState.getSerializable("valor");
        }
        Intent devolve = new Intent();
        devolve.putExtra("valor", recebido);
        setResult(RESULT_OK, devolve);
        finish();
    }
}
