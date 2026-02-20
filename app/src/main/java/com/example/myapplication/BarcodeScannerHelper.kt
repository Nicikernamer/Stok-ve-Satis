package com.example.myapplication

import android.content.Context
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

class BarcodeScannerHelper(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val onBarcodeScanned: (String) -> Unit
) {

    private val executor = Executors.newSingleThreadExecutor()
    private var cameraProvider: ProcessCameraProvider? = null

    fun start(previewView: PreviewView) {
        val providerFuture = ProcessCameraProvider.getInstance(context)

        providerFuture.addListener({
            cameraProvider = providerFuture.get()

            val preview = Preview.Builder().build().apply {
                setSurfaceProvider(previewView.surfaceProvider)
            }

            val analyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            val scanner = BarcodeScanning.getClient()

            analyzer.setAnalyzer(executor) { imageProxy ->
                val mediaImage = imageProxy.image
                if (mediaImage != null) {
                    val image = InputImage.fromMediaImage(
                        mediaImage,
                        imageProxy.imageInfo.rotationDegrees
                    )

                    scanner.process(image)
                        .addOnSuccessListener { barcodes ->
                            barcodes.firstOrNull()?.rawValue?.let { code ->
                                onBarcodeScanned(code)
                            }
                        }
                        .addOnCompleteListener {
                            imageProxy.close()
                        }
                } else {
                    imageProxy.close()
                }
            }

            cameraProvider?.unbindAll()
            cameraProvider?.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                analyzer
            )
        }, ContextCompat.getMainExecutor(context))
    }

    fun stop() {
        cameraProvider?.unbindAll()
    }
}
