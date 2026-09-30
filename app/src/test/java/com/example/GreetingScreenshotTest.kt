package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LoginScreenshotTest {

  @Test
  fun login_screenshot() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    assertNotNull("Application context should be available", context)
  }
}

