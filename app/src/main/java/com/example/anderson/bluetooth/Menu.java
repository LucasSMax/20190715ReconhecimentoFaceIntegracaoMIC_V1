package com.example.anderson.bluetooth;

import android.app.Activity;
import android.content.Intent;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Layout;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.IOException;

public class Menu extends AppCompatActivity {

    private Button btnConexao, btnDescobrir, openMic;
    //Activity activity = MainActivity. context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        btnConexao = (Button)findViewById(R.id.btnConexao);
        btnDescobrir = (Button)findViewById(R.id.btnDescobrir);
        openMic = (Button)findViewById(R.id.btnVoz);

        btnConexao.setOnClickListener(new View.OnClickListener() {
              @Override
              public void onClick(View view) {
                  Intent devolve = new Intent();
                  devolve.putExtra("teste", "aaaa");
                  setResult(RESULT_OK, devolve);
                  finish();
              }
          });


//            }
//        });
//
//        btnDescobrir.setOnClickListener(new View.OnClickListener(){
//            @Override
//            public void onClick(View view) {
//                if (!meuBluetoothAdapter.isDiscovering()){
//                    Toast.makeText(getApplicationContext(),"Descobrindo Dispositivos!",Toast.LENGTH_LONG).show();
//                    Intent listarNovosDevices = new Intent(MainActivity.this, DescobrindoDispositivos.class);
//                    startActivityForResult(listarNovosDevices,SOLICITA_DESCOBERTA_BT);
//                }
//            }
//        });

        openMic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

//                //if (conexao){
//                if(true){
//                    btnVoz();
//
//                    //connectedThread.enviar("A\r\n");
//                    //connectedThread.enviar("a\r\n");
//                } else{
//
//                    Toast.makeText(getApplicationContext(),"Bluetooth não está conectado!",Toast.LENGTH_LONG).show();
//                }
            }
        });

    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left);
    }
}
