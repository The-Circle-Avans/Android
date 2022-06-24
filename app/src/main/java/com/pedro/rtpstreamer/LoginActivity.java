package com.pedro.rtpstreamer;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
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
import com.pedro.rtpstreamer.utils.LoginManager;

import org.bouncycastle.jcajce.provider.digest.SHA256;
import org.bouncycastle.jcajce.provider.digest.SHA512;
import org.bouncycastle.jcajce.util.MessageDigestUtils;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.util.io.pem.PemReader;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.MessageDigest;
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
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import okhttp3.Headers;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.Header;

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

    private void checkUsername(String userName, String PEMuserPrivateKey) {
        Retrofit retrofit = ApiClient.getRetrofitInstance();
        final UserService api = retrofit.create(UserService.class);

        Call<LoginResponse> call = api.getUserByName(userName);
        call.enqueue(new Callback<LoginResponse>() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful()) {
                    boolean verified = false;

                    // Read the response from the API
                    Headers headers = response.headers();
                    response.body();
                    String id = response.body().getId();
                    String username = response.body().getUsername();
                    String PEMuserPublicKey = response.body().getPublicKey();
                    String PEMserverPublicKey = response.body().getTruYouPublicKey();

                    // Get the signature from the response's header
                    String truYouSig = headers.get("x-hash");

                    // Instantiate the login manager
                    LoginManager lm = new LoginManager();

                    try {


                        // Check if there is a matching hash
                        if (lm.checkSignature(username + PEMuserPublicKey, truYouSig, PEMserverPublicKey))
                        {
                            if (lm.checkOwnership(PEMuserPublicKey, PEMuserPrivateKey))
                            {
                                // The user is clear to log in
                                // So we store their credentials in the SharedPreferences for automatic logins in the future
                                SharedPreferences sharedPreferences = getSharedPreferences(LoginActivity.PREFS_NAME, 0);
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putString("privateKey", PEMuserPrivateKey);
                                editor.putString("userName", userName);
                                editor.putBoolean("hasLoggedIn", true);
                                editor.commit();

                                //Redirect the user to the screen where they can start streaming
                                startActivity(new Intent(LoginActivity.this, ExampleRtmpActivity.class));
                                finish();
                            }
                            else
                            {
                                Toast.makeText(getApplicationContext(), "This is the wrong username", Toast.LENGTH_SHORT).show();
                                Log.i(TAG, "The returned public key was from a different user");
                            }
                        }
                        else
                        {
                            Toast.makeText(getApplicationContext(), "The response was compromised", Toast.LENGTH_SHORT).show();
                            Log.i(TAG, "The hashes dit not match");
                        }
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
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
}