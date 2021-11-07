package com.example.carfinder;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import static android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS;

public class MainActivity extends AppCompatActivity {
    private static final String CHANNEL_ID = "CANAL";
    Set<String> dispositivosActivados = null;
    int numActualizaciones = 0;
    boolean debug = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i("Carfinder", "Inicio aplicacion");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Context context = this;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_BACKGROUND_LOCATION,Manifest.permission.WAKE_LOCK}, 1);
        } else {
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.WAKE_LOCK}, 1);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:" + BuildConfig.APPLICATION_ID));

            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            }
        } else {
            Intent powerUsageIntent = new Intent(Intent.ACTION_POWER_USAGE_SUMMARY);
            if (getPackageManager().resolveActivity(powerUsageIntent, 0) != null) {
                startActivity(powerUsageIntent);
            }
        }

        APIHandler api = new APIHandler(this);

        SharedPreferences sharedPref = context.getApplicationContext().getSharedPreferences("com.example.carfinder.PREFERENCE_FILE_KEY", Context.MODE_PRIVATE);
        dispositivosActivados = sharedPref.getStringSet("DISPOSITIVOS_ACTIVADOS", new HashSet<String>());
        numActualizaciones = sharedPref.getInt("NUM_ACTUALIZACIONES", 0);
        debug = sharedPref.getBoolean("DEBUG", false);
        Log.i("Carfinder", "Preferencias: " + numActualizaciones + ":" + dispositivosActivados.toString() +  ":" + debug);

        // Selection box para seleccionar el dispositivo bluetooth
        Spinner spinner = (Spinner) findViewById(R.id.spinner);
        HashMap<String, String> d = BluetoothTracker.getBluetoothPairedDevices();
        ArrayList<String> addresses = new ArrayList<String>(d.keySet());
        ArrayList<String> devices = new ArrayList<String>(d.values());

        for (int i=0; i < addresses.size(); i++) {
            String currentAddress = addresses.get(i);
            String currentDevice = devices.get(i);
            if (dispositivosActivados.contains(currentAddress)) {
                addresses.remove(i);
                addresses.add(0,currentAddress);
                devices.remove(i);
                devices.add(0,currentDevice);

            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item, devices);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id)
            {
                if (dispositivosActivados.contains(addresses.get(spinner.getSelectedItemPosition()))) {
                    Switch toggle = (Switch) findViewById(R.id.switchonoff);
                    toggle.setChecked(true);

                } else {
                    Switch toggle = (Switch) findViewById(R.id.switchonoff);
                    toggle.setChecked(false);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent)
            {
                // vacio
            }
        });

        // Slide para seleccionar si el dispositivo bluetooth se monitoriza o no
        Switch toggle = (Switch) findViewById(R.id.switchonoff);
        toggle.setOnClickListener(new Switch.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences sharedPref = context.getApplicationContext().getSharedPreferences("com.example.carfinder.PREFERENCE_FILE_KEY", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                if (((Switch)view).isChecked()) {
                    dispositivosActivados.add(addresses.get(spinner.getSelectedItemPosition()));
                } else {
                    dispositivosActivados.remove(addresses.get(spinner.getSelectedItemPosition()));
                }
                editor.putStringSet("DISPOSITIVOS_ACTIVADOS", dispositivosActivados);
                editor.putInt("NUM_ACTUALIZACIONES",++numActualizaciones);
                Log.i("Carfinder", "Preferencias: " + numActualizaciones + ":" + dispositivosActivados.toString());
                editor.commit();
            }
        });

        // Check box para seleccionar el modo de debug
        CheckBox checkBox = (CheckBox) findViewById(R.id.checkBox);
        checkBox.setChecked(debug);
        checkBox.setOnClickListener(new CheckBox.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences sharedPref = context.getApplicationContext().getSharedPreferences("com.example.carfinder.PREFERENCE_FILE_KEY", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putBoolean("DEBUG", ((CheckBox)view).isChecked());
                debug =  ((CheckBox)view).isChecked();
                Log.i("Carfinder", "Debug: " + ((CheckBox)view).isChecked());
                editor.commit();
            }
        });

        // Boton de consulta de la dirección
        Button botonConsultar = findViewById(R.id.buttonConsultar);
        botonConsultar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                api.getDeviceLocation(addresses.get(spinner.getSelectedItemPosition()));
                new CountDownTimer(15000,1000) {
                    @Override
                    public void onFinish() {
                        if (api.isReady() && api.isSuccess()) {
                            this.cancel();
                            say("Ultimo registro de " + api.getDeviceLocation().getBearer() + ".\nFecha " + api.getDeviceLocation().getDate() + ".\nDireccion: " + api.getDeviceLocation().getLocation().getDescription() + "\nPrecisión: " + api.getDeviceLocation().getLocation().getAccuracyDescription());
                        } else if (api.isReady() && !api.isSuccess()) {
                            say("Error: " + api.getErrorMessage());
                        } else {
                            say("Error: timeout" + api.getErrorMessage());
                        }
                    }
                    @Override
                    public void onTick(long millisUntilFinished) {
                        if (api.isReady() && api.isSuccess()) {
                            this.cancel();
                            say("Ultimo registro de " + api.getDeviceLocation().getBearer() + ".\nFecha " + api.getDeviceLocation().getDate() + ".\nDireccion: " + api.getDeviceLocation().getLocation().getDescription() + "\nPrecisión: " + api.getDeviceLocation().getLocation().getAccuracyDescription());
                        } else if (api.isReady() && !api.isSuccess()) {
                            this.cancel();
                            say("Error: " + api.getErrorMessage());
                        }
                    }
                }.start();

            }
        });

        // Boton de visualización de la direccion
        Button botonVisualizar = findViewById(R.id.buttonVisualizar);
        botonVisualizar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                api.getDeviceLocation(addresses.get(spinner.getSelectedItemPosition()));
                new CountDownTimer(15000,1000) {
                    @Override
                    public void onFinish() {
                        if (api.isReady() && api.isSuccess()) {
                            this.cancel();
                            say("Ultimo registro de " + api.getDeviceLocation().getBearer() + ".\nFecha " + api.getDeviceLocation().getDate() + ".\nDireccion: " + api.getDeviceLocation().getLocation().getDescription() + "\nPrecisión: " + api.getDeviceLocation().getLocation().getAccuracyDescription());
                            Uri gmmIntentUri = Uri.parse(String.format(Locale.ENGLISH, "geo:%f,%f?z=15&q=%f,%f (%s)", api.getDeviceLocation().getLocation().getLatitude(), api.getDeviceLocation().getLocation().getLongitude(), api.getDeviceLocation().getLocation().getLatitude(), api.getDeviceLocation().getLocation().getLongitude(), "coche"));
                            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                            mapIntent.setPackage("com.google.android.apps.maps");
                            if (mapIntent.resolveActivity(getPackageManager()) != null) {
                                startActivity(mapIntent);
                            }
                        } else if (api.isReady() && !api.isSuccess()) {
                            say("Error: " + api.getErrorMessage());
                        } else {
                            say("Error: timeout" + api.getErrorMessage());
                        }
                    }
                    @Override
                    public void onTick(long millisUntilFinished) {
                        if (api.isReady() && api.isSuccess()) {
                            this.cancel();
                            say("Ultimo registro de " + api.getDeviceLocation().getBearer() + ".\nFecha " + api.getDeviceLocation().getDate() + ".\nDireccion: " + api.getDeviceLocation().getLocation().getDescription() + "\nPrecisión: " + api.getDeviceLocation().getLocation().getAccuracyDescription());
                            Uri gmmIntentUri = Uri.parse(String.format(Locale.ENGLISH, "geo:%f,%f?z=15&q=%f,%f (%s)", api.getDeviceLocation().getLocation().getLatitude(), api.getDeviceLocation().getLocation().getLongitude(), api.getDeviceLocation().getLocation().getLatitude(), api.getDeviceLocation().getLocation().getLongitude(), "coche"));
                            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                            mapIntent.setPackage("com.google.android.apps.maps");
                            if (mapIntent.resolveActivity(getPackageManager()) != null) {
                                startActivity(mapIntent);
                            }
                        }
                    }
                }.start();

            }
        });

        // Boton de prueba
        Button botonTest = findViewById(R.id.buttonTest);
        botonTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (debug) {
                    Log.i("Carfinder", "Ejecutando test");
                    MainLogic mainLogic = new MainLogic(context);
                    mainLogic.disconnectLogic(context, BluetoothTracker.getPhoneName(), devices.get(spinner.getSelectedItemPosition()), addresses.get(spinner.getSelectedItemPosition()));
                }
            }
        });

    }

    private void say(String whattosay) {
        final TextView myTextView = findViewById(R.id.textView);
        myTextView.setText(whattosay);
    }

    private void announce(Context context, String whattoannounce) {
        Toast.makeText(context, whattoannounce,Toast.LENGTH_SHORT).show();
    }

    private void showAlert(Context context, String title, String alert) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);

        // Setting Dialog Title
        alertDialog.setTitle(title);

        // Setting Dialog Message
        alertDialog.setMessage(alert);

        // On pressing Settings button
        alertDialog.setPositiveButton("OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        // Showing Alert Message
        alertDialog.show();
    }

}