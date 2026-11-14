package com.joano.seoga

import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.joano.seoga.data.preferences.UserPreferencesRepository
import com.joano.seoga.data.preferences.UserSettings
import com.joano.seoga.ui.navigation.AppNavigation
import com.joano.seoga.ui.util.LocalizationUtil

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val preferencesRepo = UserPreferencesRepository(applicationContext)

        setContent {
            val userSettings by preferencesRepo.userSettingsFlow.collectAsState(
                initial = UserSettings()
            )

            val activity = this@MainActivity
            val targetLocale = remember(userSettings.language) {
                LocalizationUtil.resolveLocale(userSettings.language)
            }

            val localizedResources = remember(targetLocale) {
                val config = Configuration(activity.resources.configuration).apply {
                    setLocale(targetLocale)
                }
                activity.createConfigurationContext(config).resources
            }

            val localizedContext = remember(localizedResources) {
                object : ContextWrapper(activity) {
                    override fun getResources(): Resources = localizedResources
                }
            }

            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalConfiguration provides localizedResources.configuration
            ) {
                AppNavigation()
            }
        }
    }
}
