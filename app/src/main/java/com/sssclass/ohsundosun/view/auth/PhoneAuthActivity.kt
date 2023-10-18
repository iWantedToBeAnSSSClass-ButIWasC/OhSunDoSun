package com.sssclass.ohsundosun.view.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.orhanobut.logger.Logger
import com.sssclass.ohsundosun.R
import com.sssclass.ohsundosun.databinding.ActivityPhoneAuthBinding
import com.sssclass.ohsundosun.utils.UserInfo
import com.sssclass.ohsundosun.view.MainActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class PhoneAuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPhoneAuthBinding

    private lateinit var auth: FirebaseAuth
    private var storedVerificationId = ""
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null

    private var isResend = false

    private val phoneAuthCallbacks by lazy {
        object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            // 번호인증 혹은 기타 다른 인증(구글로그인, 이메일로그인 등) 끝난 상태
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                Toast.makeText(
                    this@PhoneAuthActivity,
                    "인증코드가 전송되었습니다. 90초 이내에 입력해주세요",
                    Toast.LENGTH_SHORT
                ).show()
                UserInfo.phoneAuthNum = credential.smsCode.toString()
                binding.authNumberEditText.setText(credential.smsCode.toString())
                binding.authNumberEditText.isEnabled = false
                lifecycleScope.launch {
                    delay(1000)
                    verifyPhoneNumberWithCode(credential)
                }
            }

            // 번호인증 실패 상태
            override fun onVerificationFailed(e: FirebaseException) {
                // This callback is invoked in an invalid request for verification is made,
                // for instance if the the phone number format is not valid.
                if (e is FirebaseAuthInvalidCredentialsException) {
                    // Invalid request
                    e.localizedMessage?.let { Logger.e(it) }
                } else if (e is FirebaseTooManyRequestsException) {
                    // The SMS quota for the project has been exceeded
                    e.localizedMessage?.let { Logger.e(it) }
                }
                Logger.d(e)
                Logger.d("인증 실패")
                Toast.makeText(this@PhoneAuthActivity, "인증 실패!!!!", Toast.LENGTH_SHORT).show()
            }

            // 전화번호는 확인 되었으나 인증코드를 입력해야 하는 상태
            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                // Save verification ID and resending token so we can use them later
                storedVerificationId = verificationId // verificationId 와 전화번호인증코드 매칭해서 인증하는데 사용예정
                resendToken = token
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhoneAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth

        initButton()
    }

    private fun initButton() {
        binding.phoneAuthButton.setOnClickListener {
            UserInfo.tel = binding.phoneNumberEditText.text.toString()
            if (UserInfo.tel != "") {
                if (!isResend) {
                    startPhoneNumberVerification("+82" + UserInfo.tel)
                } else {
                    resendVerificationCode("+82" + UserInfo.tel, resendToken)
                }
            } else {
                Toast.makeText(this@PhoneAuthActivity, "전화번호를 입력해주세요", Toast.LENGTH_SHORT).show()
            }
        }

        binding.finishAuthButton.setOnClickListener {
            try {
                val phoneCredential = PhoneAuthProvider.getCredential(
                    storedVerificationId,
                    binding.authNumberEditText.text.toString()
                )
                verifyPhoneNumberWithCode(phoneCredential)
                binding.finishAuthButton.isClickable = false
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // 전화번호 인증코드 요청
    private fun startPhoneNumberVerification(phoneNumber: String) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)       // Phone number to verify
            .setTimeout(90L, TimeUnit.SECONDS) // Timeout and unit
            .setActivity(this)                 // Activity (for callback binding)
            .setCallbacks(phoneAuthCallbacks)          // OnVerificationStateChangedCallbacks
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
        auth.setLanguageCode("kr")

        binding.phoneAuthButton.run {
            text = "재전송"
            setTextColor(ContextCompat.getColor(this@PhoneAuthActivity, R.color.dark_gray_333333))
            background =
                ContextCompat.getDrawable(this@PhoneAuthActivity, R.drawable.btn_background_default)
        }
    }

    // 전화번호 인증코드 재요청
    private fun resendVerificationCode(
        phoneNumber: String,
        token: PhoneAuthProvider.ForceResendingToken?
    ) {
        val optionsBuilder = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)       // Phone number to verify
            .setTimeout(90L, TimeUnit.SECONDS) // Timeout and unit
            .setActivity(this)                 // Activity (for callback binding)
            .setCallbacks(phoneAuthCallbacks)          // OnVerificationStateChangedCallbacks
        if (token != null) {
            optionsBuilder.setForceResendingToken(token) // callback's ForceResendingToken
        }
        PhoneAuthProvider.verifyPhoneNumber(optionsBuilder.build())
    }

    // 전화번호 인증 실행 (onCodeSent() 에서 받은 vertificationID 와
    // 문자로 받은 인증코드로 생성한 PhoneAuthCredential 사용)
    private fun verifyPhoneNumberWithCode(phoneAuthCredential: PhoneAuthCredential) {
        UserInfo.tel = binding.phoneNumberEditText.text.toString()
        if (UserInfo.tel.isNotBlank() && UserInfo.phoneAuthNum.isNotBlank() &&
            (UserInfo.tel == binding.phoneNumberEditText.text.toString() && UserInfo.phoneAuthNum == binding.authNumberEditText.text.toString())
        ) { // 이전에  인증한 번호와 인증번호인 경우
            Toast.makeText(this@PhoneAuthActivity, "인증 성공", Toast.LENGTH_SHORT).show()
            UserInfo.tel = binding.phoneNumberEditText.text.toString()
            startActivity(Intent(this@PhoneAuthActivity, MainActivity::class.java))
            return
        }
        Firebase.auth.signInWithCredential(phoneAuthCredential)
            .addOnCompleteListener(this@PhoneAuthActivity) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this@PhoneAuthActivity, "인증 성공", Toast.LENGTH_SHORT).show()
                    UserInfo.tel = binding.phoneNumberEditText.text.toString()
                    binding.authNumberEditText.isEnabled = true
                    startActivity(
                        Intent(this@PhoneAuthActivity, MainActivity::class.java)
                    )
                } else {
                    binding.authNumberTitleTextView.text = "인증번호가 잘못되었습니다."
                    binding.authNumberTitleTextView.setTextColor(
                        ContextCompat.getColor(this@PhoneAuthActivity, R.color.red)
                    )
                    binding.authNumberEditText.isEnabled = true
                }
            }
    }
}