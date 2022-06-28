package com.pedro.rtpstreamer.utils;

import android.os.Build;

import androidx.annotation.RequiresApi;

import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.concurrent.ThreadLocalRandom;

import javax.crypto.Cipher;

import kotlin.NotImplementedError;

public class LoginManager
{
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public boolean checkOwnership (String PEMpublicKey, String PEMprivateKey)
    {
        PrivateKey privateKey = generatePrivateKey(PEMprivateKey);
        PublicKey publicKey = generatePublicKey(PEMpublicKey);

        // create challenge
        byte[] challenge = new byte[10000];
        ThreadLocalRandom.current().nextBytes(challenge);

        // Sign using the private key
        try {
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initSign(privateKey);
            sig.update(challenge);
            byte[] signature = sig.sign();

            sig.initVerify(publicKey);
            sig.update(challenge);

            // verify the signature
            return sig.verify(signature);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        // Should be unreachable
        return false;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public boolean checkSignature (String expectedValues, String signature, String PEMpublicKey)
    {
        PublicKey publicKey = generatePublicKey(PEMpublicKey);
        byte[] decrypted = decrypt(signature, publicKey);

        try
        {
            // Create an expected hash
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update((expectedValues).getBytes(StandardCharsets.UTF_8));
            byte[] expectedHash = digest.digest();

            // Turn the byte arrays into comparable strings
            String hexStringExpected = String.format("%064x", new BigInteger(1, expectedHash));
            String stringDecrypted = new String(decrypted);

            // Remove some illegal character from the decrypted string
            stringDecrypted = stringDecrypted.replaceAll("[^\\w^\\d]*", "");

            System.out.println("This was the expected hash ---> " + hexStringExpected);
            System.out.println("This was the decrypted hash --> " + stringDecrypted);

            return (hexStringExpected.equals(stringDecrypted)) ? true : false;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        // Should be unreachable
        return false;
    }

    private byte[] decrypt(String encryptedText, PublicKey publicKey)
    {
        try
        {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, publicKey);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
            {
                byte[] encryptedBase64 = Base64.getDecoder().decode(encryptedText);
                byte[] decrypted = cipher.doFinal(encryptedBase64);
                return decrypted;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        // Should be unreachable
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public PrivateKey generatePrivateKey (String privateKeyPEM)  {
        try
        {
            // Generate the private key from a generated keypair
            PEMParser pemParser = new PEMParser(new StringReader(privateKeyPEM));
            JcaPEMKeyConverter convert = new JcaPEMKeyConverter();
            Object object = pemParser.readObject();
            KeyPair kp = convert.getKeyPair((PEMKeyPair) object);

            return kp.getPrivate();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        // Should be unreachable
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private PublicKey generatePublicKey(String PEMpublicKey) {
        // format the public key
        PEMpublicKey = PEMpublicKey
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace(System.lineSeparator(), "")
                .replace("-----END PUBLIC KEY-----", "");

        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
            {
                // Convert the public key to base64
                byte[] bytes = Base64.getDecoder().decode(PEMpublicKey);

                // Instantiate the keyspec and keyfactory
                X509EncodedKeySpec spec = new X509EncodedKeySpec(bytes);
                KeyFactory kf = KeyFactory.getInstance("RSA");

                // Generate the public key
                return kf.generatePublic(spec);
            }
        } catch (Exception e)
        {
            e.printStackTrace();
        }

        // Should be unreachable
        return null;
    }
}
