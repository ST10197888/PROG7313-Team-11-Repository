package com.budget.app.fragments

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.budget.app.R
import com.budget.app.activities.MainActivity
import com.budget.app.models.TransactionType
import com.budget.app.utils.AppData
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class AddTransactionFragment : Fragment(), MainActivity.OnBackPressedListener {

    private var selectedAttachmentUri: Uri? = null
    private var selectedAttachmentName: String? = null
    private var photoFile: File? = null

    private lateinit var etTitle: EditText
    private lateinit var etAmount: EditText
    private lateinit var etNotes: EditText
    private lateinit var rgType: RadioGroup
    private lateinit var spinnerCat: Spinner
    private lateinit var btnSave: Button
    private lateinit var btnAttach: Button
    private lateinit var tvAttachmentName: TextView
    private lateinit var layoutAttachment: View
    private var scrollView: ScrollView? = null

    // Custom Budget Views
    private lateinit var layoutCustomBudget: View
    private lateinit var spinnerCustomBudget: Spinner

    // Usage Indicator Views
    private lateinit var layoutUsageIndicator: View
    private lateinit var pbUsage: ProgressBar
    private lateinit var tvUsagePercent: TextView

    // Overlay Views
    private lateinit var overlayContainer: View
    private lateinit var progressBarCard: CardView
    private lateinit var tvProgressBar: TextView
    private lateinit var tvProcessingLabel: TextView

    private var countDownTimer: CountDownTimer? = null

    // Configuration
    private val TOTAL_DURATION_MS   = 3000L
    private val TICK_INTERVAL_MS    = 30L
    private val PROGRESS_BAR_LENGTH = 20
    private val FILLED_CHAR         = '█'
    private val EMPTY_CHAR          = '░'

    companion object {
        private const val ARG_TYPE = "type"
        private const val ARG_CATEGORY = "category"

        fun newInstance(type: TransactionType? = null, category: String? = null): AddTransactionFragment {
            val fragment = AddTransactionFragment()
            val args = Bundle()
            type?.let { args.putString(ARG_TYPE, it.name) }
            category?.let { args.putString(ARG_CATEGORY, it) }
            fragment.arguments = args
            return fragment
        }
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                saveUriToInternalStorage(uri)
            }
        }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            photoFile?.let {
                val uri = Uri.fromFile(it)
                setAttachment(uri, it.name)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_add_transaction, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        scrollView = view.findViewById(R.id.scrollView) ?: findScrollView(view)
        etTitle = view.findViewById(R.id.etTitle)
        etAmount = view.findViewById(R.id.etAmount)
        etNotes = view.findViewById(R.id.etNotes)
        rgType = view.findViewById(R.id.rgTransactionType)
        spinnerCat = view.findViewById(R.id.spinnerCategory)
        btnSave = view.findViewById(R.id.btnSave)
        btnAttach = view.findViewById(R.id.btnAttach)
        tvAttachmentName = view.findViewById(R.id.tvAttachmentName)
        layoutAttachment = view.findViewById(R.id.layoutAttachment)

        layoutCustomBudget = view.findViewById(R.id.layoutCustomBudget)
        spinnerCustomBudget = view.findViewById(R.id.spinnerCustomBudget)

        layoutUsageIndicator = view.findViewById(R.id.layoutUsageIndicator)
        pbUsage = view.findViewById(R.id.pbUsage)
        tvUsagePercent = view.findViewById(R.id.tvUsagePercent)

        // Overlay UI
        overlayContainer = view.findViewById(R.id.overlayContainer)
        progressBarCard = view.findViewById(R.id.progressBarCard)
        tvProgressBar = view.findViewById(R.id.tvProgressBar)
        tvProcessingLabel = view.findViewById(R.id.tvProcessingLabel)

        fun updateCategories(type: TransactionType) {
            val categories = AppData.getCategoriesForType(type)
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerCat.adapter = adapter
            layoutAttachment.visibility = if (type == TransactionType.EXPENSE) View.VISIBLE else View.GONE

            // Handle Custom Budgets for Expenses
            if (type == TransactionType.EXPENSE) {
                val customBudgets = AppData.getBudgetGoals().filter { goal ->
                    !AppData.expenseCategories.contains(goal.category)
                }
                if (customBudgets.isNotEmpty()) {
                    layoutCustomBudget.visibility = View.VISIBLE
                    val customAdapter = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_spinner_item,
                        listOf("None") + customBudgets.map { it.category }
                    )
                    customAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinnerCustomBudget.adapter = customAdapter
                } else {
                    layoutCustomBudget.visibility = View.GONE
                }
            } else {
                layoutCustomBudget.visibility = View.GONE
            }

            updateUsageIndicator()
        }

        rgType.setOnCheckedChangeListener { _, checkedId ->
            val type = when (checkedId) {
                R.id.rbIncome -> TransactionType.INCOME
                R.id.rbSavings -> TransactionType.SAVINGS
                else -> TransactionType.EXPENSE
            }
            updateCategories(type)
        }

        // Apply Arguments if present
        val initialTypeStr = arguments?.getString(ARG_TYPE)
        val initialCategory = arguments?.getString(ARG_CATEGORY)

        if (initialTypeStr != null) {
            val type = TransactionType.valueOf(initialTypeStr)
            when (type) {
                TransactionType.INCOME -> rgType.check(R.id.rbIncome)
                TransactionType.EXPENSE -> rgType.check(R.id.rbExpense)
                TransactionType.SAVINGS -> rgType.check(R.id.rbSavings)
            }
            updateCategories(type)

            if (initialCategory != null) {
                val categories = AppData.getCategoriesForType(type)
                val index = categories.indexOf(initialCategory)
                if (index >= 0) {
                    spinnerCat.setSelection(index)
                }
            }
        } else {
            updateCategories(TransactionType.EXPENSE)
        }

        etAmount.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateUsageIndicator()
            }
        })

        btnAttach.setOnClickListener {
            showAttachmentOptions()
        }

        btnSave.setOnClickListener {
            if (validateInputs()) {
                startTransactionProcessing()
            }
        }
    }

    private fun showAttachmentOptions() {
        val options = arrayOf("Take Photo", "Choose from Gallery", "Cancel")
        AlertDialog.Builder(requireContext())
            .setTitle("Add Attachment")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> launchCamera()
                    1 -> launchGallery()
                    else -> dialog.dismiss()
                }
            }
            .show()
    }

    private fun launchCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try {
            photoFile = createImageFile()
            photoFile?.let {
                val photoURI = FileProvider.getUriForFile(requireContext(), "com.budget.app.fileprovider", it)
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                cameraLauncher.launch(intent)
            }
        } catch (ex: IOException) {
            Toast.makeText(requireContext(), "Error creating file", Toast.LENGTH_SHORT).show()
        }
    }

    private fun launchGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    }

    private fun saveUriToInternalStorage(uri: Uri) {
        try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val file = File(storageDir, "GALLERY_${timeStamp}.jpg")
            
            inputStream?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            
            setAttachment(Uri.fromFile(file), file.name)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Failed to save image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setAttachment(uri: Uri, name: String) {
        selectedAttachmentUri = uri
        selectedAttachmentName = name
        tvAttachmentName.text = name
        tvAttachmentName.visibility = View.VISIBLE
        btnAttach.text = "Change Attachment"
    }

    private fun findScrollView(view: View): ScrollView? {
        if (view is ScrollView) return view
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val child = findScrollView(view.getChildAt(i))
                if (child != null) return child
            }
        }
        return null
    }

    override fun onBackPressed(): Boolean {
        scrollView?.let {
            if (it.scrollY > 0) {
                it.smoothScrollTo(0, 0)
                return true
            }
        }
        return false
    }

    private fun updateUsageIndicator() {
        val type = when (rgType.checkedRadioButtonId) {
            R.id.rbIncome -> TransactionType.INCOME
            R.id.rbSavings -> TransactionType.SAVINGS
            else -> TransactionType.EXPENSE
        }

        if (type == TransactionType.INCOME) {
            layoutUsageIndicator.visibility = View.GONE
            return
        }

        val amountStr = etAmount.text.toString()
        val amount = amountStr.toDoubleOrNull() ?: 0.0

        val totalIncome = AppData.getTotalIncome()
        if (totalIncome <= 0) {
            layoutUsageIndicator.visibility = View.GONE
            return
        }

        layoutUsageIndicator.visibility = View.VISIBLE

        val currentOutflow = AppData.getTotalExpenses() + AppData.getTotalSavings()
        val newTotalOutflow = currentOutflow + amount
        val usagePercent = ((newTotalOutflow / totalIncome) * 100).toInt()

        pbUsage.progress = usagePercent.coerceIn(0, 100)
        tvUsagePercent.text = "$usagePercent% of monthly income used"

        if (usagePercent > 100) {
            tvUsagePercent.setTextColor(requireContext().getColor(R.color.expense_red))
        } else {
            tvUsagePercent.setTextColor(requireContext().getColor(R.color.text_secondary))
        }
    }

    private fun validateInputs(): Boolean {
        val title = etTitle.text.toString().trim()
        val amtStr = etAmount.text.toString().trim()

        if (title.isEmpty()) {
            etTitle.error = "Enter a title"
            return false
        }
        if (amtStr.isEmpty() || amtStr.toDoubleOrNull() == null || amtStr.toDouble() <= 0) {
            etAmount.error = "Enter a valid amount"
            return false
        }
        return true
    }

    private fun startTransactionProcessing() {
        showOverlay()
        disableAllInputs()
        startProgressBar()
    }

    private fun startProgressBar() {
        var elapsed = 0L
        countDownTimer = object : CountDownTimer(TOTAL_DURATION_MS, TICK_INTERVAL_MS) {
            override fun onTick(millisUntilFinished: Long) {
                elapsed += TICK_INTERVAL_MS
                val progress = (elapsed.toFloat() / TOTAL_DURATION_MS.toFloat()).coerceIn(0f, 1f)
                updateProgressBarText(progress)
            }

            override fun onFinish() {
                updateProgressBarText(1f)
                hideOverlay()
                enableAllInputs()
                onTransactionComplete()
            }
        }.start()
    }

    private fun updateProgressBarText(progress: Float) {
        val filledCount = (progress * PROGRESS_BAR_LENGTH).toInt()
        val emptyCount  = PROGRESS_BAR_LENGTH - filledCount
        val percentage  = (progress * 100).toInt()

        val bar = buildString {
            append("[")
            repeat(filledCount) { append(FILLED_CHAR) }
            repeat(emptyCount)  { append(EMPTY_CHAR)  }
            append("] $percentage%")
        }
        tvProgressBar.text = bar
    }

    private fun showOverlay() {
        overlayContainer.visibility = View.VISIBLE
        progressBarCard.visibility  = View.VISIBLE
        tvProcessingLabel.text      = "Processing Transaction..."
        updateProgressBarText(0f)
    }

    private fun hideOverlay() {
        overlayContainer.visibility = View.GONE
        progressBarCard.visibility  = View.GONE
    }

    private fun disableAllInputs() {
        etTitle.isEnabled = false
        etAmount.isEnabled = false
        etNotes.isEnabled = false
        rgType.isEnabled = false
        for (i in 0 until rgType.childCount) rgType.getChildAt(i).isEnabled = false
        spinnerCat.isEnabled = false
        btnSave.isEnabled = false
        btnAttach.isEnabled = false
    }

    private fun enableAllInputs() {
        etTitle.isEnabled = true
        etAmount.isEnabled = true
        etNotes.isEnabled = true
        rgType.isEnabled = true
        for (i in 0 until rgType.childCount) rgType.getChildAt(i).isEnabled = true
        spinnerCat.isEnabled = true
        btnSave.isEnabled = true
        btnAttach.isEnabled = true
    }

    private fun onTransactionComplete() {
        val type = when (rgType.checkedRadioButtonId) {
            R.id.rbIncome -> TransactionType.INCOME
            R.id.rbSavings -> TransactionType.SAVINGS
            else -> TransactionType.EXPENSE
        }

        val mainCategory = spinnerCat.selectedItem.toString()
        val customBudget = if (layoutCustomBudget.visibility == View.VISIBLE) {
            spinnerCustomBudget.selectedItem.toString()
        } else {
            "None"
        }

        val finalCategory = if (type == TransactionType.EXPENSE && customBudget != "None") customBudget else mainCategory

        AppData.addTransaction(
            context = requireContext(),
            title = etTitle.text.toString().trim(),
            amount = etAmount.text.toString().trim().toDouble(),
            type = type,
            category = finalCategory,
            notes = etNotes.text.toString().trim(),
            attachmentUri = selectedAttachmentUri?.toString(),
            attachmentName = selectedAttachmentName
        )

        Toast.makeText(requireContext(), "Transaction added!", Toast.LENGTH_SHORT).show()
        (activity as? MainActivity)?.navigateTo(R.id.nav_dashboard)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countDownTimer?.cancel()
    }

    private fun getFileSize(uri: Uri): Long {
        return try {
            requireContext().contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                cursor.moveToFirst()
                cursor.getLong(sizeIndex)
            } ?: 0L
        } catch (e: Exception) { 0L }
    }

    private fun getFileName(uri: Uri): String {
        return try {
            requireContext().contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                cursor.moveToFirst()
                cursor.getString(nameIndex)
            } ?: "Unknown File"
        } catch (e: Exception) { "Attachment" }
    }
}
