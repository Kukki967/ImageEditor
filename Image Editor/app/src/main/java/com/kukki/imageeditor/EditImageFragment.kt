package com.kukki.imageeditor

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.RotateAnimation
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.kukki.imageeditor.databinding.FragmentEditImageBinding
import com.theartofdev.edmodo.cropper.CropImage
import kotlinx.android.synthetic.main.fragment_edit_image.*


@RequiresApi(Build.VERSION_CODES.Q)
class EditImageFragment : Fragment() {

    private lateinit var binding: FragmentEditImageBinding

    private lateinit var mainActivity: MainActivity
    private lateinit var imageUri: Uri

    private val args: EditImageFragmentArgs by navArgs()

    var fromRotation = 0f
    var toRotation = 0f
    var mCurrRotation = 0

    var originalImage: Bitmap? = null
    var croppedImage: Bitmap? = null
    var rotateBitmap: Bitmap? = null

    var imageAction: ImageAction = ImageAction.SAVE


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentEditImageBinding.inflate(inflater, container, false)
        return binding.root
    }





    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUi()
    }

    private fun initUi() {
        mainActivity = activity as MainActivity
        imageUri = args.imageUri.toUri()

        originalImage = MediaStore.Images.Media.getBitmap(requireContext().contentResolver, imageUri)
        photoEditorView.setImageURI(imageUri)

        cropBtn.setOnClickListener {
            cropImage()
        }

        saveBtn.setOnClickListener {
            save()
            imageAction = ImageAction.SAVE
        }

        undoBtn.setOnClickListener {
            undo()
        }

        rotateBtn.setOnClickListener {
            rotate()
        }
    }

    /* ******* *******  *******  *******  *******  undo methods ******* *******  *******  *******  *******  */
    private fun undo() {

        when (imageAction) {
            ImageAction.CROP -> {
                photoEditorView.setImageURI(imageUri)
            }
            ImageAction.ROTATE -> {
                undoRotation()
            }
            else -> {
                mainActivity.showSnackbar(editImageScreenLayout, "cannot undo more")
            }
        }

        imageAction = ImageAction.UNDO
    }

    private fun undoRotation() {
        val matrix = Matrix()

        mCurrRotation += 90
        toRotation = mCurrRotation.toFloat()

        val rotateAnimation = RotateAnimation(
            fromRotation, 0F, (photoEditorView.width / 2).toFloat(), (photoEditorView.height / 2).toFloat()
        )

        rotateAnimation.duration = 1000
        rotateAnimation.fillAfter = true

        matrix.setRotate(toRotation)

        rotateBitmap = Bitmap.createBitmap(originalImage!!, 0, 0, originalImage?.width!!, originalImage?.height!!, matrix, true)

        originalImage = rotateBitmap
        photoEditorView.setImageBitmap(rotateBitmap)
        photoEditorView.startAnimation(rotateAnimation)
        makeBitmapNull()
    }

    private fun makeBitmapNull() {
        mCurrRotation = 0
        toRotation = 0f
        fromRotation = 0f
        rotateBitmap = null
    }


    /* ******* *******  *******  *******  *******  save methods ******* *******  *******  *******  *******  */

    private fun save() {
        when (imageAction) {
            ImageAction.CROP -> {
                mainActivity.saveMediaToStorage(originalImage!!)
            }
            ImageAction.ROTATE -> {
                mainActivity.saveMediaToStorage(rotateBitmap!!)
            }
            else -> {
                mainActivity.saveMediaToStorage(originalImage!!)
            }
        }

        imageAction = ImageAction.SAVE
    }


    /* ******* *******  *******  *******  *******  crop methods ******* *******  *******  *******  *******  */

    private fun cropImage() {
        imageAction = ImageAction.CROP

        val intent = CropImage.activity(imageUri)
            .getIntent(requireContext())
        startActivityForResult(intent, CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE)

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            val result = CropImage.getActivityResult(data)
            if (resultCode == RESULT_OK) {

                val resultUri = result.uri
                originalImage = MediaStore.Images.Media.getBitmap(requireContext().contentResolver, resultUri)
                photoEditorView.setImageURI(resultUri)
            }
        }
    }

    /* ******* *******  *******  *******  *******  rotate methods ******* *******  *******  *******  *******  */

    private fun rotate() {
        imageAction = ImageAction.ROTATE

        mCurrRotation %= 360

        val matrix = Matrix()

        fromRotation = mCurrRotation.toFloat()
        mCurrRotation += 90
        toRotation = mCurrRotation.toFloat()

        val rotateAnimation = RotateAnimation(
            fromRotation, toRotation, (photoEditorView.width / 2).toFloat(), (photoEditorView.height / 2).toFloat()
        )

        rotateAnimation.duration = 1000
        rotateAnimation.fillAfter = true

        matrix.setRotate(toRotation)

        rotateBitmap = Bitmap.createBitmap(originalImage!!, 0, 0, originalImage?.width!!, originalImage?.height!!, matrix, true)
        originalImage = rotateBitmap

        photoEditorView.startAnimation(rotateAnimation)

    }

}

enum class ImageAction {
    CROP,
    ROTATE,
    SAVE,
    UNDO
}