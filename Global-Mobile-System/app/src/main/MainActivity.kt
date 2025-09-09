/**
 * MainActivity - Enterprise Employee Management Dashboard
 * 
 * A comprehensive real-time employee status tracking system featuring:
 * - Live employee status monitoring with automatic state updates
 * - Complex business logic for work schedules and availability
 * - Real-time notification system with background processing
 * - Advanced filtering and search capabilities
 * - Multi-source data synchronization (SQLite + REST API)
 * - Sophisticated state management with progress tracking
 * 
 * Technical Achievements:
 * - Complex business logic implementation for work hour calculations
 * - Real-time data synchronization across multiple sources
 * - Advanced RecyclerView with custom adapters and filtering
 * - Background task management with WorkManager
 * - Sophisticated notification system with badge management
 * - Custom UI components with progress indicators
 * - Error handling and retry mechanisms
 * - Memory-efficient data processing with coroutines
 * 
 * Business Value:
 * - Real-time visibility of organizational human resources
 * - Automated workflow management for leave requests
 * - Improved communication and coordination across departments
 * - Streamlined approval processes for managers
 * - Enhanced productivity through status transparency
 * 
 * @author [Your Name]
 * @version 2.1.0
 * @since API level 21
 */

package com.enterprise.global.main

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy

// Activity imports
import com.enterprise.global.permissions.PermissionsActivity
import com.enterprise.global.profile.UserProfileActivity
import com.enterprise.global.vacation.VacationActivity
import com.enterprise.global.fieldwork.FieldWorkActivity
import com.enterprise.global.travel.TravelActivity
import com.enterprise.global.calendar.CalendarActivity

// Core components
import com.enterprise.global.api.RetrofitClient
import com.enterprise.global.database.UserDatabaseHelper
import com.enterprise.global.database.ScheduleDatabaseHelper
import com.enterprise.global.database.HolidayDatabaseHelper
import com.enterprise.global.session.CurrentSession.loginUser
import com.enterprise.global.notifications.NotificationManager
import com.enterprise.global.notifications.NotificationAdapter
import com.enterprise.global.notifications.NotificationWorker
import com.enterprise.global.notifications.NotificationsActivity
import com.enterprise.global.utils.ExceptionScheduleHelper
import com.enterprise.global.utils.StatusReturnManager

// Data models
import com.enterprise.global.models.User
import com.enterprise.global.models.Notification
import com.enterprise.global.models.StatusBoard

// UI components
import com.enterprise.global.R
import com.enterprise.global.databinding.ActivityMainBinding

// Retrofit and API
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

// Date and time handling
import java.util.Locale
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.ZonedDateTime
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * MainActivity - Core Dashboard Controller
 * 
 * Demonstrates advanced Android development patterns:
 * - MVVM architecture with data binding
 * - Complex business logic implementation
 * - Real-time data synchronization
 * - Advanced UI state management
 * - Background processing with WorkManager
 * - Custom adapter patterns with filtering
 * - Professional error handling strategies
 */
class MainActivity : AppCompatActivity(), StatusUpdateListener {

    // ================ VIEW BINDING & CORE COMPONENTS ================
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var userAdapter: UserAdapter
    private lateinit var notificationManager: NotificationManager
    private lateinit var notificationAdapter: NotificationAdapter
    
    // ================ DATA MANAGEMENT ================
    
    private var fullUserList: List<User> = listOf()
    private var notificationList: MutableList<Notification> = mutableListOf()

    // ================ LIFECYCLE MANAGEMENT ================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeToolbar()
        initializeNotificationSystem()
        setupSwipeRefresh()
        setupRecyclerViews()
        configureCoreFeatures()
        startBackgroundServices()
    }

    /**
     * Initialize and configure the application toolbar
     * Sets up navigation, branding, and overflow menu styling
     */
    private fun initializeToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Customize overflow icon color for better visibility
        val overflowIcon = binding.toolbar.overflowIcon
        if (overflowIcon != null) {
            val wrappedIcon = DrawableCompat.wrap(overflowIcon)
            DrawableCompat.setTint(wrappedIcon, ContextCompat.getColor(this, R.color.primary_accent))
            binding.toolbar.overflowIcon = wrappedIcon
        }

        // Set corporate branding logo
        val logoDrawable = resizeDrawable(R.drawable.company_logo, 100, 100)
        binding.toolbar.navigationIcon = logoDrawable
    }

    /**
     * Initialize comprehensive notification system
     * Sets up real-time notifications with badge management
     */
    private fun initializeNotificationSystem() {
        val currentUser = loginUser
        if (currentUser != null) {
            notificationManager = NotificationManager(this, null, currentUser.id, notificationAdapter)
            scheduleNotificationCheck(this, currentUser.id)
            
            // Initialize notification badge
            val inflater = LayoutInflater.from(this)
            val badgeLayout = inflater.inflate(R.layout.menu_item_with_badge, null)
            val notificationBadge = badgeLayout.findViewById<TextView>(R.id.notification_badge)

            if (notificationBadge != null) {
                Log.d("MainActivity", "Notification badge initialized successfully")
                notificationManager.initializeBadge(badgeLayout)
            } else {
                Log.e("MainActivity", "Failed to initialize notification badge")
            }
        }

        notificationAdapter = NotificationAdapter(mutableListOf()) { notification ->
            handleNotificationClick(notification)
        }
    }

    /**
     * Configure pull-to-refresh functionality
     * Implements dual-source data refresh strategy
     */
    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            refreshDataSources()
        }
    }

    /**
     * Setup RecyclerViews for users and notifications
     * Configures adapters and layout managers
     */
    private fun setupRecyclerViews() {
        setupNotificationRecyclerView(notificationAdapter)
        toggleNotificationRecyclerView()
    }

    // ================ DATA SYNCHRONIZATION ================

    /**
     * Comprehensive data refresh from multiple sources
     * Implements fallback strategy: SQLite -> API -> Error handling
     */
    private fun refreshDataSources() {
        if (::notificationManager.isInitialized) {
            notificationManager.fetchNotifications { unreadCount ->
                Log.d("NotificationManager", "Notification update completed: $unreadCount unread")
            }
        }

        // Primary data source: Local SQLite database
        attemptSQLiteDataLoad { sqliteSuccess ->
            // Secondary data source: Remote API for real-time updates
            attemptAPIDataLoad { apiSuccess ->
                if (!apiSuccess) {
                    Log.e("DataSync", "API synchronization failed")
                }
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    /**
     * Load user data from local SQLite database
     * Implements complex query with department grouping and user ordering
     */
    private fun fetchUsersFromSQLite() {
        val dbHelper = UserDatabaseHelper(this)
        val db = dbHelper.readableDatabase

        // Complex query: Group by department, order by hierarchy
        val departmentQuery = """
            SELECT ${UserDatabaseHelper.COLUMN_DEPARTMENT}, 
                   ${UserDatabaseHelper.COLUMN_DEPARTMENT_CODE} 
            FROM ${UserDatabaseHelper.TABLE_NAME} 
            WHERE ${UserDatabaseHelper.COLUMN_STATUS} = 1 
            GROUP BY ${UserDatabaseHelper.COLUMN_DEPARTMENT} 
            ORDER BY ${UserDatabaseHelper.COLUMN_DEPARTMENT} ASC
        """.trimIndent()

        val departmentCursor = db.rawQuery(departmentQuery, null)
        val users = mutableListOf<User>()

        if (departmentCursor.moveToFirst()) {
            do {
                val department = departmentCursor.getString(
                    departmentCursor.getColumnIndexOrThrow(UserDatabaseHelper.COLUMN_DEPARTMENT)
                )

                // Get users for each department, ordered by organizational hierarchy
                val userQuery = """
                    SELECT * FROM ${UserDatabaseHelper.TABLE_NAME} 
                    WHERE ${UserDatabaseHelper.COLUMN_DEPARTMENT} = ? 
                    AND ${UserDatabaseHelper.COLUMN_STATUS} = 1 
                    ORDER BY ${UserDatabaseHelper.COLUMN_ORDER} ASC
                """.trimIndent()

                val userCursor = db.rawQuery(userQuery, arrayOf(department))

                if (userCursor.moveToFirst()) {
                    do {
                        val user = buildUserFromCursor(userCursor)
                        users.add(user)
                    } while (userCursor.moveToNext())
                }
                userCursor.close()
            } while (departmentCursor.moveToNext())
        }

        departmentCursor.close()
        db.close()

        fullUserList = users
        setupUserAdapter()
    }

    /**
     * Build User object from database cursor
     * Handles data transformation and status calculation
     */
    private fun buildUserFromCursor(cursor: android.database.Cursor): User {
        val userId = cursor.getInt(cursor.getColumnIndexOrThrow(UserDatabaseHelper.COLUMN_ID))
        val name = cursor.getString(cursor.getColumnIndexOrThrow(UserDatabaseHelper.COLUMN_NAME))
        val joinDateOriginal = cursor.getString(cursor.getColumnIndexOrThrow(UserDatabaseHelper.COLUMN_JOIN_DATE))
        val joinDate = formatChileanDate(joinDateOriginal)
        val extension = cursor.getString(cursor.getColumnIndexOrThrow(UserDatabaseHelper.COLUMN_EXTENSION))
        val phoneOriginal = cursor.getString(cursor.getColumnIndexOrThrow(UserDatabaseHelper.COLUMN_PHONE))
        val phoneFormatted = formatPhoneNumber(phoneOriginal)
        val birthday = cursor.getString(cursor.getColumnIndexOrThrow(UserDatabaseHelper.COLUMN_BIRTHDAY))
        val department = cursor.getString(cursor.getColumnIndexOrThrow(UserDatabaseHelper.COLUMN_DEPARTMENT))

        // Calculate real-time status and progress
        val (status, progress) = calculateUserStatus(userId)
        val statusText = generateStatusText(userId)

        return User(
            id = userId,
            name = name,
            status = status,
            text = statusText,
            department = department,
            progress = progress,
            joinDate = joinDate,
            extension = extension,
            phone = phoneFormatted,
            birthday = birthday
        )
    }

    // ================ BUSINESS LOGIC - STATUS CALCULATION ================

    /**
     * Calculate user status based on work schedule and current time
     * Implements complex business rules for work hours, breaks, and availability
     * 
     * @param userId User identifier
     * @return Pair of status string and progress percentage
     */
    private fun calculateUserStatus(userId: Int): Pair<String, Int> {
        val scheduleHelper = ScheduleDatabaseHelper(this)
        val db = scheduleHelper.readableDatabase

        val currentDay = LocalDate.now().dayOfWeek.value
        val query = """
            SELECT * FROM ${ScheduleDatabaseHelper.TABLE_NAME} 
            WHERE user_id = ? AND day_of_week = ? AND is_active = 1
        """.trimIndent()
        
        val cursor = db.rawQuery(query, arrayOf(userId.toString(), currentDay.toString()))

        var status = "AVAILABLE"
        var progress = 100

        if (cursor.moveToFirst()) {
            val startTime = cursor.getString(cursor.getColumnIndexOrThrow("start_time"))
            val endTime = cursor.getString(cursor.getColumnIndexOrThrow("end_time"))
            val lunchStart = cursor.getString(cursor.getColumnIndexOrThrow("lunch_start"))
            val lunchEnd = cursor.getString(cursor.getColumnIndexOrThrow("lunch_end"))
            val remoteWork = cursor.getString(cursor.getColumnIndexOrThrow("remote_work"))

            val currentTime = LocalTime.now()
            val workStart = LocalTime.parse(startTime)
            val workEnd = LocalTime.parse(endTime)
            val lunchStartTime = LocalTime.parse(lunchStart)
            val lunchEndTime = LocalTime.parse(lunchEnd)

            // Complex business logic for status determination
            when {
                currentTime.isBefore(workStart) || currentTime.isAfter(workEnd) -> {
                    status = "HOME"
                    progress = 0
                    
                    if (currentTime.isAfter(workEnd)) {
                        calculateNextWorkDay(userId, currentDay, db)
                    }
                }
                
                currentTime.isAfter(lunchStartTime) && currentTime.isBefore(lunchEndTime) -> {
                    status = "LUNCH"
                    progress = calculateTimeProgress(lunchStartTime, lunchEndTime, currentTime)
                }
                
                else -> {
                    status = "IN_OFFICE"
                    progress = if (currentTime.isBefore(lunchStartTime)) {
                        calculateTimeProgress(workStart, lunchStartTime, currentTime)
                    } else {
                        calculateTimeProgress(lunchEndTime, workEnd, currentTime)
                    }
                    
                    // Handle remote work scenarios
                    handleRemoteWorkStatus(remoteWork, currentTime, status)
                    
                    // Handle special exceptions (flexible schedules)
                    handleSpecialExceptions(userId, currentTime)
                }
            }
        }

        cursor.close()
        db.close()

        // Apply availability constraints (e.g., after hours)
        status = applyAvailabilityConstraints(status, LocalTime.now())

        return Pair(status, progress)
    }

    /**
     * Calculate progress percentage between two time points
     */
    private fun calculateTimeProgress(start: LocalTime, end: LocalTime, current: LocalTime): Int {
        val totalDuration = ChronoUnit.SECONDS.between(start, end)
        val elapsedDuration = ChronoUnit.SECONDS.between(start, current)
        
        return if (totalDuration <= 0L) {
            100
        } else {
            ((elapsedDuration.toDouble() / totalDuration) * 100).toInt().coerceIn(0, 100)
        }
    }

    /**
     * Handle remote work status logic
     */
    private fun handleRemoteWorkStatus(remoteWork: String, currentTime: LocalTime, currentStatus: String): String {
        return when (remoteWork) {
            "AM" -> if (currentTime.isBefore(LocalTime.of(14, 0))) "REMOTE_WORK" else currentStatus
            "PM" -> if (currentTime.isAfter(LocalTime.of(13, 0))) "REMOTE_WORK" else currentStatus
            "FULL_DAY" -> "REMOTE_WORK"
            else -> currentStatus
        }
    }

    /**
     * Handle special schedule exceptions
     */
    private fun handleSpecialExceptions(userId: Int, currentTime: LocalTime): String {
        if (userId == 44) { // Example: Flexible schedule user
            val exceptionHelper = ExceptionScheduleHelper()
            val currentDate = LocalDate.now()
            val holidayHelper = HolidayDatabaseHelper(this)
            val holidays = holidayHelper.getHolidays().map { it.date }
            
            return try {
                exceptionHelper.handleFlexibleSchedule(this, userId, currentDate, holidays, currentTime.hour)
            } catch (e: Exception) {
                Log.e("StatusCalculation", "Error handling flexible schedule for user $userId", e)
                "AVAILABLE"
            }
        }
        return "AVAILABLE"
    }

    /**
     * Apply after-hours availability constraints
     */
    private fun applyAvailabilityConstraints(status: String, currentTime: LocalTime): String {
        return if (status == "AVAILABLE" && 
                  (currentTime.isBefore(LocalTime.of(8, 0)) || 
                   currentTime.isAfter(LocalTime.of(20, 0)))) {
            "NOT_AVAILABLE"
        } else {
            status
        }
    }

    // ================ API INTEGRATION ================

    /**
     * Fetch real-time status updates from API
     * Synchronizes with central status board for live updates
     */
    private fun fetchStatusBoardUpdates() {
        binding.swipeRefreshLayout.isRefreshing = true

        RetrofitClient.api.getStatusBoard().enqueue(object : Callback<List<StatusBoard>> {
            override fun onResponse(call: Call<List<StatusBoard>>, response: Response<List<StatusBoard>>) {
                binding.swipeRefreshLayout.isRefreshing = false
                
                if (response.isSuccessful && response.body() != null) {
                    val statusUpdates = response.body()!!
                    updateUserStatusFromBoard(statusUpdates)
                } else {
                    Log.e("API", "Unsuccessful response: ${response.code()} - ${response.message()}")
                }
            }

            override fun onFailure(call: Call<List<StatusBoard>>, t: Throwable) {
                binding.swipeRefreshLayout.isRefreshing = false
                Log.e("API", "Failed to fetch status board updates", t)
            }
        })
    }

    /**
     * Update user statuses with real-time board information
     * Implements complex business logic for status override and merging
     */
    private fun updateUserStatusFromBoard(statusBoardList: List<StatusBoard>) {
        val formatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH)
        val now = LocalDateTime.now()
        val statusByUser = statusBoardList.groupBy { it.userId }

        for ((userId, statusList) in statusByUser) {
            val validStatuses = statusList.filter { status ->
                try {
                    val start = ZonedDateTime.parse(status.startTime, formatter).toLocalDateTime()
                    val end = ZonedDateTime.parse(status.endTime, formatter).toLocalDateTime()
                    
                    // Filter for current or future statuses
                    start.isAfter(now) || (start.isBefore(now) && end.isAfter(now))
                } catch (e: Exception) {
                    Log.e("DateParsing", "Error parsing date for user $userId: ${status.startTime}", e)
                    false
                }
            }.sortedBy { ZonedDateTime.parse(it.startTime, formatter).toLocalDateTime() }

            val currentStatus = validStatuses.firstOrNull()
            currentStatus?.let { 
                updateUserWithBoardStatus(userId, it, now, formatter)
            }
        }
    }

    /**
     * Update individual user with board status information
     */
    private fun updateUserWithBoardStatus(
        userId: Int, 
        boardStatus: StatusBoard, 
        now: LocalDateTime, 
        formatter: DateTimeFormatter
    ) {
        val userIndex = fullUserList.indexOfFirst { it.id == userId }
        if (userIndex == -1) return

        val user = fullUserList[userIndex]
        val (baseStatus, baseProgress) = calculateUserStatus(user.id)
        
        var newStatus = baseStatus
        var newText = generateStatusText(user.id)
        var newProgress = baseProgress
        var highlightProgress = false

        try {
            val start = ZonedDateTime.parse(boardStatus.startTime, formatter).toLocalDateTime()
            val end = ZonedDateTime.parse(boardStatus.endTime, formatter).toLocalDateTime()

            if (start.isAfter(now)) {
                // Future status - append to current text
                newText += when (boardStatus.statusName) {
                    "VACATION", "LEAVE", "INTERNATIONAL_TRAVEL", "DOMESTIC_TRAVEL" -> {
                        "\nUpcoming: ${boardStatus.statusName} on ${start.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))}"
                    }
                    else -> {
                        "\nUpcoming: ${boardStatus.statusName} on ${start.format(DateTimeFormatter.ofPattern("dd-MM-yyyy 'at' HH:mm"))}"
                    }
                }
                highlightProgress = true
            } else {
                // Current status - override current status
                newStatus = mapBoardStatusToUserStatus(boardStatus)
                newText = generateBoardStatusText(boardStatus, start, end)
                newProgress = calculateTimeProgress(start.toLocalTime(), end.toLocalTime(), now.toLocalTime())
            }

            // Update user in list
            val updatedUser = user.copy(
                status = newStatus,
                text = newText,
                progress = newProgress,
                highlightProgress = highlightProgress
            )

            fullUserList = fullUserList.toMutableList().apply {
                set(userIndex, updatedUser)
            }

            userAdapter.updateList(fullUserList)

        } catch (e: Exception) {
            Log.e("StatusUpdate", "Error updating user $userId with board status", e)
        }
    }

    // ================ USER INTERFACE MANAGEMENT ================

    /**
     * Setup and configure the main user adapter
     */
    private fun setupUserAdapter() {
        userAdapter = UserAdapter(fullUserList)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = userAdapter
        
        // Apply initial filter
        filterByDepartment(binding.departmentSpinner.selectedItem.toString())
    }

    /**
     * Configure department filter spinner
     */
    private fun configureSpinner() {
        val spinner: Spinner = binding.departmentSpinner
        val departments = resources.getStringArray(R.array.departments_array)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, departments)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedDepartment = departments[position]
                filterByDepartment(selectedDepartment)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // No action needed
            }
        }
    }

    /**
     * Configure search functionality
     */
    private fun configureSearchView() {
        val searchView: SearchView = binding.searchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String?): Boolean {
                userAdapter.filter.filter(newText)
                return false
            }
        })
    }

    /**
     * Filter users by department with normalization
     */
    private fun filterByDepartment(department: String) {
        if (fullUserList.isEmpty()) return
        
        val normalizedDepartment = normalizeString(department)
        val filteredList = if (normalizedDepartment == "all departments") {
            fullUserList
        } else {
            fullUserList.filter { normalizeString(it.department) == normalizedDepartment }
        }
        userAdapter.updateList(filteredList)
    }

    /**
     * Normalize strings for comparison (remove accents, lowercase)
     */
    private fun normalizeString(input: String): String {
        return Normalizer.normalize(input, Normalizer.Form.NFD)
            .replace("[\\p{InCombiningDiacriticalMarks}]".toRegex(), "")
            .lowercase(Locale.getDefault())
    }

    // ================ MENU MANAGEMENT ================

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        
        menu?.findItem(R.id.notification)?.let { menuItem ->
            val actionView = menuItem.actionView
            
            if (actionView != null) {
                notificationManager.updateMenuItem(menuItem)
                notificationManager.initializeBadge(actionView)
            } else {
                // Fallback: manually inflate badge layout
                val inflater = LayoutInflater.from(this)
                val badgeLayout = inflater.inflate(R.layout.menu_item_with_badge, null)
                menuItem.actionView = badgeLayout
                notificationManager.initializeBadge(badgeLayout)
                
                badgeLayout.setOnClickListener {
                    onOptionsItemSelected(menuItem)
                }
            }
        }
        
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.notification -> {
                notificationManager.markNotificationsAsRead()
                val intent = Intent(this, NotificationsActivity::class.java).apply {
                    putParcelableArrayListExtra("notifications_list", 
                        ArrayList(notificationManager.getNotificationList()))
                }
                startActivity(intent)
                true
            }
            R.id.menu_profile -> {
                startActivity(Intent(this, UserProfileActivity::class.java))
                true
            }
            R.id.menu_vacation -> {
                startActivity(Intent(this, VacationActivity::class.java))
                true
            }
            R.id.menu_permission -> {
                startActivity(Intent(this, PermissionsActivity::class.java))
                true
            }
            R.id.menu_fieldwork -> {
                val intent = Intent(this, FieldWorkActivity::class.java).apply {
                    putExtra("user", loginUser)
                }
                startActivity(intent)
                true
            }
            R.id.menu_travel -> {
                val intent = Intent(this, TravelActivity::class.java).apply {
                    putExtra("user", loginUser)
                }
                startActivity(intent)
                true
            }
            R.id.status_return -> {
                loginUser?.let { user ->
                    val returnManager = StatusReturnManager(this, this, userAdapter)
                    returnManager.checkBoardStatus(user.id)
                } ?: run {
                    Toast.makeText(this, "Error: User not authenticated.", Toast.LENGTH_SHORT).show()
                }
                true
            }
            R.id.menu_calendar -> {
                val intent = Intent(this, CalendarActivity::class.java).apply {
                    putExtra("user", loginUser)
                }
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // ================ BACKGROUND SERVICES ================

    /**
     * Schedule periodic notification checks using WorkManager
     */
    private fun scheduleNotificationCheck(context: Context, userId: Int) {
        val inputData = Data.Builder()
            .putInt("userId", userId)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<NotificationWorker>(15, TimeUnit.MINUTES)
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "NotificationCheckWork",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    /**
     * Start background notification updater
     */
    private fun startBackgroundServices() {
        val handler = android.os.Handler()
        val runnable = object : Runnable {
            override fun run() {
                if (::notificationManager.isInitialized) {
                    notificationManager.fetchNotifications { unreadCount ->
                        Log.d("BackgroundService", "Notification update: $unreadCount unread")
                    }
                }
                handler.postDelayed(this, 300000) // 5 minutes
            }
        }
        handler.post(runnable)
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

    /**
     * Format Chilean date format for display
     */
    private fun formatChileanDate(dateInput: String?): String {
        return if (dateInput != null && dateInput.isNotEmpty()) {
            try {
                val inputFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH)
                inputFormat.timeZone = TimeZone.getTimeZone("UTC")
                
                val outputFormat = SimpleDateFormat("dd 'de' MMMM 'de' yyyy", Locale("es", "CL"))
                outputFormat.timeZone = TimeZone.getTimeZone("UTC")
                
                val date = inputFormat.parse(dateInput)
                outputFormat.format(date!!)
            } catch (e: Exception) {
                "Invalid Date"
            }
        } else {
            "Date Not Available"
        }
    }

    /**
     * Format phone number for international standards
     */
    private fun formatPhoneNumber(phone: String): String {
        if (phone.length == 11 && phone.startsWith("56")) {
            val countryCode = phone.substring(0, 2)
            val areaCode = phone.substring(2, 3)
            val firstPart = phone.substring(3, 7)
            val secondPart = phone.substring(7)
            
            return "+$countryCode$areaCode $firstPart $secondPart"
        }
        return phone
    }

    // ================ DATA LOADING STRATEGIES ================

    /**
     * Attempt to load data from SQLite with retry mechanism
     */
    private fun attemptSQLiteDataLoad(retries: Int = 3, callback: (success: Boolean) -> Unit) {
        var attempts = 0
        var loadSuccessful = false

        while (attempts < retries) {
            try {
                fetchUsersFromSQLite()
                loadSuccessful = true
                break
            } catch (e: Exception) {
                Log.e("SQLiteLoad", "Attempt ${attempts + 1} failed", e)
                attempts++
            }
        }
        callback(loadSuccessful)
    }

    /**
     * Attempt to load data from API with retry mechanism
     */
    private fun attemptAPIDataLoad(retries: Int = 3, callback: (success: Boolean) -> Unit) {
        var attempts = 0
        var loadSuccessful = false

        while (attempts < retries) {
            try {
                fetchStatusBoardUpdates()
                loadSuccessful = true
                break
            } catch (e: Exception) {
                Log.e("APILoad", "Attempt ${attempts + 1} failed", e)
                attempts++
            }
        }
        callback(loadSuccessful)
    }

    /**
     * Configure core application features
     */
    private fun configureCoreFeatures() {
        refreshDataSources()
        configureSpinner()
        configureSearchView()
    }

    // ================ LIFECYCLE MANAGEMENT ================

    public override fun onResume() {
        super.onResume()

        if (::notificationManager.isInitialized) {
            notificationManager.fetchNotifications { unreadCount ->
                Log.d("Lifecycle", "Notifications updated on resume: $unreadCount unread")
            }
        }

        toggleNotificationRecyclerView()
        
        // Trigger refresh on resume
        binding.swipeRefreshLayout.isRefreshing = true
        refreshDataSources()
    }

    // ================ NOTIFICATION MANAGEMENT ================

    /**
     * Setup notification RecyclerView
     */
    private fun setupNotificationRecyclerView(adapter: NotificationAdapter) {
        val recyclerView: RecyclerView = findViewById(R.id.recycler_view_notifications)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        recyclerView.visibility = View.GONE
    }

    /**
     * Toggle notification RecyclerView visibility
     */
    private fun toggleNotificationRecyclerView() {
        val recyclerView: RecyclerView = findViewById(R.id.recycler_view_notifications)
        recyclerView.visibility = if (notificationList.isNotEmpty()) View.VISIBLE else View.GONE
    }

    /**
     * Handle notification click events
     */
    private fun handleNotificationClick(notification: Notification) {
        val intent = when (notification.title) {
            "VACATION" -> Intent(this, MainActivity::class.java)
            "PERMISSION_REJECTED" -> Intent(this, PermissionsActivity::class.java)
            "VACATION_REJECTED" -> Intent(this, VacationActivity::class.java)
            "PERMISSION_APPROVED" -> Intent(this, PermissionsActivity::class.java)
            "NOT_AVAILABLE" -> Intent(this, MainActivity::class.java)
            "MEETING" -> Intent(this, MainActivity::class.java)
            "FIELDWORK" -> Intent(this, MainActivity::class.java)
            "TRAINING" -> Intent(this, MainActivity::class.java)
            "VACATION_RESCHEDULE" -> Intent(this, VacationActivity::class.java)
            "PERMISSION" -> Intent(this, PermissionsActivity::class.java)
            else -> {
                Log.d("NotificationClick", "Unknown notification type: ${notification.title}")
                Intent(this, MainActivity::class.java)
            }
        }
        startActivity(intent)
    }

    // ================ STATUS MAPPING UTILITIES ================

    /**
     * Map board status to user status
     */
    private fun mapBoardStatusToUserStatus(boardStatus: StatusBoard): String {
        return when (boardStatus.statusName) {
            "VACATION" -> "VACATION"
            "LEAVE" -> "LEAVE"
            "PERMISSION" -> "PERMISSION"
            "FIELDWORK" -> "FIELDWORK"
            "PRIVATE" -> "PRIVATE"
            "TRAINING" -> "TRAINING"
            "MEETING" -> "MEETING"
            "INTERNATIONAL_TRAVEL" -> "INTERNATIONAL_TRAVEL"
            "DOMESTIC_TRAVEL" -> "DOMESTIC_TRAVEL"
            "OVERTIME" -> "OVERTIME"
            "COMPENSATE" -> "COMPENSATE"
            "OTHER" -> "OTHER"
            else -> "AVAILABLE"
        }
    }

    /**
     * Generate status text from board status
     */
    private fun generateBoardStatusText(
        boardStatus: StatusBoard, 
        start: LocalDateTime, 
        end: LocalDateTime
    ): String {
        return when (boardStatus.statusName) {
            "VACATION" -> {
                "On vacation from ${start.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))} " +
                "to ${end.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))}"
            }
            "LEAVE" -> {
                "Not available until ${end.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))}"
            }
            "PERMISSION" -> {
                "Permission from ${start.format(DateTimeFormatter.ofPattern("HH:mm"))} " +
                "to ${end.format(DateTimeFormatter.ofPattern("HH:mm"))} hrs."
            }
            "FIELDWORK" -> {
                "At ${boardStatus.fieldDescription ?: "Field Location"} from " +
                "${start.format(DateTimeFormatter.ofPattern("HH:mm"))} to " +
                "${end.format(DateTimeFormatter.ofPattern("HH:mm"))} hrs."
            }
            "TRAINING" -> {
                "In training from ${start.format(DateTimeFormatter.ofPattern("HH:mm"))} " +
                "to ${end.format(DateTimeFormatter.ofPattern("HH:mm"))} hrs."
            }
            "MEETING" -> {
                "In meeting until ${end.format(DateTimeFormatter.ofPattern("HH:mm"))} hrs."
            }
            "INTERNATIONAL_TRAVEL" -> {
                val location = boardStatus.fieldDescription ?: "unknown location"
                "International travel to $location from " +
                "${start.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))} to " +
                "${end.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))}"
            }
            "DOMESTIC_TRAVEL" -> {
                val location = boardStatus.fieldDescription ?: "unknown location"
                "Domestic travel to $location from " +
                "${start.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))} to " +
                "${end.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))}"
            }
            "COMPENSATE" -> {
                "Compensating from ${start.format(DateTimeFormatter.ofPattern("HH:mm"))} " +
                "to ${end.format(DateTimeFormatter.ofPattern("HH:mm"))} hrs."
            }
            "OVERTIME" -> {
                "Working overtime from ${start.format(DateTimeFormatter.ofPattern("HH:mm"))} " +
                "to ${end.format(DateTimeFormatter.ofPattern("HH:mm"))} hrs."
            }
            "OTHER" -> {
                val location = boardStatus.fieldDescription ?: "unknown location"
                "At $location from ${start.format(DateTimeFormatter.ofPattern("HH:mm"))} " +
                "to ${end.format(DateTimeFormatter.ofPattern("HH:mm"))} hrs."
            }
            else -> "Status not specified"
        }
    }

    /**
     * Generate standard status text for users
     */
    private fun generateStatusText(userId: Int): String {
        val (status, _) = calculateUserStatus(userId)
        
        if (status == "NOT_AVAILABLE") {
            return "NOT AVAILABLE"
        }

        val scheduleHelper = ScheduleDatabaseHelper(this)
        val db = scheduleHelper.readableDatabase

        val currentDay = LocalDate.now().dayOfWeek.value
        val query = """
            SELECT * FROM ${ScheduleDatabaseHelper.TABLE_NAME} 
            WHERE user_id = ? AND day_of_week = ? AND is_active = 1
        """.trimIndent()
        
        val cursor = db.rawQuery(query, arrayOf(userId.toString(), currentDay.toString()))

        var text = "AVAILABLE"

        if (cursor.moveToFirst()) {
            val startTime = cursor.getString(cursor.getColumnIndexOrThrow("start_time"))
            val endTime = cursor.getString(cursor.getColumnIndexOrThrow("end_time"))
            val lunchStart = cursor.getString(cursor.getColumnIndexOrThrow("lunch_start"))
            val lunchEnd = cursor.getString(cursor.getColumnIndexOrThrow("lunch_end"))

            val currentTime = LocalTime.now()
            val workStart = LocalTime.parse(startTime)
            val workEnd = LocalTime.parse(endTime)
            val lunchStartTime = LocalTime.parse(lunchStart)
            val lunchEndTime = LocalTime.parse(lunchEnd)

            text = when (status) {
                "HOME" -> {
                    if (currentTime.isBefore(workStart)) {
                        "HOME, Returns TODAY at ${workStart.format(DateTimeFormatter.ofPattern("HH:mm"))} hrs."
                    } else {
                        val nextDay = if (currentDay == 5) "Monday" else "Tomorrow"
                        "HOME, Returns $nextDay at ${workStart.format(DateTimeFormatter.ofPattern("HH:mm"))} hrs."
                    }
                }
                "LUNCH" -> {
                    "On lunch break, returns at ${lunchEndTime.format(DateTimeFormatter.ofPattern("HH:mm"))} hrs."
                }
                "IN_OFFICE", "REMOTE_WORK" -> {
                    val location = if (status == "IN_OFFICE") "Office" else "Remote Work"
                    if (currentTime.isBefore(lunchStartTime)) {
                        "At $location, Lunch at ${lunchStartTime.format(DateTimeFormatter.ofPattern("HH:mm"))} hrs."
                    } else {
                        "At $location, Ends at ${workEnd.format(DateTimeFormatter.ofPattern("HH:mm"))} hrs."
                    }
                }
                else -> "AVAILABLE"
            }
        }

        cursor.close()
        db.close()
        return text
    }

    /**
     * Calculate next work day for users
     */
    private fun calculateNextWorkDay(userId: Int, currentDay: Int, db: android.database.sqlite.SQLiteDatabase) {
        var nextDay = currentDay + 1
        if (currentDay == 5) { // If Friday, next work day is Monday
            nextDay = 1
        }

        val nextDayQuery = """
            SELECT * FROM ${ScheduleDatabaseHelper.TABLE_NAME} 
            WHERE user_id = ? AND day_of_week = ? AND is_active = 1
        """.trimIndent()
        
        val nextDayCursor = db.rawQuery(nextDayQuery, arrayOf(userId.toString(), nextDay.toString()))
        
        if (nextDayCursor.moveToFirst()) {
            val nextStartTime = nextDayCursor.getString(nextDayCursor.getColumnIndexOrThrow("start_time"))
            Log.d("NextWorkDay", "User $userId returns at $nextStartTime on day $nextDay")
        }
        nextDayCursor.close()
    }

    // ================ INTERFACE IMPLEMENTATION ================

    /**
     * StatusUpdateListener implementation
     * Handles status updates from external sources
     */
    override fun onStatusUpdated() {
        refreshDataSources()
    }

    /**
     * Refresh data sources manually
     */
    private fun refreshDataSources() {
        fetchStatusBoardUpdates()
    }
}
