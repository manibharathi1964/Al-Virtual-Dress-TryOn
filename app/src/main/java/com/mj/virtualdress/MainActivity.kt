package com.mj.virtualdress

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TryOnApp() }
    }
}

@Composable
private fun TryOnApp() {
    var personModel by remember { mutableStateOf<Any?>(null) }
    var dressModel by remember { mutableStateOf<Any?>(null) }
    var status by remember { mutableStateOf("Ready to create a virtual try-on") }

    val pickPerson = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        personModel = it
        if (it != null) status = "Person photo selected"
    }
    val pickDress = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        dressModel = it
        if (it != null) status = "Dress photo selected"
    }
    val takePerson = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
        if (bitmap != null) {
            personModel = bitmap.asImageBitmap()
            status = "Person photo captured"
        }
    }
    val cameraPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) takePerson.launch(null) else status = "Camera permission is required to capture a person photo."
    }

    fun capturePerson() {
        if (ContextCompat.checkSelfPermission(
                androidx.compose.ui.platform.LocalContext.current, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) takePerson.launch(null)
        else cameraPermission.launch(Manifest.permission.CAMERA)
    }

    MaterialTheme(colorScheme = lightColorScheme(
        primary = Color(0xFF5B3CC4),
        secondary = Color(0xFF8A6FE8),
        background = Color(0xFFF8F7FB)
    )) {
        Scaffold(
            topBar = { TopAppBar(title = { Text("AI Virtual Dress Try-On") }) }
        ) { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("See yourself in a new outfit", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Capture a person and choose a garment to prepare a virtual try-on.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    PhotoCard("Person", personModel, Modifier.weight(1f), Icons.Default.AddAPhoto) {
                        pickPerson.launch("image/*")
                    }
                    PhotoCard("Dress", dressModel, Modifier.weight(1f), Icons.Default.Checkroom) {
                        pickDress.launch("image/*")
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { capturePerson() }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.CameraAlt, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Camera")
                    }
                    OutlinedButton(onClick = { pickPerson.launch("image/*") }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.AddAPhoto, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Gallery")
                    }
                }

                Button(
                    onClick = {
                        status = if (personModel != null && dressModel != null)
                            "Try-on request prepared. Connect the inference service for realistic AI generation."
                        else "Select both a person photo and a dress first."
                    },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Create Virtual Try-On")
                }

                Card(shape = RoundedCornerShape(16.dp)) {
                    Text(status, modifier = Modifier.padding(16.dp))
                }

                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            personModel = null
                            dressModel = null
                            status = "Ready to create a virtual try-on"
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Refresh, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Reset")
                    }
                    OutlinedButton(
                        onClick = { status = "Save/share will be enabled when generated results are available." },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.SaveAlt, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoCard(
    title: String,
    model: Any?,
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(modifier = modifier.height(190.dp), shape = RoundedCornerShape(20.dp), onClick = onClick) {
        Box(
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (model != null) {
                AsyncImage(
                    model = model,
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(icon, null, modifier = Modifier.size(42.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(title)
                    Text("Tap to choose photo", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
