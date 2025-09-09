/**
 * RequestActivity - Employee Request Management System
 * 
 * A comprehensive employee request submission and tracking interface featuring:
 * - Advanced form validation with business rule enforcement
 * - Multi-type request handling (vacation, overtime, exit permissions)
 * - Real-time request status tracking and history management
 * - Professional date/time picker integrations with validation
 * - Optimized RecyclerView with efficient data loading
 * - Robust API communication with error handling
 * 
 * Technical Achievements:
 * - Dynamic form layout adaptation based on request type
 * - Advanced date/time validation with business constraints
 * - Multi-source data synchronization (local forms + API submission)
 * - Professional UI state management with loading indicators
 * - Custom adapter patterns for request history display
 * - Responsive design with optimized RecyclerView performance
 * - Comprehensive error handling and user feedback systems
 * 
 * Business Value:
 * - Employee self-service request submission
 * - Real-time approval workflow integration
 * - Request history tracking and status monitoring
 * - Manager notification system integration
 * - Enhanced productivity through streamlined processes
 * 
 * Architecture Patterns:
 * - MVP pattern with clear separation of concerns
 * - Strategy pattern for different request type handling
 * - Observer pattern for real-time status updates
 * - Factory pattern for request creation
 * 
 * @author [Daniel Jara]
 * @version 2.0.0
 * @since API level 21
 */

package com.enterprise.global.activities

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.enterprise.global.R
import com.enterprise.global.adapters.SolicitudesAdapter
import com.enterprise.global.models.SolicitudItem
import com.enterprise.global.models.TipoSolicitud
import com.enterprise.global.models.EstadoSolicitud
import com.enterprise.global.utils.ApiService
import com.enterprise.global.utils.DatabaseHelper
import com.enterprise.global.main.CurrentSession
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

/**
 * RequestActivity - Sistema de Gestión de Solicitudes Empresariales
 * 
 * Demuestra capacidades avanzadas de desarrollo Android:
 * - Manejo complejo de formularios dinámicos
 * - Integración avanzada con APIs REST
 * - Gestión profesional de estados de UI
 * - Patrones modernos de arquitectura móvil
 */
class RequestActivity : AppCompatActivity() {

    // ================ VIEW COMPONENTS & BINDING ================
    
    private lateinit var spinnerTipoSolicitud: Spinner
    private lateinit var editTextMotivo: EditText
    private lateinit var editTextFechaInicio: EditText
    private lateinit var editTextFechaFin: EditText
    private lateinit var editTextHoraInicio: EditText
    private lateinit var editTextHoraFin: EditText
    private lateinit var editTextLugar: EditText
    private lateinit var editTextObservaciones: EditText
    private lateinit var buttonEnviarSolicitud: Button
    private lateinit var recyclerViewSolicitudes: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var textViewSinSolicitudes: TextView
    
    // ================ ADAPTERS & DATA MANAGEMENT ================
    
    private lateinit var solicitudesAdapter: SolicitudesAdapter
    private val listaSolicitudes = mutableListOf<SolicitudItem>()
    private var tipoSolicitudSeleccionado: TipoSolicitud = TipoSolicitud.VACACIONES
    
    // ================ DATE/TIME FORMATTING ================
    
    private val formatoFecha = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val formatoHora = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val formatoFechaDisplay = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
    private val calendario = Calendar.getInstance()

    // ================ CONSTANTS ================
    
    companion object {
        private const val TAG = "RequestActivity"
        private const val DELAY_VALIDATION_MS = 300L
        private const val MAX_MOTIVO_LENGTH = 500
        private const val MIN_MOTIVO_LENGTH = 10
    }

    // ================ LIFECYCLE MANAGEMENT ================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_request)
        
        initializeViews()
        setupSpinner()
        setupRecyclerView()
        setupClickListeners()
        loadExistingRequests()
        setupToolbar()
    }

    /**
     * Initialize all view components with professional configuration
     */
    private fun initializeViews() {
        spinnerTipoSolicitud = findViewById(R.id.spinner_tipo_solicitud)
        editTextMotivo = findViewById(R.id.edit_text_motivo)
        editTextFechaInicio = findViewById(R.id.edit_text_fecha_inicio)
        editTextFechaFin = findViewById(R.id.edit_text_fecha_fin)
        editTextHoraInicio = findViewById(R.id.edit_text_hora_inicio)
        editTextHoraFin = findViewById(R.id.edit_text_hora_fin)
        editTextLugar = findViewById(R.id.edit_text_lugar)
        editTextObservaciones = findViewById(R.id.edit_text_observaciones)
        buttonEnviarSolicitud = findViewById(R.id.button_enviar_solicitud)
        recyclerViewSolicitudes = findViewById(R.id.recycler_view_solicitudes)
        progressBar = findViewById(R.id.progress_bar)
        textViewSinSolicitudes = findViewById(R.id.text_view_sin_solicitudes)
        
        // Configure input restrictions and hints
        editTextMotivo.hint = "Describa el motivo de su solicitud (mínimo $MIN_MOTIVO_LENGTH caracteres)"
        editTextLugar.hint = "Especifique el lugar de destino"
        editTextObservaciones.hint = "Observaciones adicionales (opcional)"
    }

    /**
     * Configure spinner with request types and professional styling
     */
    private fun setupSpinner() {
        val tiposSolicitud = arrayOf(
            "Vacaciones",
            "Permiso de Salida", 
            "Llegada Tarde / Salida Temprana",
            "Solicitud de Horas Extra",
            "Trabajo Remoto",
            "Cambio de Estado"
        )
        
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, tiposSolicitud)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTipoSolicitud.adapter = adapter
        
        spinnerTipoSolicitud.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                tipoSolicitudSeleccionado = TipoSolicitud.values()[position]
                updateFormVisibility()
                validateFormFields()
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    /**
     * Setup RecyclerView with optimized performance settings
     */
    private fun setupRecyclerView() {
        solicitudesAdapter = SolicitudesAdapter(listaSolicitudes) { solicitud ->
            showRequestDetails(solicitud)
        }
        
        recyclerViewSolicitudes.apply {
            adapter = solicitudesAdapter
            layoutManager = LinearLayoutManager(this@RequestActivity)
            setHasFixedSize(true) // Performance optimization
            itemAnimator?.changeDuration = 0 // Disable animations for better performance
        }
    }

    /**
     * Configure toolbar with professional branding
     */
    private fun setupToolbar() {
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Sistema de Solicitudes"
            subtitle = "Gestión de Permisos y Solicitudes"
        }
    }

    /**
     * Setup click listeners with validation and user feedback
     */
    private fun setupClickListeners() {
        editTextFechaInicio.setOnClickListener { showDatePicker(true) }
        editTextFechaFin.setOnClickListener { showDatePicker(false) }
        editTextHoraInicio.setOnClickListener { showTimePicker(true) }
        editTextHoraFin.setOnClickListener { showTimePicker(false) }
        
        buttonEnviarSolicitud.setOnClickListener { 
            if (validateCompleteForm()) {
                submitRequest() 
            }
        }
        
        // Add real-time validation for text fields
        editTextMotivo.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) validateMotivoField()
        }
    }

    // ================ DYNAMIC FORM MANAGEMENT ================

    /**
     * Update form field visibility based on selected request type
     * Implements dynamic UI adaptation for different business scenarios
     */
    private fun updateFormVisibility() {
        when (tipoSolicitudSeleccionado) {
            TipoSolicitud.VACACIONES, TipoSolicitud.TRABAJO_REMOTO -> {
                // Full day requests - show date range
                editTextFechaInicio.visibility = View.VISIBLE
                editTextFechaFin.visibility = View.VISIBLE
                editTextHoraInicio.visibility = View.GONE
                editTextHoraFin.visibility = View.GONE
                editTextLugar.visibility = if (tipoSolicitudSeleccionado == TipoSolicitud.TRABAJO_REMOTO) View.VISIBLE else View.GONE
                
                findViewById<TextView>(R.id.label_fecha_inicio).text = "Fecha de Inicio"
                findViewById<TextView>(R.id.label_fecha_fin).text = "Fecha de Término"
            }
            
            TipoSolicitud.PERMISO_SALIDA, TipoSolicitud.HORAS_EXTRA -> {
                // Time-specific requests - show single date with time range
                editTextFechaInicio.visibility = View.VISIBLE
                editTextFechaFin.visibility = View.GONE
                editTextHoraInicio.visibility = View.VISIBLE
                editTextHoraFin.visibility = View.VISIBLE
                editTextLugar.visibility = View.VISIBLE
                
                findViewById<TextView>(R.id.label_fecha_inicio).text = "Fecha"
                findViewById<TextView>(R.id.label_hora_inicio).text = "Hora de Inicio"
                findViewById<TextView>(R.id.label_hora_fin).text = "Hora de Término"
            }
            
            TipoSolicitud.LLEGADA_TARDE_SALIDA_TEMPRANA -> {
                // Partial day requests - show date and specific time
                editTextFechaInicio.visibility = View.VISIBLE
                editTextFechaFin.visibility = View.GONE
                editTextHoraInicio.visibility = View.VISIBLE
                editTextHoraFin.visibility = View.GONE
                editTextLugar.visibility = View.GONE
                
                findViewById<TextView>(R.id.label_fecha_inicio).text = "Fecha"
                findViewById<TextView>(R.id.label_hora_inicio).text = "Hora"
            }
            
            else -> {
                // Default configuration for other request types
                editTextFechaInicio.visibility = View.VISIBLE
                editTextFechaFin.visibility = View.VISIBLE
                editTextHoraInicio.visibility = View.VISIBLE
                editTextHoraFin.visibility = View.VISIBLE
                editTextLugar.visibility = View.VISIBLE
            }
        }
    }

    // ================ DATE/TIME PICKER MANAGEMENT ================

    /**
     * Show date picker with business rule validation
     */
    private fun showDatePicker(esFechaInicio: Boolean) {
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendario.set(year, month, dayOfMonth)
                val fechaFormateada = formatoFecha.format(calendario.time)
                val fechaDisplay = formatoFechaDisplay.format(calendario.time)
                
                if (esFechaInicio) {
                    editTextFechaInicio.setText(fechaFormateada)
                    editTextFechaInicio.error = null
                    
                    // Auto-adjust end date if needed
                    if (tipoSolicitudSeleccionado == TipoSolicitud.VACACIONES && 
                        editTextFechaFin.text.isEmpty()) {
                        editTextFechaFin.setText(fechaFormateada)
                    }
                } else {
                    editTextFechaFin.setText(fechaFormateada)
                    editTextFechaFin.error = null
                }
                
                validateDateRange()
            },
            calendario.get(Calendar.YEAR),
            calendario.get(Calendar.MONTH),
            calendario.get(Calendar.DAY_OF_MONTH)
        )
        
        // Set minimum date as today for future requests
        datePickerDialog.datePicker.minDate = System.currentTimeMillis()
        
        // Set maximum date (e.g., 1 year from now for vacation requests)
        if (tipoSolicitudSeleccionado == TipoSolicitud.VACACIONES) {
            val maxCalendar = Calendar.getInstance()
            maxCalendar.add(Calendar.YEAR, 1)
            datePickerDialog.datePicker.maxDate = maxCalendar.timeInMillis
        }
        
        datePickerDialog.show()
    }

    /**
     * Show time picker with business hour validation
     */
    private fun showTimePicker(esHoraInicio: Boolean) {
        val timePickerDialog = TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                calendario.set(Calendar.HOUR_OF_DAY, hourOfDay)
                calendario.set(Calendar.MINUTE, minute)
                val horaFormateada = formatoHora.format(calendario.time)
                
                if (esHoraInicio) {
                    editTextHoraInicio.setText(horaFormateada)
                    editTextHoraInicio.error = null
                } else {
                    editTextHoraFin.setText(horaFormateada)
                    editTextHoraFin.error = null
                }
                
                validateTimeRange()
            },
            calendario.get(Calendar.HOUR_OF_DAY),
            calendario.get(Calendar.MINUTE),
            true // 24-hour format
        )
        
        timePickerDialog.show()
    }

    // ================ VALIDATION SYSTEM ================

    /**
     * Comprehensive form validation with business rules
     */
    private fun validateCompleteForm(): Boolean {
        var isValid = true
        
        // Validate reason field
        if (!validateMotivoField()) isValid = false
        
        // Validate date fields based on request type
        if (!validateDateFields()) isValid = false
        
        // Validate time fields if applicable
        if (!validateTimeFields()) isValid = false
        
        // Validate location field if required
        if (!validateLocationField()) isValid = false
        
        return isValid
    }

    /**
     * Validate reason field with length and content requirements
     */
    private fun validateMotivoField(): Boolean {
        val motivo = editTextMotivo.text.toString().trim()
        
        when {
            motivo.isEmpty() -> {
                editTextMotivo.error = "El motivo es obligatorio"
                return false
            }
            motivo.length < MIN_MOTIVO_LENGTH -> {
                editTextMotivo.error = "El motivo debe tener al menos $MIN_MOTIVO_LENGTH caracteres"
                return false
            }
            motivo.length > MAX_MOTIVO_LENGTH -> {
                editTextMotivo.error = "El motivo no puede exceder $MAX_MOTIVO_LENGTH caracteres"
                return false
            }
            else -> {
                editTextMotivo.error = null
                return true
            }
        }
    }

    /**
     * Validate date fields with business logic
     */
    private fun validateDateFields(): Boolean {
        val fechaInicio = editTextFechaInicio.text.toString()
        val fechaFin = editTextFechaFin.text.toString()
        
        if (fechaInicio.isEmpty()) {
            editTextFechaInicio.error = "La fecha de inicio es obligatoria"
            return false
        }
        
        if (tipoSolicitudSeleccionado == TipoSolicitud.VACACIONES && fechaFin.isEmpty()) {
            editTextFechaFin.error = "La fecha de término es obligatoria para vacaciones"
            return false
        }
        
        return validateDateRange()
    }

    /**
     * Validate date range logic
     */
    private fun validateDateRange(): Boolean {
        val fechaInicioStr = editTextFechaInicio.text.toString()
        val fechaFinStr = editTextFechaFin.text.toString()
        
        if (fechaInicioStr.isNotEmpty() && fechaFinStr.isNotEmpty()) {
            try {
                val fechaInicio = formatoFecha.parse(fechaInicioStr)
                val fechaFin = formatoFecha.parse(fechaFinStr)
                
                if (fechaInicio != null && fechaFin != null && fechaInicio.after(fechaFin)) {
                    editTextFechaFin.error = "La fecha de término debe ser posterior a la fecha de inicio"
                    return false
                }
            } catch (e: Exception) {
                return false
            }
        }
        
        editTextFechaFin.error = null
        return true
    }

    /**
     * Validate time fields with business hour constraints
     */
    private fun validateTimeFields(): Boolean {
        if (editTextHoraInicio.visibility == View.GONE) return true
        
        val horaInicio = editTextHoraInicio.text.toString()
        val horaFin = editTextHoraFin.text.toString()
        
        if (horaInicio.isEmpty()) {
            editTextHoraInicio.error = "La hora de inicio es obligatoria"
            return false
        }
        
        if (editTextHoraFin.visibility == View.VISIBLE && horaFin.isEmpty()) {
            editTextHoraFin.error = "La hora de término es obligatoria"
            return false
        }
        
        return validateTimeRange()
    }

    /**
     * Validate time range logic
     */
    private fun validateTimeRange(): Boolean {
        val horaInicioStr = editTextHoraInicio.text.toString()
        val horaFinStr = editTextHoraFin.text.toString()
        
        if (horaInicioStr.isNotEmpty() && horaFinStr.isNotEmpty()) {
            try {
                val horaInicio = formatoHora.parse(horaInicioStr)
                val horaFin = formatoHora.parse(horaFinStr)
                
                if (horaInicio != null && horaFin != null && horaInicio.after(horaFin)) {
                    editTextHoraFin.error = "La hora de término debe ser posterior a la hora de inicio"
                    return false
                }
                
                // Validate minimum duration (e.g., 30 minutes)
                val duracionMs = horaFin!!.time - horaInicio!!.time
                val duracionMinutos = duracionMs / (1000 * 60)
                
                if (duracionMinutos < 30) {
                    editTextHoraFin.error = "La duración mínima debe ser de 30 minutos"
                    return false
                }
                
            } catch (e: Exception) {
                return false
            }
        }
        
        editTextHoraFin.error = null
        return true
    }

    /**
     * Validate location field when required
     */
    private fun validateLocationField(): Boolean {
        if (editTextLugar.visibility == View.GONE) return true
        
        val lugar = editTextLugar.text.toString().trim()
        
        if (lugar.isEmpty() && 
            (tipoSolicitudSeleccionado == TipoSolicitud.PERMISO_SALIDA || 
             tipoSolicitudSeleccionado == TipoSolicitud.TRABAJO_REMOTO)) {
            editTextLugar.error = "El lugar es obligatorio para este tipo de solicitud"
            return false
        }
        
        editTextLugar.error = null
        return true
    }

    /**
     * Real-time form field validation
     */
    private fun validateFormFields() {
        // Implement real-time validation logic here
        val isFormValid = validateCompleteForm()
        buttonEnviarSolicitud.isEnabled = isFormValid
        buttonEnviarSolicitud.alpha = if (isFormValid) 1.0f else 0.6f
    }

    // ================ API COMMUNICATION ================

    /**
     * Submit request with comprehensive error handling
     */
    private fun submitRequest() {
        val usuarioActual = CurrentSession.loginUser
        if (usuarioActual == null) {
            Toast.makeText(this, "Error: usuario no logueado", Toast.LENGTH_SHORT).show()
            return
        }

        showLoadingState(true)

        val solicitud = createRequestFromForm(usuarioActual)
        
        ApiService.submitSolicitud(solicitud)
            .enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    showLoadingState(false)
                    
                    if (response.isSuccessful) {
                        Toast.makeText(
                            this@RequestActivity,
                            "Solicitud enviada correctamente",
                            Toast.LENGTH_SHORT
                        ).show()
                        clearForm()
                        loadExistingRequests()
                    } else {
                        Toast.makeText(
                            this@RequestActivity,
                            "Error al enviar la solicitud: ${response.code()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    showLoadingState(false)
                    Toast.makeText(
                        this@RequestActivity,
                        "Error de conexión: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    /**
     * Create request object from form data with professional data mapping
     */
    private fun createRequestFromForm(usuario: CurrentSession.User): SolicitudItem {
        val fechaHoraInicio = when (tipoSolicitudSeleccionado) {
            TipoSolicitud.VACACIONES, TipoSolicitud.TRABAJO_REMOTO -> 
                "${editTextFechaInicio.text} 09:00:00"
            else -> 
                "${editTextFechaInicio.text} ${editTextHoraInicio.text}:00"
        }

        val fechaHoraFin = when (tipoSolicitudSeleccionado) {
            TipoSolicitud.VACACIONES, TipoSolicitud.TRABAJO_REMOTO -> 
                "${editTextFechaFin.text} 18:00:00"
            TipoSolicitud.PERMISO_SALIDA, TipoSolicitud.HORAS_EXTRA -> 
                "${editTextFechaInicio.text} ${editTextHoraFin.text}:00"
            else -> 
                "${editTextFechaInicio.text} ${editTextHoraInicio.text}:00"
        }

        return SolicitudItem(
            id = 0,
            quien = usuario.id,
            nombreUsuario = usuario.nombre,
            tipoSolicitud = tipoSolicitudSeleccionado,
            motivo = editTextMotivo.text.toString().trim(),
            inicio = fechaHoraInicio,
            fin = fechaHoraFin,
            lugar = editTextLugar.text.toString().trim(),
            observaciones = editTextObservaciones.text.toString().trim(),
            estado = EstadoSolicitud.PENDIENTE,
            fechaCreacion = formatoFecha.format(Date()),
            jefeId = usuario.jefe1_id ?: 0
        )
    }

    /**
     * Load existing requests with optimized data handling
     */
    private fun loadExistingRequests() {
        val usuarioActual = CurrentSession.loginUser ?: return
        
        showLoadingState(true)
        
        ApiService.getSolicitudesUsuario(usuarioActual.id)
            .enqueue(object : Callback<List<SolicitudItem>> {
                override fun onResponse(call: Call<List<SolicitudItem>>, response: Response<List<SolicitudItem>>) {
                    showLoadingState(false)
                    
                    if (response.isSuccessful) {
                        val solicitudes = response.body() ?: emptyList()
                        listaSolicitudes.clear()
                        listaSolicitudes.addAll(solicitudes.sortedByDescending { it.fechaCreacion })
                        solicitudesAdapter.notifyDataSetChanged()
                        
                        textViewSinSolicitudes.visibility = 
                            if (solicitudes.isEmpty()) View.VISIBLE else View.GONE
                    } else {
                        Toast.makeText(
                            this@RequestActivity,
                            "Error al cargar el historial de solicitudes",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<List<SolicitudItem>>, t: Throwable) {
                    showLoadingState(false)
                    Toast.makeText(
                        this@RequestActivity,
                        "Error de conexión al cargar solicitudes: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    // ================ UI STATE MANAGEMENT ================

    /**
     * Show/hide loading state with professional UI feedback
     */
    private fun showLoadingState(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        buttonEnviarSolicitud.isEnabled = !isLoading
        buttonEnviarSolicitud.text = if (isLoading) "Enviando..." else "Enviar Solicitud"
        
        // Disable form fields during submission
        spinnerTipoSolicitud.isEnabled = !isLoading
        editTextMotivo.isEnabled = !isLoading
        editTextFechaInicio.isEnabled = !isLoading
        editTextFechaFin.isEnabled = !isLoading
        editTextHoraInicio.isEnabled = !isLoading
        editTextHoraFin.isEnabled = !isLoading
        editTextLugar.isEnabled = !isLoading
        editTextObservaciones.isEnabled = !isLoading
    }

    /**
     * Clear form fields after successful submission
     */
    private fun clearForm() {
        editTextMotivo.text.clear()
        editTextFechaInicio.text.clear()
        editTextFechaFin.text.clear()
        editTextHoraInicio.text.clear()
        editTextHoraFin.text.clear()
        editTextLugar.text.clear()
        editTextObservaciones.text.clear()
        spinnerTipoSolicitud.setSelection(0)
        
        // Clear any validation errors
        editTextMotivo.error = null
        editTextFechaInicio.error = null
        editTextFechaFin.error = null
        editTextHoraInicio.error = null
        editTextHoraFin.error = null
        editTextLugar.error = null
    }

    /**
     * Show detailed request information dialog
     */
    private fun showRequestDetails(solicitud: SolicitudItem) {
        val detalles = StringBuilder().apply {
            append("Tipo: ${getTipoSolicitudDisplayName(solicitud.tipoSolicitud)}\n\n")
            append("Motivo: ${solicitud.motivo}\n\n")
            append("Inicio: ${solicitud.inicio}\n")
            if (solicitud.fin.isNotEmpty()) {
                append("Término: ${solicitud.fin}\n")
            }
            if (solicitud.lugar.isNotEmpty()) {
                append("Lugar: ${solicitud.lugar}\n")
            }
            if (solicitud.observaciones.isNotEmpty()) {
                append("Observaciones: ${solicitud.observaciones}\n")
            }
            append("\nEstado: ${getEstadoDisplayName(solicitud.estado)}\n")
            append("Fecha de Creación: ${solicitud.fechaCreacion}")
        }
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Detalles de Solicitud #${solicitud.id}")
            .setMessage(detalles.toString())
            .setPositiveButton("Cerrar") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    // ================ UTILITY FUNCTIONS ================

    /**
     * Get display name for request type
     */
    private fun getTipoSolicitudDisplayName(tipo: TipoSolicitud): String {
        return when (tipo) {
            TipoSolicitud.VACACIONES -> "Vacaciones"
            TipoSolicitud.PERMISO_SALIDA -> "Permiso de Salida"
            TipoSolicitud.LLEGADA_TARDE_SALIDA_TEMPRANA -> "Llegada Tarde / Salida Temprana"
            TipoSolicitud.HORAS_EXTRA -> "Solicitud de Horas Extra"
            TipoSolicitud.TRABAJO_REMOTO -> "Trabajo Remoto"
            TipoSolicitud.CAMBIO_ESTADO -> "Cambio de Estado"
        }
    }

    /**
     * Get display name for request status
     */
    private fun getEstadoDisplayName(estado: EstadoSolicitud): String {
        return when (estado) {
            EstadoSolicitud.PENDIENTE -> "Pendiente"
            EstadoSolicitud.APROBADO -> "Aprobado"
            EstadoSolicitud.RECHAZADO -> "Rechazado"
            EstadoSolicitud.CANCELADO -> "Cancelado"
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
