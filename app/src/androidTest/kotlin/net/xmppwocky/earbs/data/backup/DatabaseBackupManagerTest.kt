package net.xmppwocky.earbs.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import net.xmppwocky.earbs.data.db.EarbsDatabase
import net.xmppwocky.earbs.data.entity.CardEntity
import net.xmppwocky.earbs.data.entity.FsrsStateEntity
import net.xmppwocky.earbs.data.entity.ReviewSessionEntity
import net.xmppwocky.earbs.data.entity.TrialEntity
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Instrumentation tests for DatabaseBackupManager.
 * Tests backup creation, restore functionality, data integrity, and validation.
 */
@RunWith(AndroidJUnit4::class)
class DatabaseBackupManagerTest {

    private lateinit var context: Context
    private lateinit var backupManager: DatabaseBackupManager
    private lateinit var testBackupDir: File

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        backupManager = DatabaseBackupManager(context)

        // Create a temp directory for test backup files
        testBackupDir = File(context.cacheDir, "test_backups")
        testBackupDir.mkdirs()

        // Ensure clean database state
        EarbsDatabase.closeDatabase()
        context.deleteDatabase("earbs_database")
    }

    @After
    fun teardown() {
        // Clean up test files
        testBackupDir.deleteRecursively()
        EarbsDatabase.closeDatabase()
    }

    // === Backup Tests ===

    @Test
    fun backup_createsValidSqliteFile() = runBlocking {
        // Setup: Initialize database with some data
        val db = EarbsDatabase.getDatabase(context)
        insertTestCard(db)

        // Act: Create backup
        val backupFile = File(testBackupDir, "test_backup.db")
        val uri = Uri.fromFile(backupFile)
        val result = backupManager.createBackup(uri)

        // Assert: Backup succeeded and file has SQLite header
        assertTrue("Backup should succeed", result is DatabaseBackupManager.Result.Success)
        assertTrue("Backup file should exist", backupFile.exists())
        assertTrue("Backup file should not be empty", backupFile.length() > 0)

        // Verify SQLite magic header
        val header = backupFile.inputStream().use { it.readNBytes(16) }
        val expectedMagic = "SQLite format 3\u0000".toByteArray()
        assertArrayEquals("Should have SQLite magic header", expectedMagic, header)
    }

    @Test
    fun backup_containsAllTables() = runBlocking {
        // Setup: Initialize database
        val db = EarbsDatabase.getDatabase(context)
        insertTestCard(db)

        // Act: Create backup
        val backupFile = File(testBackupDir, "tables_backup.db")
        val result = backupManager.createBackup(Uri.fromFile(backupFile))

        // Assert
        assertTrue("Backup should succeed", result is DatabaseBackupManager.Result.Success)

        // Open backup as separate database to verify tables
        val backupDb = Room.databaseBuilder(context, EarbsDatabase::class.java, "verify_backup")
            .createFromFile(backupFile)
            .build()

        try {
            // Query for expected tables via raw query
            val cursor = backupDb.openHelper.readableDatabase.query(
                "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'room_%' AND name NOT LIKE 'android_%'"
            )
            val tables = mutableListOf<String>()
            while (cursor.moveToNext()) {
                tables.add(cursor.getString(0))
            }
            cursor.close()

            assertTrue("Should have cards table", tables.contains("cards"))
            assertTrue("Should have fsrs_state table", tables.contains("fsrs_state"))
            assertTrue("Should have review_sessions table", tables.contains("review_sessions"))
            assertTrue("Should have trials table", tables.contains("trials"))
        } finally {
            backupDb.close()
            context.deleteDatabase("verify_backup")
        }
    }

    @Test
    fun backup_preservesData() = runBlocking {
        // Setup: Insert specific known data
        val db = EarbsDatabase.getDatabase(context)
        val testCard = CardEntity(
            id = "test_card_major_4_arp",
            chordType = "MAJOR",
            octave = 4,
            playbackMode = "ARPEGGIATED",
            unlocked = true
        )
        val testFsrsState = FsrsStateEntity(
            cardId = testCard.id,
            gameType = "CHORD_TYPE",
            stability = 5.5,
            difficulty = 3.2,
            interval = 10,
            dueDate = System.currentTimeMillis() + 86400000,
            reviewCount = 5,
            lastReview = System.currentTimeMillis(),
            phase = 2,
            lapses = 1
        )
        db.cardDao().insert(testCard)
        db.fsrsStateDao().insert(testFsrsState)

        // Act: Create backup
        val backupFile = File(testBackupDir, "data_backup.db")
        val result = backupManager.createBackup(Uri.fromFile(backupFile))

        // Assert
        assertTrue("Backup should succeed", result is DatabaseBackupManager.Result.Success)

        // Open backup and verify data
        val backupDb = Room.databaseBuilder(context, EarbsDatabase::class.java, "verify_data")
            .createFromFile(backupFile)
            .build()

        try {
            val restoredCard = backupDb.cardDao().getById(testCard.id)
            assertNotNull("Card should exist in backup", restoredCard)
            assertEquals("Card chord type should match", "MAJOR", restoredCard?.chordType)

            val restoredFsrs = backupDb.fsrsStateDao().getByCardId(testCard.id)
            assertNotNull("FSRS state should exist in backup", restoredFsrs)
            assertEquals("Stability should match", 5.5, restoredFsrs?.stability ?: 0.0, 0.01)
            assertEquals("Difficulty should match", 3.2, restoredFsrs?.difficulty ?: 0.0, 0.01)
        } finally {
            backupDb.close()
            context.deleteDatabase("verify_data")
        }
    }

    // === Restore Tests ===

    @Test
    fun restore_replacesExistingData() = runBlocking {
        // Setup: Insert data A, backup, then insert data B
        val db = EarbsDatabase.getDatabase(context)
        val cardA = CardEntity(
            id = "card_a",
            chordType = "MAJOR",
            octave = 4,
            playbackMode = "ARPEGGIATED",
            unlocked = true
        )
        db.cardDao().insert(cardA)

        // Create backup with card A
        val backupFile = File(testBackupDir, "restore_test.db")
        backupManager.createBackup(Uri.fromFile(backupFile))

        // Insert card B (different data)
        EarbsDatabase.closeDatabase()
        val db2 = EarbsDatabase.getDatabase(context)
        val cardB = CardEntity(
            id = "card_b",
            chordType = "MINOR",
            octave = 5,
            playbackMode = "BLOCK",
            unlocked = true
        )
        db2.cardDao().insert(cardB)

        // Verify card B exists
        val beforeRestore = db2.cardDao().getById("card_b")
        assertNotNull("Card B should exist before restore", beforeRestore)

        // Act: Restore from backup
        val result = backupManager.restoreBackup(Uri.fromFile(backupFile))

        // Assert
        assertTrue("Restore should succeed", result is DatabaseBackupManager.Result.Success)

        // Re-open database and verify
        val db3 = EarbsDatabase.getDatabase(context)
        val restoredCardA = db3.cardDao().getById("card_a")
        val restoredCardB = db3.cardDao().getById("card_b")

        assertNotNull("Card A should exist after restore", restoredCardA)
        assertNull("Card B should NOT exist after restore", restoredCardB)
    }

    @Test
    fun restore_fromValidBackup_succeeds() = runBlocking {
        // Setup: Create a valid backup
        val db = EarbsDatabase.getDatabase(context)
        insertTestCard(db)

        val backupFile = File(testBackupDir, "valid_backup.db")
        backupManager.createBackup(Uri.fromFile(backupFile))

        // Act: Restore
        val result = backupManager.restoreBackup(Uri.fromFile(backupFile))

        // Assert
        assertTrue("Restore should succeed", result is DatabaseBackupManager.Result.Success)
    }

    @Test
    fun restore_preservesFsrsState() = runBlocking {
        // Setup: Insert FSRS state with specific values
        val db = EarbsDatabase.getDatabase(context)
        val testCard = CardEntity(
            id = "fsrs_test_card",
            chordType = "SUS2",
            octave = 3,
            playbackMode = "ARPEGGIATED",
            unlocked = true
        )
        val testFsrs = FsrsStateEntity(
            cardId = testCard.id,
            gameType = "CHORD_TYPE",
            stability = 12.345,
            difficulty = 6.789,
            interval = 42,
            dueDate = 1700000000000L,
            reviewCount = 15,
            lastReview = 1699999000000L,
            phase = 2,
            lapses = 3
        )
        db.cardDao().insert(testCard)
        db.fsrsStateDao().insert(testFsrs)

        // Create backup
        val backupFile = File(testBackupDir, "fsrs_backup.db")
        backupManager.createBackup(Uri.fromFile(backupFile))

        // Clear database
        EarbsDatabase.closeDatabase()
        context.deleteDatabase("earbs_database")

        // Act: Restore
        val result = backupManager.restoreBackup(Uri.fromFile(backupFile))

        // Assert
        assertTrue("Restore should succeed", result is DatabaseBackupManager.Result.Success)

        val db2 = EarbsDatabase.getDatabase(context)
        val restoredFsrs = db2.fsrsStateDao().getByCardId(testCard.id)

        assertNotNull("FSRS state should be restored", restoredFsrs)
        assertEquals("Stability should match exactly", 12.345, restoredFsrs!!.stability, 0.0001)
        assertEquals("Difficulty should match exactly", 6.789, restoredFsrs.difficulty, 0.0001)
        assertEquals("Interval should match exactly", 42, restoredFsrs.interval)
        assertEquals("Due date should match exactly", 1700000000000L, restoredFsrs.dueDate)
        assertEquals("Review count should match exactly", 15, restoredFsrs.reviewCount)
        assertEquals("Last review should match exactly", 1699999000000L, restoredFsrs.lastReview)
        assertEquals("Phase should match exactly", 2, restoredFsrs.phase)
        assertEquals("Lapses should match exactly", 3, restoredFsrs.lapses)
    }

    @Test
    fun restore_preservesTrialHistory() = runBlocking {
        // Setup: Insert session and trials
        val db = EarbsDatabase.getDatabase(context)
        val session = ReviewSessionEntity(
            id = 0,
            startedAt = System.currentTimeMillis() - 3600000,
            completedAt = System.currentTimeMillis(),
            gameType = "CHORD_TYPE",
            octave = 0,
            playbackMode = "MIXED"
        )
        val sessionId = db.reviewSessionDao().insert(session)

        val trial1 = TrialEntity(
            id = 0,
            sessionId = sessionId,
            cardId = "test_card",
            timestamp = System.currentTimeMillis(),
            wasCorrect = true,
            gameType = "CHORD_TYPE",
            answeredChordType = "MAJOR",
            answeredFunction = null
        )
        val trial2 = TrialEntity(
            id = 0,
            sessionId = sessionId,
            cardId = "test_card",
            timestamp = System.currentTimeMillis() + 1000,
            wasCorrect = false,
            gameType = "CHORD_TYPE",
            answeredChordType = "MINOR",
            answeredFunction = null
        )
        db.trialDao().insert(trial1)
        db.trialDao().insert(trial2)

        // Create backup
        val backupFile = File(testBackupDir, "trials_backup.db")
        backupManager.createBackup(Uri.fromFile(backupFile))

        // Clear database
        EarbsDatabase.closeDatabase()
        context.deleteDatabase("earbs_database")

        // Act: Restore
        val result = backupManager.restoreBackup(Uri.fromFile(backupFile))

        // Assert
        assertTrue("Restore should succeed", result is DatabaseBackupManager.Result.Success)

        val db2 = EarbsDatabase.getDatabase(context)
        val restoredTrials = db2.trialDao().getTrialsForSession(sessionId)

        assertEquals("Should have 2 trials", 2, restoredTrials.size)
        assertTrue("First trial should be correct", restoredTrials[0].wasCorrect)
        assertFalse("Second trial should be incorrect", restoredTrials[1].wasCorrect)
    }

    // === Validation Tests ===

    @Test
    fun restore_rejectsEmptyFile() = runBlocking {
        // Setup: Create empty file
        val emptyFile = File(testBackupDir, "empty.db")
        emptyFile.createNewFile()

        // Act
        val result = backupManager.restoreBackup(Uri.fromFile(emptyFile))

        // Assert
        assertTrue("Should reject empty file", result is DatabaseBackupManager.Result.Error)
        assertTrue(
            "Error should mention too small",
            (result as DatabaseBackupManager.Result.Error).message.contains("too small")
        )
    }

    @Test
    fun restore_rejectsNonSqliteFile() = runBlocking {
        // Setup: Create file with random bytes
        val randomFile = File(testBackupDir, "random.db")
        randomFile.writeBytes(ByteArray(1024) { it.toByte() })

        // Act
        val result = backupManager.restoreBackup(Uri.fromFile(randomFile))

        // Assert
        assertTrue("Should reject non-SQLite file", result is DatabaseBackupManager.Result.Error)
        assertTrue(
            "Error should mention not a SQLite database",
            (result as DatabaseBackupManager.Result.Error).message.contains("not a SQLite")
        )
    }

    @Test
    fun restore_rejectsTextFile() = runBlocking {
        // Setup: Create text file
        val textFile = File(testBackupDir, "text.db")
        textFile.writeText("This is not a SQLite database")

        // Act
        val result = backupManager.restoreBackup(Uri.fromFile(textFile))

        // Assert
        assertTrue("Should reject text file", result is DatabaseBackupManager.Result.Error)
    }

    // === Edge Cases ===

    @Test
    fun backup_afterWalWrites_includesRecentData() = runBlocking {
        // Setup: Write data that may be in WAL
        val db = EarbsDatabase.getDatabase(context)
        val card1 = CardEntity(
            id = "wal_test_1",
            chordType = "MAJOR",
            octave = 4,
            playbackMode = "ARPEGGIATED",
            unlocked = true
        )
        db.cardDao().insert(card1)

        // Immediately insert more data without closing
        val card2 = CardEntity(
            id = "wal_test_2",
            chordType = "MINOR",
            octave = 4,
            playbackMode = "ARPEGGIATED",
            unlocked = true
        )
        db.cardDao().insert(card2)

        // Act: Create backup immediately (WAL should be checkpointed)
        val backupFile = File(testBackupDir, "wal_backup.db")
        val result = backupManager.createBackup(Uri.fromFile(backupFile))

        // Assert
        assertTrue("Backup should succeed", result is DatabaseBackupManager.Result.Success)

        // Verify both cards are in backup
        val backupDb = Room.databaseBuilder(context, EarbsDatabase::class.java, "verify_wal")
            .createFromFile(backupFile)
            .build()

        try {
            val restoredCard1 = backupDb.cardDao().getById("wal_test_1")
            val restoredCard2 = backupDb.cardDao().getById("wal_test_2")

            assertNotNull("First card should be in backup", restoredCard1)
            assertNotNull("Second card (from WAL) should be in backup", restoredCard2)
        } finally {
            backupDb.close()
            context.deleteDatabase("verify_wal")
        }
    }

    @Test
    fun restore_withExistingWalFile_cleansUp() = runBlocking {
        // Setup: Create database with WAL files
        val db = EarbsDatabase.getDatabase(context)
        insertTestCard(db)

        // Create a backup for restoration
        val backupFile = File(testBackupDir, "wal_cleanup_backup.db")
        backupManager.createBackup(Uri.fromFile(backupFile))

        // Re-open and write more data to create WAL
        val db2 = EarbsDatabase.getDatabase(context)
        val extraCard = CardEntity(
            id = "extra_card",
            chordType = "DOM7",
            octave = 5,
            playbackMode = "BLOCK",
            unlocked = true
        )
        db2.cardDao().insert(extraCard)

        // Act: Restore (should clean up WAL files)
        val result = backupManager.restoreBackup(Uri.fromFile(backupFile))

        // Assert
        assertTrue("Restore should succeed", result is DatabaseBackupManager.Result.Success)

        // The extra card should NOT exist after restore
        val db3 = EarbsDatabase.getDatabase(context)
        val extraCardAfterRestore = db3.cardDao().getById("extra_card")
        assertNull("Extra card should not exist after restore (WAL was cleaned)", extraCardAfterRestore)
    }

    @Test
    fun generateBackupFilename_hasCorrectFormat() {
        // Act
        val filename = backupManager.generateBackupFilename()

        // Assert
        assertTrue("Filename should start with earbs_backup_", filename.startsWith("earbs_backup_"))
        assertTrue("Filename should end with .db", filename.endsWith(".db"))
        assertTrue("Filename should have reasonable length", filename.length in 25..35)
    }

    // === Helper functions ===

    private suspend fun insertTestCard(db: EarbsDatabase) {
        val card = CardEntity(
            id = "test_card",
            chordType = "MAJOR",
            octave = 4,
            playbackMode = "ARPEGGIATED",
            unlocked = true
        )
        db.cardDao().insert(card)
    }
}
