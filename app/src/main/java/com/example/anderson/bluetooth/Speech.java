package com.example.anderson.bluetooth;

import android.app.Activity;
import android.speech.tts.TextToSpeech;

import java.security.AccessControlContext;
import java.util.Locale;

public class Speech {

    private TextToSpeech to_Speech;

    public Speech(Activity activity)
    {
        to_Speech = new TextToSpeech(activity, new TextToSpeech.OnInitListener()
        {
            @Override
            public void onInit(int status)
            {
                if (status==TextToSpeech.SUCCESS)
                    to_Speech.setLanguage(Locale.getDefault());
            }
        });
    }

    public void toSpeech(String speech)
    {
        to_Speech.speak(speech, TextToSpeech.QUEUE_FLUSH,null);
    }

    public TextToSpeech getToSpeech()
    {
        return to_Speech;
    }
}
