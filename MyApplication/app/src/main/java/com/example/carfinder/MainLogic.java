package com.example.carfinder;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static android.content.Context.POWER_SERVICE;
import static android.content.Context.WIFI_SERVICE;

public class MainLogic {
    private final Context mContext;
    private static final String CHANNEL_ID = "1";
    int notificationId = 1;

    private static final long HIGH_ACCURACY = 15;
    private static final long MED_ACCURACY = 25;
    private static final long LOW_ACCURACY = 50;
    private static final long MIN_MILLISECS_LOCATION = 30000;
    private static final long MAX_MILLISECS_LOCATION = 300000;
    private static final long MAX_MILLISECS_API = 15000;

    public MainLogic(Context context) { mContext = context; }

    public void disconnectLogic(Context context, String phoneName, String deviceName, String deviceAddress) {
        Log.i("Carfinder", "Desconexion (inicio)");

        SharedPreferences sharedPref = context.getApplicationContext().getSharedPreferences("com.example.carfinder.PREFERENCE_FILE_KEY", Context.MODE_PRIVATE);
        Set<String> dispositivosActivados = sharedPref.getStringSet("DISPOSITIVOS_ACTIVADOS", new HashSet<String>());
        Boolean debug  = sharedPref.getBoolean("DEBUG",false);

        if (debug) sendNotification(context, "Carfinder", "Disconected from '" + deviceName + "'");

        if ((dispositivosActivados != null) && (dispositivosActivados.contains(deviceAddress))) {
            Context c = context.getApplicationContext();
            PowerManager powerManager = (PowerManager) context.getSystemService(POWER_SERVICE);
            PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"MyApp::MyWakelockTag");
            wakeLock.acquire();
            WifiManager wifiManager = (WifiManager) context.getSystemService(WIFI_SERVICE);
            WifiManager.WifiLock wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF , "MyApp::MyWifiLockTag");
            wifiLock.acquire();

            GPSTracker gps = new GPSTracker(context);
            APIHandler api = new APIHandler(context);

            if (gps.canGetLocation()) {
                gps.getLocation();

                new CountDownTimer(MAX_MILLISECS_LOCATION, 1000) {
                    @Override
                    public void onFinish() {
                        Log.i("Carfinder", "Timeout de obtencion de la localizacion");
                        gps.stopUsingGPS();
                        wifiLock.release();
                        wakeLock.release();

                        if (gps.getAccuracy() > 0) {
                            DeviceLocation deviceLocation = new DeviceLocation();
                            Location location = new Location();
                            location.setLatitude(gps.getLatitude());
                            location.setLongitude(gps.getLongitude());
                            location.setAccuracy(gps.getAccuracy());
                            location.setDescription(gps.getAddress());
                            deviceLocation.setBearer(phoneName);
                            deviceLocation.setDeviceAddress(deviceAddress);
                            deviceLocation.setDeviceName(deviceName);
                            Date dt = new Date();
                            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                            deviceLocation.setDate(sdf.format(dt));
                            deviceLocation.setLocation(location);
                            if (debug) sendNotification(context, "Carfinder", "Location calculated: " + gps.getAccuracy() + "m.");
                            api.setDeviceLocation(deviceLocation);
                            Log.i("Carfinder", "Invocacion inicial al api");
                            api.storeDeviceLocation();

                            new CountDownTimer(MAX_MILLISECS_API,1000) {
                                @Override
                                public void onFinish() {
                                    Log.i("Carfinder", "Fin de actualizacion del api");
                                    wifiLock.release();
                                    wakeLock.release();
                                    if (api.isReady() && api.isSuccess()) {
                                        sendNotification(context, "Carfinder", "Location stored");
                                    } else if (api.isReady() && !api.isSuccess()) {
                                        sendNotification(context, "Carfinder", "Location store error: " + api.getErrorMessage());
                                    } else {
                                        sendNotification(context, "Carfinder", "Location store timeout");
                                    }
                                }
                                @Override
                                public void onTick(long millisUntilFinished) {
                                    Log.i("Carfinder", "Actualizacion del api " + (millisUntilFinished / 1000) + "s. to go");
                                    if (api.isReady() && api.isSuccess()) {
                                        Log.i("Carfinder", "Location stored");
                                        this.cancel();
                                        wifiLock.release();
                                        wakeLock.release();
                                        sendNotification(context, "Carfinder", "Location stored (" + (millisUntilFinished / 1000) + "s. to go)");
                                    } else if (api.isReady() && !api.isSuccess()) {
                                        Log.i("Carfinder", "Otra Invocacion al api por error: " + api.getErrorMessage());
                                        api.storeDeviceLocation();
                                    }
                                    else {
                                        if (millisUntilFinished < (MAX_MILLISECS_API/2)) {
                                            Log.i("Carfinder", "Otra Invocacion al api por no respuesta");
                                            api.storeDeviceLocation();
                                        }
                                    }
                                }
                            }.start();

                        }
                    }

                    @Override
                    public void onTick(long millisUntilFinished) {
                        long minAccuracy = 0;
                        long millisElapsed = (MAX_MILLISECS_LOCATION - millisUntilFinished) ;
                        if (millisElapsed > (MAX_MILLISECS_LOCATION*2/3)) {
                            minAccuracy = LOW_ACCURACY;
                        } else if (millisElapsed > (MAX_MILLISECS_LOCATION/3)) {
                            minAccuracy = MED_ACCURACY;
                        } else {
                            minAccuracy = HIGH_ACCURACY;
                        }

                        Log.i("Carfinder", "Obtencion de la localizacion " + (millisUntilFinished / 1000) + "s. to go (" + gps.getAccuracy() + "m.) min (" + minAccuracy + "m.)");

                        if ((gps.getAccuracy() > 0) && (gps.getAccuracy() < minAccuracy) && (millisUntilFinished < (MAX_MILLISECS_LOCATION - MIN_MILLISECS_LOCATION))) {
                            this.cancel();
                            gps.stopUsingGPS();
                            DeviceLocation deviceLocation = new DeviceLocation();
                            Location location = new Location();
                            location.setLatitude(gps.getLatitude());
                            location.setLongitude(gps.getLongitude());
                            location.setAccuracy(gps.getAccuracy());
                            location.setDescription(gps.getAddress());
                            deviceLocation.setBearer(phoneName);
                            deviceLocation.setDeviceAddress(deviceAddress);
                            deviceLocation.setDeviceName(deviceName);
                            Date dt = new Date();
                            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                            deviceLocation.setDate(sdf.format(dt));
                            deviceLocation.setLocation(location);
                            if (debug) sendNotification(context, "Carfinder", "Location calculated: " + gps.getAccuracy() + "m. (" + (millisUntilFinished / 1000) + "s. to go)");
                            api.setDeviceLocation(deviceLocation);
                            Log.i("Carfinder", "Invocacion inicial al api");
                            api.storeDeviceLocation();

                            new CountDownTimer(MAX_MILLISECS_API,1000) {
                                @Override
                                public void onFinish() {
                                    Log.i("Carfinder", "Fin de actualizacion del api");
                                    wifiLock.release();
                                    wakeLock.release();
                                    if (api.isReady() && api.isSuccess()) {
                                        sendNotification(context, "Carfinder", "Location stored");
                                    } else if (api.isReady() && !api.isSuccess()) {
                                        sendNotification(context, "Carfinder", "Location store error: " + api.getErrorMessage());
                                    } else {
                                        sendNotification(context, "Carfinder", "Location store timeout");
                                    }
                                }
                                @Override
                                public void onTick(long millisUntilFinished) {
                                    Log.i("Carfinder", "Actualizacion del api " + (millisUntilFinished / 1000) + "s. to go");
                                    if (api.isReady() && api.isSuccess()) {
                                        Log.i("Carfinder", "Location stored");
                                        this.cancel();
                                        wifiLock.release();
                                        wakeLock.release();
                                        sendNotification(context, "Carfinder", "Location stored (" + (millisUntilFinished / 1000) + "s. to go)");
                                    } else if (api.isReady() && !api.isSuccess()) {
                                        Log.i("Carfinder", "Otra Invocacion al api por error: " + api.getErrorMessage());
                                        api.storeDeviceLocation();
                                    }
                                    else {
                                        if (millisUntilFinished < (MAX_MILLISECS_API/2)) {
                                            Log.i("Carfinder", "Otra Invocacion al api por no respuesta");
                                            api.storeDeviceLocation();
                                        }
                                    }
                                }
                            }.start();
                        }
                    }
                }.start();
            } else {
                gps.showSettingsAlert();
            }
        } else {
            if (debug) sendNotification(context, "Carfinder", "Device not activated");
        }
    }

    public void sendNotification(Context context, String notificationTitle, String notificationText) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = context.getString(R.string.channel_name);
            String description = context.getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_name)
                .setContentTitle(notificationTitle)
                .setContentText(notificationText)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(notificationId++, builder.build());
   }

    /*public void onReceive(Context context, Intent intent) {
        Log.i("Carfinder", "Aviso desconexion recibido");
        sendNotification("Carfinder", "Aviso desconexion recibido (inicio)", context);
        String deviceName = intent.getStringExtra("DEVICE_NAME");
        String deviceId = intent.getStringExtra("DEVICE_ID");
        GPSTracker gps = new GPSTracker(context);
        // Retrieve location
        if (gps.canGetLocation()) {
            gps.getLocation();
            SystemClock.sleep(30000);
            gps.stopUsingGPS();
            latitude = gps.getLatitude();
            longitude = gps.getLongitude();
        } else {
            gps.showSettingsAlert();
        }
        sendNotification("Carfinder", "Aviso desconexion recibido (fin)", context);
    }*/
}
