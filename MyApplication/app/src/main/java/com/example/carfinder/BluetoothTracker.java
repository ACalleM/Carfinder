package com.example.carfinder;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class BluetoothTracker extends BroadcastReceiver {

    public BluetoothTracker() {
    }

    public static HashMap<String, String> getBluetoothPairedDevices() {
        HashMap<String, String> bluetoothPairedDevices = new HashMap<String, String>();
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter != null) {
            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
            for (BluetoothDevice bt : pairedDevices) {
                bluetoothPairedDevices.put(bt.getAddress(), bt.getName());
                Method method = null;
                try {
                    method = bt.getClass().getMethod("getAlias");
                    if (method != null) {
                        String alias = (String)method.invoke(bt);
                        if (alias != null) {
                            bluetoothPairedDevices.put(bt.getAddress(), alias);
                        } else {
                            bluetoothPairedDevices.put(bt.getAddress(), bt.getName());
                        }
                    }
                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                    bluetoothPairedDevices.put(bt.getAddress(), bt.getName());
                }
            }
        }
        if (bluetoothPairedDevices.size() == 0) {
            bluetoothPairedDevices.put("08:EB:ED:F4:84:70", "Altavoz roto");
            bluetoothPairedDevices.put("1C:99:4C:A3:81:89", "Coche totota");
            bluetoothPairedDevices.put("00:26:7E:DF:A9:DF", "Coche audi");
            bluetoothPairedDevices.put("00:02:5B:00:FF:04", "Adaptador bluetooth");
            bluetoothPairedDevices.put("00:66:19:2D:DE:37", "Auriculares");
            bluetoothPairedDevices.put("1C:52:16:0B:85:93", "Auriculares malillos");
            bluetoothPairedDevices.put("08:EB:ED:26:DA:4C", "Altavoz pequeño");
        }

        return bluetoothPairedDevices;
    }

    public static String getPhoneName() {
        String phoneName = "Desconocido";
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter != null) {
            phoneName = mBluetoothAdapter.getName();
        }
        return phoneName;
    }

    public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            int state;
            BluetoothDevice bluetoothDevice;
            MainLogic mainLogic = new MainLogic(context);

            switch(action)
            {
                case BluetoothAdapter.ACTION_STATE_CHANGED:
                    state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                    if (state == BluetoothAdapter.STATE_OFF)
                    {
                        mainLogic.sendNotification(context,"Bluetooth","Bluetooth is off");
                    }
                    else if (state == BluetoothAdapter.STATE_TURNING_OFF)
                    {
                        mainLogic.sendNotification(context,"Bluetooth","Bluetooth is turning off");
                    }
                    else if(state == BluetoothAdapter.STATE_ON)
                    {
                        mainLogic.sendNotification(context, "Bluetooth","Bluetooth is on");
                    }
                    break;

                case BluetoothDevice.ACTION_ACL_CONNECTED:
                    bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    break;

                case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                    bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    mainLogic.disconnectLogic(context, BluetoothTracker.getPhoneName(), bluetoothDevice.getName(), bluetoothDevice.getAddress());
                    //Intent sendDisconnectionA = new Intent();
                    //sendDisconnectionA.setAction("com.example.carfinder.BLUETOOTH_DISCONNECTED");
                    //sendImplicitBroadcast(context, sendDisconnectionA);
                    break;
            }

    }

    private static void sendImplicitBroadcast(Context ctxt, Intent i) {
        PackageManager pm = ctxt.getPackageManager();
        List<ResolveInfo> matches = pm.queryBroadcastReceivers(i, 0);

        for (ResolveInfo resolveInfo : matches) {
            Intent explicit = new Intent(i);
            ComponentName cn=
                    new ComponentName(resolveInfo.activityInfo.applicationInfo.packageName,
                            resolveInfo.activityInfo.name);

            explicit.setComponent(cn);
            ctxt.sendBroadcast(explicit);
        }
    }

}
