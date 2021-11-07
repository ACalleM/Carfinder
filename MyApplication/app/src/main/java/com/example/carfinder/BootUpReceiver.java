package com.example.carfinder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootUpReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("Carfinder", "Aviso boot recibido");
        if (intent.getAction().equalsIgnoreCase(Intent.ACTION_BOOT_COMPLETED)) {
            Intent serviceIntent = new Intent(context, MainLogic.class);
            Log.i("Carfinder", "Intento de arrancar Servicio");
            context.startForegroundService(serviceIntent);
            Log.i("Carfinder", "Fin Intento de arrancar Servicio");
        }
    }

}
