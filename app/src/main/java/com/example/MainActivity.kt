package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.camerax.camera.CameraViewModel
import com.example.camerax.model.MediaItem
import com.example.camerax.ui.screens.CameraScreen
import com.example.camerax.ui.screens.GalleryScreen
import com.example.camerax.ui.screens.PhotoEditorScreen
import com.example.camerax.ui.screens.SettingsScreen
import com.example.ui.theme.CameraGold
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianDark

sealed interface AppScreen {
    object Camera : AppScreen
    object Gallery : AppScreen
    data class PhotoEditor(val item: MediaItem) : AppScreen
    object Settings : AppScreen
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val cameraViewModel: CameraViewModel = viewModel()
            val preferences by cameraViewModel.settingsRepository.preferences.collectAsState()

            MyApplicationTheme(darkTheme = preferences.darkMode) {
                MainContent(viewModel = cameraViewModel)
            }
        }
    }
}

@Composable
fun MainContent(viewModel: CameraViewModel) {
    val context = LocalContext.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val camGranted = permissions[Manifest.permission.CAMERA] ?: false
        hasCameraPermission = camGranted
    }

    if (!hasCameraPermission) {
        PermissionScreen(
            onGrantClicked = {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.CAMERA,
                        Manifest.permission.RECORD_AUDIO
                    )
                )
            }
        )
    } else {
        var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Camera) }

        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "screen_transition"
        ) { screen ->
            when (screen) {
                is AppScreen.Camera -> {
                    CameraScreen(
                        viewModel = viewModel,
                        onNavigateToGallery = { currentScreen = AppScreen.Gallery },
                        onNavigateToSettings = { currentScreen = AppScreen.Settings }
                    )
                }
                is AppScreen.Gallery -> {
                    GalleryScreen(
                        mediaRepository = viewModel.mediaRepository,
                        onNavigateBack = { currentScreen = AppScreen.Camera },
                        onEditPhoto = { item -> currentScreen = AppScreen.PhotoEditor(item) }
                    )
                }
                is AppScreen.PhotoEditor -> {
                    PhotoEditorScreen(
                        mediaItem = screen.item,
                        mediaRepository = viewModel.mediaRepository,
                        onNavigateBack = { currentScreen = AppScreen.Gallery }
                    )
                }
                is AppScreen.Settings -> {
                    SettingsScreen(
                        settingsRepository = viewModel.settingsRepository,
                        onNavigateBack = { currentScreen = AppScreen.Camera }
                    )
                }
            }
        }
    }
}

@Composable
fun PermissionScreen(onGrantClicked: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianDark)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .border(2.dp, CameraGold, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = R.drawable.camerax_brand_icon),
                    contentDescription = "CameraX Logo",
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Welcome to CameraX",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Professional flagship camera experience with manual Pro controls, Color Studio, and 4K video.",
                color = Color(0xFF94A3B8),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = CameraGold,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Camera Access", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("Required for live viewfinder and photo capture", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = CameraGold,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Microphone Access", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("Used to capture crystal-clear audio during video recording", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onGrantClicked,
                colors = ButtonDefaults.buttonColors(containerColor = CameraGold),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("grant_permission_button")
            ) {
                Text(
                    text = "Grant Permissions",
                    color = ObsidianDark,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
