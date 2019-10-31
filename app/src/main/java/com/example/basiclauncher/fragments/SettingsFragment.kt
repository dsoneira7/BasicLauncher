package com.example.basiclauncher.fragments


import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.example.basiclauncher.*
import com.example.basiclauncher.activities.*
import com.example.basiclauncher.fragments.SettingsFragment.Companion.newInstance
import com.example.basiclauncher.fragments.SettingsFragment.OnSettingsFragmentInteractionListener
import kotlinx.android.synthetic.main.fragment_settings.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.min

const val IMAGE_FROM_GALLERY: Int = 0
const val IMAGE_FROM_CAMERA: Int = 1
const val WALLPAPER_TO_WHITE: Int = 2

//RequestCodes de los Intent implícitos.
const val GALLERY_REQUEST_CODE = 3
const val SETTINGS_ACTIVITY_CODE = 4
const val CAMERA_REQUEST_CODE = 5

/**
 * Subclase de [Fragment] que solo contiene unas [ImageView] con callbacks para entrar en ajustes
 * básicos.
 *
 * Se debe utilizar el método factoría [newInstance] para crear una instancia. La actividad conte-
 * nedora debe implementar [OnSettingsFragmentInteractionListener] para la comunicación.
 */
class SettingsFragment : Fragment() {

    //Esta variable contiene el Path en el que se guarda un archivo que contendrá temporalmente
    //la fotografía que se tome para utilizar como fondo.
    private var currentBackgroundImagePath: String = ""

    private var iconsPerRow: Int = 0

    private lateinit var repository: Repository
    private var listener: OnSettingsFragmentInteractionListener? = null

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_settings, container, false)

    /**
     * En el onStart asignamos los callbacks que interactuan con la Actividad contenedora, que hará,
     * las operaciones pertinentes.
     */
    override fun onStart() {
        repository = Repository.getInstance(context!!.applicationContext)!!
        wallpaper_container.setOnClickListener {
            showPictureDialog()
        }

        settings_container.setOnClickListener {
            startActivityForResult(
                    Intent(context!!, SettingsActivity::class.java),
                    SETTINGS_ACTIVITY_CODE)
        }

        widget_container.setOnClickListener {
            Toast.makeText(context!!, "Not implemented", Toast.LENGTH_LONG).show()
            /*supportFragmentManager.beginTransaction()
                    .remove(supportFragmentManager.findFragmentByTag(SMALLER_MAIN_FRAGMENT_TAG)!!)
                    .remove(supportFragmentManager.findFragmentByTag(SETTINGS_FRAGMENT_TAG)!!)
                    .add(
                            R.appId.constraintlayout,
                            WidgetPickFragment.getInstance(getScreenMetrics().widthPixels),
                            WIDGET_PICK_FRAGMENT_TAG)
                    .commit()
                    val mAppWidgetHost = AppWidgetHost(this, 1024)
            val appWidgetId = mAppWidgetHost.allocateAppWidgetId()
            val pickIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK)
            pickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            addEmptyData(pickIntent)
            startActivityForResult(pickIntent, 32)*/
            listener!!.onSettingsFragmentInteraction(RETURN_TO_MAIN_MODE)
        }
        super.onStart()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnSettingsFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement OnSettingsFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    /**
     * Este método muestra un Dialog para seleccionar la acción a realizar para configurar un fondo.
     */
    private fun showPictureDialog() {
        if (!context!!.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            choosePhotoFromGallery()
        }
        val pictureDialog = AlertDialog.Builder(context!!)
        pictureDialog.setTitle(resources.getString(R.string.select_option))
        val pictureDialogItems = arrayOf(
                resources.getString(R.string.image_from_device),
                resources.getString(R.string.image_from_camera),
                resources.getString(R.string.white_background))
        pictureDialog.setItems(pictureDialogItems
        ) { dialog, which ->
            when (which) {
                IMAGE_FROM_GALLERY -> choosePhotoFromGallery()
                IMAGE_FROM_CAMERA -> takePhotoFromCamera()
                WALLPAPER_TO_WHITE -> resetWallpaperToWhite()
            }
        }

        pictureDialog.show()
    }

    /**
     * Lanza un [Intent] implícito para seleccionar una imagen de la galería.
     */
    private fun choosePhotoFromGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        //intent.addCategory(Intent.CATEGORY_OPENABLE)
        val chooser = Intent.createChooser(intent, resources.getString(R.string.complete_action_with))
        if (intent.resolveActivity(context!!.packageManager) != null) {
            startActivityForResult(chooser, GALLERY_REQUEST_CODE)
        }
    }

    /**
     * Lanza un [Intent] implícito para tomar una fotografía y establecerla como fondo de pantalla.
     */
    private fun takePhotoFromCamera() {
        val imageFile = try {
            //Es necesario crear un archivo en el que se guardará la imagen al tomarla.
            createImageFile()
        } catch (e: IOException) {
            Log.e("ERROR", e.stackTrace.contentToString())
            Toast.makeText(context!!, "Error al crear el archivo para guardar la imagen", Toast.LENGTH_LONG).show()
            return
        }
        val imageURI = FileProvider.getUriForFile(context!!.applicationContext, "com.example.android.fileprovider", imageFile)
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageURI)
        intent.resolveActivity(context!!.packageManager)
        startActivityForResult(intent, CAMERA_REQUEST_CODE)

    }

    /**
     * Pone el fondo de pantalla blanco y borra los datos guardados sobre el fondo de pantalla.
     */
    private fun resetWallpaperToWhite() {
        activity!!.window.setBackgroundDrawable(ColorDrawable(Color.WHITE))
        Helper.deleteBackgroundData(context!!.applicationContext)
        listener!!.onSettingsFragmentInteraction(RETURN_TO_MAIN_MODE)
    }

    /**
     * Crea un archivo en el que se guardará la imagen al tomarla con la cámara.
     */
    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File = context!!.applicationContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir).apply {
            currentBackgroundImagePath = absolutePath
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                GALLERY_REQUEST_CODE -> {
                    //Se configura el fondo según la imágen seleccionada en la galería y se vuelve
                    //al estado inicial
                    setCustomBackground(data!!.data!!)
                    listener!!.onSettingsFragmentInteraction(RETURN_TO_MAIN_MODE)
                }

                CAMERA_REQUEST_CODE -> {
                    //Se añade la fotografía a la galería, se configura el fondo según la imagen
                    //seleccionada y se vuelve al estado inicial
                    Thread {
                        addPictureToGallery()
                    }.start()
                    setCustomBackground()
                    listener!!.onSettingsFragmentInteraction(RETURN_TO_MAIN_MODE)
                }

                SETTINGS_ACTIVITY_CODE -> {
                    //Se llama cuando se presiona atrás en la actividad de preferencias,
                    //Se comprueba si ha habido algún cambio, y si es así, se recarga la activdad
                    if (iconsPerRow != data!!.getIntExtra(resources.getString(R.string.icon_size),0) ) {
                        listener!!.onSettingsFragmentInteraction(RECREATE_ACTIVITY)
                    }
                }
            }
        }
    }

    /**
     * Método destinado a obtener un bitmap de una imagen escogida del dispositivo y guardarla
     *
     * @param uri: Uri del archivo.
     */
    private fun setCustomBackground(uri: Uri) {
        var bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(activity!!.contentResolver, uri))
        } else {
            MediaStore.Images.Media.getBitmap(activity!!.contentResolver, uri)
        }
        //Reescalamos el bitmap para aumentar la eficiencia si la resolución de la imagen
        //es demasiado alta
        bitmap = rescaleBitmap(bitmap)

        //Guardamos la imagen y la configuramos como fondo
        Thread { Helper.saveNewBackground(context!!.applicationContext, bitmap) }.start()
        activity!!.window.setBackgroundDrawable(BitmapDrawable(resources, bitmap))
    }

    /**
     * Método destinado a obtener un bitmap de una imagen tomada por nosotros con la cámara
     * del dispositivo
     */
    private fun setCustomBackground() {
        val bmOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
            val scaleFactor: Int = min(outWidth / MAXIMUM_WIDTH, outHeight / MAXIMUM_HEIGHT)
            inJustDecodeBounds = false
            inSampleSize = scaleFactor
        }
        val orientation = setOrientation(currentBackgroundImagePath)
        var bitmap = BitmapFactory.decodeFile(currentBackgroundImagePath, bmOptions)
        bitmap = flipImage(bitmap, orientation)
        bitmap = rescaleBitmap(bitmap)
        Thread { Helper.saveNewBackground(context!!.applicationContext, bitmap) }.start()
        activity!!.window.setBackgroundDrawable(BitmapDrawable(resources, bitmap))
    }

    private fun setOrientation(path: String): Int {

        val exif = try {
            ExifInterface(path)
        } catch (e: IOException) {
            null
        }

        if (exif != null) {
            return when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } else {
            Log.e("ERROR", "Null exifInterface")
            return 0
        }
    }

    private fun flipImage(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(orientation.toFloat())
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * Método destinado a reescalar un bitmap según constantes dadas MAXIMUM_WIDTH Y MAXIMUM_HEIGHT.
     *
     * @param bitmap: [Bitmap] a reescalar.
     *
     * return [Bitmap] ya reescalado
     */
    private fun rescaleBitmap(bitmap: Bitmap): Bitmap {
        val imageWidth = bitmap.width
        val imageHeight = bitmap.height

        //Buscamos una resolucion de 1280x720. Obtenemos el ancho y la altura en función de la reso
        //lución que queremos. Lo que llamo factor de anchura y de altura
        val widthFactor = (imageWidth.toDouble()) / (MAXIMUM_WIDTH.toDouble())
        val heightFactor = (imageHeight.toDouble()) / (MAXIMUM_HEIGHT.toDouble())

        //El factor que sea mayor va a ser el que "sobre" más, por lo que la operación a hacer es:
        //1.- Reescalar el bitmap proporcionalmente para hacer que cuadre con la dimensión que
        //"sobre" menos.
        //2.- Recortar lo que sobre de la dimensión que sobra más.
        return if (widthFactor <= heightFactor) {
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, MAXIMUM_WIDTH, (imageHeight.toDouble() / widthFactor).toInt(), true)
            Bitmap.createBitmap(scaledBitmap, 0, (((imageHeight.toDouble() / widthFactor) - MAXIMUM_HEIGHT) / 2).toInt(), MAXIMUM_WIDTH, MAXIMUM_HEIGHT)
        } else {
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, (imageWidth / heightFactor).toInt(), MAXIMUM_HEIGHT, true)
            Bitmap.createBitmap(scaledBitmap, (((imageWidth.toDouble() / heightFactor) - MAXIMUM_WIDTH) / 2).toInt(), 0, MAXIMUM_WIDTH, MAXIMUM_HEIGHT)

        }
    }

    /**
     * Añade la imágen tomada anteriormente a la galería para que sea pública para el usuario.
     */
    private fun addPictureToGallery() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            val f = File(currentBackgroundImagePath)
            mediaScanIntent.data = Uri.fromFile(f)
            activity!!.sendBroadcast(mediaScanIntent)
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments]
     * (http://developer.android.com/training/basics/fragments/communicating.html)
     * for more information.
     */
    interface OnSettingsFragmentInteractionListener {
        fun onSettingsFragmentInteraction(event: Int)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment UnninstallAndCancelFragment.
         */
        @JvmStatic
        fun newInstance() = SettingsFragment()
    }
}
