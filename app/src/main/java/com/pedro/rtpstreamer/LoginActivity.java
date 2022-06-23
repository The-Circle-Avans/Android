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

    private void checkUsername(String userName, String privateKeyPEM) {
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
                    String publicKeyPEM = response.body().getPublicKey();
                    String truYouPublicKeyPEM = response.body().getTruYouPublicKey();

                    //TODO Verify TruYou's signature

                    // Get the signature from the response's header
                    String truYouSig = headers.get("x-hash");

                    try
                    {
                        // Generate a usable version of TruYou's public key
                        RSAPublicKey truYouPublicKey = generatePublicKey(truYouPublicKeyPEM);

                        // Convert the signature to base64
                        byte[] truYouSigBase64 = Base64.decode(truYouSig, 0);

                        // Instantiate a cipher and provide it the public key
                        // Also update the data in the cipher to be the signature in Base64
                        Cipher cipher = Cipher.getInstance("RSA");
                        cipher.init(Cipher.DECRYPT_MODE, truYouPublicKey);
                        cipher.update(truYouSigBase64);

                        // Decrypt the signature
                        String result = new String(cipher.doFinal());
                        byte[] resultBase64 = result.getBytes(StandardCharsets.UTF_8);

                        // Create a hash from the required fields
                        MessageDigest digest = MessageDigest.getInstance("SHA-256");
                        digest.update((username + publicKeyPEM).getBytes(StandardCharsets.UTF_8));
                        byte[] hash = digest.digest();

                        String hexStringRequest = String.format("%064x", new BigInteger(1, resultBase64));
                        System.out.println("Hex --------> " + hexStringRequest);

                        String hexStringSelf = String.format("%064x", new BigInteger(1, hash));
                        System.out.println("Hex --------> " + hexStringSelf);
                    } catch (NoSuchAlgorithmException | InvalidKeySpecException | NoSuchPaddingException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException | IOException e) {
                        e.printStackTrace();
                    }

//                    try {
//                        // Convert TruYou's the RSA public key to a usable key
//                        RSAPublicKey truYouPublicKey = generatePublicKey(truYouPublicKeyPEM);
//
//                        // Convert the signatures' string into a byte array
//                        byte[] truYouSigB = Base64.decode(truYouSig, 0);
//                        byte[] hash = Base64.decode(username + publicKeyPEM, 0);
//
//                        // Create a verifier and initialize it with TruYou's signature
//                        Signature verifier = Signature.getInstance("SHA256withRSA");
//
//                        // Add the public key and data to be verified to the verifier
//                        verifier.initVerify(truYouPublicKey);
//                        verifier.update(hash);
//
//                        // Verify the signature with the public key from TruYou
//                        if (!verifier.verify(truYouSigB))
//                        {
//                            Log.i(TAG, "The signature was not from TruYou");
//                            throw new InvalidKeyException();
//                        }
//
//                        Log.i(TAG, "The signature was from TruYou");
//
//                    } catch (NoSuchAlgorithmException e) {
//                        e.printStackTrace();
//                    } catch (InvalidKeySpecException e) {
//                        e.printStackTrace();
//                    } catch (InvalidKeyException e) {
//                        e.printStackTrace();
//                    } catch (SignatureException e) {
//                        e.printStackTrace();
//                    }

                    // Create the private and public keys
                    PublicKey publicKey = null;
                    PrivateKey privateKey = null;

                    // Attempt to generate the actual private and public keys
                    try {
                        KeyPair kp = generateKeyPair(privateKeyPEM);
                        privateKey = kp.getPrivate();
                        publicKey = kp.getPublic();
                    } catch (NoSuchAlgorithmException | InvalidKeySpecException | IOException e) {
                        e.printStackTrace();
                    }

                    // Verify if the private key is indeed of the provided user
                    try {
                        // create challenge
                        byte[] challenge = new byte[10000];
                        ThreadLocalRandom.current().nextBytes(challenge);

                        // Sign using the private key
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
                        SharedPreferences sharedPreferences = getSharedPreferences(LoginActivity.PREFS_NAME, 0);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString("privateKey", privateKeyPEM);
                        editor.putString("userName", userName);
                        editor.putBoolean("hasLoggedIn", true);
                        editor.commit();
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
    private KeyPair generateKeyPair (String privateKeyPEM) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {

        // Generate the private key from a generated keypair
        PEMParser pemParser = new PEMParser(new StringReader(privateKeyPEM));
        JcaPEMKeyConverter convert = new JcaPEMKeyConverter();
        Object object = pemParser.readObject();
        KeyPair kp = convert.getKeyPair((PEMKeyPair) object);

        return kp;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private RSAPublicKey generatePublicKey (String publicKeyPEM) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
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