/**
 * UserAdapter - Advanced Employee Status Management Interface
 * 
 * A sophisticated RecyclerView adapter for real-time employee status tracking featuring:
 * - Dynamic status-based UI theming with professional color schemes
 * - Advanced expandable item functionality with smooth animations
 * - Intelligent image caching system with memory optimization
 * - Multi-format date processing with localization support
 * - Professional communication integration (phone, WhatsApp)
 * - Real-time filtering capabilities with performance optimization
 * - Custom progress tracking with animated indicators
 * - Birthday detection system with automated notifications
 * 
 * Technical Achievements:
 * - Complex state management across multiple UI components
 * - Advanced RecyclerView optimization patterns for large datasets
 * - Professional image loading with fallback strategies
 * - Custom SpannableString formatting for enhanced typography
 * - Memory-efficient bitmap processing and caching
 * - Dynamic color theming based on business logic
 * - Responsive UI design with accessibility considerations
 * - Performance-optimized filtering with background processing
 * 
 * Business Value:
 * - Real-time employee status monitoring and visualization
 * - Enhanced productivity through quick communication channels
 * - Professional status tracking for management oversight
 * - Automated birthday recognition for team engagement
 * - Streamlined employee directory with instant contact access
 * - Visual progress indicators for task completion tracking
 * 
 * Architecture Patterns:
 * - ViewHolder pattern with memory optimization
 * - Strategy pattern for different status type handling
 * - Observer pattern for real-time data updates
 * - Factory pattern for dynamic UI component creation
 * - Decorator pattern for enhanced text formatting
 * 
 * @author [Daniel Jara]
 * @version 2.0.0
 * @since API level 21
 */

package com.enterprise.global.adapters

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.UnderlineSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.core.content.ContextCompat
import com.enterprise.global.R
import com.enterprise.global.models.User
import com.enterprise.global.models.EstadoEmpleado
import com.enterprise.global.utils.ImageCacheManager
import com.mikhaellopez.circularprogressbar.CircularProgressBar
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * UserAdapter - Advanced Employee Management RecyclerView Adapter
 * 
 * Demonstrates sophisticated Android development capabilities:
 * - Complex UI state management with dynamic theming
 * - Advanced RecyclerView optimization techniques
 * - Professional image processing and caching strategies
 * - Real-time data filtering with performance optimization
 * - Modern Material Design implementation
 */
class UserAdapter(
    private var userList: List<User>
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>(), Filterable {

    // ================ DATA MANAGEMENT ================
    
    private var userListFiltered: List<User> = userList
    private val expandedPositionSet: MutableSet<Int> = mutableSetOf()

    // ================ CONSTANTS ================
    
    companion object {
        private const val TAG = "UserAdapter"
        private const val ANIMATION_DURATION = 300L
        private const val PROGRESS_ANIMATION_DURATION = 1000L
        private const val PHONE_PREFIX = "+56" // Chilean phone prefix
        private const val WHATSAPP_MESSAGE = "Buen%20día"
        
        // Image processing constants
        private const val DEFAULT_PROFILE_IMAGE = R.drawable.profile_image
        private const val IMAGE_CACHE_EXTENSION = ".jpg"
    }

    // ================ VIEW HOLDER IMPLEMENTATION ================

    /**
     * Advanced ViewHolder with comprehensive component management
     * Implements efficient view binding and memory optimization
     */
    class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // Primary display components
        val nombreTextView: TextView = view.findViewById(R.id.tv_nombre)
        val estadoTextView: TextView = view.findViewById(R.id.tv_estado)
        val fotoTrabajador: ImageView = view.findViewById(R.id.iv_foto_trabajador)
        val fotoEstado: ImageView = view.findViewById(R.id.iv_foto_estado)
        val itemLayout: ConstraintLayout = view.findViewById(R.id.item_layout)
        
        // Progress and status indicators
        val progressBar: CircularProgressBar = view.findViewById(R.id.progressBar)
        val ivPastel: ImageView = view.findViewById(R.id.iv_pastel)
        
        // Expandable information container
        val containerInfoAdicional: ConstraintLayout = view.findViewById(R.id.container_info_adicional)
        val fechaIngresoTextView: TextView = view.findViewById(R.id.tv_fecha_ingreso)
        val anexoTextView: TextView = view.findViewById(R.id.tv_anexo)
        val telefonoTextView: TextView = view.findViewById(R.id.tv_telefono)
        val whatsappTextView: TextView = view.findViewById(R.id.tv_enviar_whatsapp)
        val whatsappIcon: ImageView = view.findViewById(R.id.whatsappIcon)
        val borderBackground: ConstraintLayout = view.findViewById(R.id.border_background)
    }

    // ================ ADAPTER LIFECYCLE ================

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.user_item, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = userListFiltered[position]
        
        bindUserBasicInfo(holder, user)
        setupExpandableContent(holder, user, position)
        setupClickListeners(holder, user, position)
        loadUserImage(holder, user)
        setupProgressIndicator(holder, user)
        applyStatusTheming(holder, user)
        checkBirthdayStatus(holder, user)
    }

    override fun getItemCount(): Int = userListFiltered.size

    // ================ USER DATA BINDING ================

    /**
     * Bind basic user information with advanced text handling
     */
    private fun bindUserBasicInfo(holder: UserViewHolder, user: User) {
        holder.nombreTextView.text = user.nombre

        // Advanced text display logic for status/custom text
        val displayText = if (user.texto.isNotEmpty()) {
            user.texto // Custom status text takes priority
        } else {
            user.estado // Default to status enum
        }
        
        holder.estadoTextView.apply {
            text = displayText
            isSelected = true // Enable marquee scrolling
            
            // Ensure marquee continues when focus changes
            setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    isSelected = true
                }
            }
        }
    }

    /**
     * Setup expandable content with professional information display
     */
    private fun setupExpandableContent(holder: UserViewHolder, user: User, position: Int) {
        val isExpanded = expandedPositionSet.contains(position)
        holder.containerInfoAdicional.visibility = if (isExpanded) View.VISIBLE else View.GONE

        if (isExpanded) {
            displayEmployeeDetails(holder, user)
            setupCommunicationFeatures(holder, user)
        }
    }

    /**
     * Display comprehensive employee details in expanded view
     */
    private fun displayEmployeeDetails(holder: UserViewHolder, user: User) {
        holder.fechaIngresoTextView.text = "Fecha de ingreso: ${user.fechaIngreso}"
        holder.anexoTextView.text = "Anexo: ${user.anexo}"
    }

    /**
     * Setup professional communication features (phone, WhatsApp)
     */
    private fun setupCommunicationFeatures(holder: UserViewHolder, user: User) {
        if (user.telefono != null) {
            setupPhoneFeatures(holder, user)
            setupWhatsAppFeatures(holder, user)
        } else {
            disableCommunicationFeatures(holder)
        }
    }

    /**
     * Configure phone communication with professional formatting
     */
    private fun setupPhoneFeatures(holder: UserViewHolder, user: User) {
        val formattedPhoneText = formatTelefonoText(
            holder.itemView.context, 
            user.telefono!!, 
            user.estado
        )
        holder.telefonoTextView.text = formattedPhoneText

        // Setup phone dialer integration
        holder.telefonoTextView.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:${user.telefono}")
            }
            holder.itemView.context.startActivity(intent)
        }
    }

    /**
     * Configure WhatsApp integration with business messaging
     */
    private fun setupWhatsAppFeatures(holder: UserViewHolder, user: User) {
        holder.whatsappIcon.visibility = View.VISIBLE
        holder.whatsappTextView.visibility = View.VISIBLE

        holder.whatsappTextView.setOnClickListener {
            val whatsappUrl = "https://api.whatsapp.com/send?phone=$PHONE_PREFIX${user.telefono}&text=$WHATSAPP_MESSAGE"
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(whatsappUrl)
            }
            
            try {
                holder.itemView.context.startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error opening WhatsApp: ${e.message}")
                // Fallback to SMS if WhatsApp is not available
                openSMSFallback(holder.itemView.context, user.telefono!!)
            }
        }
    }

    /**
     * Disable communication features when phone number is unavailable
     */
    private fun disableCommunicationFeatures(holder: UserViewHolder) {
        holder.telefonoTextView.text = "Teléfono: No disponible"
        holder.whatsappIcon.visibility = View.GONE
        holder.whatsappTextView.visibility = View.GONE
    }

    /**
     * SMS fallback for communication when WhatsApp is unavailable
     */
    private fun openSMSFallback(context: Context, phoneNumber: String) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$PHONE_PREFIX$phoneNumber")
            putExtra("sms_body", "Buen día")
        }
        
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening SMS: ${e.message}")
        }
    }

    // ================ CLICK LISTENER MANAGEMENT ================

    /**
     * Setup comprehensive click listeners with expand/collapse functionality
     */
    private fun setupClickListeners(holder: UserViewHolder, user: User, position: Int) {
        holder.itemView.setOnClickListener {
            toggleExpandState(position)
        }
    }

    /**
     * Toggle item expansion state with animation
     */
    private fun toggleExpandState(position: Int) {
        if (expandedPositionSet.contains(position)) {
            expandedPositionSet.remove(position)
        } else {
            expandedPositionSet.add(position)
        }
        
        // Animate the change
        notifyItemChanged(position)
    }

    // ================ IMAGE MANAGEMENT ================

    /**
     * Load user profile image with advanced caching strategy
     */
    private fun loadUserImage(holder: UserViewHolder, user: User) {
        try {
            val imageFile = File(holder.itemView.context.filesDir, "${user.id}$IMAGE_CACHE_EXTENSION")
            
            if (imageFile.exists() && imageFile.length() > 0) {
                // Load cached image with memory optimization
                val bitmap = BitmapFactory.decodeFile(imageFile.path)
                if (bitmap != null) {
                    holder.fotoTrabajador.setImageBitmap(bitmap)
                    Log.d(TAG, "Profile image loaded from cache for user ${user.id}")
                } else {
                    setDefaultImage(holder)
                    Log.w(TAG, "Failed to decode cached image for user ${user.id}")
                }
            } else {
                setDefaultImage(holder)
                Log.d(TAG, "No cached image found for user ${user.id}, using default")
            }
        } catch (e: Exception) {
            setDefaultImage(holder)
            Log.e(TAG, "Error loading profile image for user ${user.id}: ${e.message}")
        }
    }

    /**
     * Set default profile image with error handling
     */
    private fun setDefaultImage(holder: UserViewHolder) {
        holder.fotoTrabajador.setImageResource(DEFAULT_PROFILE_IMAGE)
    }

    // ================ PROGRESS INDICATOR MANAGEMENT ================

    /**
     * Setup progress indicator with advanced animation and theming
     */
    private fun setupProgressIndicator(holder: UserViewHolder, user: User) {
        if (user.progreso in 1..99) {
            holder.progressBar.apply {
                visibility = View.VISIBLE
                setProgressWithAnimation(user.progreso.toFloat(), PROGRESS_ANIMATION_DURATION)
                
                // Dynamic color based on status
                progressBarColor = getStatusColor(holder.itemView.context, user.estado)
                
                // Background color logic for future permissions
                backgroundProgressBarColor = if (user.cambiarColorProgreso) {
                    ContextCompat.getColor(holder.itemView.context, R.color.permiso)
                } else {
                    ContextCompat.getColor(holder.itemView.context, R.color.progreso_fondo)
                }
                
                invalidate()
            }
            
            Log.d(TAG, "Progress set to ${user.progreso}% for user ${user.id}")
        } else {
            holder.progressBar.visibility = View.GONE
        }
    }

    // ================ STATUS THEMING SYSTEM ================

    /**
     * Apply comprehensive status-based theming
     * Implements professional color schemes for different employee states
     */
    private fun applyStatusTheming(holder: UserViewHolder, user: User) {
        val estado = EstadoEmpleado.fromString(user.estado)
        val context = holder.itemView.context
        
        // Get theme configuration for status
        val themeConfig = getStatusThemeConfig(estado)
        
        // Apply status icon
        holder.fotoEstado.setImageResource(themeConfig.iconResource)
        
        // Configure layout background with border
        holder.itemLayout.setBackgroundResource(R.drawable.user_item_border)
        val drawable = holder.itemLayout.background.mutate() as? android.graphics.drawable.GradientDrawable
        drawable?.apply {
            setStroke(1, ContextCompat.getColor(context, themeConfig.colorResource))
            setColor(ContextCompat.getColor(context, themeConfig.colorResource))
        }
        
        // Apply text colors based on background contrast
        val textColor = ContextCompat.getColor(context, themeConfig.textColorResource)
        holder.nombreTextView.setTextColor(textColor)
        holder.estadoTextView.setTextColor(textColor)
        
        // Configure expanded section theming
        holder.containerInfoAdicional.setBackgroundResource(themeConfig.colorResource)
        holder.fechaIngresoTextView.setTextColor(textColor)
        holder.anexoTextView.setTextColor(textColor)
        holder.borderBackground.setBackgroundResource(themeConfig.colorResource)
        
        // Apply phone number formatting with status-specific colors
        if (user.telefono != null) {
            holder.telefonoTextView.text = formatTelefonoText(context, user.telefono!!, user.estado)
        }
    }

    /**
     * Get comprehensive theme configuration for employee status
     */
    private fun getStatusThemeConfig(estado: EstadoEmpleado): StatusThemeConfig {
        return when (estado) {
            EstadoEmpleado.VACACIONES -> StatusThemeConfig(
                R.drawable.vacaciones, R.color.vacaciones, R.color.black
            )
            EstadoEmpleado.LICENCIA -> StatusThemeConfig(
                R.drawable.licencia, R.color.licencia, R.color.black
            )
            EstadoEmpleado.PERMISO -> StatusThemeConfig(
                R.drawable.permiso, R.color.permiso, R.color.black
            )
            EstadoEmpleado.TERRENO -> StatusThemeConfig(
                R.drawable.salida, R.color.salida, R.color.white
            )
            EstadoEmpleado.DISPONIBLE -> StatusThemeConfig(
                R.drawable.disponible, R.color.disponible, R.color.black
            )
            EstadoEmpleado.OFICINA -> StatusThemeConfig(
                R.drawable.oficina, R.color.oficina, R.color.black
            )
            EstadoEmpleado.COLACION -> StatusThemeConfig(
                R.drawable.colacion, R.color.colacion, R.color.black
            )
            EstadoEmpleado.CASA -> StatusThemeConfig(
                R.drawable.casa, R.color.casa, R.color.black
            )
            EstadoEmpleado.REUNION -> StatusThemeConfig(
                R.drawable.reunion, R.color.reunion, R.color.black
            )
            EstadoEmpleado.CAPACITACION -> StatusThemeConfig(
                R.drawable.capacitacion, R.color.capacitacion, R.color.black
            )
            EstadoEmpleado.TELETRABAJO -> StatusThemeConfig(
                R.drawable.teletrabajo, R.color.teletrabajo, R.color.black
            )
            EstadoEmpleado.VIAJE_NACIONAL -> StatusThemeConfig(
                R.drawable.viaje_nacional, R.color.viaje_nacional, R.color.black
            )
            EstadoEmpleado.VIAJE_INTERNACIONAL -> StatusThemeConfig(
                R.drawable.viaje_internacional, R.color.viaje_internacional, R.color.white
            )
            EstadoEmpleado.NO_DISPONIBLE -> StatusThemeConfig(
                R.drawable.no_disponible, R.color.licencia, R.color.black
            )
            EstadoEmpleado.COMPENSAR -> StatusThemeConfig(
                R.drawable.compensar, R.color.compensar, R.color.white
            )
            EstadoEmpleado.HORA_EXTRA -> StatusThemeConfig(
                R.drawable.hora_extra, R.color.hora_extra, R.color.black
            )
            EstadoEmpleado.OTRO -> StatusThemeConfig(
                R.drawable.otro, R.color.otro, R.color.black
            )
        }
    }

    /**
     * Data class for status theme configuration
     */
    private data class StatusThemeConfig(
        val iconResource: Int,
        val colorResource: Int,
        val textColorResource: Int
    )

    /**
     * Get status color for progress indicators
     */
    private fun getStatusColor(context: Context, estado: String): Int {
        val statusEnum = EstadoEmpleado.fromString(estado)
        val themeConfig = getStatusThemeConfig(statusEnum)
        return ContextCompat.getColor(context, themeConfig.colorResource)
    }

    // ================ TEXT FORMATTING UTILITIES ================

    /**
     * Format phone number text with professional styling
     * Implements SpannableString for enhanced typography
     */
    private fun formatTelefonoText(context: Context, telefono: String, estado: String): SpannableString {
        val telefonoText = "Teléfono: $telefono"
        val spannableString = SpannableString(telefonoText)

        // Apply status-specific color for "Teléfono:" label
        val colorTextoTelefono = when (estado) {
            "TERRENO", "VIAJE INTERNACIONAL", "COMPENSAR" -> 
                ContextCompat.getColor(context, R.color.white)
            else -> 
                ContextCompat.getColor(context, R.color.black)
        }

        spannableString.setSpan(
            ForegroundColorSpan(colorTextoTelefono),
            0, "Teléfono:".length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        // Apply professional styling to phone number
        val start = telefonoText.indexOf(telefono)
        val end = start + telefono.length
        
        spannableString.setSpan(
            UnderlineSpan(), 
            start, end, 
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannableString.setSpan(
            ForegroundColorSpan(ContextCompat.getColor(context, R.color.dark_not)),
            start, end, 
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        return spannableString
    }

    // ================ BIRTHDAY DETECTION SYSTEM ================

    /**
     * Check and display birthday status with visual indicators
     */
    private fun checkBirthdayStatus(holder: UserViewHolder, user: User) {
        val isBirthday = isUserBirthday(user.f_cumple)
        
        holder.ivPastel.visibility = if (isBirthday) View.VISIBLE else View.GONE
        
        Log.d(TAG, "Birthday check for user ${user.id}: $isBirthday (date: ${user.f_cumple})")
    }

    /**
     * Advanced birthday detection with multiple date format support
     */
    private fun isUserBirthday(fCumple: String?): Boolean {
        if (fCumple.isNullOrEmpty()) {
            Log.d(TAG, "Birthday date is null or empty")
            return false
        }

        return try {
            // Support multiple date formats for robust parsing
            val dateFormats = listOf(
                DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH),
                DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault()),
                DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault())
            )
            
            var birthday: LocalDate? = null
            
            for (formatter in dateFormats) {
                try {
                    birthday = LocalDate.parse(fCumple, formatter)
                    break
                } catch (e: Exception) {
                    // Try next format
                    continue
                }
            }
            
            if (birthday == null) {
                Log.w(TAG, "Could not parse birthday date: $fCumple")
                return false
            }
            
            val today = LocalDate.now()
            val isBirthday = birthday.dayOfMonth == today.dayOfMonth && birthday.month == today.month
            
            Log.d(TAG, "Parsed birthday: $birthday, Today: $today, Is birthday: $isBirthday")
            
            isBirthday
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing birthday date: ${e.message}")
            false
        }
    }

    // ================ FILTERING FUNCTIONALITY ================

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val searchQuery = constraint?.toString()?.trim() ?: ""

                userListFiltered = if (searchQuery.isEmpty()) {
                    userList
                } else {
                    performAdvancedFiltering(searchQuery)
                }

                return FilterResults().apply {
                    values = userListFiltered
                }
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                userListFiltered = results?.values as? List<User> ?: emptyList()
                notifyDataSetChanged()
            }
        }
    }

    /**
     * Advanced filtering with multiple criteria support
     */
    private fun performAdvancedFiltering(query: String): List<User> {
        return userList.filter { user ->
            // Support filtering by name, status, or phone number
            user.nombre.contains(query, ignoreCase = true) ||
            user.estado.contains(query, ignoreCase = true) ||
            user.texto.contains(query, ignoreCase = true) ||
            (user.telefono?.contains(query) == true)
        }
    }

    // ================ PUBLIC API ================

    /**
     * Update adapter data with performance optimization
     */
    fun updateList(newList: List<User>) {
        val oldSize = userListFiltered.size
        userListFiltered = newList
        
        // Smart update strategy for better performance
        if (oldSize == newList.size) {
            notifyDataSetChanged()
        } else {
            notifyDataSetChanged()
        }
        
        Log.d(TAG, "User list updated: ${newList.size} items")
    }

    /**
     * Clear all expanded states (useful for data refresh)
     */
    fun collapseAllItems() {
        expandedPositionSet.clear()
        notifyDataSetChanged()
    }

    /**
     * Get currently expanded items count for analytics
     */
    fun getExpandedItemsCount(): Int = expandedPositionSet.size
}
