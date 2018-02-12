package com.kirov.prototypeapp;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity implements SensorEventListener{
    /*========== Client ==========*/
    private Button buttonDisconnect;
    //private int movement = 0;
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
    int count2;
    String dispCount;
    TextView textView;
    TextView usernameDisp;
    long prevTime = 0;
    long currTime;
    /*===================================*/

    /*========== Firebase ==========*/
    DatabaseReference ref1;
    DatabaseReference ref2;
    String email, convertedEmail;
    /*==============================*/

    PowerManager pm;
    PowerManager.WakeLock wl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /*============== Firebase ==============*/
        //Get Firebase user
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        //Get reference to users field
        ref1 = FirebaseDatabase.getInstance().getReference("users");
        //Get email from current user
        if (user.getEmail() != null) {
            email = user.getEmail();
        }
        //Convert email to remove illegal characters
        convertedEmail = email.replace('.', ',');
        //Create a new field using the converted email
        ref1.child(convertedEmail).setValue(user.getUid());
        //Create "counter" as child of converted email
        ref1.child(convertedEmail).child("counter").setValue(0);
        //Reference "counter" for read/write
        ref2 = ref1.child(convertedEmail).child("counter");
        //Add listener to "counter"
        ref2.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() != null) {
                    count2 = dataSnapshot.getValue(int.class);
                    //Reset local count variable to 0 on game start
                    if (count2 == 0) {
                        count = 0;
                        dispCount = Integer.toString(count);
                        textView.setText(dispCount);
                    }
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Getting Post failed, log a message
                Log.w("Listener:onCancelled", databaseError.toException());
            }
        });

        usernameDisp = findViewById(R.id.usernameDisplay);
        usernameDisp.setText("UserID: " + convertedEmail);
        /*=======================================*/

        pm  = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "my Tag");

        /*========== Client ==========*/
        buttonDisconnect = (Button)findViewById(R.id.buttonDisconnect);
        buttonDisconnect.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                //isDisconnected = true;
                count = 0;
                ref2.setValue(count);
                dispCount = Integer.toString(count);
                textView.setText(dispCount);
            }
        });
        /*============================*/

        /*========== Step Detector ==========*/
        sm = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sm.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST);
        textView = findViewById(R.id.displayText);
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
    /**private class Client extends AsyncTask<Void, Void, Void> {
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
    }**/
    /*==================================*/

    /*========== Step Detector ==========*/
    @Override
    public void onSensorChanged(SensorEvent event) {
        threshold = 15;
        if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            x = event.values[0];
            y = event.values[1];
            z = event.values[2];

            //Get the Current Time from the device
            currTime = System.currentTimeMillis();
            //Find the magnitude of the Y and Z axis using Pythagorean Theorem
            magnitude = (float) Math.sqrt((x*x) + (y*y) + (z*z));
            //If the magnitude of Y and Z is bigger than the threshold and reverse value
            if (magnitude>threshold && magnitude >= reverse){
                //Time limit used to slow down the detection
                if ((currTime-prevTime) > 200) {
                    //Update counter
                    count = count + 1;
                    //Send data to Firebase
                    ref2.setValue(count);
                    //Display the number of steps
                    dispCount = Integer.toString(count);
                    textView.setText(dispCount);
                    //Update the reverse magnitude and time
                    reverse = -magnitude;
                    prevTime = currTime;
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch(item.getItemId()){
            case R.id.log_out:
                FirebaseAuth.getInstance().signOut();
                /*Intent intent = getIntent();
                finish();
                startActivity(intent);
                return true;*/
                startActivity(new Intent(MainActivity.this, LoginPage.class));
                finish();
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
