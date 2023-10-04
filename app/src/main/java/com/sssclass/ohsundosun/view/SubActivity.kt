package com.sssclass.ohsundosun.view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.sssclass.ohsundosun.databinding.ActivitySubBinding

class SubActivity: AppCompatActivity() {

    private lateinit var binding: ActivitySubBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySubBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}