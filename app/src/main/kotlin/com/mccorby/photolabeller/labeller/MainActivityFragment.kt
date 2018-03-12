package com.mccorby.photolabeller.labeller

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.support.v4.app.Fragment
import android.support.v4.content.FileProvider
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import com.mccorby.movierating.trainer.MovieTrainerImpl
import com.mccorby.photolabeller.R
import com.mccorby.photolabeller.config.SharedConfig
import com.mccorby.photolabeller.filemanager.FileManagerImpl
import com.mccorby.photolabeller.labeller.presentation.LabellingPresenter
import com.mccorby.photolabeller.labeller.presentation.LabellingView
import com.mccorby.photolabeller.model.Stats
import com.mccorby.photolabeller.trainer.TrainingActivity
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_main.*
import kotlinx.android.synthetic.main.fragment_main.view.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch

class MainActivityFragment : Fragment(), LabellingView {

    companion object {
        const val REQUEST_TAKE_PHOTO = 2
        val labels = arrayOf("Airplane", "Automobile", "Bird", "Cat", "Deer", "Dog", "Frog", "Horse", "Ship", "Truck")
    }

    private var currentPhotoPath = ""
    private lateinit var presenter: LabellingPresenter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_main, container, false)

        view.labelImage.setOnClickListener { saveImageLabel() }
        view.train.setOnClickListener { trainModel() }
        buildLabels(view)

        return view
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        injectMembers()
        loadModel()
    }

    private fun loadModel() {
        launch(UI) {
           val stats = async {
                presenter.loadModel()
            }.await()
            onModelLoaded(stats)
        }
    }

    override fun onModelLoaded(stats: Stats) {
        Toast.makeText(activity, stats.summary, Toast.LENGTH_LONG).show()
        train.isEnabled = true

        val result = presenter.predict("I went and saw this movie last night after being coaxed to by a few friends of mine. I'll admit that I was reluctant to see it because from what I knew of Ashton Kutcher he was only able to do comedy. I was wrong. Kutcher played the character of Jake Fischer very well, and Kevin Costner played Ben Randall with such professionalism. The sign of a good movie is that it can toy with our emotions. This one did exactly that. The entire theater (which was sold out) was overcome by laughter during the first half of the movie, and were moved to tears during the second half. While exiting the theater I not only saw many women in tears, but many full grown men as well, trying desperately not to let anyone see them crying. This movie was great, and I suggest that you go see it before you judge.")
        Toast.makeText(activity, result.summary, Toast.LENGTH_LONG).show()
    }

    private fun trainModel() {
        startActivity(Intent(activity, TrainingActivity::class.java))
    }

    override fun onResume() {
        super.onResume()
        showImage()
    }

    private fun injectMembers() {
        val config = SharedConfig(50, 3)
        val fileManager = FileManagerImpl(context!!.getExternalFilesDir(Environment.DIRECTORY_PICTURES))
        val trainer = MovieTrainerImpl.instance
        // TODO This to be fixed injection SharedConfig in Trainer
//        trainer.config = config
        presenter = LabellingPresenter(this, fileManager, trainer)
    }

    private fun saveImageLabel() {
        val selectedLabel = labelsDropdown.selectedItem
        presenter.saveLabelledImage(currentPhotoPath, selectedLabel.toString().toLowerCase())
    }

    private fun buildLabels(view: View) {
        val adapter = ArrayAdapter(activity, android.R.layout.simple_spinner_dropdown_item, labels)
        view.labelsDropdown.adapter = adapter
    }

    fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(activity!!.packageManager) != null) {
            // Create the File where the photo should go
            val photoFile = presenter.createImageFile()
            currentPhotoPath = photoFile.absolutePath
            Log.d(MainActivity::class.java.simpleName, "Current photo path $currentPhotoPath")

            // Continue only if the File was successfully created
            val photoURI = FileProvider.getUriForFile(context!!,
                    "com.mccorby.photolabeller.fileprovider",
                    photoFile)
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
            startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == Activity.RESULT_OK) {
            Log.d(MainActivity::class.java.simpleName, "Photo path $currentPhotoPath")

            showImage()
        }
    }

    private fun showImage() {
        val location = "file://$currentPhotoPath"
        val imageSize = resources.getDimension(R.dimen.image_height).toInt()
        Picasso.with(context).load(location)
                .resize(imageSize, imageSize)
                .centerCrop()
                .error(R.mipmap.ic_launcher_round)
                .into(takenPhoto)

//        if (currentPhotoPath.isNotEmpty()) {
//            presenter.predict(File(currentPhotoPath))
//        }
    }
}
