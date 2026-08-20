package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ImportStatus
import com.example.data.NseStockRecord
import com.example.ui.theme.AccentGold
import com.example.ui.theme.BearishRed
import com.example.ui.theme.BullishGreen
import com.example.ui.theme.NavyCardBorder
import com.example.ui.theme.NavySurface
import com.example.ui.theme.NavySurfaceVariant
import com.example.ui.theme.PrimaryCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun CsvImportScreen(
    importStatus: ImportStatus,
    allStockRecords: List<NseStockRecord>,
    recordsForSelectedDate: List<NseStockRecord>,
    selectedDate: String,
    onImportCsvFromUri: (Uri) -> Unit,
    onImportCsvText: (String) -> Unit,
    onResetToSampleData: () -> Unit,
    onResetStatus: () -> Unit
) {
    val context = LocalContext.current
    var pastedCsvText by remember { mutableStateOf("") }
    var tableSearchQuery by remember { mutableStateOf("") }
    var showPasteBox by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onImportCsvFromUri(it) }
    }

    val displayedRecords = remember(recordsForSelectedDate, tableSearchQuery) {
        if (tableSearchQuery.isBlank()) recordsForSelectedDate else recordsForSelectedDate.filter {
            it.symbol.contains(tableSearchQuery, ignoreCase = true) ||
            it.series.contains(tableSearchQuery, ignoreCase = true)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        // Import CSV Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(NavyCardBorder)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Import NSE Market CSV File",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Upload official NSE Bhavcopy, Equity, or Index CSV files from your device storage or paste CSV content directly.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { filePickerLauncher.launch("text/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan, contentColor = Color.Black),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pick_csv_file_btn")
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.height(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pick CSV File", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { showPasteBox = !showPasteBox },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.height(16.dp), tint = PrimaryCyan)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Paste CSV", color = PrimaryCyan, fontSize = 12.sp)
                        }
                    }

                    if (showPasteBox) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = pastedCsvText,
                            onValueChange = { pastedCsvText = it },
                            placeholder = { Text("Paste CSV text here (SYMBOL, DATE, OPEN, HIGH, LOW, CLOSE, ...)", color = TextMuted, fontSize = 11.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = NavySurfaceVariant,
                                unfocusedContainerColor = NavySurfaceVariant,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            maxLines = 5,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                if (pastedCsvText.isNotBlank()) {
                                    onImportCsvText(pastedCsvText)
                                    pastedCsvText = ""
                                    showPasteBox = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = Color.Black),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Parse Text", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Restore Sample Data Option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(NavySurfaceVariant)
                            .clickable { onResetToSampleData() }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = AccentGold, modifier = Modifier.height(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reload Authentic NSE Sample Dataset", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Text("Reset", color = AccentGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Import Status Indicator
        when (importStatus) {
            is ImportStatus.Loading -> {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(NavySurface, RoundedCornerShape(12.dp))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(color = PrimaryCyan, modifier = Modifier.height(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Parsing CSV & calculating date analytics...", color = TextPrimary, fontSize = 12.sp)
                    }
                }
            }
            is ImportStatus.Success -> {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(BullishGreen.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .border(1.dp, BullishGreen, RoundedCornerShape(12.dp))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = BullishGreen)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(importStatus.message, color = BullishGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            is ImportStatus.Error -> {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(BearishRed.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .border(1.dp, BearishRed, RoundedCornerShape(12.dp))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = BearishRed)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(importStatus.errorMsg, color = BearishRed, fontSize = 12.sp)
                    }
                }
            }
            else -> {}
        }

        // Raw CSV Data Inspector Header & Search
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Stock Records Inspector ($selectedDate)", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("Total DB Records: ${allStockRecords.size}", color = TextMuted, fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = tableSearchQuery,
                    onValueChange = { tableSearchQuery = it },
                    placeholder = { Text("Filter table by Symbol or Series...", color = TextMuted) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = NavySurface,
                        unfocusedContainerColor = NavySurface,
                        focusedBorderColor = PrimaryCyan,
                        unfocusedBorderColor = NavyCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Data Table Column Headers
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NavySurfaceVariant, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Symbol", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f))
                Text("Close (₹)", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("Chg %", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("Volume", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("Deliv %", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.8f))
            }
        }

        // Table Rows
        if (displayedRecords.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No stock records match current date/filter", color = TextMuted, fontSize = 12.sp)
                }
            }
        } else {
            items(displayedRecords) { stock ->
                val isUp = stock.priceChangePct >= 0
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NavySurface, RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1.2f)) {
                        Text(stock.symbol, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(stock.series, color = TextMuted, fontSize = 9.sp)
                    }

                    Text("₹${String.format("%.1f", stock.close)}", color = TextPrimary, fontSize = 12.sp, modifier = Modifier.weight(1f))

                    Text(
                        text = "${if (isUp) "+" else ""}${String.format("%.2f", stock.priceChangePct)}%",
                        color = if (isUp) BullishGreen else BearishRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )

                    Text(formatQtyShort(stock.totalTradedQty), color = TextSecondary, fontSize = 11.sp, modifier = Modifier.weight(1f))

                    Text("${String.format("%.1f", stock.pctDlyQtToTrd)}%", color = AccentGold, fontSize = 11.sp, modifier = Modifier.weight(0.8f))
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

private fun formatQtyShort(qty: Long): String {
    return when {
        qty >= 1_00_00_000 -> String.format("%.1fCr", qty / 1_00_00_000.0)
        qty >= 1_00_000 -> String.format("%.1fL", qty / 1_00_000.0)
        else -> "${qty / 1000}K"
    }
}
