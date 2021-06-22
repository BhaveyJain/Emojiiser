package com.example.emojiiser

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.emojiiser.databinding.ActivityMainBinding
import com.example.emojiiser.ml.Model2
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.ml.modeldownloader.CustomModelDownloadConditions
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

class MainActivity : AppCompatActivity() {

    private companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityMainBinding
    private lateinit var resultLauncher: ActivityResultLauncher<Intent>

    private val db = Firebase.firestore
    override fun onCreate(savedInstanceState: Bundle?) {
        auth = Firebase.auth
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)

        resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // There are no request codes
                val data: Intent? = result.data
                var img  = BitmapFactory.decodeResource(resources, R.drawable.sadface);
                img = convertBitMap(img)
                img =  Bitmap.createScaledBitmap(img, 48, 48, true);

                val input = ByteBuffer.allocateDirect(48*48*4).order(ByteOrder.nativeOrder())

                for (y in 0 until 48) {
                    for (x in 0 until 48) {
                        val px = img.getPixel(x, y)

                        // Get channel values from the pixel value.
                        val value = Color.red(px).toFloat() / 255.0f
                        input.putFloat(value)

                    }
                }

                // data?.extras?.get("data") as Bitmap
                val model = Model2.newInstance(this)
                val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 48, 48, 1), DataType.FLOAT32)
                inputFeature0.loadBuffer(input)
                val outputs = model.process(inputFeature0)
                val outputFeature0 = outputs.outputFeature0AsTensorBuffer
                val arr = outputFeature0.floatArray
                model.close()
                Log.w(TAG, Arrays.toString(arr))
                //[Angry, Disgust, Fear, Happy, Neutral, Sad, Surprised]
                val unicodeList = listOf(String(Character.toChars(0x1F621)), String(Character.toChars(0x1F922)),
                        String(Character.toChars(0x1F628)), String(Character.toChars(0x1F601)),
                        String(Character.toChars(0x1F610)), String(Character.toChars(0x1F614)),
                        String(Character.toChars(0x1F632)))

                updateStatus(unicodeList[detectedEmotion(arr)])
                Log.w(TAG, "imgTakenSuccess")
            }
        }

        val view = binding.root
        setContentView(view)

        val query = db.collection("users")
        val options = FirestoreRecyclerOptions.Builder<User>().setQuery(query, User::class.java).setLifecycleOwner(this).build()
        binding.rvUsers.adapter = UserAdapter(this, options)
        binding.rvUsers.layoutManager = LinearLayoutManager(this)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    private fun detectedEmotion(arr: FloatArray): Int {
        var ans: Int = 0;
        var max: Float = arr[0]
        for (i in arr.indices) {
            if (arr[i] > max) {
                max = arr[i]
                ans = i
            }
        }
        return ans
    }

    private fun updateStatus(emojisEntered: String) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "No signed in user", Toast.LENGTH_SHORT).show()
        } else {
            db.collection("users").document(currentUser.uid)
                    .update("emojis", emojisEntered)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.miLogout) {
            Log.i(TAG,"User logged out" )
            auth.signOut()
            val logoutIntent = Intent(this, LoginActivity::class.java)
            logoutIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(logoutIntent)
        } else if (item.itemId == R.id.editIcon) {
            showAlertDialog()
        } else if (item.itemId == R.id.cameraIcon) {

            val picIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            resultLauncher.launch(picIntent)
        }

        return super.onOptionsItemSelected(item)
    }

    private fun convertBitMap(bitmap: Bitmap) : Bitmap {
        val finalMap = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)
        val height = bitmap.height
        val width = bitmap.width

        for (i in 0 until width) {
            for (j in 0 until height) {

                val colorPixel = bitmap.getPixel(i, j);
                val A = Color.alpha(colorPixel);
                var R = Color.red(colorPixel)
                var G = Color.green(colorPixel)
                var B = Color.blue(colorPixel)

                R = (R + G + B) / 3
                G = R
                B = R
                finalMap.setPixel(i, j, Color.argb(A, R, G, B))
            }
        }

        return finalMap
    }
    private fun showAlertDialog() {
        val editText = EditText(this)

        val dialog = AlertDialog.Builder(this)
                .setTitle("Update your emojis")
                .setView(editText)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("OK", null)
                .show()

        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            Log.i(TAG, "Clicked on positive button!")
            val emojisEntered = editText.text.toString()
            if (emojisEntered.isBlank()) {
                Toast.makeText(this, "Cannot submit empty text", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            updateStatus(emojisEntered)
            dialog.dismiss()
        }
    }
}