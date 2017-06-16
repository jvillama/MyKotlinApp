package com.example.android.mykotlinapp

import android.app.AlertDialog
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.Toast

import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.offline.OfflineManager
import com.mapbox.mapboxsdk.offline.OfflineRegion
import com.mapbox.mapboxsdk.offline.OfflineRegionError
import com.mapbox.mapboxsdk.offline.OfflineRegionStatus
import com.mapbox.mapboxsdk.offline.OfflineTilePyramidRegionDefinition

import kotlinx.android.synthetic.main.content_main.*

import org.json.JSONObject

import java.util.ArrayList

class MainActivity : AppCompatActivity() {

    var TAG = "MainActivity"

    // JSON encoding/decoding
    val JSON_CHARSET = "UTF-8"
    val JSON_FIELD_REGION_NAME = "FIELD_REGION_NAME"

    // UI elements
    private var map: MapboxMap? = null

    private var isEndNotified: Boolean = false
    private var regionSelected: Int = 0

    // Offline objects
    private var offlineManager: OfflineManager? = null
    private var offlineRegion: OfflineRegion? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mapbox access token is configured here. This needs to be called either in your application
        // object or in the same activity which contains the mapview.
        Mapbox.getInstance(this, getString(R.string.mapbox_access_token))

        // This contains the MapView in XML and needs to be called after the access token is configured.
        setContentView(R.layout.content_main)

        // Set up the MapView
        mapView!!.onCreate(savedInstanceState)
        mapView!!.getMapAsync { mapboxMap -> map = mapboxMap }

        // Set up the offlineManager
        offlineManager = OfflineManager.getInstance(this)

        // Bottom navigation bar button clicks are handled here.
        // Download offline button
        download_button?.setOnClickListener { downloadRegionDialog() }

        // List offline regions
        list_button?.setOnClickListener { downloadedRegionList() }
    }

    // Override Activity lifecycle methods
    override fun onStart() {
        super.onStart()
        mapView!!.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView!!.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView!!.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView!!.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView!!.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView!!.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView!!.onSaveInstanceState(outState)
    }

    private fun downloadRegionDialog() {
        // Set up download interaction. Display a dialog
        // when the user clicks download button and require
        // a user-provided region name
        val builder = AlertDialog.Builder(this@MainActivity)

        val regionNameEdit = EditText(this@MainActivity)
        regionNameEdit.hint = "Enter name"

        // Build the dialog box
        builder.setTitle("Name new region")
                .setView(regionNameEdit)
                .setMessage("Downloads the map region you currently are viewing")
                .setPositiveButton("Download") { dialog, which ->
                    val regionName = regionNameEdit.text.toString()
                    // Require a region name to begin the download.
                    // If the user-provided string is empty, display
                    // a toast message and do not begin download.
                    if (regionName.isEmpty()) {
                        Toast.makeText(this@MainActivity, "Region name cannot be empty.", Toast.LENGTH_SHORT).show()
                    } else {
                        // Begin download process
                        downloadRegion(regionName)
                    }
                }
                .setNegativeButton("Cancel") { dialog, which -> dialog.cancel() }

        // Display the dialog
        builder.show()
    }

    private fun downloadRegion(regionName: String) {
        // Define offline region parameters, including bounds,
        // min/max zoom, and metadata

        // Start the progressBar
        startProgress()

        // Create offline definition using the current
        // style and boundaries of visible map area
        val styleUrl = map!!.styleUrl
        val bounds = map!!.projection.visibleRegion.latLngBounds
        val minZoom = map!!.cameraPosition.zoom
        val maxZoom = map!!.maxZoomLevel
        val pixelRatio = this.resources.displayMetrics.density
        val definition = OfflineTilePyramidRegionDefinition(
                styleUrl, bounds, minZoom, maxZoom, pixelRatio)

        // Build a JSONObject using the user-defined offline region title,
        // convert it into string, and use it to create a metadata variable.
        // The metadata variable will later be passed to createOfflineRegion()
        var metadata: ByteArray?
        try {
            val jsonObject = JSONObject()
            jsonObject.put(JSON_FIELD_REGION_NAME, regionName)
            val json = jsonObject.toString()
            metadata = json.toByteArray(charset(JSON_CHARSET))
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to encode metadata: " + exception.message)
            metadata = null
        }

        // Create the offline region and launch the download
        offlineManager!!.createOfflineRegion(definition, metadata!!, object : OfflineManager.CreateOfflineRegionCallback {
            override fun onCreate(offlineRegion: OfflineRegion) {
                Log.d(TAG, "Offline region created: " + regionName)
                this@MainActivity.offlineRegion = offlineRegion
                launchDownload()
            }

            override fun onError(error: String) {
                Log.e(TAG, "Error: " + error)
            }
        })
    }

    private fun launchDownload() {
        // Set up an observer to handle download progress and
        // notify the user when the region is finished downloading
        offlineRegion!!.setObserver(object : OfflineRegion.OfflineRegionObserver {
            override fun onStatusChanged(status: OfflineRegionStatus) {
                // Compute a percentage
                val percentage = if (status.requiredResourceCount >= 0)
                    100.0 * status.completedResourceCount / status.requiredResourceCount
                else
                    0.0

                if (status.isComplete) {
                    // Download complete
                    endProgress("Region downloaded successfully.")
                    return
                } else if (status.isRequiredResourceCountPrecise) {
                    // Switch to determinate state
                    setPercentage(Math.round(percentage).toInt())
                }

                // Log what is being currently downloaded
                Log.d(TAG, String.format("%s/%s resources; %s bytes downloaded.",
                        status.completedResourceCount.toString(),
                        status.requiredResourceCount.toString(),
                        status.completedResourceSize.toString()))
            }

            override fun onError(error: OfflineRegionError) {
                Log.e(TAG, "onError reason: " + error.reason)
                Log.e(TAG, "onError message: " + error.message)
            }

            override fun mapboxTileCountLimitExceeded(limit: Long) {
                Log.e(TAG, "Mapbox tile count limit exceeded: " + limit)
            }
        })

        // Change the region state
        offlineRegion!!.setDownloadState(OfflineRegion.STATE_ACTIVE)
    }

    private fun downloadedRegionList() {
        // Build a region list when the user clicks the list button

        // Reset the region selected int to 0
        regionSelected = 0

        // Query the DB asynchronously
        offlineManager!!.listOfflineRegions(object : OfflineManager.ListOfflineRegionsCallback {
            override fun onList(offlineRegions: Array<OfflineRegion>?) {
                // Check result. If no regions have been
                // downloaded yet, notify user and return
                if (offlineRegions == null || offlineRegions.isEmpty()) {
                    Toast.makeText(this@MainActivity, "You have no regions yet.", Toast.LENGTH_SHORT).show()
                    return
                }

                // Add all of the region names to a list
                val offlineRegionsNames = ArrayList<String>()
                for (offlineRegion in offlineRegions) {
                    offlineRegionsNames.add(getRegionName(offlineRegion))
                }
                val items = offlineRegionsNames.toTypedArray<CharSequence>()

                // Build a dialog containing the list of regions
                val dialog = AlertDialog.Builder(this@MainActivity)
                        .setTitle("List")
                        .setSingleChoiceItems(items, 0) { dialog, which ->
                            // Track which region the user selects
                            regionSelected = which
                        }
                        .setPositiveButton("Navigate to") { dialog, id ->
                            Toast.makeText(this@MainActivity, items[regionSelected], Toast.LENGTH_LONG).show()

                            // Get the region bounds and zoom
                            val bounds = (offlineRegions[regionSelected].definition as OfflineTilePyramidRegionDefinition).bounds
                            val regionZoom = (offlineRegions[regionSelected].definition as OfflineTilePyramidRegionDefinition).minZoom

                            // Create new camera position
                            val cameraPosition = CameraPosition.Builder()
                                    .target(bounds.center)
                                    .zoom(regionZoom)
                                    .build()

                            // Move camera to new position
                            map!!.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
                        }
                        .setNeutralButton("Delete") { dialog, id ->
                            // Make progress_bar indeterminate and
                            // set it to visible to signal that
                            // the deletion process has begun
                            progress_bar!!.isIndeterminate = true
                            progress_bar!!.visibility = View.VISIBLE

                            // Begin the deletion process
                            offlineRegions[regionSelected].delete(object : OfflineRegion.OfflineRegionDeleteCallback {
                                override fun onDelete() {
                                    // Once the region is deleted, remove the
                                    // progress_bar and display a toast
                                    progress_bar!!.visibility = View.INVISIBLE
                                    progress_bar!!.isIndeterminate = false
                                    Toast.makeText(this@MainActivity, "Region deleted", Toast.LENGTH_LONG).show()
                                }

                                override fun onError(error: String) {
                                    progress_bar!!.visibility = View.INVISIBLE
                                    progress_bar!!.isIndeterminate = false
                                    Log.e(TAG, "Error: " + error)
                                }
                            })
                        }
                        .setNegativeButton("Cancel") { dialog, id ->
                            // When the user cancels, don't do anything.
                            // The dialog will automatically close
                        }.create()
                dialog.show()

            }

            override fun onError(error: String) {
                Log.e(TAG, "Error: " + error)
            }
        })
    }

    private fun getRegionName(offlineRegion: OfflineRegion): String {
        // Get the region name from the offline region metadata
        var regionName: String

        try {
            val metadata = offlineRegion.metadata
            //val json = String(metadata, JSON_CHARSET)
            val json = String(metadata)
            val jsonObject = JSONObject(json)
            regionName = jsonObject.getString(JSON_FIELD_REGION_NAME)
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to decode metadata: " + exception.message)
            regionName = "Region " + offlineRegion.id
        }

        return regionName
    }

    // Progress bar methods
    private fun startProgress() {
        // Disable buttons
        download_button!!.isEnabled = false
        list_button!!.isEnabled = false

        // Start and show the progress bar
        isEndNotified = false
        progress_bar!!.isIndeterminate = true
        progress_bar!!.visibility = View.VISIBLE
    }

    private fun setPercentage(percentage: Int) {
        progress_bar!!.isIndeterminate = false
        progress_bar!!.progress = percentage
    }

    private fun endProgress(message: String) {
        // Don't notify more than once
        if (isEndNotified) {
            return
        }

        // Enable buttons
        download_button!!.isEnabled = true
        list_button!!.isEnabled = true

        // Stop and hide the progress bar
        isEndNotified = true
        progress_bar!!.isIndeterminate = false
        progress_bar!!.visibility = View.GONE

        // Show a toast
        Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            finish()
            System.exit(0)
            return true
        }

        return super.onOptionsItemSelected(item)
    }
}
