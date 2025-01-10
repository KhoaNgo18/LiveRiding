package com.example.liveridingmf

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private val LOCATION_PERMISSION_REQUEST_CODE = 1001
    private val NOTIFICATION_PERMISSION_REQUEST_CODE = 1002

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val startServiceButton = findViewById<Button>(R.id.startServiceButton)
        val stopServiceButton = findViewById<Button>(R.id.stopServiceButton)
        val exportButton = findViewById<Button>(R.id.exportButton)

        // Start Location Service Button
        startServiceButton.setOnClickListener {
            if (hasLocationPermissions()) {
                if (hasNotificationPermission()) {
                    startLocationService()
                } else {
                    requestNotificationPermission()
                }
            } else {
                requestLocationPermissions()
            }
        }

        // Stop Location Service Button
        stopServiceButton.setOnClickListener {
            stopLocationService()
        }

        // Export Address Log Button
        exportButton.setOnClickListener {
            exportFileToDownloads()
        }
    }

    // Check if location permissions are granted
    private fun hasLocationPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    // Request location permissions
    private fun requestLocationPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.FOREGROUND_SERVICE_LOCATION
            ),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    // Check if notification permission is granted
    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    // Request notification permission
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                NOTIFICATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    // Start the Location Service
    private fun startLocationService() {
        val serviceIntent = Intent(this, LocationService::class.java)
        startForegroundService(serviceIntent)
        Toast.makeText(this, "Location Service Started", Toast.LENGTH_SHORT).show()
    }

    // Stop the Location Service
    private fun stopLocationService() {
        val serviceIntent = Intent(this, LocationService::class.java)
        stopService(serviceIntent)
        Toast.makeText(this, "Location Service Stopped", Toast.LENGTH_SHORT).show()
    }

    // Export the address log file to the Download folder
    private fun exportFileToDownloads() {
        val serviceIntent = Intent(this, LocationService::class.java)
        serviceIntent.action = "EXPORT_FILE"
        startService(serviceIntent)
        Toast.makeText(this, "Exporting Address Log to Download Folder", Toast.LENGTH_SHORT).show()
    }
}
