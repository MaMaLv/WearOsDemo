package com.og.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.wearable.Wearable
import com.google.android.material.snackbar.Snackbar
import com.og.myapplication.databinding.ActivityMainBinding

class PhoneMainActivity : AppCompatActivity() {

    companion object {
        const val TAG = "PhoneMainActivity"
    }

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    private val capabilityClient by lazy { Wearable.getCapabilityClient(this) }

    private val viewMode: PhoneViewModel by viewModels()


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
        viewMode.isAvailable(capabilityClient)
    }

    private fun initObserver() {
        viewMode.apiAvailableLD.observe(this) { isAvailable ->
            Log.d(TAG, "initObserver() called with: isAvailable = $isAvailable")
            binding.tvApiStatus.visibility = if (!isAvailable) View.VISIBLE else View.GONE
        }
    }

    private fun initView() {
        binding.btnDataClient.setOnClickListener {
            if (viewMode.apiAvailableLD.value == false) {
                Snackbar.make(
                    binding.tvApiStatus,
                    resources.getString(R.string.wearable_api_unavailable),
                    Snackbar.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            Intent(this@PhoneMainActivity, DataClientActivity::class.java).apply {
                startActivity(this)
            }
        }
    }

}
