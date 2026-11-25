package com.mj.virtualdress

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

private enum class AppTab(val label: String, val icon: ImageVector) {
    TRY_ON("Try On", Icons.Default.Checkroom),
    CATALOGUE("Catalogue", Icons.Default.Storefront),
    FAVORITES("Favorites", Icons.Default.Favorite),
    COMPARE("Compare", Icons.Default.Compare)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TryOnApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TryOnApp() {
    var tab by remember { mutableStateOf(AppTab.TRY_ON) }
    var person by remember { mutableStateOf<SelectedImage?>(null) }
    var garment by remember { mutableStateOf<SelectedImage?>(null) }
    var result by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf("Ready when you are") }
    var isGenerating by remember { mutableStateOf(false) }
    var selectedColor by remember { mutableStateOf("Black") }
    var selectedSize by remember { mutableStateOf("M") }
    var favorite by remember { mutableStateOf(false) }
    var personValidation by remember { mutableStateOf<String?>(null) }
    var garmentValidation by remember { mutableStateOf<String?>(null) }
    var garmentCategory by remember { mutableStateOf("auto") }
    var garmentColor by remember { mutableStateOf("Unknown") }

    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val api = remember { TryOnApi() }

    val pickPerson = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        person = uri?.let { context.readImageAsDataUri(it) }
        if (person != null) {
            result = null
            status = "Person photo selected"
        }
    }
    val pickDress = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        garment = uri?.let { context.readImageAsDataUri(it) }
        if (garment != null) {
            result = null
            status = "Garment selected"
        }
    }
    val takePerson = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
        if (bitmap != null) {
            person = bitmapToSelectedImage(bitmap)
            result = null
            status = "Person photo captured"
        }
    }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) takePerson.launch(null) else status = "Camera permission is required."
    }

    fun capturePerson() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            takePerson.launch(null)
        } else {
            cameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    fun generateTryOn() {
        val personImage = person
        val garmentImage = garment
        if (personImage == null || garmentImage == null) {
            status = "Select a person photo and garment first."
            return
        }

        scope.launch {
            isGenerating = true
            result = null
            status = "Preparing person image..."
            try {
                status = "Sending person + garment to AI..."
                result = api.generate(
                    TryOnRequest(
                        modelImage = personImage.dataUri,
                        garmentImage = garmentImage.dataUri,
                        category = "auto",
                        mode = "balanced"
                    )
                )
                status = "AI try-on completed."
            } catch (error: Exception) {
                status = error.message ?: "AI try-on failed. Please retry."
            } finally {
                isGenerating = false
            }
        }
    }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF5B3CC4),
            onPrimary = Color.White,
            secondary = Color(0xFF8A6FE8),
            background = Color(0xFFF7F5FA),
            surface = Color.White
        )
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("AI Virtual Dress Try-On", style = MaterialTheme.typography.titleLarge)
                            Text("Smart fitting room", style = MaterialTheme.typography.labelSmall)
                        }
                    },
                    actions = {
                        IconButton(onClick = { favorite = !favorite }) {
                            Icon(
                                if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite"
                            )
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar {
                    AppTab.entries.forEach { item ->
                        NavigationBarItem(
                            selected = tab == item,
                            onClick = { tab = item },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when (tab) {
                    AppTab.TRY_ON -> TryOnScreen(
                        person = person,
                        garment = garment,
                        result = result,
                        status = status,
                        isGenerating = isGenerating,
                        selectedColor = selectedColor,
                        selectedSize = selectedSize,
                        onPickPerson = { pickPerson.launch("image/*") },
                        onPickDress = { pickDress.launch("image/*") },
                        onCamera = { capturePerson() },
                        onColor = { selectedColor = it },
                        onSize = { selectedSize = it },
                        onGenerate = { generateTryOn() },
                        onReset = {
                            person = null
                            garment = null
                            result = null
                            isGenerating = false
                            status = "Ready when you are"
                        }
                    )
                    AppTab.CATALOGUE -> CatalogueScreen()
                    AppTab.FAVORITES -> FavoritesScreen(favorite)
                    AppTab.COMPARE -> CompareScreen()
                }
            }
        }
    }
}

@Composable
private fun TryOnScreen(
    person: SelectedImage?,
    garment: SelectedImage?,
    result: String?,
    status: String,
    isGenerating: Boolean,
    selectedColor: String,
    selectedSize: String,
    onPickPerson: () -> Unit,
    onPickDress: () -> Unit,
    onCamera: () -> Unit,
    onColor: (String) -> Unit,
    onSize: (String) -> Unit,
    onGenerate: () -> Unit,
    onReset: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Create your look", style = MaterialTheme.typography.headlineSmall)
        Text("Capture yourself, choose a garment, then generate a real AI try-on.", style = MaterialTheme.typography.bodyMedium)\n        personValidation?.let { Text(it, style = MaterialTheme.typography.bodySmall) }\n        garmentValidation?.let { Text(it, style = MaterialTheme.typography.bodySmall) }\n        if (garmentCategory != "auto") Text("Detected: $garmentCategory · $garmentColor", style = MaterialTheme.typography.labelMedium)

        if (result != null) {
            Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                AsyncImage(
                    model = result,
                    contentDescription = "AI virtual try-on result",
                    modifier = Modifier.fillMaxWidth().height(360.dp),
                    contentScale = ContentScale.Crop
                )
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                InputCard(
                    title = "Person",
                    model = person?.previewModel,
                    icon = Icons.Default.Person,
                    onClick = onPickPerson,
                    modifier = Modifier.weight(1f)
                )
                InputCard(
                    title = "Garment",
                    model = garment?.previewModel,
                    icon = Icons.Default.Checkroom,
                    onClick = onPickDress,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onCamera, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.CameraAlt, null)
                Spacer(Modifier.width(6.dp))
                Text("Camera")
            }
            OutlinedButton(onClick = onPickPerson, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.PhotoLibrary, null)
                Spacer(Modifier.width(6.dp))
                Text("Gallery")
            }
        }

        Card(shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Product options", style = MaterialTheme.typography.titleMedium)
                Text("Color", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("Black", "White", "Red", "Blue")) { color ->
                        FilterChip(
                            selected = selectedColor == color,
                            onClick = { onColor(color) },
                            label = { Text(color) }
                        )
                    }
                }
                Text("Size", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("S", "M", "L", "XL")) { size ->
                        FilterChip(
                            selected = selectedSize == size,
                            onClick = { onSize(size) },
                            label = { Text(size) }
                        )
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(18.dp)
        ) {
            Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, null)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(status, style = MaterialTheme.typography.bodyMedium)
                    if (isGenerating) {
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                    }
                }
            }
        }

        Button(
            onClick = onGenerate,
            enabled = !isGenerating,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.AutoAwesome, null)
            Spacer(Modifier.width(8.dp))
            Text(if (isGenerating) "Generating AI Try-On..." else "Create Virtual Try-On")
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onReset, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Refresh, null)
                Spacer(Modifier.width(6.dp))
                Text("Reset")
            }
            OutlinedButton(onClick = { }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Share, null)
                Spacer(Modifier.width(6.dp))
                Text("Share")
            }
        }
    }
}

@Composable
private fun InputCard(
    title: String,
    model: Any?,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(170.dp),
        shape = RoundedCornerShape(20.dp),
        onClick = onClick
    ) {
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
                    Icon(icon, null, Modifier.size(40.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text("Tap to select", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun CatalogueScreen() {
    val products = listOf(
        "Classic Black Shirt" to "₹899",
        "Premium Blue Jeans" to "₹1,299",
        "Modern Jacket" to "₹1,799",
        "Elegant Kurti" to "₹1,099"
    )
    Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Store Catalogue", style = MaterialTheme.typography.headlineSmall)
        Text("Browse products and send any item to Try On.", style = MaterialTheme.typography.bodyMedium)
        products.forEach { (name, price) ->
            Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Checkroom, null, Modifier.size(34.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(name, style = MaterialTheme.typography.titleMedium)
                        Text(price, style = MaterialTheme.typography.bodyMedium)
                    }
                    OutlinedButton(onClick = { }) { Text("Try On") }
                }
            }
        }
    }
}

@Composable
private fun FavoritesScreen(hasFavorite: Boolean) {
    Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("My Favorites", style = MaterialTheme.typography.headlineSmall)
        if (hasFavorite) {
            Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Favorite, null, Modifier.size(36.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Saved Try-On Look", style = MaterialTheme.typography.titleMedium)
                        Text("Ready for comparison or sharing.")
                    }
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete")
                    }
                }
            }
        } else {
            EmptyState(Icons.Default.FavoriteBorder, "No favorites yet", "Save your best looks here.")
        }
    }
}

@Composable
private fun CompareScreen() {
    Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Compare Looks", style = MaterialTheme.typography.headlineSmall)
        Text("Compare two or more saved try-on results side by side.", style = MaterialTheme.typography.bodyMedium)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CompareSlot("Look 1", Modifier.weight(1f))
            CompareSlot("Look 2", Modifier.weight(1f))
        }
        Button(onClick = { }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Compare, null)
            Spacer(Modifier.width(8.dp))
            Text("Compare Selected Looks")
        }
    }
}

@Composable
private fun CompareSlot(label: String, modifier: Modifier = Modifier) {
    Card(modifier.height(190.dp), shape = RoundedCornerShape(18.dp)) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.AddPhotoAlternate, null, Modifier.size(38.dp))
                Spacer(Modifier.height(8.dp))
                Text(label)
                Text("Select a saved result", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun EmptyState(icon: ImageVector, title: String, subtitle: String) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Column(
            Modifier.fillMaxWidth().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, Modifier.size(46.dp))
            Spacer(Modifier.height(10.dp))
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
