package com.example.liveridingmf

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class LocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    companion object {
        const val CHANNEL_ID = "LocationServiceChannel"
        const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()

        Log.d("DEBUG", "LocationService start")

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Create a location request
        locationRequest = LocationRequest.create().apply {
            interval = 5000 // 5 seconds
            fastestInterval = 2000 // 2 seconds
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        // Define a location callback
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation
                if (location != null) {
                    val address = getAddressFromLocation(location.latitude, location.longitude)
                    saveAddressToFile(address)
                    sendNotification("Current Location: $address")
                    Log.d("LocationService", "Location updated: $address")
                }
            }
        }

        startLocationUpdates()
    }

    // Start location updates
    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
        sendNotification("Fetching location...")
    }

    // Stop location updates
    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        Log.d("LocationService", "Location updates stopped")
    }

    // Send a notification with the current location
    private fun sendNotification(message: String) {
        // Check for Notification Permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.e("Notification", "Permission not granted to post notifications")
                return
            }
        }

        // Create the notification
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Live Location Update")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_location) // Ensure you have a valid icon
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        // Start the service as a foreground service
        startForeground(NOTIFICATION_ID, notification)
    }

    // Function to get the address from latitude and longitude
    private fun getAddressFromLocation(latitude: Double, longitude: Double): String {
        val geocoder = Geocoder(this, Locale.getDefault())
        val addresses = geocoder.getFromLocation(latitude, longitude, 1)
        return if (addresses!!.isNotEmpty()) {
            addresses[0].getAddressLine(0)
        } else {
            "Address not found"
        }
    }

    // Function to save the address to a file with a timestamp
    private fun saveAddressToFile(address: String) {
        try {
            val file = File(filesDir, "address_log.txt")
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val data = "$timestamp: $address\n"
            FileOutputStream(file, true).use { output ->
                output.write(data.toByteArray())
            }
            Log.d("AddressLogger", "Address saved: $data")
        } catch (e: Exception) {
            Log.e("AddressLogger", "Error saving address to file", e)
        }
    }

    // Function to export the address log to the Download folder
    private fun exportFileToDownloads() {
        try {
            val sourceFile = File(filesDir, "address_log.txt")
            if (!sourceFile.exists()) {
                Log.e("ExportFile", "No address log file found.")
                return
            }

            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val destFile = File(downloadsDir, "address_log.txt")

            FileInputStream(sourceFile).use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }

            Log.d("ExportFile", "File exported to: ${destFile.absolutePath}")
        } catch (e: Exception) {
            Log.e("ExportFile", "Error exporting file", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        sendNotification("Fetching location...")
        return START_STICKY
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Location Service Channel",
            NotificationManager.IMPORTANCE_HIGH
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
