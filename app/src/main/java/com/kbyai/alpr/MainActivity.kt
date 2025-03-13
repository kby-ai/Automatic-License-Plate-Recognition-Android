package com.kbyai.alpr

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import android.util.Size
import android.util.Log
import java.nio.ByteBuffer
import org.buyun.alpr.sdk.SDK_IMAGE_TYPE
import org.buyun.alpr.sdk.AlprSdk
import org.buyun.alpr.sdk.AlprCallback
import org.buyun.alpr.sdk.AlprResult
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.lang.Exception
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface

class MainActivity : AppCompatActivity() {

    companion object {
        private val SELECT_PHOTO_REQUEST_CODE = 1
    }

    private lateinit var textWarning: TextView
    private lateinit var imgView: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textWarning = findViewById<TextView>(R.id.textWarning)
        imgView = findViewById<View>(R.id.imageResult) as ImageView

        var ret = AlprSdk.setActivation(
            "hbvQsY6g6rVqVrgZNjC9hqBV6sBCDtiKoaYAnQxm/VioUr+Icbz2wDrJD+hWLsrIkbVnLq0E7zBq\n" +
                    "ZV0PGk6w9ZX9ivPmU/QHbXT/EOkZR5DHYoDL7Kmh11dm1dNvufndjgB6S0ZYyYSiLlwnbIGaPTA6\n" +
                    "U4uMs+OtkFTREs+8fvo5qbfCpgaQCdeyFCCVQXTm2rb3GIog16eSIj1wOHuIdxkhEp++GRszKL8o\n" +
                    "2Bpu2cAJ1067GRhqo0Sa6uy4RdGp5DsHvIYZgnqdF4XVbSsxl6pN6rQuhQiJzkywMen5ECuFYkie\n" +
                    "jY6RPksi+fbrbVG9UXZpvFfzDsiuVqnQyAaA6g=="
        )

        if (ret == 0) {
            val config = getJsonConfig()
            // Retrieve previously stored key from internal storage
            val alprResult = AlprUtils.assertIsOk(AlprSdk.init(
                assets,
                config.toString(),
                null
            ))
            Log.i("alprEngine", "ALPR engine initialized: " + AlprUtils.resultToString(alprResult))
        }

        if (ret != 0) {
            textWarning.setVisibility(View.VISIBLE)
            if (ret == -1) {
                textWarning.setText("Invalid license!")
            } else if (ret == -2) {
                textWarning.setText("Invalid error!")
            } else if (ret == -3) {
                textWarning.setText("License expired!")
            } else if (ret == -4) {
                textWarning.setText("No activated!")
            } else if (ret == -5) {
                textWarning.setText("Init error!")
            }
        }

        findViewById<Button>(R.id.buttonEnroll).setOnClickListener {
            val intent = Intent()
            intent.setType("image/*")
            intent.setAction(Intent.ACTION_PICK)
            startActivityForResult(Intent.createChooser(intent, getString(R.string.select_picture)), SELECT_PHOTO_REQUEST_CODE)

        }

        findViewById<Button>(R.id.buttonIdentify).setOnClickListener {
            startActivity(Intent(this, CameraActivityKt::class.java))
        }

        findViewById<Button>(R.id.buttonSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        findViewById<Button>(R.id.buttonAbout).setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SELECT_PHOTO_REQUEST_CODE && resultCode == RESULT_OK) {
            try {
                val platesMap: ArrayList<HashMap<String, Any>> = ArrayList<HashMap<String, Any>>()
                val options = BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                var bitmap: Bitmap? = null
                bitmap = Utils.getCorrectlyOrientedImage(this, data?.data!!)

                val widthInBytes = bitmap.rowBytes
                val width = bitmap.width
                val height = bitmap.height
                val nativeBuffer = ByteBuffer.allocateDirect(widthInBytes * height)
                bitmap.copyPixelsToBuffer(nativeBuffer)
                nativeBuffer.rewind()

                val alprResult: AlprResult = AlprSdk.process(
                    SDK_IMAGE_TYPE.ULTALPR_SDK_IMAGE_TYPE_RGBA32,
                    nativeBuffer, width.toLong(), height.toLong()
                )
                val plates = AlprUtils.extractPlates(alprResult);

                if(plates.isNullOrEmpty()) {
                    Toast.makeText(this, getString(R.string.no_vehicle_detected), Toast.LENGTH_SHORT).show()
                } else {
                    // Create a mutable bitmap to draw over it
                    val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                    val canvas = Canvas(mutableBitmap)
                    val paint = Paint().apply {
                        color = Color.GREEN
                        style = Paint.Style.STROKE
                        strokeWidth = 2f
                    }
                    val textPaint = Paint().apply {
                        color = Color.YELLOW
                        textSize = 20f
                        style = Paint.Style.FILL
                        typeface = Typeface.DEFAULT_BOLD
                    }

                    for(plate in plates) {
                        Log.i("alprEngine", "number: " + plate.getNumber())
                        Log.i("alprEngine", "wrapper: " + plate.getWarpedBox()[0])
                        val e: HashMap<String, Any> = HashMap<String, Any>()

                        var x1 = 65536.0f
                        var y1 = 65536.0f
                        var x2 = 0.0f
                        var y2 = 0.0f
                        val wrapper = plate.getWarpedBox()
                        if(wrapper[0] < x1) {
                            x1 = wrapper[0]
                        }
                        if(wrapper[1 * 2] < x1) {
                            x1 = wrapper[1 * 2]
                        }
                        if(wrapper[2 * 2] < x1) {
                            x1 = wrapper[2 * 2]
                        }
                        if(wrapper[3 * 2] < x1) {
                            x1 = wrapper[3 * 2]
                        }

                        if(wrapper[0 * 2 + 1] < y1) {
                            y1 = wrapper[0 * 2 + 1]
                        }
                        if(wrapper[1 * 2 + 1] < y1) {
                            y1 = wrapper[1 * 2 + 1]
                        }
                        if(wrapper[2 * 2 + 1] < y1) {
                            y1 = wrapper[2 * 2 + 1]
                        }
                        if(wrapper[3 * 2 + 1] < y1) {
                            y1 = wrapper[3 * 2 + 1]
                        }

                        if(wrapper[0 * 2] > x2) {
                            x2 = wrapper[0 * 2]
                        }
                        if(wrapper[1 * 2] > x2) {
                            x2 = wrapper[1 * 2]
                        }
                        if(wrapper[2 * 2] > x2) {
                            x2 = wrapper[2 * 2]
                        }
                        if(wrapper[3 * 2] > x2) {
                            x2 = wrapper[3 * 2]
                        }

                        if(wrapper[0 * 2 + 1] > y2) {
                            y2 = wrapper[0 * 2 + 1]
                        }
                        if(wrapper[1 * 2 + 1] > y2) {
                            y2 = wrapper[1 * 2 + 1]
                        }
                        if(wrapper[2 * 2 + 1] > y2) {
                            y2 = wrapper[2 * 2 + 1]
                        }
                        if(wrapper[3 * 2 + 1] > y2) {
                            y2 = wrapper[3 * 2 + 1]
                        }

                        e.put("x1", x1);
                        e.put("y1", y1);
                        e.put("x2", x2);
                        e.put("y2", y2);
                        e.put("frameWidth", bitmap!!.width);
                        e.put("frameHeight", bitmap!!.height);
                        e.put("number", plate.getNumber());
                        platesMap.add(e)

                        canvas.drawRect(x1, y1, x2, y2, paint)
                        canvas.drawText(plate.getNumber(), x1, y1 - 10, textPaint)
                    }

                    imgView.setImageBitmap(mutableBitmap)

                    Toast.makeText(this, getString(R.string.vehicle_detected), Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                //handle exception
                e.printStackTrace()
            }
        }
    }

    fun getJsonConfig(): JSONObject {
        val PREFERRED_SIZE = Size(1280, 720)
        val CONFIG_DEBUG_LEVEL = "info"
        val CONFIG_DEBUG_WRITE_INPUT_IMAGE = false // must be false unless you're debugging the code
        val CONFIG_NUM_THREADS = -1
        val CONFIG_GPGPU_ENABLED = true
        val CONFIG_MAX_LATENCY = -1
        val CONFIG_CHARSET = "latin"
        val CONFIG_IENV_ENABLED = false
        val CONFIG_OPENVINO_ENABLED = true
        val CONFIG_OPENVINO_DEVICE = "CPU"
        val CONFIG_DETECT_MINSCORE = 0.1 // 10%
        val CONFIG_CAR_NOPLATE_DETECT_ENABLED = false
        val CONFIG_CAR_NOPLATE_DETECT_MINSCORE = 0.8 // 80%
        val CONFIG_DETECT_ROI = listOf(0f, 0f, 0f, 0f)
        val CONFIG_PYRAMIDAL_SEARCH_ENABLED = true
        val CONFIG_PYRAMIDAL_SEARCH_SENSITIVITY = 0.28 // 28%
        val CONFIG_PYRAMIDAL_SEARCH_MINSCORE = 0.5 // 50%
        val CONFIG_PYRAMIDAL_SEARCH_MIN_IMAGE_SIZE_INPIXELS = 800 // pixels
        val CONFIG_KLASS_LPCI_ENABLED = true
        val CONFIG_KLASS_VCR_ENABLED = true
        val CONFIG_KLASS_VMMR_ENABLED = true
        val CONFIG_KLASS_VBSR_ENABLED = false
        val CONFIG_KLASS_VCR_GAMMA = 1.5
        val CONFIG_RECOGN_MINSCORE = 0.4 // 40%
        val CONFIG_RECOGN_SCORE_TYPE = "min"
        val CONFIG_RECOGN_RECTIFY_ENABLED = false

        val config = JSONObject()
        try {
            config.put("debug_level", CONFIG_DEBUG_LEVEL)
            config.put("debug_write_input_image_enabled", CONFIG_DEBUG_WRITE_INPUT_IMAGE)

            config.put("num_threads", CONFIG_NUM_THREADS)
            config.put("gpgpu_enabled", CONFIG_GPGPU_ENABLED)
            config.put("charset", CONFIG_CHARSET)
            config.put("max_latency", CONFIG_MAX_LATENCY)
            config.put("ienv_enabled", CONFIG_IENV_ENABLED)
            config.put("openvino_enabled", CONFIG_OPENVINO_ENABLED)
            config.put("openvino_device", CONFIG_OPENVINO_DEVICE)

            config.put("detect_minscore", CONFIG_DETECT_MINSCORE)
            config.put("detect_roi", JSONArray(CONFIG_DETECT_ROI))

            config.put("car_noplate_detect_enabled", CONFIG_CAR_NOPLATE_DETECT_ENABLED)
            config.put("car_noplate_detect_min_score", CONFIG_CAR_NOPLATE_DETECT_MINSCORE)

            config.put("pyramidal_search_enabled", CONFIG_PYRAMIDAL_SEARCH_ENABLED)
            config.put("pyramidal_search_sensitivity", CONFIG_PYRAMIDAL_SEARCH_SENSITIVITY)
            config.put("pyramidal_search_minscore", CONFIG_PYRAMIDAL_SEARCH_MINSCORE)
            config.put("pyramidal_search_min_image_size_inpixels", CONFIG_PYRAMIDAL_SEARCH_MIN_IMAGE_SIZE_INPIXELS)

            config.put("klass_lpci_enabled", CONFIG_KLASS_LPCI_ENABLED)
            config.put("klass_vcr_enabled", CONFIG_KLASS_VCR_ENABLED)
            config.put("klass_vmmr_enabled", CONFIG_KLASS_VMMR_ENABLED)
            config.put("klass_vbsr_enabled", CONFIG_KLASS_VBSR_ENABLED)
            config.put("klass_vcr_gamma", CONFIG_KLASS_VCR_GAMMA)

            config.put("recogn_minscore", CONFIG_RECOGN_MINSCORE)
            config.put("recogn_score_type", CONFIG_RECOGN_SCORE_TYPE)
            config.put("recogn_rectify_enabled", CONFIG_RECOGN_RECTIFY_ENABLED)

        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return config
    }
}