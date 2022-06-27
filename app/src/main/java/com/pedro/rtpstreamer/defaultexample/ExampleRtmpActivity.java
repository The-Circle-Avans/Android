/*
 * Copyright (C) 2021 pedroSG94.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pedro.rtpstreamer.defaultexample;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.pedro.encoder.input.video.CameraOpenException;
import com.pedro.rtmp.utils.ConnectCheckerRtmp;
import com.pedro.rtplibrary.rtmp.RtmpCamera1;
import com.pedro.rtpstreamer.LoginActivity;
import com.pedro.rtpstreamer.MainActivity;
import com.pedro.rtpstreamer.R;
import com.pedro.rtpstreamer.RecyclerAdapter;
import com.pedro.rtpstreamer.api.ApiClient;
import com.pedro.rtpstreamer.api.LoginResponse;
import com.pedro.rtpstreamer.api.UserService;
import com.pedro.rtpstreamer.domain.ChatMessage;
import com.pedro.rtpstreamer.utils.LoginManager;
import com.pedro.rtpstreamer.utils.PathUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import io.socket.engineio.client.transports.Polling;
import io.socket.engineio.client.transports.WebSocket;
import okhttp3.Headers;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

/**
 * More documentation see:
 * {@link com.pedro.rtplibrary.base.Camera1Base}
 * {@link com.pedro.rtplibrary.rtmp.RtmpCamera1}
 */
public class ExampleRtmpActivity extends AppCompatActivity
    implements ConnectCheckerRtmp, View.OnClickListener, SurfaceHolder.Callback {

  private RtmpCamera1 rtmpCamera1;
  private Button button;
  private String currentDateAndTime = "";
  private File folder;
  private String TAG = "RTMPACTivitY";
  private LoginManager lm = new LoginManager();
  private String PEMKeyChat = "";

  RecyclerAdapter mAdapter;
  RecyclerView mRecyclerview;
  ArrayList<ChatMessage> chats = new ArrayList<>();


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    SharedPreferences sharedPreferences = getSharedPreferences(LoginActivity.PREFS_NAME, 0);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    setContentView(R.layout.activity_example);
    folder = PathUtils.getRecordPath();
    SurfaceView surfaceView = findViewById(R.id.surfaceView);
    button = findViewById(R.id.b_start_stop);
    button.setOnClickListener(this);
    Button switchCamera = findViewById(R.id.switch_camera);
    switchCamera.setOnClickListener(this);
    rtmpCamera1 = new RtmpCamera1(surfaceView, this);
    rtmpCamera1.setAuthorization(sharedPreferences.getString("userName", null), sharedPreferences.getString("privateKey", null));
    rtmpCamera1.setReTries(10);

    //setup recyclerview for chat

    mRecyclerview = findViewById(R.id.recyclerView);
    mRecyclerview.setLayoutManager(new LinearLayoutManager(this));
    mAdapter = new RecyclerAdapter(this, chats);
    mRecyclerview.setAdapter(mAdapter);

    surfaceView.getHolder().addCallback(this);
  }

  @Override
  public void onConnectionStartedRtmp(String rtmpUrl) {
  }

  @Override
  public void onConnectionSuccessRtmp() {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Toast.makeText(ExampleRtmpActivity.this, "Connection success", Toast.LENGTH_SHORT).show();
      }
    });
  }

  @Override
  public void onConnectionFailedRtmp(final String reason) {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        if (rtmpCamera1.reTry(5000, reason, null)) {
          Toast.makeText(ExampleRtmpActivity.this, "Retry", Toast.LENGTH_SHORT)
              .show();
        } else {
          Toast.makeText(ExampleRtmpActivity.this, "Connection failed. " + reason, Toast.LENGTH_SHORT)
              .show();
          rtmpCamera1.stopStream();
          button.setText(R.string.start_button);
        }
      }
    });
  }

  @Override
  public void onNewBitrateRtmp(final long bitrate) {

  }

  @Override
  public void onDisconnectRtmp() {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Toast.makeText(ExampleRtmpActivity.this, "Disconnected", Toast.LENGTH_SHORT).show();
      }
    });
  }

  @Override
  public void onAuthErrorRtmp() {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Toast.makeText(ExampleRtmpActivity.this, "Auth error", Toast.LENGTH_SHORT).show();
        rtmpCamera1.stopStream();
        button.setText(R.string.start_button);
      }
    });
  }

  @Override
  public void onAuthSuccessRtmp() {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Toast.makeText(ExampleRtmpActivity.this, "Auth success", Toast.LENGTH_SHORT).show();
      }
    });
  }

  @Override
  public void onClick(View view) {
    switch (view.getId()) {
      case R.id.b_start_stop:
        if (!rtmpCamera1.isStreaming()) {
          if (rtmpCamera1.isRecording()
              || rtmpCamera1.prepareAudio() && rtmpCamera1.prepareVideo()) {

            button.setText(R.string.stop_button);
            SharedPreferences sharedPreferences = getSharedPreferences(LoginActivity.PREFS_NAME, 0);
            String userName = sharedPreferences.getString("userName", null);
            String streamUrl = "rtmp://10.0.2.2/live/" + userName;
            try {
              establishSocketConnection(userName);
            } catch (JSONException e) {
              e.printStackTrace();
            }
            rtmpCamera1.startStream(streamUrl);
          } else {
            Toast.makeText(this, "Error preparing stream, This device cant do it",
                Toast.LENGTH_SHORT).show();
          }
        } else {
          button.setText(R.string.start_button);
          rtmpCamera1.stopStream();
        }
        break;
      case R.id.switch_camera:
        try {
          rtmpCamera1.switchCamera();
        } catch (CameraOpenException e) {
          Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        break;
      default:
        break;
    }
  }

  private void establishSocketConnection(String userName) throws JSONException {
    URI uri = URI.create("ws://10.0.2.2:3500");
    IO.Options options = IO.Options.builder()
            // IO factory options
            .setForceNew(true)
            .setMultiplex(true)

            // low-level engine options
            .setTransports(new String[] { WebSocket.NAME })
            .setUpgrade(true)
            .setRememberUpgrade(false)
            .setPath("/socket.io/")
            .setQuery(null)
            .setExtraHeaders(null)

            // Manager options
            .setReconnection(true)
            .setReconnectionAttempts(Integer.MAX_VALUE)
            .setReconnectionDelay(1_000)
            .setReconnectionDelayMax(5_000)
            .setRandomizationFactor(0.5)
            .setTimeout(20_000)

            // Socket options
            .setAuth(null)
            .build();
    Socket mSocket = IO.socket(uri, options);
    mSocket.connect();
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("msg", "Connecting streamer");
    jsonObject.put("username", "Streamer");
    jsonObject.put("stream", userName);

    mSocket.on(Socket.EVENT_CONNECT, (args -> {
      Log.i(TAG, "we are connected to the socket");
      Log.i(TAG, "Socket ID: " + mSocket.id());

    })).emit("joinStream", jsonObject)
            .on("message", new Emitter.Listener() {
              @RequiresApi(api = Build.VERSION_CODES.KITKAT)
              @Override
              public void call(Object... args) {
                JSONObject obj = (JSONObject) args[0];

                try {
                  String sig = obj.getString("signature");

                  JSONObject deeper = null;
                  deeper = obj.getJSONObject("message");
                  String chat = deeper.getString("text");
                  String sender = deeper.getString("username");
                  Log.i(TAG, chat);

                  if (PEMKeyChat.equals(""))
                  {
                    Retrofit retrofit = ApiClient.getRetrofitInstance();
                    final UserService api = retrofit.create(UserService.class);

                    Call<LoginResponse> call = api.getUserByName("chat-backend");
                    call.enqueue(new Callback<LoginResponse>() {
                      @Override
                      public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {

                        Headers headers = response.headers();
                        response.body();

                        String username = response.body().getUsername();
                        String PEMuserPublicKey = response.body().getPublicKey();
                        String PEMserverPublicKey = response.body().getTruYouPublicKey();

                        String truYouSig = headers.get("x-hash");

                        if (lm.checkSignature(username + PEMuserPublicKey, truYouSig, PEMserverPublicKey))
                        {
                          PEMKeyChat = PEMuserPublicKey;
                        }
                        else
                        {
                          Toast.makeText(getApplicationContext(), "Unable to authenticate the server!", Toast.LENGTH_SHORT).show();
                          Log.i(TAG, "Unable to authenticate the server!");
                          return;
                        }
                      }

                      @Override
                      public void onFailure(Call<LoginResponse> call, Throwable t) {
                        Toast.makeText(getApplicationContext(), "Unable to authenticate the server!", Toast.LENGTH_SHORT).show();
                        Log.i(TAG, "Unable to authenticate the server!");
                      }
                    });
                  }

                  if (lm.checkSignature(chat, sig, PEMKeyChat))
                  {
                    runOnUiThread(new Runnable() {
                      @Override
                      public void run() {
                        if (!chat.equals("Welcome to the chat!")) {
                          chats.add(new ChatMessage(sender, chat));
                          mAdapter.notifyDataSetChanged();
                        }
                      }
                    });
                  }
                } catch (JSONException e) {
                  e.printStackTrace();
                }
              }
            });
  }


  @Override
  public void surfaceCreated(SurfaceHolder surfaceHolder) {

  }

  @Override
  public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
    rtmpCamera1.startPreview();
  }

  @Override
  public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 && rtmpCamera1.isRecording()) {
      rtmpCamera1.stopRecord();
      PathUtils.updateGallery(this, folder.getAbsolutePath() + "/" + currentDateAndTime + ".mp4");
      Toast.makeText(this,
          "file " + currentDateAndTime + ".mp4 saved in " + folder.getAbsolutePath(),
          Toast.LENGTH_SHORT).show();
      currentDateAndTime = "";
    }
    if (rtmpCamera1.isStreaming()) {
      rtmpCamera1.stopStream();
      button.setText(getResources().getString(R.string.start_button));
    }
    rtmpCamera1.stopPreview();
  }
}
