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
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
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

  // UI Elements for displaying tree data
  private lateinit var treeNameTextView: TextView
  private lateinit var treeInfoTextView: TextView
  private lateinit var treeImageView: ImageView
  private lateinit var canvasOverlay: View

  private var trees: List<Tree> = emptyList()
  private var currentIndex = 0

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)  // Ensure your XML layout has the correct ID

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

// Initialize Room Database and treeDao
/*
    val db = TreeDatabase.getDatabase(this)
*/
    //treeDao = db.treeDao()

/*// Fetch trees from database in background
    Thread {
      trees = treeDao.getAllTrees()
      // Optionally update the UI on the main thread:
      runOnUiThread {
        if (trees.isNotEmpty()) {
          //updateCanvas() // Display first tree data
        }
      }
    }.start()*/

    // Add the button layout to the root view
    val rootView = findViewById<ViewGroup>(android.R.id.content)

    // Inflate the canvas overlay layout (initially hidden).
    canvasOverlay = LayoutInflater.from(this).inflate(R.layout.canvas_overlay, rootView, false)
    canvasOverlay.visibility = View.GONE
    rootView.addView(canvasOverlay)

    val buttonContainer = LinearLayout(this).apply {
      orientation = LinearLayout.HORIZONTAL
      layoutParams = FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.WRAP_CONTENT,
        FrameLayout.LayoutParams.WRAP_CONTENT
      ).apply {
        gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL  // Align buttons to bottom-center
        bottomMargin = dpToPx(24)  // Adjust spacing from bottom
      }
    }



    // Initialize TextViews and ImageView
    treeNameTextView = canvasOverlay.findViewById(R.id.name_text)
    treeInfoTextView = canvasOverlay.findViewById(R.id.TreeInfo)
    treeImageView = canvasOverlay.findViewById(R.id.imageView)



    fun updateCanvasButtonImage(button: Button) {
      val currentTree = TreeData.treeList[currentIndex]
      val drawable = ContextCompat.getDrawable(this, currentTree.imageRes)
      drawable?.setBounds(0, 0, dpToPx(24), dpToPx(24)) // Resize image
      button.setCompoundDrawables(drawable, null, null, null)
    }


// Create the Open Canvas Button
    val showCanvasButton = Button(this).apply {
      text = "Open Scene"
      textSize = 12f

      updateCanvasButtonImage(this) // Set initial image

      setOnClickListener {
        if (canvasOverlay.visibility == View.VISIBLE) {
          canvasOverlay.visibility = View.GONE
          text = "Open Canvas"
        } else {
          canvasOverlay.visibility = View.VISIBLE
          text = "Close Canvas"
          updateCanvas()
        }
      }
    }

    // Create the Left Button (Previous)
    val leftButton = Button(this).apply {
      text = "Previous"
      textSize = 12f
      setOnClickListener {
        if (currentIndex > 0) {
          currentIndex--
          updateCanvas()
          updateCanvasButtonImage(showCanvasButton) // Update button image
        }
      }
    }

// Create the Right Button (Next)
    val rightButton = Button(this).apply {
      text = "Next"
      textSize = 12f
      setOnClickListener {
        if (currentIndex < TreeData.treeList.size - 1) {
          currentIndex++
          updateCanvas()
          updateCanvasButtonImage(showCanvasButton) // Update button image
        }
      }
    }



// Add buttons to the container
    buttonContainer.addView(leftButton)
    buttonContainer.addView(showCanvasButton)
    buttonContainer.addView(rightButton)

// Add the container to the root layout
    rootView.addView(buttonContainer)



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

  }

  // Update the canvas overlay with current tree data
  private fun updateCanvas() {
    val tree = TreeData.treeList[currentIndex]
    treeNameTextView.text = tree.name
    treeInfoTextView.text = tree.info
    treeImageView.setImageResource(tree.imageRes)
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
