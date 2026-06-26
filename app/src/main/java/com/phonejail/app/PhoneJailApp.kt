package com.phonejail.app

import android.app.Application
import com.phonejail.app.data.FocusRepository
import com.phonejail.app.data.PhoneJailDatabase
import com.phonejail.app.session.SessionManager

class PhoneJailApp : Application() {

    lateinit var repository: FocusRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = FocusRepository(PhoneJailDatabase.get(this).focusSessionDao())
        SessionManager.init(repository)
    }
}
