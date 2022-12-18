package com.company.sns_crawlling

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.CompoundButton
import android.widget.Switch
import com.google.firebase.messaging.FirebaseMessaging

class SettingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val alarmSwitch = findViewById<Switch>(R.id.alarmSwitch)
        // 이전 실행 시 저장된 값이 있으면 불러오고, 없으면 기본값으로 true 지정
        val pref = getSharedPreferences("preferences", MODE_PRIVATE)
        alarmSwitch.isChecked = pref.getBoolean("alarm", true)

        alarmSwitch.setOnCheckedChangeListener(alarmSwitchListener())
    }

    inner class alarmSwitchListener : CompoundButton.OnCheckedChangeListener {
        override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
            if (isChecked)
                FirebaseMessaging.getInstance().subscribeToTopic("날씨 알림")
            else
                FirebaseMessaging.getInstance().unsubscribeFromTopic("날씨 알림")

            // 어플을 꺼도 마지막으로 저장한 스위치 상태가 유지되도록
            val pref = getSharedPreferences("preferences", MODE_PRIVATE)
            val editor = pref.edit()
            editor.putBoolean("alarm", isChecked)
            editor.commit()
        }
    }

}