package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.data.GamePreferences
import com.example.game.GameEngine
import com.example.game.GameMode
import com.example.model.GameCharacter
import com.example.ui.battle.BattleScreen
import com.example.ui.lobby.LobbyScreen
import com.example.ui.theme.BR_DarkBg
import com.example.ui.theme.MyApplicationTheme

enum class ScreenState {
    LOBBY,
    BATTLE
}

class MainActivity : ComponentActivity() {

    private lateinit var prefs: GamePreferences
    private val gameEngine = GameEngine()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        prefs = GamePreferences(this)
        gameEngine.soundManager.isSoundEnabled = prefs.soundEnabled

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BR_DarkBg
                ) {
                    var currentScreen by remember { mutableStateOf(ScreenState.LOBBY) }

                    BackHandler(enabled = currentScreen == ScreenState.BATTLE) {
                        currentScreen = ScreenState.LOBBY
                        gameEngine.isMatchActive = false
                    }

                    when (currentScreen) {
                        ScreenState.LOBBY -> {
                            LobbyScreen(
                                prefs = prefs,
                                onStartMatch = { mode, char ->
                                    gameEngine.startMatch(char, mode)
                                    currentScreen = ScreenState.BATTLE
                                }
                            )
                        }
                        ScreenState.BATTLE -> {
                            BattleScreen(
                                gameEngine = gameEngine,
                                prefs = prefs,
                                onReturnToLobby = {
                                    gameEngine.isMatchActive = false
                                    currentScreen = ScreenState.LOBBY
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
