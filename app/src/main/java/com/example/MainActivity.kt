package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.GameDatabase
import com.example.data.GameRepository
import com.example.ui.hud.MainGameScreen
import com.example.viewmodel.GameViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private var initError: Throwable? = null
    private var database: GameDatabase? = null
    private var repository: GameRepository? = null
    private var viewModel: GameViewModel? = null

    companion object {
        @Volatile
        private var staticDatabase: GameDatabase? = null
        @Volatile
        private var staticRepository: GameRepository? = null

        @Synchronized
        fun getDatabase(context: android.content.Context, migration: Migration): GameDatabase {
            return staticDatabase ?: Room.databaseBuilder(
                context.applicationContext,
                GameDatabase::class.java,
                "shards_of_abyss_db"
            )
            .addMigrations(migration)
            .fallbackToDestructiveMigration()
            .build().also { staticDatabase = it }
        }

        @Synchronized
        fun getRepository(db: GameDatabase): GameRepository {
            return staticRepository ?: GameRepository(db.gameDao).also { staticRepository = it }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Setup global handler to capture async crashes elegantly
        val oldHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            android.util.Log.e("SHARDS_CRASH", "FATAL UNCAUGHT EXCEPTION in thread: ${thread.name}", throwable)
            if (oldHandler != null) {
                oldHandler.uncaughtException(thread, throwable)
            } else {
                android.os.Process.killProcess(android.os.Process.myPid())
                java.lang.System.exit(10)
            }
        }

        try {
            val migration1To2 = object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    try {
                        db.execSQL("ALTER TABLE pois ADD COLUMN osmTags TEXT NOT NULL DEFAULT '{}'")
                    } catch (e: Exception) {
                        android.util.Log.w("SHARDS_MIGRATION", "Column osmTags might already exist", e)
                    }
                    try {
                        db.execSQL("ALTER TABLE pois ADD COLUMN realName TEXT")
                    } catch (e: Exception) {
                        android.util.Log.w("SHARDS_MIGRATION", "Column realName might already exist", e)
                    }
                    try {
                        db.execSQL("ALTER TABLE pois ADD COLUMN cooldownUntil INTEGER NOT NULL DEFAULT 0")
                    } catch (e: Exception) {
                        android.util.Log.w("SHARDS_MIGRATION", "Column cooldownUntil might already exist", e)
                    }
                    try {
                        db.execSQL("CREATE TABLE IF NOT EXISTS poi_cooldowns (poiId TEXT NOT NULL PRIMARY KEY, cooldownUntil INTEGER NOT NULL)")
                    } catch (e: Exception) {
                        android.util.Log.w("SHARDS_MIGRATION", "Table poi_cooldowns creation failed or already exists", e)
                    }
                    try {
                        db.execSQL("CREATE INDEX IF NOT EXISTS index_pois_cooldownUntil ON pois(cooldownUntil)")
                    } catch (e: Exception) {
                        android.util.Log.w("SHARDS_MIGRATION", "Index index_pois_cooldownUntil on pois failed", e)
                    }
                    try {
                        db.execSQL("CREATE INDEX IF NOT EXISTS index_poi_cooldowns_cooldownUntil ON poi_cooldowns(cooldownUntil)")
                    } catch (e: Exception) {
                        android.util.Log.w("SHARDS_MIGRATION", "Index index_poi_cooldowns_cooldownUntil on poi_cooldowns failed", e)
                    }
                }
            }

            // Get/Initialize active static db and repo
            var db: GameDatabase
            try {
                db = getDatabase(applicationContext, migration1To2)
                db.openHelper.writableDatabase // trigger actual load/migration check
            } catch (t: Throwable) {
                android.util.Log.e("SHARDS_INIT", "Database migration/validation failed, performing destructive recovery", t)
                try {
                    applicationContext.deleteDatabase("shards_of_abyss_db")
                } catch (de: Exception) {
                    android.util.Log.e("SHARDS_INIT", "Failed to clear database files", de)
                }
                staticDatabase = null
                staticRepository = null
                db = getDatabase(applicationContext, migration1To2)
                db.openHelper.writableDatabase
            }

            database = db
            val repo = getRepository(db)
            repository = repo

            // Instantiate retained ViewModel safely through ViewModelProvider
            val vmFactory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return GameViewModel(applicationContext, repo) as T
                }
            }
            viewModel = ViewModelProvider(this, vmFactory)[GameViewModel::class.java]
        } catch (t: Throwable) {
            android.util.Log.e("SHARDS_INIT", "Database/ViewModel initialization failure", t)
            initError = t
        }

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val error = initError
                val vm = viewModel

                if (error != null || vm == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF0F0808)),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.material3.Card(
                            colors = androidx.compose.material3.CardDefaults.cardColors(
                                containerColor = Color(0xFF1F1111)
                            ),
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                androidx.compose.material3.Text(
                                    text = "⚠️ Сбой запуска игры",
                                    color = Color(0xFFFF5555),
                                    style = androidx.compose.material3.MaterialTheme.typography.titleLarge
                                )
                                androidx.compose.material3.Text(
                                    text = "Произошла непредвиденная ошибка при инициализации или сборке компонентов базы данных:\n\n${error?.localizedMessage ?: "Неизвестный сбой VM"}",
                                    color = Color.White.copy(alpha = 0.8f),
                                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    val currentTab by vm.currentTab.collectAsState()
                    val uiState by vm.uiState.collectAsState()

                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = Color(0xFF0C0D11)
                    ) {
                        MainGameScreen(
                            currentTab = currentTab,
                            uiState = uiState,
                            viewModel = vm,
                            onTabSelected = { tab -> vm.selectTab(tab) }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel?.mapViewModel?.onResume()
    }

    override fun onPause() {
        super.onPause()
        viewModel?.mapViewModel?.onPause()
    }
}
