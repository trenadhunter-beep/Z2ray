package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface VpnDao {
    
    // SERVER OPERATIONS
    @Query("SELECT * FROM vpn_servers ORDER BY id DESC")
    fun getAllServers(): Flow<List<VpnServer>>

    @Query("SELECT * FROM vpn_servers WHERE isSelected = 1 LIMIT 1")
    fun getSelectedServer(): Flow<VpnServer?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServer(server: VpnServer): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServers(servers: List<VpnServer>)

    @Update
    suspend fun updateServer(server: VpnServer)

    @Delete
    suspend fun deleteServer(server: VpnServer)

    @Query("DELETE FROM vpn_servers WHERE groupName = :groupName")
    suspend fun deleteServersByGroupName(groupName: String)

    @Query("DELETE FROM vpn_servers")
    suspend fun deleteAllServers()

    @Transaction
    suspend fun selectServer(serverId: Int) {
        clearSelectedServer()
        setSelectedServerInternal(serverId)
    }

    @Query("UPDATE vpn_servers SET isSelected = 0")
    suspend fun clearSelectedServer()

    @Query("UPDATE vpn_servers SET isSelected = 1 WHERE id = :serverId")
    suspend fun setSelectedServerInternal(serverId: Int)

    @Query("UPDATE vpn_servers SET latency = :latency WHERE id = :serverId")
    suspend fun updateServerLatency(serverId: Int, latency: Int)

    // SUBSCRIPTION OPERATIONS
    @Query("SELECT * FROM subscriptions ORDER BY id DESC")
    fun getAllSubscriptions(): Flow<List<Subscription>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscription(sub: Subscription): Long

    @Delete
    suspend fun deleteSubscription(sub: Subscription)

    // LOG OPERATIONS
    @Query("SELECT * FROM vpn_logs ORDER BY id DESC LIMIT 200")
    fun getAllLogs(): Flow<List<VpnLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: VpnLog)

    @Query("DELETE FROM vpn_logs")
    suspend fun clearLogs()
}
