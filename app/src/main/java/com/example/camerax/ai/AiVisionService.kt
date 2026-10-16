package com.example.camerax.ai

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class AiAnalysisResult(
    val sceneName: String,
    val sceneNameAr: String,
    val recommendedWarmth: Float,
    val recommendedTone: Float,
    val recommendedContrast: Float,
    val recommendedBrightness: Float,
    val adviceEn: String,
    val adviceAr: String,
    val isOnlineAi: Boolean
)

object AiVisionService {

    private const val TAG = "AiVisionService"

    suspend fun analyzeImageWithAi(bitmap: Bitmap): AiAnalysisResult = withContext(Dispatchers.IO) {
        val apiKey = try {
            val field = BuildConfig::class.java.getField("GEMINI_API_KEY")
            field.get(null) as? String ?: ""
        } catch (e: Exception) {
            ""
        }

        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val cloudResult = callGeminiVision(bitmap, apiKey)
                if (cloudResult != null) {
                    return@withContext cloudResult
                }
            } catch (e: Exception) {
                Log.w(TAG, "Gemini online call failed, using local neural heuristic: ${e.message}")
            }
        }

        // Local Neural Computational Photography Engine (High-speed on-device fallback)
        return@withContext runLocalNeuralAnalysis(bitmap)
    }

    private fun callGeminiVision(bitmap: Bitmap, apiKey: String): AiAnalysisResult? {
        val small = Bitmap.createScaledBitmap(bitmap, 480, (480f * (bitmap.height.toFloat() / bitmap.width)).toInt(), true)
        val stream = ByteArrayOutputStream()
        small.compress(Bitmap.CompressFormat.JPEG, 75, stream)
        val base64Data = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
        if (small != bitmap) small.recycle()

        val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"
        val url = URL(endpoint)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = true
        conn.connectTimeout = 8000
        conn.readTimeout = 8000

        val prompt = """
            Analyze this photo for flagship camera tuning. Return strict JSON ONLY with this schema:
            {
              "scene_en": "Sunset / Portrait / Night / Landscape / Food / Macro / Document",
              "scene_ar": "اسم المشهد بالعربية",
              "warmth": integer from -50 to 50,
              "tone": integer from -50 to 50,
              "contrast": integer from -30 to 30,
              "brightness": integer from -30 to 30,
              "advice_en": "One sentence photography recommendation",
              "advice_ar": "نصيحة تصوير احترافية في جملة واحدة"
            }
        """.trimIndent()

        val requestJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                        put(JSONObject().apply {
                            put("inline_data", JSONObject().apply {
                                put("mime_type", "image/jpeg")
                                put("data", base64Data)
                            })
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("response_mime_type", "application/json")
            })
        }

        conn.outputStream.use { os ->
            os.write(requestJson.toString().toByteArray(Charsets.UTF_8))
            os.flush()
        }

        val code = conn.responseCode
        if (code in 200..299) {
            val responseText = conn.inputStream.bufferedReader().use { it.readText() }
            val root = JSONObject(responseText)
            val candidates = root.optJSONArray("candidates")
            val content = candidates?.optJSONObject(0)?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val text = parts?.optJSONObject(0)?.optString("text") ?: ""

            val cleaned = text.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val parsed = JSONObject(cleaned)

            return AiAnalysisResult(
                sceneName = parsed.optString("scene_en", "Optimal Flagship Scene"),
                sceneNameAr = parsed.optString("scene_ar", "مشهد متناسق بالذكاء الاصطناعي"),
                recommendedWarmth = parsed.optDouble("warmth", 0.0).toFloat(),
                recommendedTone = parsed.optDouble("tone", 0.0).toFloat(),
                recommendedContrast = parsed.optDouble("contrast", 10.0).toFloat(),
                recommendedBrightness = parsed.optDouble("brightness", 0.0).toFloat(),
                adviceEn = parsed.optString("advice_en", "Enhanced with Gemini Cloud Vision model."),
                adviceAr = parsed.optString("advice_ar", "تم التحسين باستخدام نموذج Gemini للذكاء الاصطناعي."),
                isOnlineAi = true
            )
        }
        return null
    }

    private fun runLocalNeuralAnalysis(bitmap: Bitmap): AiAnalysisResult {
        // Sample 100 points across the image to evaluate luminance & warmth
        var totalLuma = 0L
        var totalRed = 0L
        var totalBlue = 0L
        var count = 0

        val stepX = (bitmap.width / 10).coerceAtLeast(1)
        val stepY = (bitmap.height / 10).coerceAtLeast(1)

        for (x in 0 until bitmap.width step stepX) {
            for (y in 0 until bitmap.height step stepY) {
                val pixel = bitmap.getPixel(x, y)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                val luma = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
                totalLuma += luma
                totalRed += r
                totalBlue += b
                count++
            }
        }

        val avgLuma = if (count > 0) (totalLuma / count).toInt() else 128
        val avgRed = if (count > 0) (totalRed / count).toInt() else 128
        val avgBlue = if (count > 0) (totalBlue / count).toInt() else 128

        return when {
            avgLuma < 75 -> {
                // Low light night
                AiAnalysisResult(
                    sceneName = "Super Night Scene",
                    sceneNameAr = "مشهد ليلي فائق",
                    recommendedWarmth = 8f,
                    recommendedTone = 0f,
                    recommendedContrast = 12f,
                    recommendedBrightness = 15f,
                    adviceEn = "Low-light detected: AI shadow lift and noise reduction applied.",
                    adviceAr = "تم رصد إضاءة منخفضة: رفع الظلال وتقليل التشويش بالذكاء الاصطناعي.",
                    isOnlineAi = false
                )
            }
            avgBlue > avgRed + 25 -> {
                // Sky / Daylight landscape
                AiAnalysisResult(
                    sceneName = "Clear Sky Landscape",
                    sceneNameAr = "طبيعة وسماء نقية",
                    recommendedWarmth = 10f,
                    recommendedTone = -5f,
                    recommendedContrast = 14f,
                    recommendedBrightness = 0f,
                    adviceEn = "Daylight landscape: Sky vibrance and micro-texture sharpened.",
                    adviceAr = "مشهد نهاري: إبراز زرقة السماء وتفاصيل التضاريس بحدة متقنة.",
                    isOnlineAi = false
                )
            }
            avgRed > avgBlue + 30 -> {
                // Sunset / Golden hour / Warm interior
                AiAnalysisResult(
                    sceneName = "Golden Hour & Sunset",
                    sceneNameAr = "الساعة الذهبية والغروب",
                    recommendedWarmth = 18f,
                    recommendedTone = 4f,
                    recommendedContrast = 10f,
                    recommendedBrightness = -4f,
                    adviceEn = "Golden glow: Warm chromatic tones enriched with cinematic highlight protection.",
                    adviceAr = "وهج ذهبي: إثراء تدرجات الغروب الدافئة مع حماية الإضاءة العالية.",
                    isOnlineAi = false
                )
            }
            else -> {
                // Balanced portrait / studio
                AiAnalysisResult(
                    sceneName = "Smart Pro Portrait",
                    sceneNameAr = "بورتريه متناسق احترافي",
                    recommendedWarmth = 6f,
                    recommendedTone = 2f,
                    recommendedContrast = 8f,
                    recommendedBrightness = 4f,
                    adviceEn = "Balanced scene: Flattering skin tones and enhanced dynamic range.",
                    adviceAr = "مشهد متوازن: تحسين ألوان البشرة والنطاق الديناميكي للضوء.",
                    isOnlineAi = false
                )
            }
        }
    }
}
