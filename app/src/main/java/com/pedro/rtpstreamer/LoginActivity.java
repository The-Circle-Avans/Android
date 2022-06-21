package com.pedro.rtpstreamer;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
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
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.concurrent.ThreadLocalRandom;

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

    private void checkUsername(String userName, String privateKeyPEM) {
        Retrofit retrofit = ApiClient.getRetrofitInstance();
        final UserService api = retrofit.create(UserService.class);

        Call<LoginResponse> call = api.getUserByName(userName);
        call.enqueue(new Callback<LoginResponse>() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful()) {
                    response.body();
                    String id = response.body().getId();
                    String username = response.body().getUsername();
                    String publicKeyPEM = response.body().getPublicKey();

                    RSAPublicKey publicKey = null;
                    RSAPrivateKey privateKey = null;
                    try {
                        publicKey = generatePublicKey(publicKeyPEM);
                        privateKey = generatePrivateKey(privateKeyPEM);
                    } catch (NoSuchAlgorithmException e) {
                        e.printStackTrace();
                    } catch (InvalidKeySpecException e) {
                        e.printStackTrace();
                    }

                    SharedPreferences sharedPreferences = getSharedPreferences(LoginActivity.PREFS_NAME, 0);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("privateKey", privateKeyPEM);
                    editor.putString("userName", userName);
                    editor.commit();

                    boolean verified = false;

                    try {
                        // create challenge
                        byte[] challenge = new byte[10000];
                        ThreadLocalRandom.current().nextBytes(challenge);

                        // sing using the private key
                        Signature sig = Signature.getInstance("SHA256withRSA");
                        sig.initSign(privateKey);
                        sig.update(challenge);
                        byte[] signature = sig.sign();

                        sig.initVerify(publicKey);
                        sig.update(challenge);

                        // verify the signature
                        verified = sig.verify(signature);
                    } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException e) {
                        e.printStackTrace();
                    }

                    if (verified)
                    {
                        editor.putBoolean("hasLoggedIn", true);
                        startActivity(new Intent(LoginActivity.this, ExampleRtmpActivity.class));
                        finish();
                    }
                    else
                    {
                        Toast.makeText(getApplicationContext(), "Keypair could not be verified", Toast.LENGTH_SHORT).show();
                        Log.i(TAG, "Could not verify keypair");
                    }
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
                Log.i(TAG, call.toString() + " " + t.getMessage());
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private RSAPrivateKey generatePrivateKey (String privateKeyPEM) throws NoSuchAlgorithmException, InvalidKeySpecException {
        privateKeyPEM = privateKeyPEM
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replaceAll(System.lineSeparator(), "")
                .replace("-----END RSA PRIVATE KEY-----", "");

        byte[] encoded = Base64.decode(privateKeyPEM, 0);

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);

        return (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private RSAPublicKey generatePublicKey (String publicKeyPEM) throws NoSuchAlgorithmException, InvalidKeySpecException {
        // format the public key
        publicKeyPEM = publicKeyPEM
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replaceAll(System.lineSeparator(), "")
                .replace("-----END PUBLIC KEY-----", "");

        // convert the PEM public key to a byte array
        byte[] encoded = Base64.decode(publicKeyPEM, 0);

        KeyFactory keyFactory = null;

        keyFactory = KeyFactory.getInstance("RSA");
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
        return (RSAPublicKey) keyFactory.generatePublic(keySpec);
    }
}