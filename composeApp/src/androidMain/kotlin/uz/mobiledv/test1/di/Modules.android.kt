package uz.mobiledv.test1.di

import android.content.Context
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module
import uz.mobiledv.test1.util.FileSaver

actual val platformModule = module {
    single<Settings> {
        SharedPreferencesSettings(
            delegate = androidContext().getSharedPreferences(
                "auth_prefs", // Name of the SharedPreferences file
                Context.MODE_PRIVATE
            )
        )
    }
}

actual val platformFileSaverModule = module { // New actual
    single { FileSaver(androidContext()) } // Provide context
}