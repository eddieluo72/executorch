/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.example.executorchllamademo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import androidx.annotation.NonNull;
import java.io.FileWriter;
import java.io.IOException;

public class Benchmarking extends Activity implements ModelRunnerCallback {
  ModelRunner mModelRunner;

  String mPrompt;
  TextView mTextView;
  StatsDump mStatsDump;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_benchmarking);
    mTextView = findViewById(R.id.log_view);

    Intent intent = getIntent();

    String modelPath = intent.getStringExtra("model_path");
    if (modelPath == null) {
      modelPath = MainActivity.listLocalFile("/data/local/tmp/llama/", ".pte")[0];
    }

    String tokenizerPath = intent.getStringExtra("tokenizer_path");
    if (tokenizerPath == null) {
      tokenizerPath = MainActivity.listLocalFile("/data/local/tmp/llama/", ".bin")[0];
    }

    float temperature = intent.getFloatExtra("temperature", 0.8f);
    mPrompt = intent.getStringExtra("prompt");
    if (mPrompt == null) {
      mPrompt = "The ultimate answer";
    }

    mStatsDump = new StatsDump();
    mModelRunner = new ModelRunner(modelPath, tokenizerPath, temperature, this);
    mStatsDump.loadStart = System.currentTimeMillis();
  }

  @Override
  public void onModelLoaded(int status) {
    mStatsDump.loadEnd = System.currentTimeMillis();
    if (status != 0) {
      Log.e("Benchmarking", "Loaded failed: " + status);
      onGenerationStopped();
      return;
    }
    mStatsDump.generateStart = System.currentTimeMillis();
    mModelRunner.generate(mPrompt);
  }

  @Override
  public void onTokenGenerated(String token) {
    runOnUiThread(
        () -> {
          mTextView.append(token);
        });
  }

  @Override
  public void onStats(String stats) {
    mStatsDump.tokens = stats;
  }

  @Override
  public void onGenerationStopped() {
    mStatsDump.generateEnd = System.currentTimeMillis();
    runOnUiThread(
        () -> {
          mTextView.append(mStatsDump.toString());
        });

    try (FileWriter writer = new FileWriter(getFilesDir() + "/benchmark_results.txt")) {
      writer.write(mStatsDump.toString());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}

class StatsDump {
  long loadStart;
  long loadEnd;
  long generateStart;
  long generateEnd;
  String tokens;

  @NonNull
  @Override
  public String toString() {
    return "loadStart: "
        + loadStart
        + "\nloadEnd: "
        + loadEnd
        + "\ngenerateStart: "
        + generateStart
        + "\ngenerateEnd: "
        + generateEnd
        + "\n"
        + tokens;
  }
}
