package com.example.carfinder;

import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.icu.util.Calendar;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

import java.io.IOException;
import java.util.List;

public class GPSTracker extends Service implements LocationListener  {

    private final Context mContext;

    // flag for GPS status
    boolean isGPSEnabled = false;

    // flag for network status
    boolean isNetworkEnabled = false;

    // flag for GPS status
    boolean canGetLocation = false;

    Location location; // location
    double latitude = 0; // latitude
    double longitude = 0; // longitude
    float accuracy = 0; //accuracy

    // The minimum distance to change Updates in meters
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 1; // 1 meters

    // The minimum time between updates in milliseconds
    private static final long MIN_TIME_BW_UPDATES = 200; // 200 milliseconds

    private static final long MAX_AGE_FOR_LOCATION = 60000; // 1 minute

    // Declaring a Location Manager
    protected LocationManager locationManager;

    public GPSTracker(Context context) {
        mContext = context;
        //getLocation();
        locationManager = (LocationManager) mContext
                .getSystemService(LOCATION_SERVICE);
        // getting GPS status
        isGPSEnabled = locationManager
                .isProviderEnabled(LocationManager.GPS_PROVIDER);

        // getting network status
        isNetworkEnabled = locationManager
                .isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        Log.d("Carfinder","GPSTrackerStarted Network/GPS: "+ isNetworkEnabled + "," + isGPSEnabled);

        if (!isGPSEnabled && !isNetworkEnabled) {
            // no network provider is enabled
            this.canGetLocation = false;
        } else {
            this.canGetLocation = true;
        }
    }

    public Location getLocation() throws SecurityException {
        Log.d("Carfinder","Get Location");
        locationManager = (LocationManager) mContext
                .getSystemService(LOCATION_SERVICE);

        // getting GPS status
        isGPSEnabled = locationManager
                .isProviderEnabled(LocationManager.GPS_PROVIDER);

        // getting network status
        isNetworkEnabled = locationManager
                .isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (!isGPSEnabled && !isNetworkEnabled) {
            // no network provider is enabled
            this.canGetLocation = false;
        } else {
            this.canGetLocation = true;

            // if GPS Enabled get lat/long using GPS Services
            if (isGPSEnabled) {
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        MIN_TIME_BW_UPDATES,
                        MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                if (locationManager != null) {
                    location = locationManager
                            .getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (location != null) {
                        Log.d("Carfinder","Location received from GPS: " + location.getLatitude() + "," + location.getLongitude() + " (" + location.getAccuracy() + "m.) (" + (Calendar.getInstance().getTimeInMillis() - location.getTime())/1000 + "s.)");
                        if ((Calendar.getInstance().getTimeInMillis() - location.getTime()) < MAX_AGE_FOR_LOCATION) {
                            if (accuracy== 0) {
                                Log.d("Carfinder","First location updated");
                                latitude = location.getLatitude();
                                longitude = location.getLongitude();
                                accuracy = location.getAccuracy();
                            } else if (location.getAccuracy() < accuracy) {
                                Log.d("Carfinder","Location with more accuracy updated");
                                latitude = location.getLatitude();
                                longitude = location.getLongitude();
                                accuracy = location.getAccuracy();
                            } else Log.d("Carfinder","Location with less accuracy discarded");
                        } else {
                            Log.d("Carfinder","Location discarded due to age");
                        }
                    }
                }
            }

            // First get location from Network Provider
            if (isNetworkEnabled) {
                if (location == null) {
                    locationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            MIN_TIME_BW_UPDATES,
                            MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                    if (locationManager != null) {
                        location = locationManager
                                .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                        if (location != null) {
                            Log.d("Carfinder","Location received from Network: " + location.getLatitude() + "," + location.getLongitude() + " (" + location.getAccuracy() + ") (" + (Calendar.getInstance().getTimeInMillis() - location.getTime())/1000 + "s.)");
                            if ((Calendar.getInstance().getTimeInMillis() - location.getTime()) < MAX_AGE_FOR_LOCATION) {
                                if (accuracy== 0) {
                                    Log.d("Carfinder","First location updated");
                                    latitude = location.getLatitude();
                                    longitude = location.getLongitude();
                                    accuracy = location.getAccuracy();
                                } else if (location.getAccuracy() < accuracy) {
                                    Log.d("Carfinder","Location with more accuracy updated");
                                    latitude = location.getLatitude();
                                    longitude = location.getLongitude();
                                    accuracy = location.getAccuracy();
                                } else Log.d("Carfinder","Location with less accuracy discarded");
                            } else {
                                Log.d("Carfinder","Location discarded due to age");
                            }
                        }
                    }
                }
            }

        }
        return location;
    }

    /**
     * Stop using GPS listener Calling this function will stop using GPS in your
     * app
     * */
    public void stopUsingGPS() {
        if (locationManager != null) {
            locationManager.removeUpdates(GPSTracker.this);
        }
    }

    /**
     * Function to get latitude
     * */
    public double getLatitude() {
        // return latitude
        return latitude;
    }

    /**
     * Function to get longitude
     * */
    public double getLongitude() {
        // return longitude
        return longitude;
    }

    /**
     * Function to get accuracy
     * */
    public float getAccuracy() {
        // return longitude
        return accuracy;
    }

    public String getAddress() {
        List<Address> addresses = null;
        Geocoder geocoder = new Geocoder(mContext);
        try {
            addresses = geocoder.getFromLocation(latitude, longitude,1);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(addresses != null && addresses.size() > 0 ){
            StringBuilder addressline= new StringBuilder();
            Address address = addresses.get(0);

            for (int n = 0; n <= address.getMaxAddressLineIndex(); n++) {
                addressline.append(address.getAddressLine(n)).append(", ");
            }
            return (addressline.toString());
        }
        return("Direccion desconocida");
    }

    /**
     * Function to check GPS/wifi enabled
     *
     * @return boolean
     * */
    public boolean canGetLocation() {
        return this.canGetLocation;
    }

    /**
     * Function to show settings alert dialog On pressing Settings button will
     * lauch Settings Options
     * */
    public void showSettingsAlert() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(mContext);

        // Setting Dialog Title
        alertDialog.setTitle("GPS settings");

        // Setting Dialog Message
        alertDialog
                .setMessage("GPS is not enabled. Do you want to go to settings menu?");

        // On pressing Settings button
        alertDialog.setPositiveButton("Settings",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(
                                Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        mContext.startActivity(intent);
                    }
                });

        // on pressing cancel button
        alertDialog.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

        // Showing Alert Message
        alertDialog.show();
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d("Carfinder","Location change: " + location.getLatitude() + "," + location.getLongitude() + " (" + location.getAccuracy() + ") (" + (Calendar.getInstance().getTimeInMillis() - location.getTime())/1000 + "s.)");
        if ((Calendar.getInstance().getTimeInMillis() - location.getTime()) < MAX_AGE_FOR_LOCATION) {
            if (accuracy== 0) {
                Log.d("Carfinder","First location updated");
                latitude = location.getLatitude();
                longitude = location.getLongitude();
                accuracy = location.getAccuracy();
            } else if (location.getAccuracy() < accuracy) {
                Log.d("Carfinder","Location with more accuracy updated");
                latitude = location.getLatitude();
                longitude = location.getLongitude();
                accuracy = location.getAccuracy();
            } else Log.d("Carfinder","Location with less accuracy discarded");
        } else {
            Log.d("Carfinder","Location discarded due to age");
        }
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

}
