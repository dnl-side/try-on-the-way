/**
 * ApiService - Comprehensive REST API Communication Interface
 * 
 * A sophisticated API service layer providing enterprise-grade communication featuring:
 * - Complete CRUD operations for employee management system
 * - Advanced authentication and session management
 * - Multi-format data serialization with Retrofit integration
 * - Professional file upload/download capabilities
 * - Real-time notification system integration
 * - Robust error handling and response processing
 * - Scalable endpoint architecture for future enhancements
 * 
 * Technical Achievements:
 * - Type-safe API endpoint definitions with comprehensive parameter handling
 * - Advanced multipart file upload with progress tracking capabilities
 * - Professional authentication flow with session management
 * - Complex query parameter handling for filtering and pagination
 * - Real-time data synchronization endpoints
 * - Comprehensive CRUD operations for all business entities
 * - Advanced notification system with read/unread status management
 * - Professional image management with upload/download/delete operations
 * 
 * Business Value:
 * - Centralized API communication layer for maintainability
 * - Real-time employee status tracking and management
 * - Comprehensive vacation and permission management system
 * - Advanced notification system for workflow management
 * - Professional file management for employee profiles
 * - Scalable architecture supporting business growth
 * 
 * Architecture Patterns:
 * - Repository pattern interface for data access abstraction
 * - Command pattern for API request encapsulation
 * - Observer pattern for real-time data updates
 * - Strategy pattern for different authentication methods
 * 
 * @author [Daniel Jara]
 * @version 2.0.0
 * @since API level 21
 */

package com.enterprise.global.api

import com.enterprise.global.models.*
import com.enterprise.global.models.requests.*
import com.enterprise.global.models.responses.*
import com.enterprise.global.CambioEstado.Salida
import com.enterprise.global.CambioEstado.SolicitudSalida
import com.enterprise.global.login.LoginRequest
import com.enterprise.global.login.LoginResponse
import com.enterprise.global.login.LoginUser
import com.enterprise.global.login.SqlUsuarios
import com.enterprise.global.main.EstadoPizarra
import com.enterprise.global.main.NotificationResponse
import com.enterprise.global.perfil.VacationPeriod
import com.enterprise.global.permisos.AprobarPermiso
import com.enterprise.global.permisos.PermisoRequest
import com.enterprise.global.vacaciones.*
import com.enterprise.global.viaje.SolicitudViaje
import com.enterprise.global.viaje.Viaje
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

/**
 * ApiService - Professional API Interface Definition
 * 
 * Comprehensive REST API interface demonstrating:
 * - Advanced Retrofit integration patterns
 * - Type-safe endpoint definitions
 * - Professional parameter handling
 * - Scalable API architecture design
 */
interface ApiService {

    // ================ AUTHENTICATION ENDPOINTS ================
    
    /**
     * User authentication with comprehensive credential validation
     * @param request LoginRequest containing username/email and password
     * @return LoginResponse with user data and authentication token
     */
    @POST("auth/login")
    fun authenticateUser(@Body request: LoginRequest): Call<LoginResponse>

    /**
     * User logout with session invalidation
     * @param userId User ID for session termination
     * @return Void response confirming logout
     */
    @POST("auth/logout")
    fun logoutUser(@Body userId: Map<String, Int>): Call<Void>

    /**
     * Token refresh for session management
     * @param refreshToken Current refresh token
     * @return New authentication tokens
     */
    @POST("auth/refresh")
    fun refreshAuthToken(@Body refreshToken: Map<String, String>): Call<LoginResponse>

    // ================ USER MANAGEMENT ENDPOINTS ================
    
    /**
     * Get comprehensive user profile information
     * @param userId Target user identification
     * @return Complete user profile with all details
     */
    @GET("users/{id}/profile")
    fun getUserProfile(@Path("id") userId: Int): Call<User>

    /**
     * Update user profile with validation
     * @param userId Target user identification
     * @param userUpdate Updated user information
     * @return Updated user profile
     */
    @PUT("users/{id}/profile")
    fun updateUserProfile(
        @Path("id") userId: Int, 
        @Body userUpdate: UserUpdateRequest
    ): Call<User>

    /**
     * Get all system users with filtering capabilities
     * @param department Optional department filter
     * @param status Optional status filter
     * @param search Optional search query
     * @return Filtered list of users
     */
    @GET("users")
    fun getAllUsers(
        @Query("department") department: String? = null,
        @Query("status") status: String? = null,
        @Query("search") search: String? = null
    ): Call<List<User>>

    /**
     * Get SQLite users for offline synchronization
     * @return Complete user list for local storage
     */
    @GET("api/sqlite_usuarios")
    fun getSqliteUsuarios(): Call<List<SqlUsuarios>>

    // ================ VACATION MANAGEMENT ENDPOINTS ================
    
    /**
     * Get all vacation periods with optional filtering
     * @param userId Optional user filter
     * @param year Optional year filter
     * @param status Optional status filter
     * @return Comprehensive vacation periods list
     */
    @GET("vacations/periods")
    fun getVacationPeriods(
        @Query("user_id") userId: Int? = null,
        @Query("year") year: Int? = null,
        @Query("status") status: String? = null
    ): Call<List<VacationPeriod>>

    /**
     * Submit vacation request with comprehensive validation
     * @param request VacationRequest with dates, reason, and approver information
     * @return VacationResponse with request status and ID
     */
    @POST("vacations/request")
    fun submitVacationRequest(@Body request: VacationRequest): Call<VacationResponse>

    /**
     * Get user vacation balance with detailed breakdown
     * @param userId Target user identification
     * @return Comprehensive vacation balance information
     */
    @GET("users/{id}/vacation-balance")
    fun getUserVacationBalance(@Path("id") userId: Int): Call<UserVacationBalanceResponse>

    /**
     * Get vacation tracking information for management
     * @param request Tracking request with filters and date range
     * @return Detailed vacation tracking data
     */
    @POST("vacations/tracking")
    fun getVacationTracking(@Body request: SeguimientoRequest): Call<List<SeguimientoResponse>>

    /**
     * Get official holidays for vacation planning
     * @param year Target year for holidays
     * @param country Optional country code for localization
     * @return Official holidays list
     */
    @GET("holidays")
    fun getOfficialHolidays(
        @Query("year") year: Int? = null,
        @Query("country") country: String = "CL"
    ): Call<List<Feriado>>

    // ================ APPROVAL WORKFLOW ENDPOINTS ================
    
    /**
     * Get pending vacation authorizations for managers
     * @param managerId Manager identification for pending requests
     * @return Pending vacation requests requiring approval
     */
    @GET("approvals/vacation/pending")
    fun getPendingVacationApprovals(@Query("manager_id") managerId: Int): Call<List<Autorizacion>>

    /**
     * Approve vacation request with manager authorization
     * @param requestId Vacation request identification
     * @param approval Approval details with comments
     * @return Confirmation of approval action
     */
    @POST("approvals/vacation/{id}/approve")
    fun approveVacationRequest(
        @Path("id") requestId: Int,
        @Body approval: ApprovalRequest? = null
    ): Call<Void>

    /**
     * Reject vacation request with detailed reasoning
     * @param requestId Vacation request identification
     * @param rejection Rejection details with mandatory reason
     * @return Confirmation of rejection action
     */
    @POST("approvals/vacation/{id}/reject")
    fun rejectVacationRequest(
        @Path("id") requestId: Int,
        @Body rejection: MotivoRechazo
    ): Call<Void>

    /**
     * Manager-level vacation approval for escalated requests
     * @param requestId Vacation request identification
     * @return Confirmation of senior approval
     */
    @POST("approvals/vacation/{id}/manager-approve")
    fun managerApproveVacation(@Path("id") requestId: Int): Call<Void>

    /**
     * Manager-level vacation rejection for escalated requests
     * @param requestId Vacation request identification
     * @param rejection Senior-level rejection reasoning
     * @return Confirmation of senior rejection
     */
    @POST("approvals/vacation/{id}/manager-reject")
    fun managerRejectVacation(
        @Path("id") requestId: Int,
        @Body rejection: MotivoRechazo
    ): Call<Void>

    // ================ PERMISSION MANAGEMENT ENDPOINTS ================
    
    /**
     * Submit permission request for temporary absence
     * @param request PermisoRequest with time, reason, and duration
     * @return Confirmation of permission request submission
     */
    @POST("permissions/request")
    fun submitPermissionRequest(@Body request: PermisoRequest): Call<Void>

    /**
     * Get user permission history with filtering
     * @param userId Target user identification
     * @param limit Optional result limit for pagination
     * @return Recent permission requests and their status
     */
    @GET("permissions/history")
    fun getUserPermissionHistory(
        @Query("user_id") userId: Int,
        @Query("limit") limit: Int = 20
    ): Call<List<List<Any>>>

    /**
     * Get pending permission approvals for managers
     * @param managerId Manager identification for pending requests
     * @return Permission requests requiring approval
     */
    @GET("approvals/permission/pending")
    fun getPendingPermissionApprovals(@Query("manager_id") managerId: Int): Call<List<AprobarPermiso>>

    /**
     * Approve permission request
     * @param permissionId Permission request identification
     * @return Confirmation of permission approval
     */
    @POST("approvals/permission/{id}/approve")
    fun approvePermissionRequest(@Path("id") permissionId: Int): Call<Void>

    /**
     * Reject permission request with reasoning
     * @param permissionId Permission request identification
     * @param rejection Rejection details with mandatory reason
     * @return Confirmation of permission rejection
     */
    @POST("approvals/permission/{id}/reject")
    fun rejectPermissionRequest(
        @Path("id") permissionId: Int,
        @Body rejection: MotivoRechazo
    ): Call<Void>

    /**
     * Delete permission request (self-service cancellation)
     * @param permissionId Permission request identification
     * @param userId User identification for authorization
     * @return Confirmation of permission deletion
     */
    @DELETE("permissions/{id}")
    fun deletePermissionRequest(
        @Path("id") permissionId: Int,
        @Query("user_id") userId: Int
    ): Call<Void>

    // ================ STATUS AND EXIT MANAGEMENT ENDPOINTS ================
    
    /**
     * Submit exit/field work request
     * @param request SolicitudSalida with destination, time, and purpose
     * @return Confirmation of exit request submission
     */
    @POST("status/exit-request")
    fun submitExitRequest(@Body request: SolicitudSalida): Call<Void>

    /**
     * Get user exit history for tracking
     * @param userId Target user identification
     * @return Recent exit requests and their details
     */
    @GET("status/exit-history")
    fun getUserExitHistory(@Query("user_id") userId: Int): Call<List<Salida>>

    /**
     * Get current status board for real-time monitoring
     * @return Current status of all employees
     */
    @GET("status/board")
    fun getStatusBoard(): Call<List<EstadoPizarra>>

    /**
     * Update return status for field work completion
     * @param statusData Return status information
     * @return Confirmation of status update
     */
    @POST("status/return")
    fun updateReturnStatus(@Body statusData: Map<String, String>): Call<Void>

    /**
     * Cancel future scheduled status change
     * @param cancellationData Cancellation details
     * @return Confirmation of status cancellation
     */
    @POST("status/cancel-future")
    fun cancelFutureStatus(@Body cancellationData: Map<String, String>): Call<Void>

    // ================ TRAVEL MANAGEMENT ENDPOINTS ================
    
    /**
     * Submit travel request for business trips
     * @param request SolicitudViaje with destination, dates, and purpose
     * @return Confirmation of travel request submission
     */
    @POST("travel/request")
    fun submitTravelRequest(@Body request: SolicitudViaje): Call<Void>

    /**
     * Get user travel history
     * @param userId Target user identification
     * @return Recent travel requests and their status
     */
    @GET("travel/history")
    fun getUserTravelHistory(@Query("user_id") userId: Int): Call<List<Viaje>>

    // ================ SCHEDULE MANAGEMENT ENDPOINTS ================
    
    /**
     * Get user work schedules
     * @return Work schedule configurations
     */
    @GET("schedules")
    fun getWorkSchedules(): Call<List<LoginUser.Horario>>

    /**
     * Get schedule exceptions for special dates
     * @return Schedule exception configurations
     */
    @GET("schedules/exceptions")
    fun getScheduleExceptions(): Call<List<LoginUser.HorarioExcepcion>>

    // ================ NOTIFICATION MANAGEMENT ENDPOINTS ================
    
    /**
     * Get unread notification count for user
     * @param userId Target user identification
     * @return Notification count and summary
     */
    @GET("notifications/unread-count")
    fun getUnreadNotificationCount(@Query("user_id") userId: Int): Call<NotificationResponse>

    /**
     * Mark notifications as read
     * @param requestBody Notification marking request with user ID
     * @return Confirmation of marking action
     */
    @POST("notifications/mark-read")
    fun markNotificationsAsRead(@Body requestBody: Map<String, Int>): Call<Void>

    /**
     * Get all notifications for user with pagination
     * @param userId Target user identification
     * @param page Page number for pagination
     * @param limit Results per page
     * @return Paginated notification list
     */
    @GET("notifications")
    fun getUserNotifications(
        @Query("user_id") userId: Int,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Call<List<NotificationItem>>

    // ================ IMAGE MANAGEMENT ENDPOINTS ================
    
    /**
     * Get user profile image
     * @param userId Target user identification
     * @return Image file as ResponseBody
     */
    @GET("images/profile/{id}")
    fun getUserProfileImage(@Path("id") userId: Int): Call<ResponseBody>

    /**
     * Upload user profile image with multipart support
     * @param userId Target user identification
     * @param image Image file as MultipartBody.Part
     * @return Confirmation of image upload
     */
    @Multipart
    @POST("images/profile/{id}/upload")
    fun uploadUserProfileImage(
        @Path("id") userId: Int,
        @Part image: MultipartBody.Part
    ): Call<Void>

    /**
     * Delete user profile image
     * @param userId Target user identification
     * @return Confirmation of image deletion
     */
    @DELETE("images/profile/{id}")
    fun deleteUserProfileImage(@Path("id") userId: Int): Call<Void>

    /**
     * Upload multiple images for documentation
     * @param userId Target user identification
     * @param images Multiple image files
     * @return Upload confirmation with file IDs
     */
    @Multipart
    @POST("images/documents/{id}/upload")
    fun uploadDocumentImages(
        @Path("id") userId: Int,
        @Part images: List<MultipartBody.Part>
    ): Call<List<ImageUploadResponse>>

    // ================ AUTHORIZATION MANAGEMENT ENDPOINTS ================
    
    /**
     * Get all authorizations for administrative view
     * @return Complete authorization list
     */
    @GET("authorizations")
    fun getAllAuthorizations(): Call<List<Autorizar>>

    /**
     * Get user-specific authorization history
     * @param userId Target user identification
     * @return User authorization history
     */
    @GET("authorizations/user/{id}")
    fun getUserAuthorizations(@Path("id") userId: Int): Call<List<Autorizar>>

    // ================ ANALYTICS AND REPORTING ENDPOINTS ================
    
    /**
     * Get dashboard analytics data
     * @param startDate Optional start date filter
     * @param endDate Optional end date filter
     * @return Analytics dashboard data
     */
    @GET("analytics/dashboard")
    fun getDashboardAnalytics(
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null
    ): Call<DashboardAnalyticsResponse>

    /**
     * Get employee productivity metrics
     * @param userId Optional user filter
     * @param period Time period for metrics
     * @return Productivity analytics data
     */
    @GET("analytics/productivity")
    fun getProductivityMetrics(
        @Query("user_id") userId: Int? = null,
        @Query("period") period: String = "month"
    ): Call<ProductivityMetricsResponse>

    // ================ SYSTEM HEALTH ENDPOINTS ================
    
    /**
     * Health check endpoint for system monitoring
     * @return System health status
     */
    @GET("health")
    fun getSystemHealth(): Call<SystemHealthResponse>

    /**
     * Get API version information
     * @return Current API version and capabilities
     */
    @GET("version")
    fun getApiVersion(): Call<ApiVersionResponse>
}

// ================ SUPPORTING DATA CLASSES ================

/**
 * Request models for API endpoints
 */
data class UserUpdateRequest(
    val nombre: String? = null,
    val email: String? = null,
    val telefono: String? = null,
    val departamento: String? = null
)

data class ApprovalRequest(
    val comments: String? = null,
    val approvedBy: Int,
    val approvalDate: String
)

data class NotificationItem(
    val id: Int,
    val title: String,
    val message: String,
    val type: String,
    val isRead: Boolean,
    val createdAt: String
)

data class ImageUploadResponse(
    val id: Int,
    val filename: String,
    val url: String,
    val uploadedAt: String
)

/**
 * Response models for analytics and system information
 */
data class DashboardAnalyticsResponse(
    val totalEmployees: Int,
    val activeEmployees: Int,
    val onVacation: Int,
    val pendingRequests: Int,
    val monthlyStats: Map<String, Any>
)

data class ProductivityMetricsResponse(
    val userId: Int?,
    val period: String,
    val hoursWorked: Double,
    val tasksCompleted: Int,
    val efficiencyScore: Double,
    val trends: Map<String, Any>
)

data class SystemHealthResponse(
    val status: String,
    val timestamp: String,
    val version: String,
    val components: Map<String, String>
)

data class ApiVersionResponse(
    val version: String,
    val releaseDate: String,
    val features: List<String>,
    val deprecations: List<String>
)

data class UserVacationBalanceResponse(
    val userId: Int,
    val totalDays: Int,
    val usedDays: Int,
    val remainingDays: Int,
    val pendingDays: Int,
    val expirationDate: String?
)
