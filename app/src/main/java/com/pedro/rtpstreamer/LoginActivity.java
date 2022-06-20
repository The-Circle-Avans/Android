package com.pedro.rtpstreamer;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.pedro.rtpstreamer.api.ApiClient;
import com.pedro.rtpstreamer.api.LoginResponse;
import com.pedro.rtpstreamer.api.UserService;
import com.pedro.rtpstreamer.defaultexample.ExampleRtmpActivity;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class LoginActivity extends AppCompatActivity {

    // TO-DO ADD VALIDATION

    public static String PREFS_NAME="MyPrefsFile";
    private static final String TAG = "APICALLHIERRRR";
    private Button signIn;
    private EditText prv, user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        signIn=findViewById(R.id.button);
        prv=findViewById(R.id.privateKey);
        user=findViewById(R.id.userName);
        signIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String privateKey = prv.getText().toString();
                String userName = user.getText().toString();

                checkUsername(userName, privateKey);

            }
        });
    }

    private void checkUsername(String userName, String privateKey) {
        Retrofit retrofit = ApiClient.getRetrofitInstance();
        final UserService api = retrofit.create(UserService.class);

        Call<LoginResponse> call = api.getUserByName(userName);
        call.enqueue(new Callback<LoginResponse>() {


            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful()) {
                    response.body();
                    Integer id = response.body().getId();
                    String username = response.body().getUsername();
                    String publicKey = response.body().getPublicKey();

                    SharedPreferences sharedPreferences = getSharedPreferences(LoginActivity.PREFS_NAME, 0);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putBoolean("hasLoggedIn", true);
                    editor.putString("privateKey", privateKey);
                    editor.putString("userName", userName);
                    editor.commit();

                    startActivity(new Intent(LoginActivity.this, ExampleRtmpActivity.class));
                    finish();
                } else {
                    try {
                        Toast.makeText(getApplicationContext(),response.errorBody().string(),Toast.LENGTH_SHORT).show();
                        String error = response.errorBody().string();
                        Log.i(TAG, error);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                Log.i(TAG, call.toString());
            }
        });


    }
}