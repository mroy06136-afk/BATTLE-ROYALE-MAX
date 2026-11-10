package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.game.GameEngine
import com.example.game.GameMode
import com.example.model.GameCharacter
import com.example.model.SafeZone
import com.example.model.Weapon
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("BATTLE ROYALE MAX", appName)
  }

  @Test
  fun `game engine initializes 50 players`() {
    val engine = GameEngine()
    engine.soundManager.isSoundEnabled = false
    val character = GameCharacter.getAllCharacters()[0]
    engine.startMatch(character, GameMode.SOLO)

    assertEquals(50, engine.players.size)
    assertEquals(50, engine.alivePlayersCount)
    assertFalse(engine.isGameOver)
  }

  @Test
  fun `safe zone calculation works`() {
    val safeZone = SafeZone(currentCenterX = 0f, currentCenterZ = 0f, currentRadius = 300f)
    assertTrue(safeZone.isInsideSafeZone(0f, 0f))
    assertTrue(safeZone.isInsideSafeZone(200f, 200f))
    assertFalse(safeZone.isInsideSafeZone(350f, 0f))
  }

  @Test
  fun `weapon headshot damage multiplier applies`() {
    val ak = Weapon.createAK47()
    val bodyDamage = ak.getEffectiveDamage(false)
    val headDamage = ak.getEffectiveDamage(true)
    assertTrue(headDamage > bodyDamage)
  }
}
