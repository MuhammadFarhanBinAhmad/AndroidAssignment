/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.ar.core.examples.kotlin.helloar

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.core.Config
import com.google.ar.core.Config.InstantPlacementMode
import com.google.ar.core.Session
import com.google.ar.core.examples.java.common.helpers.CameraPermissionHelper
import com.google.ar.core.examples.java.common.helpers.DepthSettings
import com.google.ar.core.examples.java.common.helpers.FullScreenHelper
import com.google.ar.core.examples.java.common.helpers.InstantPlacementSettings
import com.google.ar.core.examples.java.common.samplerender.SampleRender
import com.google.ar.core.examples.kotlin.common.helpers.ARCoreSessionLifecycleHelper
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.UnavailableApkTooOldException
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException
import com.google.ar.core.exceptions.UnavailableSdkTooOldException
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException
import android.media.MediaPlayer
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.view.Gravity
import android.widget.FrameLayout
import androidx.core.content.ContextCompat

/**
 * This is a simple example that shows how to create an augmented reality (AR) application using the
 * ARCore API. The application will display any detected planes and will allow the user to tap on a
 * plane to place a 3D model.
 */
class HelloArActivity : AppCompatActivity() {
  companion object {
    private const val TAG = "HelloArActivity"
  }

  lateinit var arCoreSessionHelper: ARCoreSessionLifecycleHelper
  lateinit var view: HelloArView
  lateinit var renderer: HelloArRenderer

  val instantPlacementSettings = InstantPlacementSettings()
  val depthSettings = DepthSettings()
  private var mediaPlayer1: MediaPlayer? = null
  private var mediaPlayer2: MediaPlayer? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Create MediaPlayer instance with the audio file from res/raw
    mediaPlayer1 = MediaPlayer.create(this, R.raw.birds)
    mediaPlayer2 = MediaPlayer.create(this, R.raw.rainforest)

    // Setup ARCore session lifecycle helper and configuration.
    arCoreSessionHelper = ARCoreSessionLifecycleHelper(this)
    // If Session creation or Session.resume() fails, display a message and log detailed
    // information.
    arCoreSessionHelper.exceptionCallback =
      { exception ->
        val message =
          when (exception) {
            is UnavailableUserDeclinedInstallationException ->
              "Please install Google Play Services for AR"
            is UnavailableApkTooOldException -> "Please update ARCore"
            is UnavailableSdkTooOldException -> "Please update this app"
            is UnavailableDeviceNotCompatibleException -> "This device does not support AR"
            is CameraNotAvailableException -> "Camera not available. Try restarting the app."
            else -> "Failed to create AR session: $exception"
          }
        Log.e(TAG, "ARCore threw an exception", exception)
        view.snackbarHelper.showError(this, message)
      }

    // Configure session features, including: Lighting Estimation, Depth mode, Instant Placement.
    arCoreSessionHelper.beforeSessionResume = ::configureSession
    lifecycle.addObserver(arCoreSessionHelper)

    // Set up the Hello AR renderer.
    renderer = HelloArRenderer(this)
    lifecycle.addObserver(renderer)

    // Set up Hello AR UI.
    view = HelloArView(this)
    lifecycle.addObserver(view)
    setContentView(view.root)

    // Sets up an example renderer using our HelloARRenderer.
    SampleRender(view.surfaceView, renderer, assets)

    depthSettings.onCreate(this)
    instantPlacementSettings.onCreate(this)

    // Helper function to convert dp to pixels.
    fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()



// Create a horizontal LinearLayout to hold both buttons
    val buttonLayout = LinearLayout(this).apply {
      orientation = LinearLayout.HORIZONTAL
      layoutParams = FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.WRAP_CONTENT,
        FrameLayout.LayoutParams.WRAP_CONTENT
      ).apply {
        gravity = Gravity.BOTTOM or Gravity.START  // Position at the bottom left
        leftMargin = dpToPx(16)
        bottomMargin = dpToPx(16)
      }
    }

    // Create the second button (New Button beside Show Canvas)
    val LeftButton = Button(this).apply {
      text = "Next"
      textSize = 12f  // Adjust text size as needed (in sp)

      // Load the drawable and set custom size
      val drawable = ContextCompat.getDrawable(context, R.drawable.left_arrow)
      val drawableSize = dpToPx(24)  // Keep the image at 24dp x 24dp
      drawable?.setBounds(0, 0, drawableSize, drawableSize)

      // Set the drawable to the left of the text
      setCompoundDrawables(drawable, null, null, null)
      compoundDrawablePadding = dpToPx(4)  // Space between text and icon

      layoutParams = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.WRAP_CONTENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
      ).apply {
        leftMargin = dpToPx(8)  // Space between the two buttons
      }
    }
// Create the first button (Show Canvas)
    val showCanvasButton = Button(this).apply {
      text = "OpenScene"
      textSize = 12f  // Adjust text size as needed (in sp)

      // Get and scale the drawable.
      val drawable = ContextCompat.getDrawable(context, R.drawable.raintree)
      val drawableSize = dpToPx(24)  // desired drawable size, e.g., 24dp by 24dp
      drawable?.setBounds(0, 0, drawableSize, drawableSize)
      // Set the drawable on the left; the other positions are null.
      setCompoundDrawables(drawable, null, null, null)
      compoundDrawablePadding = dpToPx(4) // space between image and text
    }

    val RightButton = Button(this).apply {
      text = "Next"
      textSize = 12f  // Adjust text size as needed (in sp)

      // Load the drawable and set custom size
      val drawable = ContextCompat.getDrawable(context, R.drawable.right_arrow)
      val drawableSize = dpToPx(24)  // Keep the image at 24dp x 24dp
      drawable?.setBounds(0, 0, drawableSize, drawableSize)

      // Set the drawable to the left of the text
      setCompoundDrawables(drawable, null, null, null)
      compoundDrawablePadding = dpToPx(4)  // Space between text and icon

      layoutParams = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.WRAP_CONTENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
      ).apply {
        leftMargin = dpToPx(8)  // Space between the two buttons
      }
    }



// Add both buttons to the horizontal layout
    buttonLayout.addView(LeftButton)
    buttonLayout.addView(showCanvasButton)
    buttonLayout.addView(RightButton)

// Add the button layout to the root view
    val rootView = view.root as ViewGroup
    rootView.addView(buttonLayout)

    // Inflate the canvas overlay layout (initially hidden).
    val canvasOverlay = LayoutInflater.from(this).inflate(R.layout.canvas_overlay, rootView, false)
    canvasOverlay.visibility = android.view.View.GONE
    rootView.addView(canvasOverlay)

    // When the button is clicked, display the overlay.
    showCanvasButton.setOnClickListener {
      if (canvasOverlay.visibility == android.view.View.VISIBLE) {
        canvasOverlay.visibility = android.view.View.GONE
        showCanvasButton.text = "Open Canvas"
      } else {
        canvasOverlay.visibility = android.view.View.VISIBLE
        showCanvasButton.text = "Close Canvas"
      }
    }

    // Set up the "X" button inside the overlay to hide it.
/*    val closeButton = canvasOverlay.findViewById<Button>(R.id.close_button)
    closeButton.setOnClickListener {
      canvasOverlay.visibility = android.view.View.GONE
    }*/

  }
  override fun onStart() {
    super.onStart()
    // Start playing the audio
    mediaPlayer1?.start()
    mediaPlayer2?.start()

  }
  override fun onStop() {
    super.onStop()
    // Stop and release the MediaPlayer when not needed
    mediaPlayer1?.release()
    mediaPlayer1 = null
    mediaPlayer2?.release()
    mediaPlayer2 = null
  }
  // Configure the session, using Lighting Estimation, and Depth mode.
  fun configureSession(session: Session) {
    session.configure(
      session.config.apply {
        lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR

        // Depth API is used if it is configured in Hello AR's settings.
        depthMode =
          if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
            Config.DepthMode.AUTOMATIC
          } else {
            Config.DepthMode.DISABLED
          }

        // Instant Placement is used if it is configured in Hello AR's settings.
        instantPlacementMode =
          if (instantPlacementSettings.isInstantPlacementEnabled) {
            InstantPlacementMode.LOCAL_Y_UP
          } else {
            InstantPlacementMode.DISABLED
          }
      }
    )
  }

  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<String>,
    results: IntArray
  ) {
    super.onRequestPermissionsResult(requestCode, permissions, results)
    if (!CameraPermissionHelper.hasCameraPermission(this)) {
      // Use toast instead of snackbar here since the activity will exit.
      Toast.makeText(this, "Camera permission is needed to run this application", Toast.LENGTH_LONG)
        .show()
      if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
        // Permission denied with checking "Do not ask again".
        CameraPermissionHelper.launchPermissionSettings(this)
      }
      finish()
    }
  }

  override fun onWindowFocusChanged(hasFocus: Boolean) {
    super.onWindowFocusChanged(hasFocus)
    FullScreenHelper.setFullScreenOnWindowFocusChanged(this, hasFocus)
  }
}
