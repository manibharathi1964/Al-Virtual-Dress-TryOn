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
    COMPARE("Compare", Icons.Default.Compare),
    STAFF("Staff", Icons.Default.Badge)
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
    var resultUrl by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf("Ready when you are") }
    var isGenerating by remember { mutableStateOf(false) }
    var selectedColor by remember { mutableStateOf("Black") }
    var selectedSize by remember { mutableStateOf("M") }
    var favorite by remember { mutableStateOf(false) }
    val savedLooks by CustomerSessionStore.looks.collectAsState()
    val selectedProducts by CustomerSessionStore.selected.collectAsState()
    var personValidation by remember { mutableStateOf<String?>(null) }
    var garmentValidation by remember { mutableStateOf<String?>(null) }
    var garmentCategory by remember { mutableStateOf("auto") }
    var garmentColor by remember { mutableStateOf("Unknown") }
    var regenerationMode by remember { mutableStateOf("Regenerate") }

    val context = androidx.compose.ui.platform.LocalContext.current
    var consentGranted by remember { mutableStateOf(PrivacyManager.hasConsent(context)) }
    var showPrivacy by remember { mutableStateOf(!consentGranted) }
    var qrDialog by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val api = remember { TryOnApi() }

    LaunchedEffect(Unit) {
        if (PrivacyManager.shouldExpire(context)) {
            PrivacyManager.clearTemporarySession(context)
            consentGranted = false
            showPrivacy = true
            status = "Previous customer session expired for privacy."
        }
        PrivacyManager.touch(context)
    }

    fun analyzePerson(image: SelectedImage?) {
        if (image == null) return
        scope.launch {
            personValidation = "Checking person, pose and photo quality..."
            personValidation = runCatching { PersonInputAnalyzer.analyze(image.bitmap) }.fold(
                { if (it.accepted) "✓ Person, pose and background detected." else "⚠ " + it.pose.message },
                { "⚠ Photo analysis failed: " + (it.message ?: "Unknown error") }
            )
        }
    }

    fun analyzeGarment(image: SelectedImage?) {
        if (image == null) return
        scope.launch {
            garmentValidation = "Analyzing garment..."
            runCatching { GarmentAnalyzer.analyze(image.bitmap) }.fold(
                { analysis ->
                    garment = bitmapToSelectedImage(analysis.isolatedBitmap)
                    garmentCategory = analysis.category
                    garmentColor = analysis.color
                    garmentValidation = "✓ " + analysis.category + " · " + analysis.color
                },
                { garmentValidation = "⚠ Garment analysis failed: " + (it.message ?: "Unknown error") }
            )
        }
    }

    val pickPerson = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        person = uri?.let { context.readImageAsDataUri(it) }
        result = null
        if (person != null) {
            status = "Person photo selected"
            analyzePerson(person)
        }
    }
    val pickDress = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        garment = uri?.let { context.readImageAsDataUri(it) }
        result = null
        if (garment != null) {
            status = "Garment selected"
            analyzeGarment(garment)
        }
    }
    val takePerson = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
        if (bitmap != null) {
            person = bitmapToSelectedImage(bitmap)
            result = null
            status = "Person photo captured"
            analyzePerson(person)
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
        if (!consentGranted) {
            showPrivacy = true
            status = "Privacy consent is required before processing photos."
            return
        }
        PrivacyManager.touch(context)
        val personImage = person
        val garmentImage = garment
        if (personImage == null || garmentImage == null) {
            status = "Select a person photo and garment first."
            return
        }

        scope.launch {
            isGenerating = true
            result = null
            try {
                status = "Checking person, pose and photo quality..."
                val personCheck = PersonInputAnalyzer.analyze(personImage.bitmap)
                if (!personCheck.accepted) {
                    status = personCheck.pose.message
                    return@launch
                }

                status = "Checking garment isolation and classification..."
                val garmentCheck = GarmentValidator.validate(garmentImage.bitmap)
                if (!garmentCheck.accepted) {
                    status = garmentCheck.message
                    return@launch
                }

                status = "Applying garment with identity-preserving AI..."
                val response = api.generate(
                    TryOnRequest(
                        modelImage = personImage.dataUri,
                        garmentImage = garmentImage.dataUri,
                        category = garmentCategory,
                        mode = "quality",
                        instruction = when (regenerationMode) {
                            "Better Fit" -> "Prioritize natural garment fit and body alignment."
                            "Preserve Face" -> "Prioritize exact preservation of face, skin, hair and identity."
                            "Improve Details" -> "Prioritize fabric texture, seams, folds, draping and garment detail."
                            else -> "Regenerate the result with improved overall realism."
                        }
                    )
                )
                result = response.imageDataUri
                resultUrl = response.resultUrl
                status = "AI try-on completed and quality checked."
            } catch (error: Exception) {
                status = error.message ?: "AI try-on failed. Please retry."
            } finally {
                isGenerating = false
            }
        }
    }

    qrDialog?.let { url ->
        AlertDialog(
            onDismissRequest = { qrDialog = null },
            title = { Text("Scan to open result") },
            text = {
                androidx.compose.foundation.Image(
                    bitmap = ResultActions.qrBitmap(url).asImageBitmap(),
                    contentDescription = "Result QR code",
                    modifier = Modifier.fillMaxWidth().height(280.dp),
                    contentScale = ContentScale.Fit
                )
            },
            confirmButton = { TextButton(onClick = { qrDialog = null }) { Text("Close") } }
        )
    }

    if (showPrivacy) {
        AlertDialog(
            onDismissRequest = { if (consentGranted) showPrivacy = false },
            title = { Text("Privacy & Photo Processing") },
            text = { Text(PrivacyManager.privacySummary()) },
            confirmButton = {
                TextButton(onClick = {
                    PrivacyManager.grantConsent(context)
                    consentGranted = true
                    showPrivacy = false
                    status = "Privacy consent granted."
                }) { Text("I Agree & Continue") }
            },
            dismissButton = {
                if (consentGranted) {
                    TextButton(onClick = { showPrivacy = false }) { Text("Close") }
                }
            }
        )
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
                        personValidation = personValidation,
                        garmentValidation = garmentValidation,
                        garmentCategory = garmentCategory,
                        garmentColor = garmentColor,
                        onPickPerson = { pickPerson.launch("image/*") },
                        onPickDress = { pickDress.launch("image/*") },
                        onCamera = { capturePerson() },
                        onColor = { selectedColor = it },
                        onSize = { selectedSize = it },
                        onGenerate = { generateTryOn() },
                        onSave = { result?.let { runCatching { ResultActions.saveToGallery(context, it); status = "Saved to Gallery." }.onFailure { status = it.message ?: "Save failed." } } },
                        onShare = { result?.let { runCatching { ResultActions.share(context, it) }.onFailure { status = it.message ?: "Share failed." } } },
                        onQr = { resultUrl?.let { qrDialog = it } },
                        regenerationMode = regenerationMode,
                        onRegenerationMode = { regenerationMode = it },
                        onSuggestion = { product ->
                            scope.launch {
                                runCatching { CatalogueRepository.loadProductImage(product) }.fold(
                                    { image -> garment = image; garmentCategory = product.category; garmentColor = product.colors.firstOrNull() ?: "Unknown"; garmentValidation = "✓ Style Assistant: ${product.name}"; status = "Recommended product loaded. Ready for Try On." },
                                    { status = "Could not load recommendation." }
                                )
                            }
                        },
                        onReset = {
                            person = null
                            garment = null
                            result = null
                            isGenerating = false
                            personValidation = null
                            garmentValidation = null
                            garmentCategory = "auto"
                            garmentColor = "Unknown"
                            status = "Ready when you are"
                        }
                    )
                    AppTab.CATALOGUE -> CatalogueScreen(onSelect = { product -> CustomerSessionStore.addProduct(SelectedProduct(product, product.colors.firstOrNull() ?: "Default", product.sizes.firstOrNull() ?: "M")) }, onTryOn = { product ->
                        tab = AppTab.TRY_ON
                        status = "Loading ${product.name}..."
                        scope.launch {
                            runCatching { CatalogueRepository.loadProductImage(product) }.fold(
                                { image ->
                                    garment = image
                                    garmentCategory = product.category
                                    garmentColor = product.colors.firstOrNull() ?: "Unknown"
                                    garmentValidation = "✓ ${product.name} · ${product.category}"
                                    status = "Catalogue product ready for try-on."
                                },
                                { status = "Could not load catalogue product: ${it.message}" }
                            )
                        }
                    })
                    AppTab.FAVORITES -> FavoritesScreen(savedLooks, selectedProducts, onDelete = { CustomerSessionStore.removeLook(it) }, onRemoveProduct = { CustomerSessionStore.removeProduct(it) })
                    AppTab.COMPARE -> CompareScreen(savedLooks)
                    AppTab.STAFF -> StaffModeScreen(onScan = { bitmap ->
                        scope.launch {
                            status = "Scanning product barcode..."
                            val code = runCatching { BarcodeRepository.scan(bitmap) }.getOrNull()
                            val product = code?.let { StaffMode.findProduct(it) }
                            status = if (product != null) "Found ${product.name} (${product.code})" else "Barcode not found in this catalogue."
                            if (product != null) {
                                tab = AppTab.TRY_ON
                                runCatching { CatalogueRepository.loadProductImage(product) }.fold(
                                    { image -> garment = image; garmentCategory = product.category; garmentColor = product.colors.firstOrNull() ?: "Unknown"; garmentValidation = "✓ Staff scan: ${product.name}" },
                                    { status = "Could not load scanned product." }
                                )
                            }
                        }
                    })
                }
            }
        }
    }
}

@Composable
private fun StaffModeScreen(onScan: (Bitmap) -> Unit) {
    val takeScan = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) onScan(bitmap)
    }
    Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Staff Mode", style = MaterialTheme.typography.headlineSmall)
        Text("Fast customer capture and product scanning for store-floor use.", style = MaterialTheme.typography.bodyMedium)
        Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.QrCodeScanner, null, Modifier.size(44.dp))
                Text("Scan Product Barcode / QR", style = MaterialTheme.typography.titleMedium)
                Text("Scan a product code to open its catalogue item and continue directly to Try On.")
                Button(onClick = { takeScan.launch(null) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.QrCodeScanner, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Scan Product")
                }
            }
        }
        Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Staff workflow", style = MaterialTheme.typography.titleMedium)
                Text("1. Capture customer")
                Text("2. Scan product")
                Text("3. Try On")
                Text("4. Save selected items")
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
    personValidation: String?,
    garmentValidation: String?,
    garmentCategory: String,
    garmentColor: String,
    onPickPerson: () -> Unit,
    onPickDress: () -> Unit,
    onCamera: () -> Unit,
    onColor: (String) -> Unit,
    onSize: (String) -> Unit,
    onGenerate: () -> Unit,
    onReset: () -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit,
    onQr: () -> Unit,
    regenerationMode: String,
    onRegenerationMode: (String) -> Unit,
    onSuggestion: (CatalogueProduct) -> Unit
) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Create your look", style = MaterialTheme.typography.headlineSmall)
        Text("Capture yourself, choose a garment, then generate a real AI try-on.", style = MaterialTheme.typography.bodyMedium)
        personValidation?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        garmentValidation?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        if (garmentCategory != "auto") {
            Text("Detected: $garmentCategory · $garmentColor", style = MaterialTheme.typography.labelMedium)
        }

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

        if (garmentCategory != "auto") {
            val suggestions = StyleAssistant.recommend(garmentCategory, garmentColor)
            if (suggestions.isNotEmpty()) {
                Card(shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("AI Style Assistant", style = MaterialTheme.typography.titleMedium)
                        Text("Catalogue-available matches for your current garment.")
                        suggestions.forEach { suggestion ->
                            OutlinedButton(
                                onClick = { onSuggestion(suggestion.product) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(suggestion.title + " · " + suggestion.reason)
                            }
                        }
                    }
                }
            }
        }

        Card(shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("AI regeneration controls", style = MaterialTheme.typography.titleMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("Regenerate", "Better Fit", "Preserve Face", "Improve Details")) { mode ->
                        FilterChip(
                            selected = regenerationMode == mode,
                            onClick = { onRegenerationMode(mode) },
                            label = { Text(mode) }
                        )
                    }
                }
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
            OutlinedButton(onClick = onSave, modifier = Modifier.weight(1f), enabled = result != null) {
                Icon(Icons.Default.Download, null)
                Spacer(Modifier.width(6.dp))
                Text("Save")
            }
            OutlinedButton(onClick = onShare, modifier = Modifier.weight(1f), enabled = result != null) {
                Icon(Icons.Default.Share, null)
                Spacer(Modifier.width(6.dp))
                Text("Share")
            }
            OutlinedButton(onClick = onQr, modifier = Modifier.weight(1f), enabled = resultUrl != null) {
                Icon(Icons.Default.QrCode2, null)
                Spacer(Modifier.width(6.dp))
                Text("QR")
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
private fun CatalogueScreen(onSelect: (CatalogueProduct) -> Unit, onTryOn: (CatalogueProduct) -> Unit) {
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("All") }
    val categories = listOf("All", "Shirt", "Jeans", "Jacket", "Kurti")
    val products = StoreCatalogue.products.filter {
        (category == "All" || it.category == category) &&
            (query.isBlank() || it.name.contains(query, ignoreCase = true) || it.code.contains(query, ignoreCase = true))
    }

    Column(
        Modifier.fillMaxSize().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Store Catalogue", style = MaterialTheme.typography.headlineSmall)
        Text("Product → Catalogue → Try On", style = MaterialTheme.typography.bodyMedium)
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Search product or code") },
            leadingIcon = { Icon(Icons.Default.Search, null) }
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(categories) { item ->
                FilterChip(
                    selected = category == item,
                    onClick = { category = item },
                    label = { Text(item) }
                )
            }
        }
        products.forEach { product ->
            Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = product.imageUrl,
                        contentDescription = product.name,
                        modifier = Modifier.size(76.dp),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(product.name, style = MaterialTheme.typography.titleMedium)
                        Text(product.code + " · " + product.category, style = MaterialTheme.typography.labelSmall)
                        Text("₹" + product.price, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            if (product.inStock) "In stock · " + product.colors.joinToString() else "Out of stock",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = { onSelect(product) }, enabled = product.inStock) { Text("Select") }
                        OutlinedButton(onClick = { onTryOn(product) }, enabled = product.inStock) { Text("Try On") }
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoritesScreen(looks: List<SavedLook>, selectedProducts: List<SelectedProduct>, onDelete: (Long) -> Unit, onRemoveProduct: (String) -> Unit) {
    Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("My Favorites", style = MaterialTheme.typography.headlineSmall)
        if (selectedProducts.isNotEmpty()) {
            Text("Selected Products", style = MaterialTheme.typography.titleMedium)
            selectedProducts.forEach { item ->
                Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(item.product.name, style = MaterialTheme.typography.titleMedium)
                            Text(item.product.code + " · " + item.color + " · " + item.size)
                            Text("₹" + item.product.price)
                        }
                        IconButton(onClick = { onRemoveProduct(item.product.code) }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Remove product")
                        }
                    }
                }
            }
        }

        if (looks.isEmpty()) {
            EmptyState(Icons.Default.FavoriteBorder, "No favorites yet", "Save your best looks here.")
        } else {
            looks.forEach { look ->
                Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = look.resultDataUri,
                            contentDescription = "Saved try-on",
                            modifier = Modifier.size(96.dp),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(look.productName ?: "Saved Try-On Look", style = MaterialTheme.typography.titleMedium)
                            Text("Color: ${look.color ?: "—"} · Size: ${look.size ?: "—"}")
                            Text("Saved for compare and selection.", style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { onDelete(look.id) }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompareScreen(looks: List<SavedLook>) {
    var selectedCount by remember { mutableStateOf(2) }
    Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Compare Looks", style = MaterialTheme.typography.headlineSmall)
        Text("Before/after and multi-dress comparison.", style = MaterialTheme.typography.bodyMedium)
        if (looks.size >= 2) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(looks.take(4)) { look ->
                    Card(Modifier.width(150.dp), shape = RoundedCornerShape(16.dp)) {
                        AsyncImage(model = look.resultDataUri, contentDescription = "Saved look", modifier = Modifier.height(190.dp).fillMaxWidth(), contentScale = ContentScale.Crop)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = selectedCount == 2, onClick = { selectedCount = 2 }, label = { Text("2 looks") })
                FilterChip(selected = selectedCount == 4, onClick = { selectedCount = 4 }, label = { Text("4 looks") })
            }
        } else {
            EmptyState(Icons.Default.Compare, "Need more saved looks", "Save at least two try-on results to compare.")
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
