package com.og.myapplication

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.AvailabilityException
import com.google.android.gms.common.api.GoogleApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class PhoneViewModel : ViewModel() {
    companion object {
        const val TAG = "PhoneViewModel"


    }

    val apiAvailableLD = MutableLiveData<Boolean>(false)

    val counterLd = MutableLiveData<Int>(0)

    var imageLd = MutableLiveData<Bitmap?>(null)

    /**
     * 判断Google api 是否可用
     */
    fun isAvailable(api: GoogleApi<*>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                GoogleApiAvailability.getInstance()
                    .checkApiAvailability(api)
                    .await()
                Log.d(TAG, "${api.javaClass.simpleName} API is available in this device.")
                apiAvailableLD.postValue(true)
            } catch (e: AvailabilityException) {
                Log.e(TAG, "${api.javaClass.simpleName} API is not available in this device.")
                Log.e(TAG, "isAvailable: ${e.message}")
                apiAvailableLD.postValue(false)
            }
        }
    }

    fun incrementCounter() {
        var counter = counterLd.value ?: 0
        counterLd.value = ++counter
    }

    fun decrementCounter() {
        var counter = counterLd.value ?: 0
        counterLd.value = --counter
    }


    fun onPictureTaken(bitmap: Bitmap?) {
        imageLd.value = bitmap ?: return
    }


}