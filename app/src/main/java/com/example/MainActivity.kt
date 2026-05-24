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
                }
            }

            // Initialize SQLite Room DB
            database = Room.databaseBuilder(
                applicationContext,
                GameDatabase::class.java,
                "shards_of_abyss_db"
            )
            .addMigrations(migration1To2)
            .fallbackToDestructiveMigration()
            .build()

            val db = database ?: throw IllegalStateException("Database is null")
            repository = GameRepository(db.gameDao)
            val repo = repository ?: throw IllegalStateException("Repository is null")
            viewModel = GameViewModel(applicationContext, repo)
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
}
