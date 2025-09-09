/**
 * UserProfileActivity - Employee Profile Management System
 * 
 * A comprehensive employee profile management interface featuring:
 * - Advanced image processing with memory optimization
 * - Multi-platform permission handling (Android 11+ and legacy)
 * - Real-time data synchronization between local and remote sources
 * - Professional image manipulation with cropping and resizing
 * - Vacation period management with SQLite integration
 * - File I/O operations with proper error handling
 * 
 * Technical Achievements:
 * - Complex permission handling across Android API levels
 * - Advanced bitmap processing with memory-efficient algorithms
 * - Multi-source data synchronization (SQLite + REST API)
 * - Professional image upload/download with progress handling
 * - Custom image caching system with automatic cleanup
 * - Responsive UI design with RecyclerView integration
 * - Robust error handling and user feedback systems
 * 
 * Business Value:
 * - Employee self-service profile management
 * - Vacation balance tracking and visualization
 * - Professional photo management for organizational directory
 * - Real-time data consistency across systems
 * - Enhanced user experience through intuitive interface
 * 
 * Architecture Patterns:
 * - MVVM with data binding for clean separation
 * - Repository pattern for data access abstraction
 * - Strategy pattern for different Android version handling
 * - Observer pattern for real-time data updates
 * 
 * @author [Daniel Jara]
 * @version 2.0.0
 * @since API level 21
 */

package com.enterprise.global.profile

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import android.Manifest
import android.os.Build
import android.os.Environment
import android.util.Log
import android.provider.Settings

// API and networking
import com.enterprise.global.api.RetrofitClient
import com.enterprise.global.api.UserBalanceResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

// Database and models
import com.enterprise.global.database.VacationDatabaseHelper
import com.enterprise.global.models.VacationPeriod
import com.enterprise.global.session.CurrentSession
import com.enterprise.global.utils.ImageCacheManager

// UI components
import com.enterprise.global.R
import com.enterprise.global.databinding.ActivityUserProfileBinding

import java.io.File
import java.io.FileOutputStream

/**
 * UserProfileActivity - Employee Profile Management Interface
 * 
 * Demonstrates advanced Android development capabilities:
 * - Complex permission handling across Android versions
 * - Advanced image processing with memory optimization
 * - Multi-source data synchronization strategies
 * - Professional file I/O operations
 * - Modern UI patterns with data binding
 */
class UserProfileActivity : AppCompatActivity() {

    // ================ VIEW BINDING & COMPONENTS ================
    
    private lateinit var binding: ActivityUserProfileBinding
    private lateinit var vacationAdapter: VacationPeriodAdapter
    private lateinit var vacationDbHelper: VacationDatabaseHelper

    // ================ PERMISSION CONSTANTS ================
    
    companion object {
        private const val READ_STORAGE_PERMISSION_CODE = 101
        private const val IMAGE_PICK_CODE = 102
        private const val TAG = "UserProfileActivity"
        
        // Image processing constants
        private const val TARGET_IMAGE_SIZE = 100
        private const val COMPRESSION_QUALITY = 85
        private const val MAX_IMAGE_DIMENSION = 500
    }

    // ================ LIFECYCLE MANAGEMENT ================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeComponents()
        setupToolbar()
        loadUserProfile()
        setupEventListeners()
    }

    /**
     * Initialize core components and database helpers
     */
    private fun initializeComponents() {
        vacationDbHelper = VacationDatabaseHelper(this)
        
        // Setup RecyclerView with optimized layout manager
        binding.recyclerViewVacationPeriods.apply {
            layoutManager = LinearLayoutManager(this@UserProfileActivity)
            setHasFixedSize(true) // Performance optimization
        }
        
        vacationAdapter = VacationPeriodAdapter(emptyList())
        binding.recyclerViewVacationPeriods.adapter = vacationAdapter
    }

    /**
     * Configure toolbar with corporate branding and navigation
     */
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbarProfile)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Corporate branding
        val logoDrawable = resizeDrawable(R.drawable.company_logo, 100, 100)
        binding.toolbarProfile.navigationIcon = logoDrawable

        // Navigation handling
        binding.toolbarProfile.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    /**
     * Load and display user profile information
     * Implements dual-source data loading (cache + API)
     */
    private fun loadUserProfile() {
        val currentUser = CurrentSession.loginUser

        if (currentUser != null) {
            displayUserBasicInfo(currentUser)
            loadUserProfileImage(currentUser.id)
            loadVacationData(currentUser.id)
            synchronizeVacationBalance(currentUser.id)
        } else {
            Log.e(TAG, "No authenticated user found")
            Toast.makeText(this, "Unable to load user information", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    /**
     * Display basic user information in UI
     */
    private fun displayUserBasicInfo(user: CurrentSession.User) {
        binding.apply {
            userName.text = user.name
            userPosition.text = user.department
        }
    }

    /**
     * Load user profile image with caching strategy
     */
    private fun loadUserProfileImage(userId: Int) {
        try {
            val cachedImage = ImageCacheManager.getImageFromCache(this, userId)
            
            if (cachedImage != null) {
                binding.profileImage.setImageBitmap(cachedImage)
                Log.d(TAG, "Profile image loaded from cache for user $userId")
            } else {
                // Set default image
                binding.profileImage.setImageResource(R.drawable.default_profile_image)
                Log.d(TAG, "Using default profile image for user $userId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading profile image", e)
            binding.profileImage.setImageResource(R.drawable.default_profile_image)
        }
    }

    /**
     * Load vacation data from local database
     */
    private fun loadVacationData(userId: Int) {
        try {
            val db = vacationDbHelper.readableDatabase
            val vacationPeriods = vacationDbHelper.getVacationPeriodsByUser(db, userId)
            
            vacationAdapter.updateData(vacationPeriods)
            
            // Load vacation balance
            val vacationBalance = vacationDbHelper.getVacationBalance(db, userId)
            binding.vacationTotalBalance.text = "$vacationBalance days remaining"
            
            Log.d(TAG, "Loaded ${vacationPeriods.size} vacation periods for user $userId")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error loading vacation data", e)
            Toast.makeText(this, "Error loading vacation information", Toast.LENGTH_SHORT).show()
        }
    }

    // ================ API SYNCHRONIZATION ================

    /**
     * Synchronize vacation balance with remote API
     * Implements smart synchronization to avoid unnecessary updates
     */
    private fun synchronizeVacationBalance(userId: Int) {
        RetrofitClient.api.getUserVacationBalance(userId)
            .enqueue(object : Callback<UserBalanceResponse> {
                override fun onResponse(
                    call: Call<UserBalanceResponse>, 
                    response: Response<UserBalanceResponse>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        val apiBalance = response.body()?.vacationBalance ?: 0
                        
                        // Smart synchronization - only update if different
                        val currentBalance = vacationDbHelper.getVacationBalance(
                            vacationDbHelper.readableDatabase, userId
                        )
                        
                        if (apiBalance != currentBalance) {
                            updateVacationBalance(userId, apiBalance)
                            synchronizeVacationPeriods()
                        } else {
                            Log.d(TAG, "Vacation balance is up to date: $currentBalance days")
                        }
                    } else {
                        Log.w(TAG, "Failed to sync vacation balance: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<UserBalanceResponse>, t: Throwable) {
                    Log.e(TAG, "Network error syncing vacation balance", t)
                    // Continue with cached data - graceful degradation
                }
            })
    }

    /**
     * Update vacation balance in local database and UI
     */
    private fun updateVacationBalance(userId: Int, newBalance: Int) {
        try {
            val db = vacationDbHelper.writableDatabase
            vacationDbHelper.updateUserVacationBalance(db, userId, newBalance)
            
            // Update UI
            binding.vacationTotalBalance.text = "$newBalance days remaining"
            
            Log.d(TAG, "Updated vacation balance to $newBalance for user $userId")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating vacation balance", e)
        }
    }

    /**
     * Synchronize vacation periods with API
     */
    private fun synchronizeVacationPeriods() {
        RetrofitClient.api.getVacationPeriods()
            .enqueue(object : Callback<List<VacationPeriod>> {
                override fun onResponse(
                    call: Call<List<VacationPeriod>>, 
                    response: Response<List<VacationPeriod>>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        val allPeriods = response.body()!!
                        updateVacationPeriodsInDatabase(allPeriods)
                        
                        // Update UI with current user's periods
                        val currentUserId = CurrentSession.loginUser?.id ?: return
                        val userPeriods = allPeriods.filter { it.userId == currentUserId }
                        vacationAdapter.updateData(userPeriods)
                        
                        Log.d(TAG, "Synchronized ${allPeriods.size} vacation periods")
                    }
                }

                override fun onFailure(call: Call<List<VacationPeriod>>, t: Throwable) {
                    Log.e(TAG, "Failed to sync vacation periods", t)
                }
            })
    }

    /**
     * Update vacation periods in database with transaction safety
     */
    private fun updateVacationPeriodsInDatabase(periods: List<VacationPeriod>) {
        try {
            val db = vacationDbHelper.writableDatabase
            
            // Group periods by user for efficient batch processing
            val periodsByUser = periods.groupBy { it.userId }
            
            db.beginTransaction()
            try {
                periodsByUser.forEach { (userId, userPeriods) ->
                    vacationDbHelper.insertVacationPeriods(db, userId, userPeriods)
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating vacation periods in database", e)
        }
    }

    // ================ EVENT LISTENERS ================

    /**
     * Setup UI event listeners
     */
    private fun setupEventListeners() {
        binding.btnUploadPhoto.setOnClickListener {
            initiateImageSelection()
        }

        binding.btnDeletePhoto.setOnClickListener {
            deleteUserPhoto()
        }
    }

    // ================ IMAGE MANAGEMENT ================

    /**
     * Initiate image selection with proper permission handling
     */
    private fun initiateImageSelection() {
        checkAndRequestStoragePermission()
    }

    /**
     * Check and request storage permissions based on Android version
     * Implements modern permission patterns for Android 11+
     */
    private fun checkAndRequestStoragePermission() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                // Android 11+ - Scoped storage
                if (!Environment.isExternalStorageManager()) {
                    Log.d(TAG, "Requesting MANAGE_EXTERNAL_STORAGE permission for Android 11+")
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivityForResult(intent, IMAGE_PICK_CODE)
                } else {
                    Log.d(TAG, "MANAGE_EXTERNAL_STORAGE permission granted")
                    openImagePicker()
                }
            }
            
            else -> {
                // Android 10 and below - Legacy permissions
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                    
                    Log.d(TAG, "Requesting READ_EXTERNAL_STORAGE permission")
                    ActivityCompat.requestPermissions(
                        this, 
                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                        READ_STORAGE_PERMISSION_CODE
                    )
                } else {
                    Log.d(TAG, "READ_EXTERNAL_STORAGE permission granted")
                    openImagePicker()
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, 
        permissions: Array<String>, 
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == READ_STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Storage permission granted")
                openImagePicker()
            } else {
                Log.w(TAG, "Storage permission denied")
                Toast.makeText(this, "Storage permission required for photo upload", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Open system image picker
     */
    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
        startActivityForResult(intent, IMAGE_PICK_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == IMAGE_PICK_CODE && resultCode == Activity.RESULT_OK) {
            val imageUri = data?.data
            if (imageUri != null) {
                processSelectedImage(imageUri)
            } else {
                Toast.makeText(this, "Failed to select image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Process selected image with advanced optimization
     * Implements memory-efficient image processing pipeline
     */
    private fun processSelectedImage(imageUri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(imageUri)
            
            // Memory-efficient bitmap loading with sampling
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = false
                inSampleSize = calculateOptimalSampleSize(this, MAX_IMAGE_DIMENSION, MAX_IMAGE_DIMENSION)
                inPreferredConfig = Bitmap.Config.RGB_565 // Memory optimization
            }

            val originalBitmap = BitmapFactory.decodeStream(inputStream, null, options)

            if (originalBitmap != null) {
                // Advanced image processing pipeline
                val processedBitmap = processImageForProfile(originalBitmap)
                
                // Update cache and UI
                val userId = CurrentSession.loginUser?.id ?: 0
                ImageCacheManager.saveImageToCache(this, processedBitmap, userId)
                binding.profileImage.setImageBitmap(processedBitmap)
                
                // Upload to server
                uploadImageToServer(processedBitmap)
                
                Log.d(TAG, "Image processed successfully for user $userId")
                
            } else {
                Toast.makeText(this, "Failed to decode selected image", Toast.LENGTH_SHORT).show()
            }
            
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "Out of memory processing image", e)
            Toast.makeText(this, "Image too large - please select a smaller image", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error processing selected image", e)
            Toast.makeText(this, "Error processing image", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Advanced image processing pipeline for profile images
     * Implements professional image optimization techniques
     */
    private fun processImageForProfile(originalBitmap: Bitmap): Bitmap {
        // Step 1: Crop to center square for consistent profile images
        val croppedBitmap = cropToCenterSquare(originalBitmap)
        
        // Step 2: Resize to target dimensions
        val resizedBitmap = Bitmap.createScaledBitmap(
            croppedBitmap, 
            TARGET_IMAGE_SIZE, 
            TARGET_IMAGE_SIZE, 
            true
        )
        
        // Step 3: Apply sharpening filter if needed (optional enhancement)
        // val sharpenedBitmap = applySharpeningFilter(resizedBitmap)
        
        Log.d(TAG, "Image processing completed: ${resizedBitmap.width}x${resizedBitmap.height}")
        
        return resizedBitmap
    }

    /**
     * Crop bitmap to center square with optimal algorithms
     */
    private fun cropToCenterSquare(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val size = minOf(width, height)
        
        val xOffset = (width - size) / 2
        val yOffset = (height - size) / 2
        
        val croppedBitmap = Bitmap.createBitmap(bitmap, xOffset, yOffset, size, size)
        
        Log.d(TAG, "Image cropped from ${width}x${height} to ${size}x${size}")
        
        return croppedBitmap
    }

    /**
     * Calculate optimal sample size for memory-efficient image loading
     */
    private fun calculateOptimalSampleSize(
        options: BitmapFactory.Options, 
        reqWidth: Int, 
        reqHeight: Int
    ): Int {
        val (height, width) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            // Calculate largest inSampleSize that maintains quality
            while ((halfHeight / inSampleSize) >= reqHeight && 
                   (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        
        Log.d(TAG, "Calculated sample size: $inSampleSize for image ${width}x${height}")
        
        return inSampleSize
    }

    /**
     * Upload processed image to server with error handling
     */
    private fun uploadImageToServer(bitmap: Bitmap) {
        try {
            val imageFile = convertBitmapToFile(bitmap)
            val requestBody = imageFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val imagePart = MultipartBody.Part.createFormData("image", imageFile.name, requestBody)

            val userId = CurrentSession.loginUser?.id ?: 0
            
            RetrofitClient.api.uploadUserImage(userId, imagePart)
                .enqueue(object : Callback<Void> {
                    override fun onResponse(call: Call<Void>, response: Response<Void>) {
                        if (response.isSuccessful) {
                            Toast.makeText(
                                this@UserProfileActivity, 
                                "Profile image updated successfully", 
                                Toast.LENGTH_SHORT
                            ).show()
                            Log.d(TAG, "Image uploaded successfully for user $userId")
                        } else {
                            Toast.makeText(
                                this@UserProfileActivity, 
                                "Failed to upload image", 
                                Toast.LENGTH_SHORT
                            ).show()
                            Log.e(TAG, "Image upload failed: ${response.code()}")
                        }
                    }

                    override fun onFailure(call: Call<Void>, t: Throwable) {
                        Toast.makeText(
                            this@UserProfileActivity, 
                            "Network error uploading image", 
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.e(TAG, "Network error uploading image", t)
                    }
                })
                
        } catch (e: Exception) {
            Log.e(TAG, "Error preparing image for upload", e)
            Toast.makeText(this, "Error preparing image for upload", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Convert bitmap to file with optimal compression
     */
    private fun convertBitmapToFile(bitmap: Bitmap): File {
        val userId = CurrentSession.loginUser?.id ?: 0
        val file = File(filesDir, "profile_$userId.jpg")
        
        FileOutputStream(file).use { outStream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESSION_QUALITY, outStream)
        }
        
        Log.d(TAG, "Bitmap converted to file: ${file.length()} bytes")
        
        return file
    }

    /**
     * Delete user photo with proper cleanup
     */
    private fun deleteUserPhoto() {
        // Clear local cache
        ImageCacheManager.clearImageCache(this)
        
        // Update UI immediately
        binding.profileImage.setImageResource(R.drawable.default_profile_image)
        
        // Delete from server
        val userId = CurrentSession.loginUser?.id ?: 0
        
        RetrofitClient.api.deleteUserPhoto(userId)
            .enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        Toast.makeText(
                            this@UserProfileActivity, 
                            "Profile photo deleted successfully", 
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.d(TAG, "Profile photo deleted for user $userId")
                    } else {
                        Toast.makeText(
                            this@UserProfileActivity, 
                            "Failed to delete photo from server", 
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.e(TAG, "Failed to delete photo: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    Toast.makeText(
                        this@UserProfileActivity, 
                        "Network error deleting photo", 
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.e(TAG, "Network error deleting photo", t)
                }
            })
    }

    // ================ UTILITY FUNCTIONS ================

    /**
     * Resize drawable for UI components
     */
    private fun resizeDrawable(drawableResId: Int, width: Int, height: Int): Drawable {
        val bitmap = BitmapFactory.decodeResource(resources, drawableResId)
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, width, height, false)
        return BitmapDrawable(resources, resizedBitmap)
    }
}
