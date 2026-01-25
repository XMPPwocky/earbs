package net.xmppwocky.earbs.data.backup

import android.content.Context
import android.net.Uri
import android.util.Log
import net.xmppwocky.earbs.data.db.EarbsDatabase
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "DatabaseBackupManager"

/**
 * Manages database backup and restore operations using Android's Storage Access Framework.
 *
 * Backup: Checkpoints WAL, copies database file to user-selected location
 * Restore: Validates SQLite file, replaces current database
 */
class DatabaseBackupManager(private val context: Context) {

    sealed class Result {
        object Success : Result()
        data class Error(val message: String) : Result()
    }

    companion object {
        // SQLite magic header bytes: "SQLite format 3\0"
        private val SQLITE_MAGIC = byteArrayOf(
            0x53, 0x51, 0x4c, 0x69, 0x74, 0x65, 0x20, 0x66,
            0x6f, 0x72, 0x6d, 0x61, 0x74, 0x20, 0x33, 0x00
        )
        private const val DATABASE_NAME = "earbs_database"
    }

    /**
     * Creates a backup of the database to the specified URI.
     *
     * Steps:
     * 1. Checkpoint WAL to flush pending writes
     * 2. Close database connections
     * 3. Copy database file to outputUri
     * 4. Re-open database (handled by caller on next access)
     */
    suspend fun createBackup(outputUri: Uri): Result {
        Log.i(TAG, "Creating backup to $outputUri")

        return try {
            // Get database instance and checkpoint WAL
            val db = EarbsDatabase.getDatabase(context)
            Log.d(TAG, "Checkpointing WAL...")
            db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close()
            Log.d(TAG, "WAL checkpoint complete")

            // Close database connections
            Log.d(TAG, "Closing database...")
            EarbsDatabase.closeDatabase()
            Log.d(TAG, "Database closed")

            // Copy database file to output
            val dbFile = getDatabaseFile()
            if (!dbFile.exists()) {
                Log.e(TAG, "Database file not found: ${dbFile.absolutePath}")
                return Result.Error("Database file not found")
            }

            Log.d(TAG, "Copying database file (${dbFile.length()} bytes)...")
            context.contentResolver.openOutputStream(outputUri)?.use { outputStream ->
                FileInputStream(dbFile).use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: run {
                Log.e(TAG, "Failed to open output stream")
                return Result.Error("Failed to open output location")
            }

            Log.i(TAG, "Backup created successfully")
            Result.Success
        } catch (e: Exception) {
            Log.e(TAG, "Backup failed", e)
            Result.Error("Backup failed: ${e.message}")
        }
    }

    /**
     * Restores the database from the specified URI.
     *
     * Steps:
     * 1. Validate file (SQLite header check)
     * 2. Close database connections
     * 3. Delete existing database files (-shm, -wal)
     * 4. Copy backup to database location
     * 5. Return success (caller handles restart)
     */
    suspend fun restoreBackup(inputUri: Uri): Result {
        Log.i(TAG, "Restoring backup from $inputUri")

        return try {
            // First, validate the file is a SQLite database
            Log.d(TAG, "Validating backup file...")
            val validationResult = validateSqliteFile(inputUri)
            if (validationResult is Result.Error) {
                return validationResult
            }
            Log.d(TAG, "Backup file validated successfully")

            // Close database connections
            Log.d(TAG, "Closing database...")
            EarbsDatabase.closeDatabase()
            Log.d(TAG, "Database closed")

            // Delete existing database files
            val dbFile = getDatabaseFile()
            val shmFile = File(dbFile.parent, "$DATABASE_NAME-shm")
            val walFile = File(dbFile.parent, "$DATABASE_NAME-wal")

            Log.d(TAG, "Deleting existing database files...")
            if (shmFile.exists()) {
                shmFile.delete()
                Log.d(TAG, "Deleted -shm file")
            }
            if (walFile.exists()) {
                walFile.delete()
                Log.d(TAG, "Deleted -wal file")
            }
            if (dbFile.exists()) {
                dbFile.delete()
                Log.d(TAG, "Deleted main database file")
            }

            // Copy backup to database location
            Log.d(TAG, "Copying backup to database location...")
            context.contentResolver.openInputStream(inputUri)?.use { inputStream ->
                FileOutputStream(dbFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: run {
                Log.e(TAG, "Failed to open input stream")
                return Result.Error("Failed to read backup file")
            }

            Log.i(TAG, "Restore completed successfully (${dbFile.length()} bytes)")
            Result.Success
        } catch (e: Exception) {
            Log.e(TAG, "Restore failed", e)
            Result.Error("Restore failed: ${e.message}")
        }
    }

    /**
     * Validates that the given URI points to a valid SQLite database file.
     * Checks the 16-byte magic header.
     */
    private fun validateSqliteFile(uri: Uri): Result {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val header = ByteArray(16)
                val bytesRead = inputStream.read(header)

                if (bytesRead < 16) {
                    Log.e(TAG, "File too small to be a SQLite database (read $bytesRead bytes)")
                    return Result.Error("Invalid backup file: too small")
                }

                if (!header.contentEquals(SQLITE_MAGIC)) {
                    Log.e(TAG, "Invalid SQLite header: ${header.toHexString()}")
                    return Result.Error("Invalid backup file: not a SQLite database")
                }

                Log.d(TAG, "SQLite header validated")
                Result.Success
            } ?: Result.Error("Failed to read backup file")
        } catch (e: Exception) {
            Log.e(TAG, "Validation failed", e)
            Result.Error("Failed to validate backup file: ${e.message}")
        }
    }

    /**
     * Generates a suggested filename for the backup.
     * Format: earbs_backup_YYYYMMDD_HHMMSS.db
     */
    fun generateBackupFilename(): String {
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        val timestamp = dateFormat.format(Date())
        return "earbs_backup_$timestamp.db"
    }

    /**
     * Gets the database file path.
     */
    fun getDatabaseFile(): File {
        return context.getDatabasePath(DATABASE_NAME)
    }

    /**
     * Extension function to convert ByteArray to hex string for logging.
     */
    private fun ByteArray.toHexString(): String {
        return joinToString(" ") { "%02x".format(it) }
    }
}
