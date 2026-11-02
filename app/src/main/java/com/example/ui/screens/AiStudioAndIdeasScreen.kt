package com.example.ui.screens

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.SuggestedFeatureIdea
import com.example.ui.theme.OnVibrantBerryContainer
import com.example.ui.theme.OnVibrantEmeraldContainer
import com.example.ui.theme.OnVibrantPurpleContainer
import com.example.ui.theme.OnVibrantSaffronContainer
import com.example.ui.theme.OnVibrantSkyContainer
import com.example.ui.theme.VibrantBerryContainer
import com.example.ui.theme.VibrantBerryPink
import com.example.ui.theme.VibrantEmeraldContainer
import com.example.ui.theme.VibrantEmeraldGreen
import com.example.ui.theme.VibrantPurpleContainer
import com.example.ui.theme.VibrantRoyalPurple
import com.example.ui.theme.VibrantSaffronContainer
import com.example.ui.theme.VibrantSaffronGold
import com.example.ui.theme.VibrantSkyBlue
import com.example.ui.theme.VibrantSkyContainer
import com.example.viewmodel.AiStudioViewModel
import com.example.viewmodel.ConfectioneryImageModel
import com.example.viewmodel.ImageStudioUiState
import com.example.viewmodel.StudioImageResolution

@Composable
fun AiStudioAndIdeasScreen(
    aiViewModel: AiStudioViewModel,
    suggestedFeatures: List<SuggestedFeatureIdea>
) {
    val context = LocalContext.current
    val selectedModel by aiViewModel.selectedModel.collectAsStateWithLifecycle()
    val selectedResolution by aiViewModel.selectedResolution.collectAsStateWithLifecycle()
    val sourceBitmapForEdit by aiViewModel.sourceBitmapForEdit.collectAsStateWithLifecycle()
    val imageUiState by aiViewModel.imageUiState.collectAsStateWithLifecycle()

    var promptText by rememberSaveable {
        mutableStateOf("A festive Sandeep Cafe gift box of pistachio rose baklava, saffron kaju katli, and dark chocolate truffles with warm golden lighting")
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val decoded = BitmapFactory.decodeStream(stream)
                    if (decoded != null) {
                        aiViewModel.setSourceBitmapForEdit(decoded)
                        aiViewModel.selectModel(ConfectioneryImageModel.NANO_BANANA_2_1)
                        promptText = "Add a festive Sandeep Cafe gold ribbon and warm bokeh lights around these sweets"
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    val ideaCardBackgrounds = listOf(
        VibrantBerryContainer,
        VibrantSaffronContainer,
        VibrantEmeraldContainer,
        VibrantPurpleContainer,
        VibrantSkyContainer
    )
    val ideaCardAccents = listOf(
        VibrantBerryPink,
        VibrantSaffronGold,
        VibrantEmeraldGreen,
        VibrantRoyalPurple,
        VibrantSkyBlue
    )
    val ideaCardTextColors = listOf(
        OnVibrantBerryContainer,
        OnVibrantSaffronContainer,
        OnVibrantEmeraldContainer,
        OnVibrantPurpleContainer,
        OnVibrantSkyContainer
    )

    val isGenerating = imageUiState is ImageStudioUiState.Loading
    val generatedSuccess = imageUiState as? ImageStudioUiState.Success
    val displayBitmap = generatedSuccess?.bitmap ?: sourceBitmapForEdit
    val errorText = (imageUiState as? ImageStudioUiState.Error)?.message

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("ai_studio_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. AI Confectionery Visual Studio Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = VibrantPurpleContainer.copy(alpha = 0.7f)
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = VibrantRoyalPurple
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Sandeep Cafe AI Visual Studio",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = OnVibrantPurpleContainer
                            )
                            Text(
                                text = "Create & edit cafe photos or generate 1K/2K/4K promotional posters",
                                style = MaterialTheme.typography.bodySmall,
                                color = OnVibrantPurpleContainer.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Mode Selector: gemini-nano-banana-2.1 vs gemini-3-pro-image-preview
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedModel == ConfectioneryImageModel.NANO_BANANA_2_1,
                            onClick = { aiViewModel.selectModel(ConfectioneryImageModel.NANO_BANANA_2_1) },
                            label = { Text("Create & Edit Photo") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = VibrantBerryPink,
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White
                            )
                        )
                        FilterChip(
                            selected = selectedModel == ConfectioneryImageModel.GEMINI_3_PRO_IMAGE,
                            onClick = { aiViewModel.selectModel(ConfectioneryImageModel.GEMINI_3_PRO_IMAGE) },
                            label = { Text("Pro Poster (1K/2K/4K)") },
                            leadingIcon = { Icon(Icons.Default.HighQuality, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = VibrantRoyalPurple,
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White
                            )
                        )
                    }

                    if (selectedModel == ConfectioneryImageModel.GEMINI_3_PRO_IMAGE) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Output Size:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = OnVibrantPurpleContainer
                            )
                            StudioImageResolution.entries.forEach { resOption ->
                                FilterChip(
                                    selected = selectedResolution == resOption,
                                    onClick = { aiViewModel.selectResolution(resOption) },
                                    label = { Text(resOption.label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = VibrantSaffronGold,
                                        selectedLabelColor = Color.White
                                    ),
                                    modifier = Modifier.testTag("resolution_chip_${resOption.label}")
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = promptText,
                        onValueChange = { promptText = it },
                        label = {
                            Text(
                                if (selectedModel == ConfectioneryImageModel.NANO_BANANA_2_1) {
                                    "Describe Sandeep Cafe sweet box to create or how to edit image"
                                } else {
                                    "Describe high-resolution ${selectedResolution.label} Sandeep Cafe poster"
                                }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_prompt_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (selectedModel == ConfectioneryImageModel.NANO_BANANA_2_1) {
                            OutlinedButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Pick Photo")
                            }
                            Button(
                                onClick = {
                                    if (sourceBitmapForEdit != null) {
                                        aiViewModel.editConfectioneryImage(promptText)
                                    } else {
                                        aiViewModel.generateConfectioneryImage(promptText)
                                    }
                                },
                                enabled = !isGenerating,
                                colors = ButtonDefaults.buttonColors(containerColor = VibrantBerryPink),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .testTag("nano_generate_edit_button")
                            ) {
                                Text(if (sourceBitmapForEdit != null) "Edit with AI" else "Create Image")
                            }
                        } else {
                            Button(
                                onClick = {
                                    aiViewModel.generateConfectioneryImage(promptText)
                                },
                                enabled = !isGenerating,
                                colors = ButtonDefaults.buttonColors(containerColor = VibrantRoyalPurple),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("pro_generate_hq_button")
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Generate ${selectedResolution.label} Pro Poster")
                            }
                        }
                    }

                    if (isGenerating) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp,
                                color = VibrantRoyalPurple
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Generating Sandeep Cafe visual with ${selectedModel.label}...",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    if (!errorText.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = errorText,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    displayBitmap?.let { bmp ->
                        Spacer(modifier = Modifier.height(14.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Generated Sandeep Cafe Visual",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        if (generatedSuccess != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Model: ${generatedSuccess.modelUsed} (${generatedSuccess.resolutionUsed})",
                                style = MaterialTheme.typography.labelSmall,
                                color = VibrantRoyalPurple,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 2. Suggested Smart Features for Sandeep Cafe
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = VibrantSaffronGold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Suggested Growth Features for Sandeep Cafe",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tailored recommendations & built-in modules for your cafe & confectionery shop",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        itemsIndexed(suggestedFeatures, key = { _, it -> it.id }) { index, idea ->
            val bgColor = ideaCardBackgrounds[index % ideaCardBackgrounds.size]
            val accentColor = ideaCardAccents[index % ideaCardAccents.size]
            val textColor = ideaCardTextColors[index % ideaCardTextColors.size]

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = bgColor.copy(alpha = 0.7f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = idea.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            modifier = Modifier.weight(1f)
                        )
                        Surface(
                            color = accentColor,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = idea.benefitTag,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Text(
                        text = idea.subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = idea.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor.copy(alpha = 0.88f)
                    )
                }
            }
        }
    }
}
