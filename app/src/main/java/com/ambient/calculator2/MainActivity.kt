package com.ambient.calculator2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ambient.calculator2.ui.theme.CalculatorTheme
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
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = colorScheme.background
    ) {
        BoxWithConstraints {
            val width = maxWidth
            val height = maxHeight
            
            // Logic for different screen sizes and orientations
            val isWideLayout = width >= 840.dp
            val isPhoneLandscape = height < 500.dp && width > height

            Box(modifier = Modifier.fillMaxSize()) {
                Row(modifier = Modifier.fillMaxSize()) {
                    if (isWideLayout) {
                        Surface(
                            modifier = Modifier
                                .width(300.dp)
                                .fillMaxHeight()
                                .padding(16.dp),
                            shape = RoundedCornerShape(32.dp),
                            color = colorScheme.surfaceContainerLow,
                            tonalElevation = 2.dp
                        ) {
                            HistorySection(
                                history = viewModel.history,
                                onAction = viewModel::onAction,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }

                    Scaffold(
                        modifier = Modifier.weight(1f),
                        topBar = {
                            if (!isWideLayout) {
                                CenterAlignedTopAppBar(
                                    title = { 
                                        Text(
                                            "Calculator",
                                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                        ) 
                                    },
                                    actions = {
                                        IconButton(onClick = { showHistoryOverlay = !showHistoryOverlay }) {
                                            Icon(
                                                if (showHistoryOverlay) Icons.Default.MoreVert else Icons.Default.History, 
                                                contentDescription = "History"
                                            )
                                        }
                                    },
                                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                        containerColor = Color.Transparent
                                    )
                                )
                            }
                        },
                        containerColor = Color.Transparent
                    ) { innerPadding ->
                        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                            if (isPhoneLandscape) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Surface(
                                        modifier = Modifier.weight(1f).fillMaxHeight(),
                                        shape = RoundedCornerShape(32.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        tonalElevation = 4.dp
                                    ) {
                                        DisplaySection(
                                            expression = viewModel.display,
                                            result = viewModel.result,
                                            modifier = Modifier.padding(16.dp)
                                        )
                                    }
                                    Box(modifier = Modifier.weight(1.5f)) {
                                        ButtonsGrid(viewModel = viewModel, isAdvanced = viewModel.isAdvancedMode)
                                    }
                                }
                            } else {
                                CalculatorScreen(viewModel = viewModel)
                            }
                        }
                    }
                }

                // Global History Overlay for Phone/Portrait
                androidx.compose.animation.AnimatedVisibility(
                    visible = showHistoryOverlay && !isWideLayout,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = colorScheme.surface,
                        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                        tonalElevation = 8.dp
                    ) {
                        HistorySection(
                            history = viewModel.history,
                            onAction = viewModel::onAction,
                            onClose = { showHistoryOverlay = false },
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun CalculatorScreen(
    viewModel: CalculatorViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            modifier = Modifier
                .weight(1.2f)
                .fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 4.dp
        ) {
            DisplaySection(
                expression = viewModel.display,
                result = viewModel.result,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(28.dp)
            )
        }

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            FilledTonalButton(
                onClick = { viewModel.onAction(CalculatorAction.ToggleMode) },
                shape = CircleShape,
                modifier = Modifier.animateContentSize()
            ) {
                Text(
                    if (viewModel.isAdvancedMode) "Basic" else "Advanced",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        Surface(
            modifier = Modifier.weight(3f),
            color = Color.Transparent
        ) {
            ButtonsGrid(
                viewModel = viewModel,
                isAdvanced = viewModel.isAdvancedMode
            )
        }
    }
}

@Composable
fun DisplaySection(
    expression: String,
    result: String,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.End
        ) {
            AnimatedContent(
                targetState = expression.ifEmpty { "0" },
                transitionSpec = {
                    (fadeIn() + slideInVertically()).togetherWith(fadeOut() + slideOutVertically())
                },
                label = "expression"
            ) { targetText ->
                val fontSize = when {
                    targetText.length > 25 -> 14.sp
                    targetText.length > 15 -> 18.sp
                    else -> 24.sp
                }
                Text(
                    text = targetText,
                    style = MaterialTheme.typography.headlineSmall.copy(fontSize = fontSize),
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 2,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            AnimatedContent(
                targetState = result,
                transitionSpec = {
                    scaleIn().togetherWith(scaleOut())
                },
                label = "result"
            ) { targetResult ->
                val fontSize = when {
                    targetResult.length > 15 -> 28.sp
                    targetResult.length > 12 -> 36.sp
                    targetResult.length > 10 -> 44.sp
                    targetResult.length > 8 -> 52.sp
                    else -> 64.sp
                }
                
                Text(
                    text = targetResult,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = fontSize
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun HistorySection(
    history: List<HistoryItem>,
    onAction: (CalculatorAction) -> Unit,
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("History", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
            Row {
                TextButton(onClick = { onAction(CalculatorAction.ClearHistory) }) {
                    Text("Clear")
                }
                if (onClose != null) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Close")
                    }
                }
            }
        }
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            val grouped = history.groupBy { 
                SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date(it.timestamp)) 
            }
            
            grouped.forEach { (date, items) ->
                item {
                    Text(
                        text = date,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                items(items) { item ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { 
                                onAction(CalculatorAction.SelectHistory(item))
                                onClose?.invoke()
                            }
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .padding(16.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = item.expression, 
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = item.result,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ButtonsGrid(
    viewModel: CalculatorViewModel,
    isAdvanced: Boolean
) {
    val haptic = LocalHapticFeedback.current
    
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // We use weights for all rows to ensure they fill the available height without overlapping or being oversized
        if (isAdvanced) {
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CalcButton("sin", Modifier.weight(1f), MaterialTheme.colorScheme.secondaryContainer) { viewModel.onAction(CalculatorAction.Function("sin")) }
                CalcButton("cos", Modifier.weight(1f), MaterialTheme.colorScheme.secondaryContainer) { viewModel.onAction(CalculatorAction.Function("cos")) }
                CalcButton("tan", Modifier.weight(1f), MaterialTheme.colorScheme.secondaryContainer) { viewModel.onAction(CalculatorAction.Function("tan")) }
                CalcButton("π", Modifier.weight(1f), MaterialTheme.colorScheme.secondaryContainer) { viewModel.onAction(CalculatorAction.Constant("π")) }
            }
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CalcButton("log", Modifier.weight(1f), MaterialTheme.colorScheme.secondaryContainer) { viewModel.onAction(CalculatorAction.Function("log")) }
                CalcButton("ln", Modifier.weight(1f), MaterialTheme.colorScheme.secondaryContainer) { viewModel.onAction(CalculatorAction.Function("ln")) }
                CalcButton("^", Modifier.weight(1f), MaterialTheme.colorScheme.secondaryContainer) { viewModel.onAction(CalculatorAction.Operator("^")) }
                CalcButton("√", Modifier.weight(1f), MaterialTheme.colorScheme.secondaryContainer) { viewModel.onAction(CalculatorAction.Function("sqrt")) }
            }
        }

        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CalcButton("AC", Modifier.weight(1f), MaterialTheme.colorScheme.errorContainer) { viewModel.onAction(CalculatorAction.Clear) }
            CalcButton("(", Modifier.weight(1f), MaterialTheme.colorScheme.tertiaryContainer) { viewModel.onAction(CalculatorAction.Operator("(")) }
            CalcButton(")", Modifier.weight(1f), MaterialTheme.colorScheme.tertiaryContainer) { viewModel.onAction(CalculatorAction.Operator(")")) }
            CalcButton("÷", Modifier.weight(1f), MaterialTheme.colorScheme.primaryContainer) { viewModel.onAction(CalculatorAction.Operator("/")) }
        }
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CalcButton("7", Modifier.weight(1f)) { viewModel.onAction(CalculatorAction.Number(7)) }
            CalcButton("8", Modifier.weight(1f)) { viewModel.onAction(CalculatorAction.Number(8)) }
            CalcButton("9", Modifier.weight(1f)) { viewModel.onAction(CalculatorAction.Number(9)) }
            CalcButton("×", Modifier.weight(1f), MaterialTheme.colorScheme.primaryContainer) { viewModel.onAction(CalculatorAction.Operator("*")) }
        }
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CalcButton("4", Modifier.weight(1f)) { viewModel.onAction(CalculatorAction.Number(4)) }
            CalcButton("5", Modifier.weight(1f)) { viewModel.onAction(CalculatorAction.Number(5)) }
            CalcButton("6", Modifier.weight(1f)) { viewModel.onAction(CalculatorAction.Number(6)) }
            CalcButton("-", Modifier.weight(1f), MaterialTheme.colorScheme.primaryContainer) { viewModel.onAction(CalculatorAction.Operator("-")) }
        }
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CalcButton("1", Modifier.weight(1f)) { viewModel.onAction(CalculatorAction.Number(1)) }
            CalcButton("2", Modifier.weight(1f)) { viewModel.onAction(CalculatorAction.Number(2)) }
            CalcButton("3", Modifier.weight(1f)) { viewModel.onAction(CalculatorAction.Number(3)) }
            CalcButton("+", Modifier.weight(1f), MaterialTheme.colorScheme.primaryContainer) { viewModel.onAction(CalculatorAction.Operator("+")) }
        }
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CalcButton("0", Modifier.weight(1f)) { viewModel.onAction(CalculatorAction.Number(0)) }
            CalcButton(".", Modifier.weight(1f)) { viewModel.onAction(CalculatorAction.Decimal) }
            CalcButton("⌫", Modifier.weight(1f), MaterialTheme.colorScheme.tertiaryContainer) { viewModel.onAction(CalculatorAction.Backspace) }
            CalcButton("=", Modifier.weight(1f), MaterialTheme.colorScheme.primary) { 
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.onAction(CalculatorAction.Calculate) 
            }
        }
    }
}

@Composable
fun CalcButton(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    val contentColor = when (containerColor) {
        MaterialTheme.colorScheme.primary -> MaterialTheme.colorScheme.onPrimary
        MaterialTheme.colorScheme.primaryContainer -> MaterialTheme.colorScheme.onPrimaryContainer
        MaterialTheme.colorScheme.secondaryContainer -> MaterialTheme.colorScheme.onSecondaryContainer
        MaterialTheme.colorScheme.tertiaryContainer -> MaterialTheme.colorScheme.onTertiaryContainer
        MaterialTheme.colorScheme.errorContainer -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = modifier
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .fillMaxHeight() // Fill available height in the weighted row
            .clip(RoundedCornerShape(28.dp)) // Expressive large corners
            .background(containerColor)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.KeyboardTap)
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = if (text.length > 2) 18.sp else 26.sp
            ),
            color = contentColor
        )
    }
}


@Preview(showBackground = true)
@Composable
fun CalculatorPreview() {
    CalculatorTheme {
        CalculatorApp()
    }
}
