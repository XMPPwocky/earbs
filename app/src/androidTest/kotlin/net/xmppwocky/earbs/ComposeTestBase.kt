package net.xmppwocky.earbs

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.mockk.mockk
import net.xmppwocky.earbs.audio.AudioEngine
import net.xmppwocky.earbs.data.db.EarbsDatabase
import net.xmppwocky.earbs.data.repository.EarbsRepository
import org.junit.After
import org.junit.Before
import org.junit.Rule

/**
 * Base class for Compose UI integration tests.
 * Provides an in-memory Room database, repository, and Compose test rule.
 */
abstract class ComposeTestBase {

    @get:Rule
    val composeTestRule = createComposeRule()

    protected lateinit var db: EarbsDatabase
    protected lateinit var repository: EarbsRepository
    protected lateinit var context: Context
    protected lateinit var prefs: SharedPreferences

    protected val mockAudioEngine: AudioEngine = mockk(relaxed = true)

    @Before
    open fun setup() {
        context = ApplicationProvider.getApplicationContext()
        prefs = context.getSharedPreferences("test_prefs", Context.MODE_PRIVATE)

        db = Room.inMemoryDatabaseBuilder(context, EarbsDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        repository = EarbsRepository(
            cardDao = db.cardDao(),
            functionCardDao = db.functionCardDao(),
            fsrsStateDao = db.fsrsStateDao(),
            reviewSessionDao = db.reviewSessionDao(),
            trialDao = db.trialDao(),
            historyDao = db.historyDao(),
            prefs = prefs
        )
    }

    @After
    open fun tearDown() {
        db.close()
        prefs.edit().clear().apply()
    }

    companion object {
        const val HOUR_MS = 60 * 60 * 1000L
        const val DAY_MS = 24 * HOUR_MS
    }
}
