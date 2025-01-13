package com.og.myapplication

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.og.myapplication.databinding.ActivityDataClientBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.time.Instant
import kotlin.coroutines.cancellation.CancellationException

class DataClientActivity : AppCompatActivity() {

    companion object {
        const val TAG = "DataClientActivity"

        private const val START_ACTIVITY_PATH = "/start-activity"
        private const val COUNT_PATH = "/count"
        private const val IMAGE_PATH = "/image"
        private const val IMAGE_KEY = "photo"
        private const val TIME_KEY = "time"
        private const val COUNT_KEY = "count"
        private const val CAMERA_CAPABILITY = "camera"
        private const val WEAR_CAPABILITY = "wear"
    }

    private val binding by lazy { ActivityDataClientBinding.inflate(layoutInflater) }

    private val viewMode: PhoneViewModel by viewModels()

    private val dataClient by lazy { Wearable.getDataClient(this) }

    private val takePhotoLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        viewMode.onPictureTaken(bitmap = bitmap)
    }

    private val isCameraSupported by lazy {
        packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initView()
        initObserver()

    }

    private fun initObserver() {
        viewMode.counterLd.observe(this) {
            binding.tvValue.text = "Value: $it"
            lifecycleScope.launch(Dispatchers.IO) {
                sendCount(it)
            }
        }
        viewMode.imageLd.observe(this) { image ->
            binding.btnSendPicture.isEnabled = image != null
            image?.let {
                binding.ivDisplay.setImageBitmap(it)
            }
        }
    }

    private fun initView() {
        binding.btnAdd.setOnClickListener {
            viewMode.incrementCounter()
        }

        binding.btnDrop.setOnClickListener {
            viewMode.decrementCounter()
        }

        binding.btnGetPicture.setOnClickListener {
            takePhoto()
        }
        binding.btnSendPicture.setOnClickListener {
            sendPhoto()
        }
    }


    private suspend fun sendCount(count: Int) {
        Log.d(TAG, "sendCount() called with: count = $count")
        try {
            val request = PutDataMapRequest.create(COUNT_PATH).apply {
                dataMap.putInt(COUNT_KEY, count)
            }
                .asPutDataRequest()
                .setUrgent()

            val result = dataClient.putDataItem(request).await()
            Log.d(TAG, "DataItem saved: $result")
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (exception: Exception) {
            Log.d(TAG, "Saving DataItem failed: $exception")
        }
    }

    private fun takePhoto() {
        if (!isCameraSupported) return
        takePhotoLauncher.launch(null)
    }

    private fun sendPhoto() {
        lifecycleScope.launch {
            try {
                val image = viewMode.imageLd.value ?: return@launch
                val imageAsset = image.toAsset()
                val request = PutDataMapRequest.create(IMAGE_PATH).apply {
                    dataMap.putAsset(IMAGE_KEY, imageAsset)
                    dataMap.putLong(TIME_KEY, Instant.now().epochSecond)
                }
                    .asPutDataRequest()
                    .setUrgent()

                val result = dataClient.putDataItem(request).await()

                Log.d(TAG, "DataItem saved: $result")
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (exception: Exception) {
                Log.d(TAG, "Saving DataItem failed: $exception")
            }
        }
    }

    private suspend fun Bitmap.toAsset(): Asset =
        withContext(Dispatchers.Default) {
            ByteArrayOutputStream().use { byteStream ->
                compress(Bitmap.CompressFormat.PNG, 100, byteStream)
                Asset.createFromBytes(byteStream.toByteArray())
            }
        }
}