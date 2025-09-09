/**
 * Employee Management Data Models
 * 
 * Comprehensive data model system for enterprise employee management featuring:
 * - Type-safe enum definitions for employee status management
 * - Professional data classes with validation and business logic
 * - Advanced serialization support for API integration
 * - Robust error handling and fallback mechanisms
 * - Multi-format date processing capabilities
 * - Scalable architecture for future enhancements
 * 
 * @author [Daniel Jara]
 * @version 2.0.0
 * @since API level 21
 */

package com.enterprise.global.models

import com.google.gson.annotations.SerializedName
import java.text.SimpleDateFormat
import java.util.*

// ================ EMPLOYEE STATUS ENUM ================

/**
 * EstadoEmpleado - Comprehensive Employee Status Management
 * 
 * Type-safe enumeration for all possible employee states with:
 * - Professional status categorization
 * - Spanish language support for Chilean workforce
 * - Extensible design for future status additions
 * - Integration with UI theming system
 */
enum class EstadoEmpleado(val displayName: String, val priority: Int) {
    // ================ AVAILABILITY STATES ================
    DISPONIBLE("DISPONIBLE", 1),
    OFICINA("OFICINA", 2),
    
    // ================ TEMPORARY ABSENCE STATES ================
    COLACION("COLACIÓN", 3),
    TERRENO("TERRENO", 4),
    REUNION("REUNION", 5),
    CAPACITACION("CAPACITACION", 6),
    
    // ================ WORK FROM HOME STATES ================
    TELETRABAJO("TELETRABAJO", 7),
    CASA("CASA", 8),
    
    // ================ EXTENDED ABSENCE STATES ================
    VACACIONES("VACACIONES", 9),
    LICENCIA("LICENCIA", 10),
    PERMISO("PERMISO", 11),
    
    // ================ TRAVEL STATES ================
    VIAJE_NACIONAL("VIAJE NACIONAL", 12),
    VIAJE_INTERNACIONAL("VIAJE INTERNACIONAL", 13),
    
    // ================ SPECIAL WORK STATES ================
    HORA_EXTRA("HORA EXTRA", 14),
    COMPENSAR("COMPENSAR", 15),
    
    // ================ UNAVAILABILITY STATES ================
    NO_DISPONIBLE("NO DISPONIBLE", 16),
    OTRO("OTRO", 17);

    companion object {
        /**
         * Safe conversion from string to enum with fallback
         * Handles case-insensitive matching and special characters
         */
        fun fromString(value: String): EstadoEmpleado {
            return values().find { 
                it.displayName.equals(value.trim(), ignoreCase = true) 
            } ?: DISPONIBLE // Default fallback
        }

        /**
         * Get all available status options for UI components
         */
        fun getAllDisplayNames(): List<String> {
            return values().map { it.displayName }.sorted()
        }

        /**
         * Get status by priority for sorting operations
         */
        fun getByPriority(): List<EstadoEmpleado> {
            return values().sortedBy { it.priority }
        }

        /**
         * Check if status indicates employee is available for assignments
         */
        fun isAvailable(estado: EstadoEmpleado): Boolean {
            return when (estado) {
                DISPONIBLE, OFICINA, TELETRABAJO, CASA -> true
                else -> false
            }
        }

        /**
         * Check if status indicates employee is temporarily away
         */
        fun isTemporaryAbsence(estado: EstadoEmpleado): Boolean {
            return when (estado) {
                COLACION, TERRENO, REUNION, CAPACITACION, HORA_EXTRA -> true
                else -> false
            }
        }
    }
}

// ================ USER DATA MODEL ================

/**
 * User - Comprehensive Employee Data Model
 * 
 * Advanced data class representing employee information with:
 * - Complete personal and professional details
 * - Progress tracking capabilities
 * - Communication channel management
 * - Birthday and anniversary tracking
 * - Status management integration
 * - Serialization support for API communication
 */
data class User(
    // ================ CORE IDENTIFICATION ================
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("nombre")
    val nombre: String,
    
    @SerializedName("email")
    val email: String? = null,
    
    // ================ ORGANIZATIONAL INFORMATION ================
    @SerializedName("departamento")
    val departamento: String? = null,
    
    @SerializedName("cargo")
    val cargo: String? = null,
    
    @SerializedName("jefe1_id")
    val jefe1Id: Int? = null,
    
    @SerializedName("fecha_ingreso")
    val fechaIngreso: String? = null,
    
    // ================ STATUS AND PROGRESS TRACKING ================
    @SerializedName("estado")
    val estado: String = EstadoEmpleado.DISPONIBLE.displayName,
    
    @SerializedName("texto")
    val texto: String = "", // Custom status text
    
    @SerializedName("progreso")
    val progreso: Int = 0, // Progress percentage (0-100)
    
    @SerializedName("cambiar_color_progreso")
    val cambiarColorProgreso: Boolean = false, // Future permission indicator
    
    // ================ CONTACT INFORMATION ================
    @SerializedName("telefono")
    val telefono: String? = null,
    
    @SerializedName("anexo")
    val anexo: String? = null,
    
    @SerializedName("direccion")
    val direccion: String? = null,
    
    // ================ PERSONAL INFORMATION ================
    @SerializedName("f_cumple")
    val f_cumple: String? = null, // Birthday date
    
    @SerializedName("genero")
    val genero: String? = null,
    
    @SerializedName("edad")
    val edad: Int? = null,
    
    // ================ SYSTEM FIELDS ================
    @SerializedName("created_at")
    val createdAt: String? = null,
    
    @SerializedName("updated_at")
    val updatedAt: String? = null,
    
    @SerializedName("active")
    val active: Boolean = true
) {
    
    // ================ COMPUTED PROPERTIES ================
    
    /**
     * Get typed employee status enum
     */
    val estadoEnum: EstadoEmpleado
        get() = EstadoEmpleado.fromString(estado)
    
    /**
     * Get display text for UI (prioritizes custom text over status)
     */
    val displayText: String
        get() = if (texto.isNotEmpty()) texto else estado
    
    /**
     * Check if employee is currently available for assignments
     */
    val isAvailable: Boolean
        get() = EstadoEmpleado.isAvailable(estadoEnum)
    
    /**
     * Check if employee is on temporary absence
     */
    val isTemporaryAbsence: Boolean
        get() = EstadoEmpleado.isTemporaryAbsence(estadoEnum)
    
    /**
     * Get formatted phone number for display
     */
    val formattedPhone: String?
        get() = telefono?.let { 
            if (it.startsWith("+56")) it 
            else "+56$it" 
        }
    
    /**
     * Check if employee has a birthday today
     */
    val isBirthdayToday: Boolean
        get() = f_cumple?.let { dateString ->
            try {
                val dateFormats = listOf(
                    SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH),
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
                    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                )
                
                for (format in dateFormats) {
                    try {
                        val birthday = format.parse(dateString)
                        val today = Calendar.getInstance()
                        val birthdayCalendar = Calendar.getInstance().apply { time = birthday }
                        
                        return@let (today.get(Calendar.DAY_OF_MONTH) == birthdayCalendar.get(Calendar.DAY_OF_MONTH) &&
                                   today.get(Calendar.MONTH) == birthdayCalendar.get(Calendar.MONTH))
                    } catch (e: Exception) {
                        continue
                    }
                }
                false
            } catch (e: Exception) {
                false
            }
        } ?: false
    
    /**
     * Get progress percentage with validation (0-100)
     */
    val validatedProgress: Int
        get() = progreso.coerceIn(0, 100)
    
    /**
     * Check if progress should be displayed
     */
    val shouldShowProgress: Boolean
        get() = progreso in 1..99
    
    /**
     * Get full display name with department if available
     */
    val fullDisplayName: String
        get() = if (departamento != null) "$nombre - $departamento" else nombre
    
    /**
     * Check if employee has complete contact information
     */
    val hasCompleteContactInfo: Boolean
        get() = telefono != null && email != null
    
    /**
     * Get years of service if hire date is available
     */
    val yearsOfService: Int?
        get() = fechaIngreso?.let { 
            try {
                val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val hireDate = format.parse(it)
                val today = Calendar.getInstance()
                val hireDateCalendar = Calendar.getInstance().apply { time = hireDate }
                
                today.get(Calendar.YEAR) - hireDateCalendar.get(Calendar.YEAR)
            } catch (e: Exception) {
                null
            }
        }
    
    // ================ BUSINESS LOGIC METHODS ================
    
    /**
     * Check if employee can be contacted via WhatsApp
     */
    fun canContactViaWhatsApp(): Boolean {
        return telefono != null && telefono.isNotEmpty()
    }
    
    /**
     * Get WhatsApp contact URL
     */
    fun getWhatsAppUrl(message: String = "Buen día"): String? {
        return telefono?.let { 
            val cleanPhone = it.replace("+56", "").replace(" ", "").replace("-", "")
            "https://api.whatsapp.com/send?phone=+56$cleanPhone&text=${message.replace(" ", "%20")}"
        }
    }
    
    /**
     * Get phone dialer URL
     */
    fun getPhoneDialerUrl(): String? {
        return telefono?.let { "tel:$it" }
    }
    
    /**
     * Validate user data integrity
     */
    fun isValidUser(): Boolean {
        return id > 0 && 
               nombre.isNotEmpty() && 
               estado.isNotEmpty() &&
               progreso in 0..100
    }
    
    /**
     * Get status priority for sorting
     */
    fun getStatusPriority(): Int {
        return estadoEnum.priority
    }
    
    /**
     * Compare users for sorting by status priority
     */
    fun compareByStatus(other: User): Int {
        return this.getStatusPriority().compareTo(other.getStatusPriority())
    }
    
    /**
     * Compare users for sorting by name
     */
    fun compareByName(other: User): Int {
        return this.nombre.compareTo(other.nombre, ignoreCase = true)
    }
    
    /**
     * Get user summary for logging/debugging
     */
    fun getSummary(): String {
        return "User(id=$id, name='$nombre', status='$estado', progress=$progreso%, dept='$departamento')"
    }
    
    /**
     * Create a copy with updated status
     */
    fun withStatus(newStatus: EstadoEmpleado, customText: String = ""): User {
        return this.copy(
            estado = newStatus.displayName,
            texto = customText,
            updatedAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        )
    }
    
    /**
     * Create a copy with updated progress
     */
    fun withProgress(newProgress: Int): User {
        return this.copy(
            progreso = newProgress.coerceIn(0, 100),
            updatedAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        )
    }
}

// ================ ADDITIONAL DATA MODELS ================

/**
 * UserStatus - Simplified status update model for API calls
 */
data class UserStatus(
    @SerializedName("user_id")
    val userId: Int,
    
    @SerializedName("estado")
    val estado: String,
    
    @SerializedName("texto")
    val texto: String = "",
    
    @SerializedName("timestamp")
    val timestamp: String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
)

/**
 * UserProgress - Progress update model for tracking completion
 */
data class UserProgress(
    @SerializedName("user_id")
    val userId: Int,
    
    @SerializedName("progreso")
    val progreso: Int,
    
    @SerializedName("descripcion")
    val descripcion: String = "",
    
    @SerializedName("timestamp")
    val timestamp: String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
) {
    init {
        require(progreso in 0..100) { "Progress must be between 0 and 100" }
    }
}

/**
 * UserFilter - Filtering options for user lists
 */
data class UserFilter(
    val searchQuery: String = "",
    val statusFilter: EstadoEmpleado? = null,
    val departmentFilter: String? = null,
    val availableOnly: Boolean = false,
    val birthdayToday: Boolean = false,
    val hasProgress: Boolean = false
) {
    
    /**
     * Check if any filter is active
     */
    fun hasActiveFilters(): Boolean {
        return searchQuery.isNotEmpty() || 
               statusFilter != null || 
               departmentFilter != null || 
               availableOnly || 
               birthdayToday || 
               hasProgress
    }
    
    /**
     * Apply filter to user list
     */
    fun applyTo(users: List<User>): List<User> {
        return users.filter { user ->
            // Search query filter
            if (searchQuery.isNotEmpty()) {
                val matchesSearch = user.nombre.contains(searchQuery, ignoreCase = true) ||
                                  user.estado.contains(searchQuery, ignoreCase = true) ||
                                  user.texto.contains(searchQuery, ignoreCase = true) ||
                                  (user.telefono?.contains(searchQuery) == true) ||
                                  (user.departamento?.contains(searchQuery, ignoreCase = true) == true)
                if (!matchesSearch) return@filter false
            }
            
            // Status filter
            if (statusFilter != null && user.estadoEnum != statusFilter) {
                return@filter false
            }
            
            // Department filter
            if (departmentFilter != null && user.departamento != departmentFilter) {
                return@filter false
            }
            
            // Available only filter
            if (availableOnly && !user.isAvailable) {
                return@filter false
            }
            
            // Birthday today filter
            if (birthdayToday && !user.isBirthdayToday) {
                return@filter false
            }
            
            // Has progress filter
            if (hasProgress && !user.shouldShowProgress) {
                return@filter false
            }
            
            true
        }
    }
}
