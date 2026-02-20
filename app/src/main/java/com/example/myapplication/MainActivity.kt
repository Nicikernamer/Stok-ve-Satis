package com.example.myapplication

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    companion object {
        private const val CAMERA_PERMISSION_REQUEST = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkCameraPermission()

        findViewById<Button>(R.id.btnProducts).setOnClickListener {
            startActivity(Intent(this, ProductListActivity::class.java))
        }

        findViewById<Button>(R.id.btnAdd).setOnClickListener {
            startActivity(Intent(this, ProductAddActivity::class.java))
        }

        findViewById<Button>(R.id.btnSale).setOnClickListener {
            startActivity(Intent(this, SaleActivity::class.java))
        }

        findViewById<Button>(R.id.btnHistory).setOnClickListener {
            startActivity(Intent(this, SaleHistoryActivity::class.java))
        }
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST
            )
        }
    }
}
