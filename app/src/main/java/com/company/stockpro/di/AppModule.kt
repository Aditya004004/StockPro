package com.company.stockpro.di

import android.content.Context
import androidx.room.Room
import com.company.stockpro.core.data.repository.InventoryRepositoryImpl
import com.company.stockpro.core.data.sync.InventorySyncManager
import com.company.stockpro.core.database.StockProDatabase
import com.company.stockpro.core.domain.repository.InventoryRepository
import com.company.stockpro.core.domain.usecase.PostMovementUseCase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): StockProDatabase = Room.databaseBuilder(
        context,
        StockProDatabase::class.java,
        "stockpro.db",
    ).fallbackToDestructiveMigration().build()

    @Provides
    @Singleton
    fun provideSyncManager(
        @ApplicationContext context: Context,
        database: StockProDatabase,
    ): InventorySyncManager? {
        return runCatching {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            InventorySyncManager(
                database = database,
                firestore = FirebaseFirestore.getInstance(),
            )
        }.getOrNull()
    }

    @Provides
    @Singleton
    fun provideInventoryRepository(
        database: StockProDatabase,
        syncManager: InventorySyncManager?,
    ): InventoryRepository = InventoryRepositoryImpl(
        database = database,
        syncManager = syncManager,
    )

    @Provides
    @Singleton
    fun providePostMovementUseCase(
        repository: InventoryRepository,
    ): PostMovementUseCase = PostMovementUseCase(repository)

    @Provides
    @Singleton
    fun provideFirebaseAuth(
        @ApplicationContext context: Context,
    ): FirebaseAuth {
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }
        return FirebaseAuth.getInstance()
    }
}
