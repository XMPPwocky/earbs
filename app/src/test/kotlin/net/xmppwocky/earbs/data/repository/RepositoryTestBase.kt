package net.xmppwocky.earbs.data.repository

import net.xmppwocky.earbs.data.DatabaseTestBase
import org.junit.Before

/**
 * Base class for repository tests that need EarbsRepository.
 * Provides shared repository setup to reduce boilerplate.
 */
abstract class RepositoryTestBase : DatabaseTestBase() {
    protected lateinit var repository: EarbsRepository

    @Before
    fun setupRepository() {
        repository = EarbsRepository(
            cardDao = cardDao,
            functionCardDao = functionCardDao,
            progressionCardDao = progressionCardDao,
            intervalCardDao = intervalCardDao,
            fsrsStateDao = fsrsStateDao,
            reviewSessionDao = reviewSessionDao,
            trialDao = trialDao,
            historyDao = historyDao,
            prefs = prefs
        )
    }

    protected fun setSessionSize(size: Int) {
        prefs.edit().putInt("session_size", size).apply()
    }
}
