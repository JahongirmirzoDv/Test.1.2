package uz.mobiledv.test1.di

import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

fun initKoin(config: KoinAppDeclaration? = null) {
    startKoin {
        config?.invoke(this)
        modules(
            sharedModule,
            platformModule,
            viewModelsModule,
            supabaseStorageModule,
            platformFileSaverModule
        )
    }
}