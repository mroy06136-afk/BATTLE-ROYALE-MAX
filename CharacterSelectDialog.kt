package com.example.ui.lobby

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.GamePreferences
import com.example.model.GameCharacter
import com.example.ui.theme.*

@Composable
fun CharacterSelectDialog(
    prefs: GamePreferences,
    onSelectCharacter: (GameCharacter) -> Unit,
    onDismiss: () -> Unit
) {
    val allCharacters = remember { GameCharacter.getAllCharacters() }
    var selectedChar by remember {
        mutableStateOf(allCharacters.firstOrNull { it.id == prefs.selectedCharacterId } ?: allCharacters.first())
    }
    var gold by remember { mutableStateOf(prefs.gold) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            shape = RoundedCornerShape(16.dp),
            color = BR_DarkBg,
            border = androidx.compose.foundation.BorderStroke(2.dp, BR_Orange)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "CHOOSE YOUR COMBAT SPECIALIST",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Row(modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
                    // Left: Character Roster
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(allCharacters) { char ->
                            val isChosen = (selectedChar.id == char.id)
                            val isEquipped = (prefs.selectedCharacterId == char.id)

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedChar = char }
                                    .border(
                                        1.5.dp,
                                        if (isChosen) BR_Orange else BR_Border,
                                        RoundedCornerShape(10.dp)
                                    ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isChosen) Color(0xFF1F2436) else BR_Surface
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .background(Color(char.roleColorHex), CircleShape)
                                        ) {
                                            Text(
                                                char.iconEmoji,
                                                fontSize = 20.sp,
                                                modifier = Modifier.align(Alignment.Center)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(char.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text(char.title, color = BR_TextSecondary, fontSize = 11.sp)
                                        }
                                    }

                                    if (isEquipped) {
                                        Icon(Icons.Default.Check, contentDescription = "Equipped", tint = BR_Cyan)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Right: Detailed Ability & Stats Preview
                    Card(
                        modifier = Modifier.weight(1.3f).fillMaxHeight(),
                        colors = CardDefaults.cardColors(containerColor = BR_Surface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .background(Color(selectedChar.roleColorHex), CircleShape)
                                        .align(Alignment.CenterHorizontally)
                                ) {
                                    Text(
                                        selectedChar.iconEmoji,
                                        fontSize = 32.sp,
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    selectedChar.name,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                                Text(
                                    selectedChar.title,
                                    fontSize = 12.sp,
                                    color = BR_Orange,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )

                                Divider(color = BR_Border, modifier = Modifier.padding(vertical = 12.dp))

                                Text("TACTICAL SPECIAL ABILITY:", fontSize = 11.sp, color = BR_TextSecondary, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    selectedChar.abilityName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BR_Cyan
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    selectedChar.abilityDescription,
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    lineHeight = 16.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Cooldown: ${selectedChar.abilityCooldownSec}s | Speed: +${((selectedChar.speedMultiplier - 1f) * 100).toInt()}%",
                                    fontSize = 11.sp,
                                    color = BR_Gold,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            val isCurrentlyEquipped = (prefs.selectedCharacterId == selectedChar.id)
                            Button(
                                onClick = {
                                    prefs.selectedCharacterId = selectedChar.id
                                    onSelectCharacter(selectedChar)
                                    onDismiss()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isCurrentlyEquipped) BR_CyanDark else BR_Orange
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("equip_character_button")
                            ) {
                                Text(
                                    if (isCurrentlyEquipped) "EQUIPPED" else "SELECT SPECIALIST",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
