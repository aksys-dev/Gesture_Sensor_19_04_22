package com.example.edwinvalarezoa.Gestures_Demos;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.mbientlab.metawear.Route;
import com.mbientlab.metawear.android.BtleService;

import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.ops.impl.indexaccum.IAMax;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.NDArrayIndex;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.Callable;

import bolts.Continuation;
import bolts.Task;

import static com.example.edwinvalarezoa.Gestures_Demos.GamepadIMUController.getIntfromByte;
import static java.lang.StrictMath.abs;

public class MainActivity extends AppCompatActivity implements ServiceConnection {
    private static final int IMU_LENGTH = 16;
    private static final byte DATA_IMU_HEADER_PREFIX = (byte) 0x00;
    private static final byte CMD_ENABLE_IMU = (byte) 0x80;
    private static final byte CMD_DISABLE_IMU = (byte) 0x81;
    private static final byte DATA_IMU_HEADER = (byte) 0x55;

    private static final String TAG = "Hand_Gestures";

    //Initialization of Variables
    private final Handler handler = new Handler();
    public ArrayList<Float> input_Accx = new ArrayList<>();
    public ArrayList<Float> input_Accy = new ArrayList<>();
    public ArrayList<Float> input_Accz = new ArrayList<>();
    public ArrayList<Float> input_Gyrx = new ArrayList<>();
    public ArrayList<Float> input_Gyry = new ArrayList<>();
    public ArrayList<Float> input_Gyrz = new ArrayList<>();
    public Boolean valor = true;

    public int first_time = 0;
    public int sample;
    private TextView inference, num_sample;
    private String MW_MAC_ADDRESS = "00:00:00:00:00:00";
    static final String GAMEPAD_NAME_RELEASE = "TIMGamepad";
    static final String GAMEPAD_NAME_DEBUG = "CSRB5342 - NEW";
    private GamepadIMUController board;
    public String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Download/Control_Gestures";
    public long startTime;
    public long endTime;

    // CONFIG CUSTOMIZING FOR GAMEPAD
    public void Config_Sensor() {
        //waiting 2 sconds to turn on the bluetooth
        startTime = System.currentTimeMillis();
        long tmp = 0;
        while (tmp < 2) {
            endTime = System.currentTimeMillis();
            tmp = (endTime - startTime) / 1000;
        }
        Toast.makeText(MainActivity.this, "Connection Initialized", Toast.LENGTH_SHORT).show();
        final BluetoothManager btManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        // EDIT START
        Set<BluetoothDevice> set = btManager.getAdapter().getBondedDevices();
        for (BluetoothDevice device : set) {
            if (device.getName().contains(GAMEPAD_NAME_DEBUG) || device.getName().contains(GAMEPAD_NAME_RELEASE)) {
                MW_MAC_ADDRESS = device.getAddress();
                break;
                // first pad's mac address
            }
        }
        final BluetoothDevice remoteDevice = btManager.getAdapter().getRemoteDevice(MW_MAC_ADDRESS);
        // ~ Find Pad and Select
        startTime = System.currentTimeMillis();
        //board = serviceBinder.getMetaWearBoard(remoteDevice);
        board = new GamepadIMUController(remoteDevice);
        board.connectAsync().onSuccessTask(new Continuation<Void, Task<Route>>() {
            @Override
            public Task<Route> then(Task<Void> task) throws Exception {
                Log.i("Hand_Gestures", "Connection to " + MW_MAC_ADDRESS);

                Task.callInBackground(new Callable<Void>() {
                    public Void call() {
                        // Do a bunch of stuff.
                        int bytes = 0;
                        byte[] recv = new byte[2];
                        byte[] imuBuf = new byte[1];
                        byte[] otherBuf = new byte[4];
                        byte[] data = new byte[IMU_LENGTH];
//                        int imuCount=0;

//                        long hzStartTime;
//                        long hzEndTime;
                        long partStartTime;
                        long partEndTime;

                        InputStream inputStream;
                        try {
                            BluetoothSocket socket = board.getSocket();
                            inputStream = socket.getInputStream();
//                            hzStartTime=System.nanoTime();
                            partStartTime = 0;
                            while (board.isConnected() && inputStream != null) {
                                bytes = inputStream.read(recv);
                                if (recv[0] == DATA_IMU_HEADER_PREFIX && recv[1] == DATA_IMU_HEADER) {
                                    inputStream.read(otherBuf);
                                    inputStream.read(data, 0, getIntfromByte(otherBuf[0], otherBuf[1]));
                                    partEndTime = System.nanoTime();
                                    /*
                                    Delay is initialized 29500000L to save value.
                                    It resulted almost 30Hz, but it should be changing in other device
                                     */
                                    if (partEndTime - partStartTime > 29500000L) {
                                        input_Accx.add((float) getIntfromByte(data[2], data[3]) * -0.001f);
                                        if (input_Accx.size() > 60) {
                                            input_Accx.remove(0);
                                        }
                                       // input_Accy.add((float) getIntfromByte(data[4], data[5]) * -0.001f - 0.98f); // -0.98 remove gravity
                                        input_Accy.add((float) getIntfromByte(data[4], data[5]) * -0.001f);
                                        if (input_Accy.size() > 60) {
                                            input_Accy.remove(0);
                                        }
                                        input_Accz.add((float) getIntfromByte(data[0], data[1]) * -0.001f);
                                        if (input_Accz.size() > 60) {
                                            input_Accz.remove(0);
                                        }
                                        input_Gyrx.add((float) getIntfromByte(data[8], data[9]) * -1);
                                        if (input_Gyrx.size() > 60) {
                                            input_Gyrx.remove(0);
                                        }
                                        input_Gyry.add((float) getIntfromByte(data[10], data[11]) * -1);
                                        if (input_Gyry.size() > 60) {
                                            input_Gyry.remove(0);
                                        }
                                        input_Gyrz.add((float) getIntfromByte(data[6], data[7]) * -1);
                                        if (input_Gyrz.size() > 60) {
                                            input_Gyrz.remove(0);
                                        }
                                        /*if(input_Accy.get(input_Accy.size()-1) > 1.2) {
                                            Log.d(TAG, "call: " + String.format(Locale.ENGLISH, "%f, %f, %f, %f, %f, %f",
                                                    input_Accx.get(input_Accx.size() - 1),
                                                    input_Accy.get(input_Accz.size() - 1),
                                                    input_Accz.get(input_Accy.size() - 1),
                                                    input_Gyrx.get(input_Gyrx.size() - 1),
                                                    input_Gyry.get(input_Gyry.size() - 1),
                                                    input_Gyrz.get(input_Gyrz.size() - 1))
                                            );
                                        }*/
//                                        imuCount++;
                                        partStartTime = System.nanoTime();
                                    }
                                } else if (recv[0] == CMD_ENABLE_IMU) {
                                    inputStream.read(imuBuf);
                                    Log.d(TAG, "call: IMU On");
                                } else if (recv[0] == CMD_DISABLE_IMU) {
                                    inputStream.read(imuBuf);
                                    Log.d(TAG, "call: IMU Off");
                                } else {
                                    inputStream.read(otherBuf);
                                    inputStream.read(data, 0, getIntfromByte(otherBuf[0], otherBuf[1]));
                                    Log.d(TAG, "call: else case");
                                }
                                /*hzEndTime=System.nanoTime();
                                if((hzEndTime-hzStartTime)>1000000000L) {
                                    Log.d(TAG, "call: "+imuCount);
                                    imuCount=0;
                                    hzStartTime=System.nanoTime();
                                }// if*/
                            }// while
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return null;
                    }
                });

                return null;
            }
        }).continueWith(new Continuation<Route, Void>() {
            @Override
            public Void then(Task<Route> task) throws Exception {
                if (task.isFaulted()) {
                    Log.e("Hand_Gestures", board.isConnected() ? "Error setting up route" : "Error connecting", task.getError());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "Connection Fail", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Log.i("Hand_Gestures", "Connected");
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "Connection OK", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                return null;
            }
        });
    }
    // CONFIG CUSTOMIZING FOR GAMEPAD END

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getApplicationContext().bindService(new Intent(this, BtleService.class), this, Context.BIND_AUTO_CREATE);

        inference = (TextView) findViewById(R.id.Prediction);
        num_sample = (TextView) findViewById(R.id.Inference_Probability);

        // check if the bluetooth is compatible
        BluetoothAdapter BTAdapter = BluetoothAdapter.getDefaultAdapter();
        if (BTAdapter == null) {
            new AlertDialog.Builder(this)
                    .setTitle("Not compatible")
                    .setMessage("Your phone does not support Bluetooth")
                    .setPositiveButton("Exit", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            System.exit(0);
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        }
        //request turn on bluetooth
        if (!BTAdapter.isEnabled()) {
            Intent enableBT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBT, 1);
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        try {
            board.getSocket().close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Unbind the service when the activity is destroyed
        getApplicationContext().unbindService(this);
    }// onDestroy

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        // Typecast the binder to the service's LocalBinder class
        //serviceBinder = (BtleService.LocalBinder) service;
        Config_Sensor();
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
    }

    //GESTURE RECONGNITION APP
    public void OnclickStart(View v) throws IOException {
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Permissions to read and write on the phone
            if (shouldShowRequestPermissionRationale(
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {
            }

            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    1);
        }
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            if (shouldShowRequestPermissionRationale(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            }

            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    1);
        }
        //start sensing
        board.getSocket().getOutputStream().write(CMD_ENABLE_IMU);
        input_Gyrx.clear();
        input_Gyry.clear();
        input_Gyrz.clear();
        input_Accx.clear();
        input_Accy.clear();
        input_Accz.clear();
        valor = true;
        sample = 0;
        //Classes labels
        final Handler circle = new Handler() {
            @Override
            public void handleMessage(Message msg) { inference.setText("Circle");}};
        final Handler Rigth = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                inference.setText("Rigth");
            }
        };
        final Handler Up = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                inference.setText("Up");
            }
        };
        final Handler Left = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                inference.setText("Left");
            }
        };
        final Handler Down = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                inference.setText("Down");
            }
        };
        final Handler NO_OPT = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                inference.setText("....");
            }
        };

        final Handler NUM = new Handler() {
            @Override
            public void handleMessage(Message msg) { num_sample.setText("Epoch #" + Integer.toString(sample));}
        };
        new Thread() {
            public void run() {
                while (valor) {
                    try {
                        //collecting data for the first prediction (only first prediction)
                        if (first_time == 0) {
                            sleep(2000);
                            first_time = 1;
                        }
                        ArrayList arreglo = new ArrayList();
                        arreglo.clear();
                        INDArray entrada;
                        //The amplitude of the SMA should be bigger than the threshold (threshold value 1.7)
                        //SMA=|Accelerometer channel X|+| Accelerometer channel Y |+| Accelerometer channel Z |
                        if (input_Accy.size() == 60 && (abs(abs((float) input_Accy.get(59))) + abs((float) input_Accx.get(59)) + abs((float) input_Accz.get(59))) > 1.7) {
                            //waiting until the gesture finish
                            if (first_time > 0) {
                                sleep(1850);
                            }
                            arreglo.add(input_Accx.clone());
                            arreglo.add(input_Accy.clone());
                            arreglo.add(input_Accz.clone());
                            arreglo.add(input_Gyrx.clone());
                            arreglo.add(input_Gyry.clone());
                            arreglo.add(input_Gyrz.clone());
                            input_Gyrx.clear();
                            input_Gyry.clear();
                            input_Gyrz.clear();
                            input_Accx.clear();
                            input_Accy.clear();
                            input_Accz.clear();
                            //preparing the data
                            entrada = Prepare_data(arreglo);
                            entrada=demean_windows(entrada);
                            //LSTM predction
                            int predic_General = Inference_General(entrada);
                            //SHOW THE INFERENCE
                            sample = sample + 1;
                            NUM.sendEmptyMessage(0);
                            if (predic_General == 0) {
                                Rigth.sendEmptyMessage(0);
                            } else if (predic_General == 1) {
                                Up.sendEmptyMessage(0);
                            } else if (predic_General == 2) {
                                Left.sendEmptyMessage(0);
                            } else if (predic_General == 3) {
                                Down.sendEmptyMessage(0);
                            } else if (predic_General == 4) {
                                circle.sendEmptyMessage(0);
                            }
                            //waiting time to collect new data
                            sleep(1000);
                        } else {
                            NO_OPT.sendEmptyMessage(0);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (ArrayIndexOutOfBoundsException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }

    //stop option
    public void Onclickstop(View v) throws IOException {
        valor = false;
        inference.setText("Thanks");
        first_time = 1;
        board.getSocket().getOutputStream().write(CMD_DISABLE_IMU);
    }

    //Algorithm to remove the mean of the signals
    public INDArray demean_windows(INDArray vector) {
        INDArray data= Nd4j.zeros(1,6,60);
        for (int i_t=0;i_t<6;i_t++) {
            float mean_val = 0;
            for (int j_t = 0; j_t < 60; j_t++) {
                mean_val = mean_val + vector.getFloat(new int[]{0, i_t, j_t});
            }
            mean_val = mean_val / 60;
            float value = (float) 0.0;
            for (int j_t = 0; j_t < 60; j_t++) {
                value = vector.getFloat(new int[]{0, i_t, j_t}) - mean_val;
                data.putScalar(new int[]{0, i_t, j_t}, value);
            }
        }
        return data;
    }

    //Algorithm to remove the mean of the signals
    public float remove_g(ArrayList<Float> vector) {
        float sum_val = 0;
        for (int j_t = 0; j_t < 60; j_t++) {
            sum_val = sum_val + vector.get(j_t);
        }
        return (sum_val / 60);
    }

    //PREPARE THE ROW DATA
    public INDArray Prepare_data(ArrayList lista){
        ArrayList window =lista;
        //demean and normalizing
        INDArray datos= Nd4j.zeros(1,6,60);
        Float max[]=new Float[6];
        Float min[]=new Float[6];
        // maximum and minimum values for normalizing
        max[0]= (float)1.30;max[1]= (float)1.70;max[2]= (float)1.30;max[3]= (float)260;max[4]= (float)220;max[5]= (float)200;
        min[0]=(float)-1.30;min[1]=(float)-1.70;min[2]=(float)-2;min[3]=(float)-500;min[4]=(float)-220;min[5]=(float)-200;

        for (int i_t=0;i_t<6;i_t++){
            ArrayList<Float> temporal= (ArrayList<Float>) window.get(i_t);
            float mean_val=remove_g(temporal);
            for (int j_t=0;j_t<60;j_t++){
                Float value_temp=temporal.get(j_t);
                value_temp=value_temp-mean_val;
                Float val=(value_temp-((max[i_t]+min[i_t])/2))/((max[i_t]-min[i_t])/2);
                datos.putScalar(new int[]{0, i_t, j_t}, val);
            }
        }
        return datos;
    }
    //LOAD THE RNN MODEL AND MAKES THE INFERENCE
    public int Inference_General (INDArray Input)
    {
        int last_index=0;
        String texto_modelo= "/App_5_Control_Gestures.zip";
        try {
            MultiLayerNetwork General_Model = ModelSerializer.restoreMultiLayerNetwork(new File(path+texto_modelo));
            INDArray predicted = General_Model.output(Input, false);
            INDArray last_probability1=predicted.get(NDArrayIndex.point(0), NDArrayIndex.all(), NDArrayIndex.point(60-1));
            last_index= Nd4j.getExecutioner().execAndReturn(new IAMax(last_probability1)).getFinalResult();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return last_index;
    }
}
