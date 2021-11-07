package com.example.carfinder;

import android.content.Context;
import android.util.Log;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;

import org.json.JSONObject;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;

public class APIHandler {

    private final Context mContext;

    private DeviceLocation deviceLocation;
    private boolean success;
    private boolean ready;
    private String errorMessage;

    public DeviceLocation getDeviceLocation() {
        return deviceLocation;
    }
    public void setDeviceLocation(DeviceLocation deviceLocation) { this.deviceLocation = deviceLocation; }
    public String getErrorMessage() {
        return errorMessage;
    }
    public boolean isSuccess() {
        return success;
    }
    public boolean isReady() {
        return ready;
    }

    public APIHandler(Context context) {
        this.mContext = context;

        try {
            AndroidNetworking.initialize(context, getCustomOKHttpClient(context));
        } catch (Exception e) {
            Log.d("Error: ", e.toString());
        }
    }

    public static OkHttpClient getCustomOKHttpClient(Context context) throws Exception {
        // loading CAs from an InputStream
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        InputStream cert = context.getResources().openRawResource(R.raw.acallem_cert);
        Certificate ca;
        try {
            ca = cf.generateCertificate(cert);
        } finally { cert.close(); }

        // creating a KeyStore containing our trusted CAs
        String keyStoreType = KeyStore.getDefaultType();
        KeyStore keyStore = KeyStore.getInstance(keyStoreType);
        keyStore.load(null, null);
        keyStore.setCertificateEntry("ca", ca);

        // creating a TrustManager that trusts the CAs in our KeyStore
        String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
        tmf.init(keyStore);

        // creating an SSLSocketFactory that uses our TrustManager
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, tmf.getTrustManagers(), null);

        OkHttpClient.Builder builder = new OkHttpClient.Builder();

        builder.sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) tmf.getTrustManagers()[0]);

        builder.hostnameVerifier(new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        });

        OkHttpClient okHttpClient = builder
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .build();

        // creating a RestAdapter using the custom client
        return okHttpClient;
    }

    protected void getDeviceLocation(String deviceAddress) {
        ready = false;
        success = false;
        errorMessage = "";

        try {

            /*ANRequest request = AndroidNetworking.get("https://acallem.ddns.jazztel.es/carfinder/devicelocations")
                    .addQueryParameter("deviceAddress", deviceAddress)
                    .addQueryParameter("last", "true")
                    .build();

            ANResponse<JSONObject> response = request.executeForJSONObject();

            ANError error = response.getError();
            boolean resultado = response.isSuccess();
            String descripcion = error.getErrorDetail();
            String stacktrace = error.getStackTrace().toString();*/

            AndroidNetworking.get("https://acallem.ddns.jazztel.es/carfinder/devicelocations")
                    .addQueryParameter("deviceAddress", deviceAddress)
                    .addQueryParameter("last", "true")
                    .build()
                    .getAsJSONObject(new JSONObjectRequestListener() {
                        @Override
                        public void onResponse(JSONObject response) {
                            Log.d("Respuesta: ", response.toString());
                            // do anything with response
                            try {
                                Location location = new Location();
                                deviceLocation = new DeviceLocation();
                                deviceLocation.setId(response.getString("id"));
                                deviceLocation.setBearer(response.getString("bearer"));
                                deviceLocation.setDeviceAddress(response.getString("deviceAddress"));
                                deviceLocation.setDeviceName(response.getString("deviceName"));
                                deviceLocation.setDate(response.getString("date"));
                                location.setLatitude(response.getJSONObject("location").getDouble("latitude"));
                                location.setLongitude(response.getJSONObject("location").getDouble("longitude"));
                                location.setAccuracy(response.getJSONObject("location").getLong("accuracy"));
                                location.setDescription(response.getJSONObject("location").getString("description"));
                                deviceLocation.setLocation(location);
                                success = true;
                                ready = true;
                            } catch (Exception e) {
                                success = false;
                                errorMessage = e.toString();
                                ready = true;
                                //notifyAll();
                                Log.d("Excepcion: ", e.toString());
                            }
                        }

                        @Override
                        public void onError(ANError error) {
                            success = false;
                            errorMessage = error.getErrorDetail();
                            ready = true;
                            Log.d("Respuesta: ", error.getErrorDetail());
                        }
                    });



        } catch (Exception e) {
            success = false;
            errorMessage = e.toString();
            ready = true;
            Log.d("Excepcion: ", e.toString());
        }
        return;
    }

    protected void storeDeviceLocation() {
        ready = false;
        success = false;
        errorMessage = "";

        try {
            JSONObject jsonDeviceLocation = new JSONObject();
            JSONObject jsonLocation = new JSONObject();
            jsonLocation.put("latitude", deviceLocation.getLocation().getLatitude());
            jsonLocation.put("longitude", deviceLocation.getLocation().getLongitude());
            jsonLocation.put("accuracy", deviceLocation.getLocation().getAccuracy());
            jsonLocation.put("description", deviceLocation.getLocation().getDescription());
            jsonDeviceLocation.put("id", deviceLocation.getId());
            jsonDeviceLocation.put("bearer", deviceLocation.getBearer());
            jsonDeviceLocation.put("deviceName", deviceLocation.getDeviceName());
            jsonDeviceLocation.put("deviceAddress", deviceLocation.getDeviceAddress());
            jsonDeviceLocation.put("date", deviceLocation.getDate());
            jsonDeviceLocation.put("location", jsonLocation);

            /*ANRequest request = AndroidNetworking.post("https://acallem.ddns.jazztel.es/carfinder/devicelocations")
                    .addJSONObjectBody(jsonDeviceLocation)
                    .build();

            ANResponse response = request.executeForJSONObject();
            ANError error = response.getError();
            boolean resultado = response.isSuccess();
            String descripcion = error.getErrorDetail();
            String stacktrace = error.getStackTrace().toString();*/

            AndroidNetworking.post("https://acallem.ddns.jazztel.es/carfinder/devicelocations")
                    .addJSONObjectBody(jsonDeviceLocation)
                    .build()
                    .getAsJSONObject(new JSONObjectRequestListener() {
                        @Override
                        public void onResponse(JSONObject response) {
                            success = true;
                            ready = true;
                            Log.d("Respuesta: ", response.toString());
                        }

                        @Override
                        public void onError(ANError error) {
                            success = false;
                            errorMessage = error.getErrorDetail();
                            ready = true;
                            Log.d("Respuesta:", error.toString());
                        }
                    });
        } catch (Exception e) {
            success = false;
            errorMessage = e.toString();
            ready = true;
            Log.d("Excepcion: ", e.toString());
        }

        return;
    }
}
