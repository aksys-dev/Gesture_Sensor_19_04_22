package com.example.edwinvalarezoa.Gestures_Demos;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.mbientlab.metawear.Route;

import java.io.IOException;
import java.util.UUID;

import bolts.Task;
import bolts.TaskCompletionSource;

public class GamepadIMUController {
    private static final String SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB";
    private static final String TEMP_MAC_ADDRESS= "00:00:00:00:00:00";
    private static final String TAG = "GamepadIMUController";

    private BluetoothDevice device;

    private boolean isConnected;
    private BluetoothSocket socket;

    public GamepadIMUController(BluetoothDevice _device) {
        device=_device;
    }

    /**
     * connect Game pad socket to receive IMU data
     * @return
     */
    public Task<Void> connectAsync() {
        // Create Task
        TaskCompletionSource<Void> connectionTask=new TaskCompletionSource<>();
        BluetoothSocket tmp;
        try {
            if(device.getAddress().equals(TEMP_MAC_ADDRESS)) throw new IOException("No Device:");
            tmp = device.createInsecureRfcommSocketToServiceRecord( UUID.fromString( SPP_UUID ) );
            socket = tmp;
            connectSocket();
            connectionTask.setResult(null);
            return connectionTask.getTask();
        } catch ( IOException e ) {
            Log.d(TAG, "Failed Create Socket: " + e.getMessage() );
            connectionTask.setError(new IOException("Failed Create Socket: " + e.getMessage()));
            return connectionTask.getTask();
        }// try catch
    }// connectAsync

    private void connectSocket() throws IOException {
        if(socket==null) return;
        socket.connect();
        if(socket.isConnected()) isConnected=true;
    }// connectSocket

    /**
     * Game pad socket check
     * @return
     */
    public boolean isConnected() {
        return isConnected;
    }// isConnected

    public BluetoothSocket getSocket() {
        return socket;
    }// getSocket

    public static int getIntfromByte(byte high, byte low) {
        int value = ((high & 0xFF) << 8) + (low & 0xFF);
        if (value >= 32768) value -= 65536;
        return value;
    }// getIntfromByte
}
