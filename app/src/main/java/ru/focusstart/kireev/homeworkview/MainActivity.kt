package ru.focusstart.kireev.homeworkview

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MotionEvent
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        button.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                customSpeedometerView.drive()
                customSpeedometerView2.drive()
            } else {
                v.performClick()
                customSpeedometerView.stopDrive()
                customSpeedometerView2.stopDrive()
            }
                return@setOnTouchListener false
        }

    }
}