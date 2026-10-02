package com.example.data.db.dao

import androidx.room.*
import com.example.data.db.entity.McpServerEntity
import com.example.data.db.entity.McpToolEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface McpDao {
    @Query("SELECT * FROM mcp_servers ORDER BY createdAt ASC")
    fun getAllServers(): Flow<List<McpServerEntity>>

    @Query("SELECT * FROM mcp_servers WHERE isEnabled = 1")
    suspend fun getActiveServers(): List<McpServerEntity>

    @Query("SELECT * FROM mcp_servers WHERE id = :serverId LIMIT 1")
    suspend fun getServerById(serverId: String): McpServerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServer(server: McpServerEntity)

    @Update
    suspend fun updateServer(server: McpServerEntity)

    @Query("DELETE FROM mcp_servers WHERE id = :serverId")
    suspend fun deleteServer(serverId: String)

    @Query("SELECT * FROM mcp_tools ORDER BY name ASC")
    fun getAllTools(): Flow<List<McpToolEntity>>

    @Query("SELECT * FROM mcp_tools WHERE isEnabled = 1")
    suspend fun getActiveToolsList(): List<McpToolEntity>

    @Query("SELECT * FROM mcp_tools WHERE serverId = :serverId")
    fun getToolsForServer(serverId: String): Flow<List<McpToolEntity>>

    @Query("SELECT * FROM mcp_tools WHERE serverId = :serverId")
    suspend fun getToolsListForServer(serverId: String): List<McpToolEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTools(tools: List<McpToolEntity>)

    @Query("DELETE FROM mcp_tools WHERE serverId = :serverId")
    suspend fun deleteToolsForServer(serverId: String)

    @Query("UPDATE mcp_servers SET status = :status, lastError = :lastError, lastSyncedAt = :syncedAt WHERE id = :serverId")
    suspend fun updateServerStatus(serverId: String, status: String, lastError: String? = null, syncedAt: Long = System.currentTimeMillis())
}
