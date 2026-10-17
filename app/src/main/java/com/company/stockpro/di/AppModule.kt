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
    ): StockProDatabase = StockProDatabase.getInstance(context)

    @Provides
    @Singleton
    fun provideSyncManager(
        @ApplicationContext context: Context,
        database: StockProDatabase,
    ): InventorySyncManager? {
        return runCatching {
            InventorySyncManager(
                database = database,
                firestore = FirebaseFirestore.getInstance(),
            )
        }.getOrNull()
    }

    @Provides
    @Singleton
    fun provideInventoryRepository(
        @ApplicationContext context: Context,
        database: StockProDatabase,
        syncManager: InventorySyncManager?,
    ): InventoryRepository = InventoryRepositoryImpl(
        database = database,
        syncManager = syncManager,
        context = context,
    )

    @Provides
    @Singleton
    fun providePostMovementUseCase(
        repository: InventoryRepository,
    ): PostMovementUseCase = PostMovementUseCase(repository)

    @Provides
    @Singleton
    fun provideStreamingDataFetcher(
        database: StockProDatabase,
    ): com.company.stockpro.core.domain.repository.StreamingDataFetcher =
        com.company.stockpro.core.data.repository.StreamingDataFetcherImpl(database)

    @Provides
    @Singleton
    fun provideExportHistoryRepository(
        database: StockProDatabase,
    ): com.company.stockpro.core.domain.repository.ExportHistoryRepository =
        com.company.stockpro.core.data.repository.ExportHistoryRepositoryImpl(database)

    @Provides
    @Singleton
    fun provideExportFileManager(
        @ApplicationContext context: Context,
    ): com.company.stockpro.core.data.export.ExportFileManager =
        com.company.stockpro.core.data.export.ExportFileManager(context)

    @Provides
    @Singleton
    fun provideExportScheduler(
        @ApplicationContext context: Context,
    ): com.company.stockpro.core.data.export.ExportScheduler =
        com.company.stockpro.core.data.export.ExportScheduler(context)

    @Provides
    @Singleton
    fun provideExportManager(
        @ApplicationContext context: Context,
        fetcher: com.company.stockpro.core.domain.repository.StreamingDataFetcher,
        fileManager: com.company.stockpro.core.data.export.ExportFileManager,
        historyRepository: com.company.stockpro.core.domain.repository.ExportHistoryRepository,
        scheduler: com.company.stockpro.core.data.export.ExportScheduler,
    ): com.company.stockpro.core.domain.repository.ExportManager =
        com.company.stockpro.core.data.export.ExportManagerImpl(
            context = context,
            fetcher = fetcher,
            fileManager = fileManager,
            historyRepository = historyRepository,
            scheduler = scheduler,
        )

    @Provides
    @Singleton
    fun provideFirebaseAuth(
        @ApplicationContext context: Context,
    ): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }
}
