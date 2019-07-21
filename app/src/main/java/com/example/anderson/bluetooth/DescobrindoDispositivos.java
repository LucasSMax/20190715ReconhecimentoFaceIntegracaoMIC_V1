package com.example.anderson.bluetooth;

import android.Manifest;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class DescobrindoDispositivos extends ListActivity {
    /*  Um adaptador para conter os elementos da lista de dispositivos descobertos.
     */
    ArrayAdapter<String> ArrayBluetooth;
    private BluetoothAdapter meuBluetoothAdapter3 = null;
    static String ENDERECO_MAC_NOVO = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        ArrayAdapter<String> ArrayBluetooth = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);


        /*  Pede permissao de localizaçao ao usuario.
         *   Necessario para API > 22 */
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1001);
        }

        /*  Usa o adaptador Bluetooth padrão para iniciar o processo de descoberta.
         */
        meuBluetoothAdapter3 = BluetoothAdapter.getDefaultAdapter();
        meuBluetoothAdapter3.startDiscovery();

        /*  Cria um filtro que captura o momento em que um dispositivo é descoberto.
            Registra o filtro e define um receptor para o evento de descoberta.
         */
        // Register the BroadcastReceiver
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy

    }

    /*  Este método é executado quando o usuário seleciona um elemento da lista.
     */
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {

        /*  Extrai nome e endereço a partir do conteúdo do elemento selecionado.
            Nota: position-1 é utilizado pois adicionamos um título à lista e o
        valor de position recebido pelo método é deslocado em uma unidade.
         */
        String item = (String) getListAdapter().getItem(position-1);
        String devName = item.substring(0, item.indexOf("\n"));
        String devAddress = item.substring(item.indexOf("\n")+1, item.length());

        /*  Utiliza um Intent para encapsular as informações de nome e endereço.
            Informa à Activity principal que tudo foi um sucesso!
            Finaliza e retorna à Activity principal.
         */
        Intent returnIntent = new Intent();
        returnIntent.putExtra("btDevName", devName);
        returnIntent.putExtra("btDevAddress", devAddress);
        returnIntent.putExtra(ENDERECO_MAC_NOVO, devAddress);

        setResult(RESULT_OK, returnIntent);
        finish();
    }

    /*  Define um receptor para o evento de descoberta de dispositivo.
     */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        /*  Este método é executado sempre que um novo dispositivo for descoberto.*/
        public void onReceive(Context context, Intent intent) {
            /*  Obtem o Intent que gerou a ação.
                Verifica se a ação corresponde à descoberta de um novo dispositivo.
                Obtem um objeto que representa o dispositivo Bluetooth descoberto.
                Exibe seu nome e endereço na lista.
             */
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                ArrayBluetooth.add(device.getName() + "\n" + device.getAddress());
            }
            setListAdapter(ArrayBluetooth);
        }
    };


    /*  Executado quando a Activity é finalizada.
     */
    @Override
    protected void onDestroy() {

        super.onDestroy();

        /*  Remove o filtro de descoberta de dispositivos do registro.*/
        unregisterReceiver(mReceiver);
    }
/*
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_paired_devices, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }*/
}
