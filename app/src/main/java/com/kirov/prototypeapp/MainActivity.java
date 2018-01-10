package com.kirov.prototypeapp;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class MainActivity extends AppCompatActivity implements SensorEventListener{
    /*========== Client ==========*/
    private String address;
    private int portNum = 8080;
    private Button buttonConnect;
    private Button buttonDisconnect;
    private EditText addressText;
    private int movement = 0;
    private boolean isDisconnected;
    /*============================*/

    /*========== Step Detector ==========*/
    SensorManager sm;
    Sensor sensor;
    float magnitude;
    float reverse = 0.f;
    float threshold;
    float x;
    float y;
    float z;
    int count = 0;
    String dispCount;
    TextView textView;
    long prevTime = 0;
    long currTime;
    /*===================================*/

    /*========== Database ==========*/
    FirebaseDatabase database = FirebaseDatabase.getInstance("https://unity-test-97af2.firebaseio.com/");
    DatabaseReference myRef = database.getReference("counter");
    /*==============================*/
    PowerManager pm;
    PowerManager.WakeLock wl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pm  = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "my Tag");
        /*========== Client ==========*/
        //addressText = (EditText)findViewById(R.id.ipInput);
        //buttonConnect = (Button)findViewById(R.id.buttonConnect);
        buttonDisconnect = (Button)findViewById(R.id.buttonDisconnect);
        /*buttonConnect.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Client mClient = new Client(addressText.getText().toString());
                mClient.execute();
            }
        });*/
        buttonDisconnect.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                //isDisconnected = true;
                count = 0;
                myRef.setValue(count);
                dispCount = Integer.toString(count);
                textView.setText(dispCount);
            }
        });
        /*============================*/

        /*========== Step Detector ==========*/
        sm = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sm.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST);
        textView = (TextView)findViewById(R.id.displayText);
        /*===================================*/

        wl.acquire();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        wl.release();
    }
    /*========== Client Class ==========*/
    /** Obsolete **/
    private class Client extends AsyncTask<Void, Void, Void> {
        boolean isConnected;
        Client(String add){
            address = add;
        }
        @Override
        protected Void doInBackground(Void... params) {
            Socket socket;
            try{
                //Try to Connect
                Log.d("[ASYNCTASK]", "Connecting...");
                showToast("Connecting...");
                socket = new Socket(address, portNum);
                //Upon Successful Connection
                Log.d("[ASYNCTASK]", "Connected!");
                showToast("Connected!");
                isConnected = true;
                //TODO Implement sending operations
                while (isConnected && !isDisconnected){
                    if (movement == 1){
                        DataOutputStream DOS = new DataOutputStream(socket.getOutputStream());
                        DOS.write(movement);
                        movement = 0;
                    }
                }
                //Close Socket Upon Task Completion
                showToast("Disconnected!");
                socket.close();
            } catch (IOException e){
                e.printStackTrace();
                showToast("Connection Error");
            }
            return null;
        }
    }
    /*==================================*/

    /*========== Step Detector ==========*/
    @Override
    public void onSensorChanged(SensorEvent event) {
        threshold = 15;
        if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            //x = event.values[0];
            y = event.values[1];
            z = event.values[2];

            //Get the Current Time from the device
            currTime = System.currentTimeMillis();
            //Find the magnitude of the Y and Z axis using Pythagorean Theorem
            magnitude = (float) Math.sqrt((y*y) + (z*z));
            //If the magnitude of Y and Z is bigger than the threshold and reverse value
            if (magnitude>threshold && magnitude >= reverse){
                //Time limit used to slow down the detection
                if ((currTime-prevTime) > 200) {
                    movement = 1;
                    //Display the number of steps
                    count = count + 1;
                    myRef.setValue(count);
                    //Send data to Firebase
                    dispCount = Integer.toString(count);
                    textView.setText(dispCount);
                    //Update the reverse magnitude and time
                    reverse = -magnitude;
                    prevTime = currTime;
                    //movement = 0;
                    //myRef.setValue(movement);
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy){}
    /*===================================*/

    public void showToast (final String toast){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), toast, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
