package com.example.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.AspectRatio
import com.google.firebase.ai.type.ImageConfig
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.ImageSize
import com.google.firebase.ai.type.QuotaExceededException
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

private const val TAG = "AiStudioViewModel"

enum class ConfectioneryImageModel(val modelId: String, val label: String, val description: String) {
    NANO_BANANA_2_1(
        modelId = "gemini-nano-banana-2.1",
        label = "Nano Banana 2.1 (Create & Edit)",
        description = "Fast confectionery image creation & photo editing from text prompts"
    ),
    GEMINI_3_PRO_IMAGE(
        modelId = "gemini-3-pro-image-preview",
        label = "Gemini 3 Pro Image (1K / 2K / 4K)",
        description = "Studio-quality high-resolution confectionery product & poster generation"
    )
}

enum class StudioImageResolution(val label: String, val sdkSize: ImageSize) {
    SIZE_1K("1K", ImageSize.SIZE_1K),
    SIZE_2K("2K", ImageSize.SIZE_2K),
    SIZE_4K("4K", ImageSize.SIZE_4K)
}

enum class StudioAspectRatio(val label: String, val sdkRatio: AspectRatio) {
    SQUARE_1_1("1:1 Square", AspectRatio.SQUARE_1x1),
    LANDSCAPE_16_9("16:9 Banner", AspectRatio.LANDSCAPE_16x9),
    PORTRAIT_3_4("3:4 Poster", AspectRatio.PORTRAIT_3x4)
}

sealed interface ImageStudioUiState {
    data object Idle : ImageStudioUiState
    data object Loading : ImageStudioUiState
    data class Success(
        val bitmap: Bitmap,
        val promptUsed: String,
        val modelUsed: String,
        val resolutionUsed: String
    ) : ImageStudioUiState
    data class Error(val message: String) : ImageStudioUiState
}

sealed interface AiAdvisorUiState {
    data object Idle : AiAdvisorUiState
    data object Loading : AiAdvisorUiState
    data class Streaming(val text: String) : AiAdvisorUiState
    data class Success(val text: String) : AiAdvisorUiState
    data class Error(val message: String) : AiAdvisorUiState
}

class AiStudioViewModel : ViewModel() {

    private val _selectedModel = MutableStateFlow(ConfectioneryImageModel.GEMINI_3_PRO_IMAGE)
    val selectedModel: StateFlow<ConfectioneryImageModel> = _selectedModel.asStateFlow()

    private val _selectedResolution = MutableStateFlow(StudioImageResolution.SIZE_1K)
    val selectedResolution: StateFlow<StudioImageResolution> = _selectedResolution.asStateFlow()

    private val _selectedAspectRatio = MutableStateFlow(StudioAspectRatio.SQUARE_1_1)
    val selectedAspectRatio: StateFlow<StudioAspectRatio> = _selectedAspectRatio.asStateFlow()

    private val _sourceBitmapForEdit = MutableStateFlow<Bitmap?>(null)
    val sourceBitmapForEdit: StateFlow<Bitmap?> = _sourceBitmapForEdit.asStateFlow()

    private val _imageUiState = MutableStateFlow<ImageStudioUiState>(ImageStudioUiState.Idle)
    val imageUiState: StateFlow<ImageStudioUiState> = _imageUiState.asStateFlow()

    private val _advisorUiState = MutableStateFlow<AiAdvisorUiState>(AiAdvisorUiState.Idle)
    val advisorUiState: StateFlow<AiAdvisorUiState> = _advisorUiState.asStateFlow()

    private val textAdvisorModel by lazy {
        Firebase.ai.generativeModel("gemini-3.8-flash")
    }

    private val nanoBananaDefaultModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-nano-banana-2.1",
            generationConfig = generationConfig {
                responseModalities = listOf(ResponseModality.IMAGE)
            }
        )
    }

    private fun buildConfiguredImageModel(
        modelChoice: ConfectioneryImageModel,
        resolution: StudioImageResolution,
        aspectRatio: StudioAspectRatio
    ): GenerativeModel {
        val config = generationConfig {
            responseModalities = listOf(ResponseModality.IMAGE)
            imageConfig = ImageConfig.builder()
                .setAspectRatio(aspectRatio.sdkRatio)
                .setImageSize(resolution.sdkSize)
                .build()
        }
        return Firebase.ai.generativeModel(
            modelName = modelChoice.modelId,
            generationConfig = config
        )
    }

    fun selectModel(model: ConfectioneryImageModel) {
        _selectedModel.value = model
    }

    fun selectResolution(resolution: StudioImageResolution) {
        _selectedResolution.value = resolution
    }

    fun selectAspectRatio(aspectRatio: StudioAspectRatio) {
        _selectedAspectRatio.value = aspectRatio
    }

    fun setSourceBitmapForEdit(bitmap: Bitmap?) {
        _sourceBitmapForEdit.value = bitmap
    }

    /**
     * Generates a new confectionery image using either `gemini-3-pro-image-preview` (with 1K/2K/4K size)
     * or `gemini-nano-banana-2.1`.
     */
    fun generateConfectioneryImage(prompt: String) {
        if (prompt.isBlank()) {
            _imageUiState.value = ImageStudioUiState.Error("Please enter a prompt describing the confectionery image.")
            return
        }
        val currentModel = _selectedModel.value
        val currentRes = _selectedResolution.value
        val currentAspect = _selectedAspectRatio.value

        viewModelScope.launch {
            _imageUiState.value = ImageStudioUiState.Loading
            try {
                val model = buildConfiguredImageModel(currentModel, currentRes, currentAspect)
                val bitmap = withTimeout(60_000L) {
                    val response = model.generateContent(prompt.trim())
                    response.candidates.firstOrNull()
                        ?.content?.parts?.filterIsInstance<ImagePart>()
                        ?.firstOrNull()?.image
                }
                if (bitmap != null) {
                    _sourceBitmapForEdit.value = bitmap
                    _imageUiState.value = ImageStudioUiState.Success(
                        bitmap = bitmap,
                        promptUsed = prompt.trim(),
                        modelUsed = currentModel.modelId,
                        resolutionUsed = currentRes.label
                    )
                } else {
                    _imageUiState.value = ImageStudioUiState.Error("No image was returned by ${currentModel.modelId}.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Image generation timed out", e)
                _imageUiState.value = ImageStudioUiState.Error("Image generation timed out after 60 seconds.")
            } catch (e: QuotaExceededException) {
                Log.e(TAG, "Quota exceeded for image generation", e)
                _imageUiState.value = ImageStudioUiState.Error("Rate limit or quota exceeded. Please check your Firebase Blaze plan or try again later.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Image generation failed", e)
                _imageUiState.value = ImageStudioUiState.Error(e.localizedMessage ?: "Image generation failed.")
            }
        }
    }

    /**
     * Edits an existing image using `gemini-nano-banana-2.1` and text instructions.
     */
    fun editConfectioneryImage(editInstruction: String) {
        val inputBitmap = _sourceBitmapForEdit.value
        if (inputBitmap == null) {
            _imageUiState.value = ImageStudioUiState.Error("Select or generate an image first before applying an edit prompt.")
            return
        }
        if (editInstruction.isBlank()) {
            _imageUiState.value = ImageStudioUiState.Error("Enter edit instructions (e.g., 'Add gold ribbon and pistachio garnish').")
            return
        }

        viewModelScope.launch {
            _imageUiState.value = ImageStudioUiState.Loading
            try {
                val bitmap = withTimeout(60_000L) {
                    val response = nanoBananaDefaultModel.generateContent(
                        content {
                            image(inputBitmap)
                            text(editInstruction.trim())
                        }
                    )
                    response.candidates.firstOrNull()
                        ?.content?.parts?.filterIsInstance<ImagePart>()
                        ?.firstOrNull()?.image
                }
                if (bitmap != null) {
                    _sourceBitmapForEdit.value = bitmap
                    _imageUiState.value = ImageStudioUiState.Success(
                        bitmap = bitmap,
                        promptUsed = "Edited: ${editInstruction.trim()}",
                        modelUsed = ConfectioneryImageModel.NANO_BANANA_2_1.modelId,
                        resolutionUsed = _selectedResolution.value.label
                    )
                } else {
                    _imageUiState.value = ImageStudioUiState.Error("No edited image was returned by gemini-nano-banana-2.1.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Image edit timed out", e)
                _imageUiState.value = ImageStudioUiState.Error("Image editing timed out after 60 seconds.")
            } catch (e: QuotaExceededException) {
                Log.e(TAG, "Quota exceeded during image edit", e)
                _imageUiState.value = ImageStudioUiState.Error("Rate limit exceeded. Please try again later.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Image editing failed", e)
                _imageUiState.value = ImageStudioUiState.Error(e.localizedMessage ?: "Image editing failed.")
            }
        }
    }

    /**
     * Uses `gemini-3.8-flash` streaming to generate customized confectionery growth ideas,
     * festival hamper bundles, or polite SMS credit reminders.
     */
    fun generateShopAdviceStream(topicPrompt: String) {
        viewModelScope.launch {
            _advisorUiState.value = AiAdvisorUiState.Loading
            val accumulatedText = StringBuilder()
            try {
                withTimeout(60_000L) {
                    textAdvisorModel.generateContentStream(topicPrompt).collect { chunk ->
                        val chunkText = chunk.text.orEmpty()
                        accumulatedText.append(chunkText)
                        _advisorUiState.value = AiAdvisorUiState.Streaming(accumulatedText.toString())
                    }
                }
                if (accumulatedText.isNotBlank()) {
                    _advisorUiState.value = AiAdvisorUiState.Success(accumulatedText.toString())
                } else {
                    _advisorUiState.value = AiAdvisorUiState.Error("Model returned an empty response.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Streaming advice timed out", e)
                _advisorUiState.value = AiAdvisorUiState.Error("Generation timed out after 60 seconds.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Streaming advice failed", e)
                _advisorUiState.value = AiAdvisorUiState.Error(e.localizedMessage ?: "Failed to generate advice.")
            }
        }
    }
}
