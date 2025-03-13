package com.kbyai.alpr

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Size
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.fotoapparat.Fotoapparat
import io.fotoapparat.preview.Frame
import io.fotoapparat.preview.FrameProcessor
import io.fotoapparat.selector.front
import io.fotoapparat.selector.back
import io.fotoapparat.view.CameraView
import org.buyun.alpr.sdk.SDK_IMAGE_TYPE
import org.buyun.alpr.sdk.AlprSdk
import org.buyun.alpr.sdk.AlprCallback
import org.buyun.alpr.sdk.AlprResult
import java.nio.ByteBuffer

import android.media.ExifInterface
import android.util.Log

class CameraActivityKt : AppCompatActivity() {

    val TAG = "KBY-AI ALPR"
    val PREVIEW_WIDTH = 720
    val PREVIEW_HEIGHT = 1280

    private lateinit var cameraView: CameraView
    private lateinit var faceView: FaceView
    private lateinit var fotoapparat: Fotoapparat
    private lateinit var context: Context

    private var recognized = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_kt)

        context = this
        cameraView = findViewById(R.id.preview)
        faceView = findViewById(R.id.faceView)

        if (SettingsActivity.getCameraLens(context) == CameraSelector.LENS_FACING_BACK) {
            fotoapparat = Fotoapparat.with(this)
                .into(cameraView)
                .lensPosition(back())
                .frameProcessor(FaceFrameProcessor())
                .build()
        } else  {
            fotoapparat = Fotoapparat.with(this)
                .into(cameraView)
                .lensPosition(front())
                .frameProcessor(FaceFrameProcessor())
                .build()
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_DENIED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 1)
        } else {
            fotoapparat.start()
        }
    }

    override fun onResume() {
        super.onResume()
        recognized = false
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            fotoapparat.start()
        }
    }

    override fun onPause() {
        super.onPause()
        fotoapparat.stop()
        faceView.setFaceBoxes(null)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
            ) {
                fotoapparat.start()
            }
        }
    }

    inner class FaceFrameProcessor : FrameProcessor {

        override fun process(frame: Frame) {

            if(recognized == true) {
                return
            }

            val exifOrientation: Int = when (frame.rotation) {
                90 -> ExifInterface.ORIENTATION_ROTATE_270
                180 -> ExifInterface.ORIENTATION_ROTATE_180
                270 -> ExifInterface.ORIENTATION_ROTATE_90
                else -> ExifInterface.ORIENTATION_NORMAL
            }

            val bitmap = AlprSdk.yuv2Bitmap(frame.image, frame.size.width, frame.size.height, exifOrientation)

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

            val platesMap: ArrayList<HashMap<String, Any>> = ArrayList<HashMap<String, Any>>()

            runOnUiThread {

                if(!plates.isNullOrEmpty()) {
                    for(plate in plates) {
//                        Log.i("alprEngine", "number: " + plate.getNumber())
//                        Log.i("alprEngine", "wrapper: " + plate.getWarpedBox()[0])
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

                    }
                }

                faceView.setFrameSize(Size(bitmap.width, bitmap.height))
                faceView.setFaceBoxes(platesMap)

            }
        }
    }
}