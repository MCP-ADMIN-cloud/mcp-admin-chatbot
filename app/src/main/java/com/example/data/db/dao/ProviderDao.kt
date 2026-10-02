package com.example.data.db.dao

import androidx.room.*
import com.example.data.db.entity.ProviderKeyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProviderDao {
    @Query("SELECT * FROM provider_keys")
    fun getAllProviders(): Flow<List<ProviderKeyEntity>>

    @Query("SELECT * FROM provider_keys WHERE providerId = :providerId LIMIT 1")
    suspend fun getProviderById(providerId: String): ProviderKeyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProvider(provider: ProviderKeyEntity)

    @Update
    suspend fun updateProvider(provider: ProviderKeyEntity)

    @Query("DELETE FROM provider_keys WHERE providerId = :providerId")
    suspend fun deleteProvider(providerId: String)
}
