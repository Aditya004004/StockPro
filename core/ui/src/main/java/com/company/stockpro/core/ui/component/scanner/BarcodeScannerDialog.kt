package com.company.stockpro.core.ui.component.scanner

import android.Manifest
import android.content.pm.PackageManager
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn as AndroidXOptIn
import kotlin.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.company.stockpro.core.designsystem.component.ButtonVariant
import com.company.stockpro.core.designsystem.component.StockProButton
import com.company.stockpro.core.designsystem.component.StockProTextField
import com.company.stockpro.core.designsystem.theme.Radius
import com.company.stockpro.core.designsystem.theme.Spacing
import com.company.stockpro.core.designsystem.theme.StockProColors
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

/**
 * Barcode Scanner Dialog adhering strictly to docs/phases.md § Phase 4:
 * - CameraX + ML Kit bundled model
 * - ImageAnalysis with STRATEGY_KEEP_ONLY_LATEST
 * - imageProxy.close() in a finally block
 * - Barcode formats limited to QR_CODE and CODE_128
 * - ~1.5s duplicate debounce
 * - Analyzer executor shut down in onDispose alongside unbindAll()
 * - Permission rationale, torch toggle, manual-entry fallback
 */
@AndroidXOptIn(ExperimentalGetImage::class)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BarcodeScannerDialog(
    onDismissRequest: () -> Unit,
    onBarcodeScanned: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA,
            ) == PackageManager.PERMISSION_GRANTED,
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var isTorchEnabled by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var showManualEntry by remember { mutableStateOf(false) }
    var manualCodeInput by remember { mutableStateOf("") }
    var lastScannedTimestamp by remember { mutableLongStateOf(0L) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    Dialog(
        onDismissRequest = {
            focusManager.clearFocus()
            keyboardController?.hide()
            onDismissRequest()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect {
            dialogWindow?.let { win ->
                win.setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                )
                @Suppress("DEPRECATION")
                win.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        onDismissRequest()
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    shape = Radius.lg,
                    color = StockProColors.Canvas,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .clip(Radius.lg)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { /* Consume clicks inside card */ },
                        ),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.x4)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                // Top Header: Title + Torch + Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Scan Item Barcode",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = StockProColors.TextPrimary,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (hasCameraPermission && !showManualEntry) {
                            IconButton(
                                onClick = {
                                    val nextTorch = !isTorchEnabled
                                    camera?.cameraControl?.enableTorch(nextTorch)
                                    isTorchEnabled = nextTorch
                                },
                            ) {
                                Icon(
                                    imageVector = if (isTorchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                    contentDescription = "Toggle flashlight",
                                    tint = if (isTorchEnabled) StockProColors.Warning else StockProColors.TextSecondary,
                                )
                            }
                        }
                        IconButton(onClick = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            onDismissRequest()
                        }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close scanner",
                                tint = StockProColors.TextSecondary,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.x2))

                if (showManualEntry) {
                    // Manual entry fallback
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Enter Item Code",
                            style = MaterialTheme.typography.bodyMedium,
                            color = StockProColors.TextSecondary,
                        )
                        Spacer(modifier = Modifier.height(Spacing.x2))
                        StockProTextField(
                            value = manualCodeInput,
                            onValueChange = { manualCodeInput = it },
                            label = "Item Code (e.g. BRG-001)",
                            placeholder = "BRG-001",
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    if (manualCodeInput.isNotBlank()) {
                                        focusManager.clearFocus()
                                        keyboardController?.hide()
                                        onBarcodeScanned(manualCodeInput.trim())
                                    }
                                }
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(Spacing.x3))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Spacing.x2),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            StockProButton(
                                text = "Use Camera",
                                onClick = {
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                    showManualEntry = false
                                },
                                variant = ButtonVariant.SECONDARY,
                                modifier = Modifier.weight(1f),
                            )
                            StockProButton(
                                text = "Lookup Item",
                                onClick = {
                                    if (manualCodeInput.isNotBlank()) {
                                        focusManager.clearFocus()
                                        keyboardController?.hide()
                                        onBarcodeScanned(manualCodeInput.trim())
                                    }
                                },
                                enabled = manualCodeInput.isNotBlank(),
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                } else if (!hasCameraPermission) {
                    // Permission rationale
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.x4),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "Camera Permission Required",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = StockProColors.TextPrimary,
                        )
                        Spacer(modifier = Modifier.height(Spacing.x2))
                        Text(
                            text = "StockPro requires camera access to scan physical item QR codes and Code-128 barcodes.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = StockProColors.TextSecondary,
                        )
                        Spacer(modifier = Modifier.height(Spacing.x3))
                        StockProButton(
                            text = "Grant Permission",
                            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(Spacing.x2))
                        StockProButton(
                            text = "Enter Code Manually",
                            onClick = { showManualEntry = true },
                            variant = ButtonVariant.GHOST,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                } else {
                    // CameraX Viewfinder
                    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
                    var cameraProviderRef by remember { mutableStateOf<ProcessCameraProvider?>(null) }
                    var barcodeScannerRef by remember { mutableStateOf<com.google.mlkit.vision.barcode.BarcodeScanner?>(null) }

                    DisposableEffect(Unit) {
                        onDispose {
                            try {
                                cameraProviderRef?.unbindAll()
                            } catch (_: Exception) {}
                            try {
                                barcodeScannerRef?.close()
                            } catch (_: Exception) {}
                            cameraExecutor.shutdown()
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .clip(Radius.md)
                            .background(Color.Black),
                        contentAlignment = Alignment.Center,
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                val previewView = PreviewView(ctx)
                                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                                cameraProviderFuture.addListener({
                                    val cameraProvider = cameraProviderFuture.get()
                                    cameraProviderRef = cameraProvider
                                    val preview = Preview.Builder().build().also {
                                        it.surfaceProvider = previewView.surfaceProvider
                                    }

                                    val options = BarcodeScannerOptions.Builder()
                                        .setBarcodeFormats(Barcode.FORMAT_QR_CODE, Barcode.FORMAT_CODE_128)
                                        .build()
                                    val barcodeScanner = BarcodeScanning.getClient(options)
                                    barcodeScannerRef = barcodeScanner

                                    val imageAnalysis = ImageAnalysis.Builder()
                                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                        .build()

                                    imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                        val mediaImage = imageProxy.image
                                        if (mediaImage != null) {
                                            val image = InputImage.fromMediaImage(
                                                mediaImage,
                                                imageProxy.imageInfo.rotationDegrees,
                                            )
                                            barcodeScanner.process(image)
                                                .addOnSuccessListener { barcodes ->
                                                    for (barcode in barcodes) {
                                                        val rawValue = barcode.rawValue ?: continue
                                                        val now = System.currentTimeMillis()
                                                        // ~1.5s duplicate debounce
                                                        if (now - lastScannedTimestamp > 1500L) {
                                                            lastScannedTimestamp = now
                                                            onBarcodeScanned(rawValue)
                                                            break
                                                        }
                                                    }
                                                }
                                                .addOnCompleteListener {
                                                    // imageProxy.close() guaranteed
                                                    try {
                                                        imageProxy.close()
                                                    } catch (_: Exception) {}
                                                }
                                        } else {
                                            try {
                                                imageProxy.close()
                                            } catch (_: Exception) {}
                                        }
                                    }

                                    try {
                                        cameraProvider.unbindAll()
                                        camera = cameraProvider.bindToLifecycle(
                                            lifecycleOwner,
                                            CameraSelector.DEFAULT_BACK_CAMERA,
                                            preview,
                                            imageAnalysis,
                                        )
                                    } catch (e: Exception) {
                                        // Camera bind error
                                    }
                                }, ContextCompat.getMainExecutor(ctx))

                                previewView
                            },
                            modifier = Modifier.fillMaxSize(),
                        )

                        // Reticle overlay
                        Box(
                            modifier = Modifier
                                .size(180.dp)
                                .border(
                                    width = 2.dp,
                                    color = StockProColors.Primary,
                                    shape = RoundedCornerShape(12.dp),
                                ),
                        )
                    }

                    Spacer(modifier = Modifier.height(Spacing.x3))

                    // Fallback to manual entry
                    StockProButton(
                        text = "Enter Code Manually",
                        onClick = { showManualEntry = true },
                        variant = ButtonVariant.GHOST,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
        }
    }
