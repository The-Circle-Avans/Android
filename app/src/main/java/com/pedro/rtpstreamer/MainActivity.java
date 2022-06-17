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

package com.pedro.rtpstreamer;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Handler;
import android.widget.Toast;

import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR2;
import static android.os.Build.VERSION_CODES.LOLLIPOP;

import com.pedro.rtpstreamer.domain.User;

public class MainActivity extends AppCompatActivity  {


  private final String[] PERMISSIONS = {
      Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA,
      Manifest.permission.WRITE_EXTERNAL_STORAGE
  };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    overridePendingTransition(R.transition.slide_in, R.transition.slide_out);


    if (!hasPermissions(this, PERMISSIONS)) {
      ActivityCompat.requestPermissions(this, PERMISSIONS, 1);
    }

  }



  private void showMinSdkError(int minSdk) {
    String named;
    switch (minSdk) {
      case JELLY_BEAN_MR2:
        named = "JELLY_BEAN_MR2";
        break;
      case LOLLIPOP:
        named = "LOLLIPOP";
        break;
      default:
        named = "JELLY_BEAN";
        break;
    }
    Toast.makeText(this, "You need min Android " + named + " (API " + minSdk + " )",
        Toast.LENGTH_SHORT).show();
  }

  private void showPermissionsErrorAndRequest() {
    Toast.makeText(this, "You need permissions before", Toast.LENGTH_SHORT).show();
    ActivityCompat.requestPermissions(this, PERMISSIONS, 1);
  }

  private boolean hasPermissions(Context context, String... permissions) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
      for (String permission : permissions) {
        if (ActivityCompat.checkSelfPermission(context, permission)
            != PackageManager.PERMISSION_GRANTED) {
          return false;
        }
      }
    }
    return true;
  }
}