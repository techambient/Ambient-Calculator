package com.ambient.calculator2

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ambient.calculator2.ui.theme.CalculatorTheme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CalculatorTheme {
                CalculatorApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorApp(viewModel: CalculatorViewModel = viewModel()) {
    val colorScheme = MaterialTheme.colorScheme
    var showHistoryOverlay by remember { mutableStateOf(false) }
    var showCameraScanner by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    
    val isAmoled = viewModel.isAmoledMode
    val context = LocalContext.current

    LaunchedEffect(viewModel.isKeepAwakeEnabled) {
        val window = (context as? android.app.Activity)?.window
        if (viewModel.isKeepAwakeEnabled) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showCameraScanner = true
        }
    }

    if (showSettings) {
        AlertDialog(
            onDismissRequest = { showSettings = false },
            title = { Text("Settings") },
            text = {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Business mode", style = MaterialTheme.typography.bodyLarge)
                            Text("Treat A + B% as A + (A * B / 100)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        }
                        Switch(checked = viewModel.isBusinessMode, onCheckedChange = { viewModel.onAction(CalculatorAction.ToggleBusinessMode) })
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("AMOLED Mode", style = MaterialTheme.typography.bodyLarge)
                            Text("Pure black theme with white text", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        }
                        Switch(checked = viewModel.isAmoledMode, onCheckedChange = { viewModel.onAction(CalculatorAction.ToggleAmoledMode) })
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Incognito Mode", style = MaterialTheme.typography.bodyLarge)
                            Text("Stop saving calculation history", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        }
                        Switch(checked = viewModel.isIncognitoMode, onCheckedChange = { viewModel.onAction(CalculatorAction.ToggleIncognitoMode) })
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Haptic Feedback", style = MaterialTheme.typography.bodyLarge)
                            Text("Vibrate when pressing buttons", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        }
                        Switch(checked = viewModel.isHapticEnabled, onCheckedChange = { viewModel.onAction(CalculatorAction.ToggleHaptic) })
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auto Copy Results", style = MaterialTheme.typography.bodyLarge)
                            Text("Automatically copy to clipboard on '='", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        }
                        Switch(checked = viewModel.isAutoCopyEnabled, onCheckedChange = { viewModel.onAction(CalculatorAction.ToggleAutoCopy) })
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Keep Device Awake", style = MaterialTheme.typography.bodyLarge)
                            Text("Prevent screen from turning off", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        }
                        Switch(checked = viewModel.isKeepAwakeEnabled, onCheckedChange = { viewModel.onAction(CalculatorAction.ToggleKeepAwake) })
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Handwriting to Math", style = MaterialTheme.typography.bodyLarge)
                            Text("Draw math syntax directly on display", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        }
                        Switch(checked = viewModel.isHandwritingEnabled, onCheckedChange = { viewModel.onAction(CalculatorAction.ToggleHandwriting) })
                    }
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    TextButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/techambient/Ambient-Calculator/blob/main/LICENSE"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Open Source License", textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth())
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSettings = false }) {
                    Text("Close")
                }
            }
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = if (isAmoled) Color.Black else colorScheme.background
    ) {
        BoxWithConstraints {
            val width = maxWidth
            val height = maxHeight
            val isWideLayout = width >= 840.dp
            val isPhoneLandscape = height < 500.dp && width > height

            Box(modifier = Modifier.fillMaxSize()) {
                Row(modifier = Modifier.fillMaxSize()) {
                    if (isWideLayout) {
                        Surface(
                            modifier = Modifier.width(300.dp).fillMaxHeight().padding(16.dp),
                            shape = RoundedCornerShape(32.dp),
                            color = if (isAmoled) Color.Black else colorScheme.surfaceContainerLow,
                            tonalElevation = if (isAmoled) 0.dp else 2.dp,
                            border = if (isAmoled) BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)) else null
                        ) {
                            HistorySection(history = viewModel.history, onAction = viewModel::onAction, isAmoledMode = isAmoled, modifier = Modifier.padding(16.dp))
                        }
                    }

                    Scaffold(
                        modifier = Modifier.weight(1f),
                        topBar = {
                            CenterAlignedTopAppBar(
                                title = { if (!isWideLayout) Text("Calculator", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = if (isAmoled) Color.White else colorScheme.onSurface) },
                                actions = {
                                    IconButton(onClick = { 
                                        val permission = Manifest.permission.CAMERA
                                        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) showCameraScanner = true
                                        else permissionLauncher.launch(permission)
                                    }) {
                                        Icon(Icons.Default.PhotoCamera, contentDescription = "Scan", tint = if (isAmoled) Color.White else colorScheme.onSurface)
                                    }
                                    IconButton(onClick = { viewModel.onAction(CalculatorAction.ToggleIncognitoMode) }) {
                                        Icon(if (viewModel.isIncognitoMode) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = "Incognito", tint = if (isAmoled) Color.White else colorScheme.onSurface)
                                    }
                                    IconButton(onClick = { showSettings = true }) {
                                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = if (isAmoled) Color.White else colorScheme.onSurface)
                                    }
                                    if (!isWideLayout) {
                                        IconButton(onClick = { showHistoryOverlay = !showHistoryOverlay }) {
                                            Icon(if (showHistoryOverlay) Icons.Default.MoreVert else Icons.Default.History, contentDescription = "History", tint = if (isAmoled) Color.White else colorScheme.onSurface)
                                        }
                                    }
                                },
                                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                            )
                        },
                        containerColor = Color.Transparent
                    ) { innerPadding ->
                        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                            if (isPhoneLandscape) {
                                Row(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Surface(
                                        modifier = Modifier.weight(1f).fillMaxHeight(),
                                        shape = RoundedCornerShape(32.dp),
                                        color = if (isAmoled) Color.Black else MaterialTheme.colorScheme.surfaceContainerHigh,
                                        tonalElevation = if (isAmoled) 0.dp else 4.dp,
                                        border = if (isAmoled) BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)) else null
                                    ) {
                                        DisplaySection(viewModel = viewModel, expression = viewModel.display, result = viewModel.result, livePreview = viewModel.livePreview, isAmoledMode = isAmoled, modifier = Modifier.padding(16.dp))
                                    }
                                    Box(modifier = Modifier.weight(1.5f)) { ButtonsGrid(viewModel = viewModel, isAdvanced = viewModel.isAdvancedMode) }
                                }
                            } else {
                                CalculatorScreen(viewModel = viewModel)
                            }
                        }
                    }
                }

                if (showCameraScanner) {
                    CameraScanner(onTextScanned = { scannedText -> viewModel.onAction(CalculatorAction.InsertScannedText(scannedText)); showCameraScanner = false }, onDismiss = { showCameraScanner = false })
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = showHistoryOverlay && !isWideLayout,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = if (isAmoled) Color.Black else colorScheme.surface,
                        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                        tonalElevation = 8.dp,
                        border = if (isAmoled) BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)) else null
                    ) {
                        HistorySection(history = viewModel.history, onAction = viewModel::onAction, onClose = { showHistoryOverlay = false }, isAmoledMode = isAmoled, modifier = Modifier.padding(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun CalculatorScreen(viewModel: CalculatorViewModel, modifier: Modifier = Modifier) {
    val isAmoled = viewModel.isAmoledMode
    Column(modifier = modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Surface(
            modifier = Modifier.weight(1.2f).fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            color = if (isAmoled) Color.Black else MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = if (isAmoled) 0.dp else 4.dp,
            border = if (isAmoled) BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)) else null
        ) {
            DisplaySection(viewModel = viewModel, expression = viewModel.display, result = viewModel.result, livePreview = viewModel.livePreview, isAmoledMode = isAmoled, modifier = Modifier.fillMaxSize().padding(28.dp))
        }

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            FilledTonalButton(
                onClick = { viewModel.onAction(CalculatorAction.ToggleMode) },
                shape = CircleShape,
                modifier = Modifier.animateContentSize(),
                colors = if (isAmoled) ButtonDefaults.filledTonalButtonColors(containerColor = Color.Black, contentColor = Color.White) else ButtonDefaults.filledTonalButtonColors(),
                border = if (isAmoled) BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)) else null
            ) {
                Text(if (viewModel.isAdvancedMode) "Basic" else "Advanced", style = MaterialTheme.typography.labelLarge)
            }
        }

        Surface(modifier = Modifier.weight(3f), color = Color.Transparent) {
            ButtonsGrid(viewModel = viewModel, isAdvanced = viewModel.isAdvancedMode)
        }
    }
}

@Composable
fun DisplaySection(viewModel: CalculatorViewModel, expression: String, result: String, livePreview: String = "", isAmoledMode: Boolean = false, modifier: Modifier = Modifier) {
    val isHandwritingEnabled = viewModel.isHandwritingEnabled
    val context = LocalContext.current
    var lines by remember { mutableStateOf(listOf<List<Offset>>()) }
    var currentLine by remember { mutableStateOf(listOf<Offset>()) }
    
    // Ensure the callback always has access to the latest state
    val onHandwritingRecognized = rememberUpdatedState { recognizedText: String ->
        if (recognizedText.isNotEmpty()) {
            viewModel.onAction(CalculatorAction.InsertScannedText(recognizedText))
        }
        lines = emptyList()
    }

    val handwritingManager = remember { HandwritingManager(context) { text -> onHandwritingRecognized.value(text) } }
    var lastDrawTime by remember { mutableStateOf(0L) }

    LaunchedEffect(lastDrawTime) {
        if (lastDrawTime > 0) {
            delay(1000)
            if (System.currentTimeMillis() - lastDrawTime >= 1000) {
                handwritingManager.recognize()
                lastDrawTime = 0
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            handwritingManager.close()
        }
    }

    BoxWithConstraints(
        modifier = modifier.then(if (isHandwritingEnabled) Modifier.pointerInput(isHandwritingEnabled) {
            detectDragGestures(
                onDragStart = { offset -> 
                    currentLine = listOf(offset)
                    handwritingManager.startStroke(offset.x, offset.y, System.currentTimeMillis())
                    lastDrawTime = 0 
                },
                onDrag = { change, _ -> 
                    change.consume()
                    val newOffset = change.position
                    currentLine = currentLine + newOffset
                    handwritingManager.addPoint(newOffset.x, newOffset.y, System.currentTimeMillis())
                },
                onDragEnd = { 
                    lines = lines + listOf(currentLine)
                    currentLine = emptyList()
                    handwritingManager.endStroke()
                    lastDrawTime = System.currentTimeMillis()
                }
            )
        } else Modifier)
    ) {
        if (isHandwritingEnabled) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                lines.forEach { line ->
                    val path = Path()
                    line.firstOrNull()?.let { path.moveTo(it.x, it.y) }
                    line.drop(1).forEach { path.lineTo(it.x, it.y) }
                    drawPath(path = path, color = if (isAmoledMode) Color.White else Color.Gray, style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
                val currentPath = Path()
                currentLine.firstOrNull()?.let { currentPath.moveTo(it.x, it.y) }
                currentLine.drop(1).forEach { currentPath.lineTo(it.x, it.y) }
                drawPath(path = currentPath, color = if (isAmoledMode) Color.White else Color.Gray, style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.End) {
            AnimatedContent(targetState = expression.ifEmpty { "0" }, transitionSpec = { (fadeIn() + slideInVertically()).togetherWith(fadeOut() + slideOutVertically()) }, label = "expression") { targetText ->
                val fontSize = when { targetText.length > 25 -> 14.sp; targetText.length > 15 -> 18.sp; else -> 24.sp }
                Text(text = targetText, style = MaterialTheme.typography.headlineSmall.copy(fontSize = fontSize), color = if (isAmoledMode) Color.White else MaterialTheme.colorScheme.secondary, maxLines = 2, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
            }
            Spacer(modifier = Modifier.height(8.dp))
            val targetResultText = if (result.isNotEmpty()) result else livePreview
            AnimatedContent(targetState = targetResultText, transitionSpec = { scaleIn().togetherWith(scaleOut()) }, label = "result") { targetResult ->
                val fontSize = if (result.isNotEmpty()) { when { targetResult.length > 15 -> 28.sp; targetResult.length > 12 -> 36.sp; targetResult.length > 10 -> 44.sp; targetResult.length > 8 -> 52.sp; else -> 64.sp } } else { when { targetResult.length > 20 -> 14.sp; targetResult.length > 15 -> 18.sp; else -> 24.sp } }
                Text(text = targetResult, style = MaterialTheme.typography.displayLarge.copy(fontWeight = if (result.isNotEmpty()) FontWeight.ExtraBold else FontWeight.Medium, fontSize = fontSize), color = if (isAmoledMode) Color.White else if (result.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, textAlign = TextAlign.End, maxLines = 1, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun HistorySection(history: List<HistoryItem>, onAction: (CalculatorAction) -> Unit, isAmoledMode: Boolean = false, modifier: Modifier = Modifier, onClose: (() -> Unit)? = null) {
    Column(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("History", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = if (isAmoledMode) Color.White else MaterialTheme.colorScheme.onSurface)
            Row {
                TextButton(onClick = { onAction(CalculatorAction.ClearHistory) }) { Text("Clear", color = if (isAmoledMode) Color.White else MaterialTheme.colorScheme.primary) }
                if (onClose != null) { IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = "Close", tint = if (isAmoledMode) Color.White else MaterialTheme.colorScheme.onSurface) } }
            }
        }
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            val grouped = history.groupBy { SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date(it.timestamp)) }
            grouped.forEach { (date, items) ->
                item { Text(text = date, style = MaterialTheme.typography.labelLarge, color = if (isAmoledMode) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 8.dp)) }
                items(items) { item ->
                    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { onAction(CalculatorAction.SelectHistory(item)); onClose?.invoke() }.background(if (isAmoledMode) Color.Black else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)).then(if (isAmoledMode) Modifier.border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp)) else Modifier).padding(16.dp), horizontalAlignment = Alignment.End) {
                        Text(text = item.expression, style = MaterialTheme.typography.bodyLarge, color = if (isAmoledMode) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = item.result, style = MaterialTheme.typography.headlineSmall, color = if (isAmoledMode) Color.White else MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ButtonsGrid(viewModel: CalculatorViewModel, isAdvanced: Boolean) {
    val context = LocalContext.current
    val isAmoled = viewModel.isAmoledMode
    val triggerVibration = remember(viewModel.isHapticEnabled, context) { { try { if (viewModel.isHapticEnabled) { val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager; vibratorManager?.defaultVibrator } else { @Suppress("DEPRECATION") context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator }; vibrator?.let { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) it.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE)) else @Suppress("DEPRECATION") it.vibrate(500) } } } catch (e: Exception) { } } }
    val copyToClipboard = remember(viewModel.isAutoCopyEnabled, context) { { text: String -> try { if (viewModel.isAutoCopyEnabled && text.isNotEmpty() && text != "Error") { val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager; clipboard?.let { val clip = ClipData.newPlainText("Calculation Result", text); it.setPrimaryClip(clip) } } } catch (e: Exception) { } } }
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (isAdvanced) {
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CalcButton("sin", Modifier.weight(1f), MaterialTheme.colorScheme.secondaryContainer, isAmoled) { triggerVibration(); viewModel.onAction(CalculatorAction.Function("sin")) }
                CalcButton("cos", Modifier.weight(1f), MaterialTheme.colorScheme.secondaryContainer, isAmoled) { triggerVibration(); viewModel.onAction(CalculatorAction.Function("cos")) }
                CalcButton("tan", Modifier.weight(1f), MaterialTheme.colorScheme.secondaryContainer, isAmoled) { triggerVibration(); viewModel.onAction(CalculatorAction.Function("tan")) }
                CalcButton("π", Modifier.weight(1f), MaterialTheme.colorScheme.secondaryContainer, isAmoled) { triggerVibration(); viewModel.onAction(CalculatorAction.Constant("π")) }
            }
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CalcButton("log", Modifier.weight(1f), MaterialTheme.colorScheme.secondaryContainer, isAmoled) { triggerVibration(); viewModel.onAction(CalculatorAction.Function("log")) }
                CalcButton("ln", Modifier.weight(1f), MaterialTheme.colorScheme.secondaryContainer, isAmoled) { triggerVibration(); viewModel.onAction(CalculatorAction.Function("ln")) }
                CalcButton("(", Modifier.weight(1f), MaterialTheme.colorScheme.secondaryContainer, isAmoled) { triggerVibration(); viewModel.onAction(CalculatorAction.Operator("(")) }
                CalcButton(")", Modifier.weight(1f), MaterialTheme.colorScheme.secondaryContainer, isAmoled) { triggerVibration(); viewModel.onAction(CalculatorAction.Operator(")")) }
            }
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CalcButton("^", Modifier.weight(1f), MaterialTheme.colorScheme.secondaryContainer, isAmoled) { triggerVibration(); viewModel.onAction(CalculatorAction.Operator("^")) }
                CalcButton("√", Modifier.weight(1f), MaterialTheme.colorScheme.secondaryContainer, isAmoled) { triggerVibration(); viewModel.onAction(CalculatorAction.Function("sqrt")) }
                CalcButton("e", Modifier.weight(1f), MaterialTheme.colorScheme.secondaryContainer, isAmoled) { triggerVibration(); viewModel.onAction(CalculatorAction.Constant("e")) }
                CalcButton("inv", Modifier.weight(1f), MaterialTheme.colorScheme.secondaryContainer, isAmoled) { triggerVibration() }
            }
        }
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CalcButton("AC", Modifier.weight(1f), MaterialTheme.colorScheme.errorContainer, isAmoled) { triggerVibration(); viewModel.onAction(CalculatorAction.Clear) }
            CalcButton("+/-", Modifier.weight(1f), isAmoledMode = isAmoled) { triggerVibration(); viewModel.onAction(CalculatorAction.Sign) }
            CalcButton("%", Modifier.weight(1f), MaterialTheme.colorScheme.tertiaryContainer, isAmoled) { triggerVibration(); viewModel.onAction(CalculatorAction.Operator("%")) }
            CalcButton("÷", Modifier.weight(1f), MaterialTheme.colorScheme.primaryContainer, isAmoled) { triggerVibration(); viewModel.onAction(CalculatorAction.Operator("/")) }
        }
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CalcButton("7", Modifier.weight(1f), isAmoledMode = isAmoled) { triggerVibration(); viewModel.onAction(CalculatorAction.Number(7)) }
            CalcButton("8", Modifier.weight(1f), isAmoledMode = isAmoled) { triggerVibration(); viewModel.onAction(CalculatorAction.Number(8)) }
            CalcButton("9", Modifier.weight(1f), isAmoledMode = isAmoled) { triggerVibration(); viewModel.onAction(CalculatorAction.Number(9)) }
            CalcButton("×", Modifier.weight(1f), MaterialTheme.colorScheme.primaryContainer, isAmoled) { triggerVibration(); viewModel.onAction(CalculatorAction.Operator("*")) }
        }
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CalcButton("4", Modifier.weight(1f), isAmoledMode = isAmoled) { triggerVibration(); viewModel.onAction(CalculatorAction.Number(4)) }
            CalcButton("5", Modifier.weight(1f), isAmoledMode = isAmoled) { triggerVibration(); viewModel.onAction(CalculatorAction.Number(5)) }
            CalcButton("6", Modifier.weight(1f), isAmoledMode = isAmoled) { triggerVibration(); viewModel.onAction(CalculatorAction.Number(6)) }
            CalcButton("-", Modifier.weight(1f), MaterialTheme.colorScheme.primaryContainer, isAmoled) { triggerVibration(); viewModel.onAction(CalculatorAction.Operator("-")) }
        }
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CalcButton("1", Modifier.weight(1f), isAmoledMode = isAmoled) { triggerVibration(); viewModel.onAction(CalculatorAction.Number(1)) }
            CalcButton("2", Modifier.weight(1f), isAmoledMode = isAmoled) { triggerVibration(); viewModel.onAction(CalculatorAction.Number(2)) }
            CalcButton("3", Modifier.weight(1f), isAmoledMode = isAmoled) { triggerVibration(); viewModel.onAction(CalculatorAction.Number(3)) }
            CalcButton("+", Modifier.weight(1f), MaterialTheme.colorScheme.primaryContainer, isAmoled) { triggerVibration(); viewModel.onAction(CalculatorAction.Operator("+")) }
        }
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CalcButton(".", Modifier.weight(1f), isAmoledMode = isAmoled) { triggerVibration(); viewModel.onAction(CalculatorAction.Decimal) }
            CalcButton("0", Modifier.weight(1f), isAmoledMode = isAmoled) { triggerVibration(); viewModel.onAction(CalculatorAction.Number(0)) }
            CalcButton("⌫", Modifier.weight(1f), MaterialTheme.colorScheme.tertiaryContainer, isAmoled) { triggerVibration(); viewModel.onAction(CalculatorAction.Backspace) }
            CalcButton("=", Modifier.weight(1f), MaterialTheme.colorScheme.primary, isAmoled) { triggerVibration(); viewModel.onAction(CalculatorAction.Calculate); copyToClipboard(viewModel.result) }
        }
    }
}

@Composable
fun CalcButton(text: String, modifier: Modifier = Modifier, containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh, isAmoledMode: Boolean = false, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.94f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow), label = "scale")
    val colorScheme = MaterialTheme.colorScheme
    val contentColor = remember(containerColor, colorScheme, isAmoledMode) { if (isAmoledMode) Color.White else when (containerColor) { colorScheme.primary -> colorScheme.onPrimary; colorScheme.primaryContainer -> colorScheme.onPrimaryContainer; colorScheme.secondaryContainer -> colorScheme.onSecondaryContainer; colorScheme.tertiaryContainer -> colorScheme.onTertiaryContainer; colorScheme.errorContainer -> colorScheme.onErrorContainer; else -> colorScheme.onSurface } }
    Box(modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale }.fillMaxHeight().clip(RoundedCornerShape(28.dp)).background(if (isAmoledMode) Color.Black else containerColor).then(if (isAmoledMode) Modifier.border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(28.dp)) else Modifier).clickable(interactionSource = interactionSource, indication = LocalIndication.current, onClick = { haptic.performHapticFeedback(HapticFeedbackType.KeyboardTap); onClick() }), contentAlignment = Alignment.Center) {
        Text(text = text, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = if (text.length > 2) 18.sp else 26.sp), color = contentColor)
    }
}

@Preview(showBackground = true)
@Composable
fun CalculatorPreview() {
    CalculatorTheme { CalculatorApp() }
}
