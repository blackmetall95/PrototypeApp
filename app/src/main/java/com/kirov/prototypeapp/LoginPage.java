package com.kirov.prototypeapp;

/**
 * Created by rzcc5 on 01-Feb-18.
 */

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.ResultCodes;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;


import java.util.Arrays;


public class LoginPage extends AppCompatActivity{

    //Choose an arbitrary request code value
    private static final int RC_SIGN_IN = 123;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null){
            //already signed in
            startActivity(new Intent(LoginPage.this, MainActivity.class));
            finish();
        }
        else{
            //not signed in
            startActivityForResult(
                    AuthUI.getInstance()
                    .createSignInIntentBuilder()
                    .setAvailableProviders(
                            Arrays.asList(new AuthUI.IdpConfig.Builder(AuthUI.EMAIL_PROVIDER).build()))//, new AuthUI.IdpConfig.Builder(AuthUI.GOOGLE_PROVIDER).build()))
                    .build(), RC_SIGN_IN);

        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        //RC_SIGN_IN is the request code passed into startActivityForResult when starting the sign in flow
        if (requestCode == RC_SIGN_IN){
            IdpResponse response = IdpResponse.fromResultIntent(data);

            //Successfully signed in
            if (resultCode == ResultCodes.OK){
                startActivity(new Intent(LoginPage.this, MainActivity.class));
                finish();

                /*//Get firebase user
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                //Get reference
                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users");
                ref.child(user.getUid()).setValue("user_class");*/
            }
            else {
                //Sign in Failed
                if (response == null){
                    //User pressed back button
                    Log.e("Login", "Login canceled by User");
                    return;
                }
                if(response.getErrorCode() == ErrorCodes.NO_NETWORK){
                    Log.e("Login", "No Internet Connection");
                    return;
                }
                if(response.getErrorCode() == ErrorCodes.UNKNOWN_ERROR){
                    Log.e("Login","Unknown Error");
                    return;
                }
                Log.e("Login","Unknown sign in response");
            }
        }
    }
}
