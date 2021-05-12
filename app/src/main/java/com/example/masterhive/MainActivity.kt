 package com.example.masterhive

import android.animation.Animator
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import androidx.annotation.RequiresApi
import com.example.masterhive.databinding.ActivityMainBinding
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    lateinit var activityMainBinding: ActivityMainBinding

    private lateinit var auth: FirebaseAuth
    private lateinit var callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    private var storedVerificationId: String? = ""
    private lateinit var resendToken: PhoneAuthProvider.ForceResendingToken

    var check_click : Boolean = false


    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        activityMainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(activityMainBinding.root)

        auth = FirebaseAuth.getInstance()
        // [START phone_auth_callbacks]
        callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            override fun onVerificationCompleted(credential: PhoneAuthCredential) {

                Log.d("onVerificationCompleted", "$credential")
                activityMainBinding.otp.setText(credential.smsCode)   //otp set

                object : CountDownTimer(2000,1000)
                {
                    override fun onTick(millisUntilFinished: Long) {

                    }

                    override fun onFinish() {
                        signInWithPhoneAuthCredential(credential)
                    }

                }.start()

            }

            override fun onVerificationFailed(e: FirebaseException) {
                // This callback is invoked in an invalid request for verification is made,
                // for instance if the the phone number format is not valid.
                Log.w( "onVerificationFailed", e)



            }

            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                storedVerificationId = verificationId
                resendToken = token

            }
        }
        // [END phone_auth_callbacks]

        activityMainBinding.otpLayout.visibility = View.GONE

        animation()
        activityMainBinding.button.setOnClickListener(View.OnClickListener {
            if(!check_click) {
                var size = activityMainBinding.mobileNumber.text?.length
                if (size == 10) {
                    activityMainBinding.otpLayout.visibility = View.VISIBLE
                    activityMainBinding.mobileNumberLayout.visibility = View.GONE
                    activityMainBinding.resendOtp.visibility = View.VISIBLE
                    activityMainBinding.button.text = "Sumbit"
                    check_click = true
                    get_otp(activityMainBinding.mobileNumber.text.toString())

                } else {
                    activityMainBinding.mobileNumber.error = "Invalid Mobile Number"
                }
            }

            else
            {
                var otp = activityMainBinding.otp.text.toString()
                verify_otp(otp)
            }
        })

    activityMainBinding.resendOtp.setOnClickListener { v ->
        resendVerificationCode(activityMainBinding.mobileNumber.text.toString(),resendToken)
    }

    }

    private fun verify_otp(otp : String) {
        val credential = PhoneAuthProvider.getCredential(storedVerificationId!!, otp)
        signInWithPhoneAuthCredential(credential)           // verify otp send on phone number in another mobile : code
    }

    private fun get_otp(text: String?) {

        if (text != null) {
            startPhoneNumberVerification(text)
        }

    }

    private fun startPhoneNumberVerification(phoneNumber: String) {
        // [START start_phone_auth]
        val options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber("+91$phoneNumber")       // Phone number to verify
                .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
                .setActivity(this)                 // Activity (for callback binding)
                .setCallbacks(callbacks)          // OnVerificationStateChangedCallbacks
                .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
        // [END start_phone_auth]


            // RESEND TEXT CUM TIMER
        object : CountDownTimer(1000*60,1000)
        {
            override fun onTick(millisUntilFinished: Long) {
                activityMainBinding.resendOtp.setText( "Remaining ${millisUntilFinished.toInt()/1000}")
            }

            override fun onFinish() {
            activityMainBinding.resendOtp.setText("Resend OTP")
            }

        }.start()
        // FINISH RESEND TEXT CUM TIMER
    }

    // [START sign_in_with_phone]
    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        val user : FirebaseUser? = task.result?.user
                        val i = Intent(this, Home_page::class.java)
                        startActivity(i)
                        finish()

                    } else {
                        // Sign in failed, display a message and update the UI
                        Log.w("signInWithCredential", task.exception)
                        if (task.exception is FirebaseAuthInvalidCredentialsException) {
                            // The verification code entered was invalid
                        }
                        // Update UI
                    }
                }
    }
    // [END sign_in_with_phone]
    // Resend verification ... start
    private fun resendVerificationCode(
            phoneNumber: String,
            token: PhoneAuthProvider.ForceResendingToken?
    ) {
        val optionsBuilder = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber("+91$phoneNumber")       // Phone number to verify
                .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
                .setActivity(this)                 // Activity (for callback binding)
                .setCallbacks(callbacks)          // OnVerificationStateChangedCallbacks
        if (token != null) {
            optionsBuilder.setForceResendingToken(token) // callback's ForceResendingToken
        }
        PhoneAuthProvider.verifyPhoneNumber(optionsBuilder.build())

    }
    // [END resend_verification]

    // [START on_start_check_user]
    override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser
        if(currentUser != null) {
            val i = Intent(this, Home_page::class.java)
            startActivity(i)
            finish()
        }
    }
    // [END on_start_check_user]


    @RequiresApi(Build.VERSION_CODES.N)
    private fun animation() {
        // progress animation
        val progressBar = ProgressBar(this)
        progressBar.setProgress(50,true)



        // book animation
        val viewPropertyAnimator = activityMainBinding.animatedLogo.animate()
        viewPropertyAnimator.x(250f)
        viewPropertyAnimator.y(450f)
        viewPropertyAnimator.duration = 1000
        viewPropertyAnimator.setListener(object : Animator.AnimatorListener {
            override fun onAnimationRepeat(animation: Animator?) {

            }

            override fun onAnimationEnd(animation: Animator?) {
                activityMainBinding.aterAnimationView.visibility = View.VISIBLE
                activityMainBinding.bookText.visibility = View.GONE

            }

            override fun onAnimationCancel(animation: Animator?) {

            }

            override fun onAnimationStart(animation: Animator?) {

            }
        })
    }

    override fun onBackPressed() {
        moveTaskToBack(true)
    }
}