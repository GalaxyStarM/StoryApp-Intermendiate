package id.ac.unri.storyapp.ui

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import id.ac.unri.storyapp.R
import id.ac.unri.storyapp.databinding.ActivityAddStoryBinding
import id.ac.unri.storyapp.utils.Message
import id.ac.unri.storyapp.utils.createCustomTempFile
import id.ac.unri.storyapp.utils.reduceFileImage
import id.ac.unri.storyapp.utils.uriToFile
import id.ac.unri.storyapp.viewmodel.AddStoryViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

@Suppress("DEPRECATION", "NAME_SHADOWING")
@AndroidEntryPoint
class AddStoryActivity : AppCompatActivity() {

    private var binding : ActivityAddStoryBinding? = null

    private var getFile: File? = null
    private var job: Job = Job()
    private var token: String = " "
    private val addStoryViewModel: AddStoryViewModel by viewModels()

    private lateinit var currentPhotoPath: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddStoryBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        supportActionBar?.hide()

        lifecycleScope.launchWhenResumed {
            launch {
                addStoryViewModel.getAuthToken().collect { itToken ->
                    if (!itToken.isNullOrEmpty()) token = itToken
                }
            }
        }
        binding?.btnCamera?.setOnClickListener{ startIntentCamera() }

        binding?.btnGallery?.setOnClickListener{ startIntentGalery() }

        binding?.btnSubmit?.setOnClickListener{ uploadStory() }
    }

    private val launcherIntentGallery = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if(result.resultCode == RESULT_OK) {
            val selectedImg: Uri = result.data?.data as Uri
            val myFile = uriToFile(selectedImg, this@AddStoryActivity)
            getFile = myFile

            binding?.ivPreview?.setImageURI(selectedImg)
        }
    }

    private val launcherIntentCamera = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ){ result ->
        if(result.resultCode == RESULT_OK) {
            val myFile = File(currentPhotoPath)
            getFile = myFile
            val result = BitmapFactory.decodeFile(myFile.path)
            binding?.ivPreview?.setImageBitmap(result)
        }
    }

    private fun startIntentGalery() {
        val intent = Intent()
        intent.action = Intent.ACTION_GET_CONTENT
        intent.type = "image/*"
        val chooser = Intent.createChooser(intent, getString(R.string.choose_picture))
        launcherIntentGallery.launch(chooser)
    }

    private fun startIntentCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.resolveActivity(packageManager)
        createCustomTempFile(application).also {
            val photoURI: Uri = FileProvider.getUriForFile(
                this@AddStoryActivity,
                "id.ac.unri.storyapp.ui",
                it
            )
            currentPhotoPath = it.absolutePath
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
            launcherIntentCamera.launch(intent)
        }
    }

    private fun uploadStory() {
        setLoadingState(true)

        val desc = binding?.etDescription
        var isValid = true

        if (desc?.text.toString().isBlank()) {
            desc?.error = resources.getString(R.string.valid_desc)
            isValid = false
        }

        if(getFile == null) {
            binding?.root?.let {
                Snackbar.make(
                    it,
                    resources.getString(R.string.valid_image),
                    Snackbar.LENGTH_SHORT
                ).show()
            }
            isValid = false
        }

        if(isValid) {
            val file = reduceFileImage(getFile as File)
            val description = desc?.text?.toString()?.toRequestBody("text/plain".toMediaType())
            val requestImageFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val imageMultiPart: MultipartBody.Part = MultipartBody.Part.createFormData(
                "photo",
                file.name,
                requestImageFile
            )

            lifecycle.coroutineScope.launchWhenCreated {
                if (job.isActive) job.cancel()
                job = launch {
                    addStoryViewModel.addStory(token, imageMultiPart, description!!).collect { response ->
                        response.onSuccess {
                            Message.setMessage(this@AddStoryActivity, getString(R.string.success_add_story))
                            startActivity(Intent(this@AddStoryActivity, MainActivity::class.java))
                            finishAffinity()
                        }

                        response.onFailure {
                            setLoadingState(false)
                            Message.setMessage(this@AddStoryActivity, getString(R.string.failed_add_story))
                        }
                    }
                }
            }
        } else setLoadingState(false)
    }

    private fun setLoadingState(loading: Boolean) {
        when(loading) {
            true -> {
                binding?.btnSubmit?.visibility = View.INVISIBLE
                binding?.progressBar?.visibility = View.VISIBLE
            }
            false -> {
                binding?.btnSubmit?.visibility = View.VISIBLE
                binding?.progressBar?.visibility = View.INVISIBLE
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return super.onSupportNavigateUp()
    }
}