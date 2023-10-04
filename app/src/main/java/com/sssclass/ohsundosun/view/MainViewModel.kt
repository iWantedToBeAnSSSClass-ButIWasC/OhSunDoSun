package com.sssclass.ohsundosun.view

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

class MainViewModel: ViewModel() {

    var phoneNumber = MutableLiveData<String>()
    var authNumber = MutableLiveData<String>()
}