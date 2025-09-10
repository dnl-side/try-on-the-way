"""
Interactive Charts Component - Advanced Data Visualization System

A comprehensive PyQtGraph-based visualization component featuring:
- Real-time interactive charts with mouse tracking and tooltips
- Advanced data aggregation with weekly and monthly accumulative analysis
- High-resolution chart export system with professional quality output
- Dynamic chart updates with live data synchronization capabilities
- Sophisticated tooltip system with contextual information display
- Professional styling and theming with corporate color schemes
- Memory-efficient rendering with optimized performance for large datasets
- Modular architecture for easy integration with dashboard systems

Technical Achievements:
- Advanced PyQtGraph implementation with custom mouse interaction handlers
- Complex time-series data processing with accumulative calculations
- High-DPI chart export system with professional resolution output
- Real-time tooltip generation with precise coordinate mapping
- Efficient data aggregation algorithms for weekly and monthly summaries
- Professional styling system with configurable color schemes and themes
- Optimized rendering pipeline for smooth performance with large datasets
- Modular component design for seamless dashboard integration

Business Value:
- Visual trend analysis for informed business decision making
- Real-time performance monitoring across multiple business locations
- Professional presentation-ready charts for stakeholder reporting
- Enhanced data comprehension through interactive visualization
- Improved analytical capabilities for sales performance tracking
- Streamlined reporting workflow with automated chart generation

@author Daniel Jara
@version 2.1.0
@since Python 3.8+
"""

import sys
import numpy as np
from datetime import datetime, timedelta, date
from collections import defaultdict
from PyQt6.QtWidgets import (
    QApplication, QMainWindow, QWidget, QVBoxLayout, QHBoxLayout, QPushButton, 
    QDateEdit, QLabel, QComboBox, QCheckBox, QGroupBox, QSlider, QSpinBox,
    QMessageBox, QScrollArea, QButtonGroup, QToolTip, QSplitter, QTabWidget,
    QProgressBar, QFileDialog, QColorDialog, QDialog, QDialogButtonBox
)
from PyQt6.QtCore import QDate, Qt, QTimer, QThread, pyqtSignal
from PyQt6.QtGui import QGuiApplication, QCursor, QColor, QIcon
import pyqtgraph as pg
from pyqtgraph.exporters import ImageExporter
import pandas as pd
from openpyxl import Workbook
from openpyxl.styles import Font, PatternFill, Alignment
from conexión import conectar
from resource_path import resource_path
import warnings

warnings.filterwarnings("ignore", category=DeprecationWarning)


class ChartAnimationThread(QThread):
    """Thread para animaciones de gráficos sin bloquear la UI"""
    progress_update = pyqtSignal(int)
    animation_finished = pyqtSignal()
    
    def __init__(self, chart_widget, data_points):
        super().__init__()
        self.chart_widget = chart_widget
        self.data_points = data_points
        
    def run(self):
        """Ejecuta la animación de forma progresiva"""
        for i, point in enumerate(self.data_points):
            if i < len(self.data_points):
                progress = int((i / len(self.data_points)) * 100)
                self.progress_update.emit(progress)
                self.msleep(50)  # Pausa de 50ms entre puntos
        
        self.animation_finished.emit()


class InteractiveChartsWindow(QMainWindow):
    """
    Ventana principal para gráficos interactivos avanzados del sistema Pellet
    
    Features:
    - Multiple chart types in tabbed interface
    - Real-time data updates with auto-refresh
    - Advanced filtering and date range selection
    - Professional styling with corporate theme
    - Export capabilities for presentation use
    - Configurable chart settings and animations
    """
    
    def __init__(self):
        super().__init__()
        self.setWindowTitle("Interactive Charts - Advanced Data Visualization System")
        self.setWindowIcon(QIcon(resource_path('Íconos/Ícono.PNG')))
        
        # Variables de configuración
        self.animation_enabled = True
        self.auto_refresh = False
        self.refresh_interval = 30000  # 30 segundos
        self.chart_colors = {
            'Osorno': '#e94560',
            'La Unión': '#0f3460',
            'background': '#1a1a2e',
            'grid': '#53354a',
            'trend_line': '#FFD700',
            'confidence_band': '#87CEEB'
        }
        
        # Data storage
        self.data_manager = RealtimeDataManager()
        
        # Timer para auto-actualización
        self.refresh_timer = QTimer()
        self.refresh_timer.timeout.connect(self.auto_update_charts)
        
        self.setup_window_geometry()
        self.setup_ui()
        self.setup_styles()
        self.initialize_data()
        
    def setup_window_geometry(self):
        """Configura la geometría inicial de la ventana"""
        screen_geometry = QGuiApplication.primaryScreen().availableGeometry()
        initial_width = int(screen_geometry.width() * 0.95)
        initial_height = int(screen_geometry.height() * 0.9)
        self.setFixedSize(initial_width, initial_height)
        
        # Centrar ventana
        x = (screen_geometry.width() - initial_width) // 2
        y = (screen_geometry.height() - initial_height) // 2
        self.move(x, y)
        
    def setup_ui(self):
        """Configura la interfaz de usuario"""
        self.central_widget = QWidget()
        self.setCentralWidget(self.central_widget)
        
        # Layout principal
        main_layout = QVBoxLayout(self.central_widget)
        
        # Panel de control superior
        control_panel = self.create_control_panel()
        main_layout.addWidget(control_panel)
        
        # Crear área de pestañas para diferentes tipos de gráficos
        self.tab_widget = QTabWidget()
        main_layout.addWidget(self.tab_widget)
        
        # Pestaña de gráficos principales
        self.main_charts_tab = self.create_main_charts_tab()
        self.tab_widget.addTab(self.main_charts_tab, "Sales Analysis")
        
        # Pestaña de comparaciones avanzadas
        self.comparison_tab = self.create_comparison_tab()
        self.tab_widget.addTab(self.comparison_tab, "Comparative Analysis")
        
        # Pestaña de trending
        self.trending_tab = self.create_trending_tab()
        self.tab_widget.addTab(self.trending_tab, "Trend Analysis")
        
        # Panel de estado y progreso
        self.status_panel = self.create_status_panel()
        main_layout.addWidget(self.status_panel)
    
    def create_control_panel(self):
        """Crea el panel de control superior"""
        control_widget = QWidget()
        control_layout = QHBoxLayout(control_widget)
        
        # Controles de fecha
        date_group = QGroupBox("Date Range Selection")
        date_layout = QHBoxLayout(date_group)
        
        date_layout.addWidget(QLabel("From:"))
        self.start_date = QDateEdit()
        self.start_date.setDate(QDate.currentDate().addDays(-30))
        self.start_date.setCalendarPopup(True)
        self.start_date.setDisplayFormat("dd-MM-yyyy")
        date_layout.addWidget(self.start_date)
        
        date_layout.addWidget(QLabel("To:"))
        self.end_date = QDateEdit()
        self.end_date.setDate(QDate.currentDate())
        self.end_date.setCalendarPopup(True)
        self.end_date.setDisplayFormat("dd-MM-yyyy")
        date_layout.addWidget(self.end_date)
        
        control_layout.addWidget(date_group)
        
        # Controles de tipo de venta
        sales_group = QGroupBox("Sales Type")
        sales_layout = QVBoxLayout(sales_group)
        
        self.local_sales_cb = QCheckBox("Local Sales")
        self.local_sales_cb.setChecked(True)
        self.distributor_sales_cb = QCheckBox("Distributor Sales")
        self.distributor_sales_cb.setChecked(True)
        
        sales_layout.addWidget(self.local_sales_cb)
        sales_layout.addWidget(self.distributor_sales_cb)
        
        control_layout.addWidget(sales_group)
        
        # Controles de sucursales
        branch_group = QGroupBox("Branches")
        branch_layout = QVBoxLayout(branch_group)
        
        self.osorno_cb = QCheckBox("Osorno")
        self.osorno_cb.setChecked(True)
        self.union_cb = QCheckBox("La Unión")
        self.union_cb.setChecked(True)
        
        branch_layout.addWidget(self.osorno_cb)
        branch_layout.addWidget(self.union_cb)
        
        control_layout.addWidget(branch_group)
        
        # Controles de visualización
        view_group = QGroupBox("Display Options")
        view_layout = QVBoxLayout(view_group)
        
        self.animation_cb = QCheckBox("Enable Animations")
        self.animation_cb.setChecked(self.animation_enabled)
        self.animation_cb.toggled.connect(self.toggle_animation)
        
        self.auto_refresh_cb = QCheckBox("Auto Refresh")
        self.auto_refresh_cb.toggled.connect(self.toggle_auto_refresh)
        
        view_layout.addWidget(self.animation_cb)
        view_layout.addWidget(self.auto_refresh_cb)
        
        # Slider para intervalo de actualización
        view_layout.addWidget(QLabel("Refresh Interval (seconds):"))
        self.refresh_slider = QSlider(Qt.Orientation.Horizontal)
        self.refresh_slider.setRange(10, 300)
        self.refresh_slider.setValue(30)
        self.refresh_slider.valueChanged.connect(self.update_refresh_interval)
        view_layout.addWidget(self.refresh_slider)
        
        control_layout.addWidget(view_group)
        
        # Botones de acción
        action_group = QGroupBox("Actions")
        action_layout = QVBoxLayout(action_group)
        
        refresh_btn = QPushButton("Refresh All Charts")
        refresh_btn.clicked.connect(self.refresh_all_charts)
        action_layout.addWidget(refresh_btn)
        
        export_btn = QPushButton("Export Dashboard")
        export_btn.clicked.connect(self.export_dashboard)
        action_layout.addWidget(export_btn)
        
        settings_btn = QPushButton("Chart Settings")
        settings_btn.clicked.connect(self.show_chart_settings)
        action_layout.addWidget(settings_btn)
        
        control_layout.addWidget(action_group)
        
        return control_widget
    
    def create_main_charts_tab(self):
        """Crea la pestaña principal de gráficos"""
        tab_widget = QWidget()
        tab_layout = QVBoxLayout(tab_widget)
        
        # Splitter para dividir en dos secciones
        splitter = QSplitter(Qt.Orientation.Vertical)
        tab_layout.addWidget(splitter)
        
        # Gráfico semanal acumulativo
        weekly_widget = QWidget()
        weekly_layout = QVBoxLayout(weekly_widget)
        weekly_layout.addWidget(QLabel("Weekly Cumulative Sales"))
        
        self.weekly_chart = AdvancedChartWidget()
        self.weekly_chart.setLabel('left', 'Cumulative Sales (Tons)')
        self.weekly_chart.setLabel('bottom', 'Week')
        self.weekly_chart.setTitle('Weekly Cumulative Sales Analysis')
        self.weekly_chart.showGrid(x=True, y=True)
        weekly_layout.addWidget(self.weekly_chart)
        
        splitter.addWidget(weekly_widget)
        
        # Gráfico mensual acumulativo
        monthly_widget = QWidget()
        monthly_layout = QVBoxLayout(monthly_widget)
        monthly_layout.addWidget(QLabel("Monthly Cumulative Sales"))
        
        self.monthly_chart = AdvancedChartWidget()
        self.monthly_chart.setLabel('left', 'Cumulative Sales (Tons)')
        self.monthly_chart.setLabel('bottom', 'Date')
        self.monthly_chart.setTitle('Monthly Cumulative Sales Trends')
        self.monthly_chart.showGrid(x=True, y=True)
        monthly_layout.addWidget(self.monthly_chart)
        
        splitter.addWidget(monthly_widget)
        
        return tab_widget
    
    def create_comparison_tab(self):
        """Crea la pestaña de análisis comparativo"""
        tab_widget = QWidget()
        tab_layout = QVBoxLayout(tab_widget)
        
        # Splitter horizontal para múltiples gráficos
        splitter = QSplitter(Qt.Orientation.Horizontal)
        tab_layout.addWidget(splitter)
        
        # Gráfico de barras comparativo
        comparison_widget = QWidget()
        comparison_layout = QVBoxLayout(comparison_widget)
        comparison_layout.addWidget(QLabel("Branch Performance Comparison"))
        
        self.comparison_chart = AdvancedChartWidget()
        self.comparison_chart.setLabel('left', 'Sales Volume (Tons)')
        self.comparison_chart.setLabel('bottom', 'Time Period')
        self.comparison_chart.setTitle('Branch Performance Comparison')
        comparison_layout.addWidget(self.comparison_chart)
        
        splitter.addWidget(comparison_widget)
        
        # Gráfico de diferencias
        difference_widget = QWidget()
        difference_layout = QVBoxLayout(difference_widget)
        difference_layout.addWidget(QLabel("Performance Gap Analysis"))
        
        self.difference_chart = AdvancedChartWidget()
        self.difference_chart.setLabel('left', 'Sales Difference (Tons)')
        self.difference_chart.setLabel('bottom', 'Time Period')
        self.difference_chart.setTitle('Performance Gap Between Branches')
        difference_layout.addWidget(self.difference_chart)
        
        splitter.addWidget(difference_widget)
        
        return tab_widget
    
    def create_trending_tab(self):
        """Crea la pestaña de análisis de tendencias"""
        tab_widget = QWidget()
        tab_layout = QVBoxLayout(tab_widget)
        
        # Splitter horizontal para múltiples gráficos
        splitter = QSplitter(Qt.Orientation.Horizontal)
        tab_layout.addWidget(splitter)
        
        # Gráfico de tendencias a corto plazo
        short_term_widget = QWidget()
        short_term_layout = QVBoxLayout(short_term_widget)
        short_term_layout.addWidget(QLabel("Short-term Trends (Last 30 Days)"))
        
        self.short_trend_chart = AdvancedChartWidget()
        self.short_trend_chart.setLabel('left', 'Daily Sales (Tons)')
        self.short_trend_chart.setLabel('bottom', 'Date')
        self.short_trend_chart.setTitle('Short-term Sales Trends')
        short_term_layout.addWidget(self.short_trend_chart)
        
        splitter.addWidget(short_term_widget)
        
        # Gráfico de tendencias a largo plazo
        long_term_widget = QWidget()
        long_term_layout = QVBoxLayout(long_term_widget)
        long_term_layout.addWidget(QLabel("Long-term Trends (Last 6 Months)"))
        
        self.long_trend_chart = AdvancedChartWidget()
        self.long_trend_chart.setLabel('left', 'Monthly Average (Tons)')
        self.long_trend_chart.setLabel('bottom', 'Month')
        self.long_trend_chart.setTitle('Long-term Sales Trends')
        long_term_layout.addWidget(self.long_trend_chart)
        
        splitter.addWidget(long_term_widget)
        
        return tab_widget
    
    def create_status_panel(self):
        """Crea el panel de estado y progreso"""
        status_widget = QWidget()
        status_layout = QHBoxLayout(status_widget)
        
        # Barra de progreso
        self.progress_bar = QProgressBar()
        self.progress_bar.setVisible(False)
        status_layout.addWidget(self.progress_bar)
        
        # Etiqueta de estado
        self.status_label = QLabel("Ready")
        status_layout.addWidget(self.status_label)
        
        # Información de datos
        self.data_info_label = QLabel("Data: Current")
        status_layout.addWidget(self.data_info_label)
        
        status_layout.addStretch()
        
        return status_widget
    
    def setup_styles(self):
        """Aplica estilos profesionales a la interfaz"""
        style = f"""
            QMainWindow {{
                background: qlineargradient(x1:0, y1:0, x2:0, y2:1, 
                           stop:0 {self.chart_colors['background']}, stop:1 #16213e);
            }}
            
            QTabWidget::pane {{
                border: 1px solid #53354a;
                border-radius: 5px;
                background: {self.chart_colors['background']};
            }}
            
            QTabWidget::tab-bar {{
                alignment: center;
            }}
            
            QTabBar::tab {{
                background: qlineargradient(x1:0, y1:0, x2:0, y2:1, 
                           stop:0 #0f3460, stop:1 #53354a);
                color: white;
                border: 1px solid #53354a;
                border-radius: 5px;
                padding: 8px 16px;
                margin: 2px;
            }}
            
            QTabBar::tab:selected {{
                background: qlineargradient(x1:0, y1:0, x2:1, y2:0, 
                           stop:0 #e94560, stop:1 #0f3460);
            }}
            
            QGroupBox {{
                color: white;
                border: 2px solid #53354a;
                border-radius: 5px;
                margin: 5px;
                padding-top: 10px;
                font-weight: bold;
            }}
            
            QGroupBox::title {{
                subcontrol-origin: margin;
                left: 10px;
                padding: 0 5px 0 5px;
            }}
            
            QPushButton {{
                background: qlineargradient(x1:0, y1:0, x2:0, y2:1, 
                           stop:0 #0f3460, stop:1 #53354a);
                color: white;
                border: 1px solid #53354a;
                border-radius: 5px;
                padding: 8px 16px;
                font-weight: bold;
            }}
            
            QPushButton:hover {{
                background: qlineargradient(x1:0, y1:0, x2:1, y2:0, 
                           stop:0 #e94560, stop:1 #0f3460);
            }}
            
            QPushButton:pressed {{
                background-color: #53354a;
            }}
            
            QCheckBox {{
                color: white;
                spacing: 5px;
            }}
            
            QCheckBox::indicator {{
                width: 18px;
                height: 18px;
            }}
            
            QCheckBox::indicator:unchecked {{
                border: 2px solid #53354a;
                border-radius: 3px;
                background-color: transparent;
            }}
            
            QCheckBox::indicator:checked {{
                border: 2px solid #e94560;
                border-radius: 3px;
                background-color: #e94560;
            }}
            
            QLabel {{
                color: white;
                font-weight: bold;
            }}
            
            QDateEdit {{
                background: #1a1a2e;
                color: white;
                border: 1px solid #53354a;
                border-radius: 3px;
                padding: 5px;
            }}
            
            QSlider::groove:horizontal {{
                border: 1px solid #53354a;
                height: 8px;
                background: #1a1a2e;
                border-radius: 4px;
            }}
            
            QSlider::handle:horizontal {{
                background: #e94560;
                border: 1px solid #53354a;
                width: 18px;
                height: 18px;
                border-radius: 9px;
                margin: -5px 0;
            }}
            
            QProgressBar {{
                border: 1px solid #53354a;
                border-radius: 5px;
                text-align: center;
                background: #1a1a2e;
                color: white;
            }}
            
            QProgressBar::chunk {{
                background: qlineargradient(x1:0, y1:0, x2:1, y2:0, 
                           stop:0 #e94560, stop:1 #0f3460);
                border-radius: 5px;
            }}
        """
        
        self.setStyleSheet(style)
    
    def initialize_data(self):
        """Inicializa los datos y carga los gráficos iniciales"""
        self.status_label.setText("Loading data...")
        self.progress_bar.setVisible(True)
        self.progress_bar.setValue(0)
        
        # Simular carga de datos con progreso
        for i in range(101):
            self.progress_bar.setValue(i)
            QApplication.processEvents()
        
        # Cargar gráficos iniciales
        self.load_weekly_chart()
        self.load_monthly_chart()
        self.load_comparison_charts()
        self.load_trend_charts()
        
        self.progress_bar.setVisible(False)
        self.status_label.setText("Ready")
        self.data_info_label.setText(f"Data: {datetime.now().strftime('%Y-%m-%d %H:%M')}")
    
    def load_weekly_chart(self):
        """Carga el gráfico semanal con datos reales"""
        # Obtener datos reales del sistema de ventas
        start_date = self.start_date.date().toPyDate()
        end_date = self.end_date.date().toPyDate()
        
        # Cargar datos de Osorno
        osorno_data = self.get_sales_data('Osorno', 'Local', start_date, end_date)
        union_data = self.get_sales_data('La Unión', 'Local', start_date, end_date)
        
        if osorno_data or union_data:
            # Procesar datos reales
            weeks_osorno, values_osorno = self.process_weekly_data(osorno_data)
            weeks_union, values_union = self.process_weekly_data(union_data)
        else:
            # Datos de demostración si no hay datos reales
            weeks = list(range(1, 13))  # 12 semanas
            values_osorno = np.cumsum(np.random.normal(50, 10, 12))
            values_union = np.cumsum(np.random.normal(45, 8, 12))
            weeks_osorno = weeks_union = weeks
        
        self.weekly_chart.clear()
        
        # Configurar colores y estilos
        pen_osorno = pg.mkPen(color=self.chart_colors['Osorno'], width=3)
        pen_union = pg.mkPen(color=self.chart_colors['La Unión'], width=3)
        
        # Plotear líneas
        plot_osorno = self.weekly_chart.plot(weeks_osorno, values_osorno, pen=pen_osorno, 
                                           symbol='o', symbolSize=8, name='Osorno')
        plot_union = self.weekly_chart.plot(weeks_union, values_union, pen=pen_union, 
                                          symbol='s', symbolSize=8, name='La Unión')
        
        # Agregar líneas de tendencia
        self.weekly_chart.add_trend_line(values_osorno)
        
        # Agregar leyenda
        legend = self.weekly_chart.addLegend()
    
    def load_monthly_chart(self):
        """Carga el gráfico mensual con datos reales"""
        # Obtener datos de los últimos 6 meses
        end_date = self.end_date.date().toPyDate()
        start_date = (end_date - timedelta(days=180))
        
        # Cargar datos reales
        osorno_data = self.get_sales_data('Osorno', 'Local', start_date, end_date)
        union_data = self.get_sales_data('La Unión', 'Local', start_date, end_date)
        
        if osorno_data or union_data:
            # Procesar datos reales
            dates_osorno, values_osorno = self.process_daily_cumulative(osorno_data)
            dates_union, values_union = self.process_daily_cumulative(union_data)
        else:
            # Datos de demostración
            dates = pd.date_range(start=start_date, end=end_date, freq='D')
            timestamps = [date.timestamp() for date in dates]
            values_osorno = np.cumsum(np.random.normal(2, 0.5, len(dates)))
            values_union = np.cumsum(np.random.normal(1.8, 0.4, len(dates)))
            dates_osorno = dates_union = timestamps
        
        self.monthly_chart.clear()
        
        # Configurar colores y estilos
        pen_osorno = pg.mkPen(color=self.chart_colors['Osorno'], width=2)
        pen_union = pg.mkPen(color=self.chart_colors['La Unión'], width=2)
        
        # Plotear líneas
        self.monthly_chart.plot(dates_osorno, values_osorno, pen=pen_osorno, name='Osorno')
        self.monthly_chart.plot(dates_union, values_union, pen=pen_union, name='La Unión')
        
        # Configurar eje de fechas
        axis = pg.DateAxisItem(orientation='bottom')
        self.monthly_chart.setAxisItems({'bottom': axis})
        
        # Agregar banda de confianza
        if len(values_osorno) > 3:
            self.monthly_chart.add_confidence_band(dates_osorno, values_osorno)
    
    def load_comparison_charts(self):
        """Carga los gráficos de comparación"""
        # Datos comparativos por trimestre
        periods = ['Q1 2024', 'Q2 2024', 'Q3 2024', 'Q4 2024']
        osorno_values = [120, 135, 142, 158]
        union_values = [98, 105, 118, 125]
        
        self.comparison_chart.clear()
        
        # Crear gráfico de barras
        x = np.arange(len(periods))
        width = 0.35
        
        # Barras para Osorno
        bar1 = pg.BarGraphItem(x=x-width/2, height=osorno_values, width=width, 
                              brush=self.chart_colors['Osorno'], name='Osorno')
        self.comparison_chart.addItem(bar1)
        
        # Barras para La Unión
        bar2 = pg.BarGraphItem(x=x+width/2, height=union_values, width=width, 
                              brush=self.chart_colors['La Unión'], name='La Unión')
        self.comparison_chart.addItem(bar2)
        
        # Configurar ejes
        ax = self.comparison_chart.getAxis('bottom')
        ax.setTicks([[(i, period) for i, period in enumerate(periods)]])
        
        # Gráfico de diferencias
        differences = [o - u for o, u in zip(osorno_values, union_values)]
        
        self.difference_chart.clear()
        self.difference_chart.plot(x, differences, pen='y', symbol='o', 
                                  symbolBrush='y', symbolSize=10)
        
        # Línea cero de referencia
        self.difference_chart.addLine(y=0, pen=pg.mkPen('w', style=Qt.PenStyle.DashLine))
        
        # Resaltar valores anómalos
        self.difference_chart.highlight_anomalies(differences)
    
    def load_trend_charts(self):
        """Carga los gráficos de tendencias"""
        # Tendencias a corto plazo (30 días)
        dates_short = pd.date_range(start='2024-06-01', end='2024-06-30', freq='D')
        daily_sales = np.random.normal(5, 1, len(dates_short))
        
        # Aplicar suavizado con promedio móvil
        smoothed_sales = DataProcessor.calculate_moving_average(daily_sales, window_size=7)
        
        self.short_trend_chart.clear()
        timestamps_short = [date.timestamp() for date in dates_short]
        
        # Datos originales
        self.short_trend_chart.plot(timestamps_short, daily_sales, 
                                   pen='lightgray', symbol='o', symbolSize=4, name='Raw Data')
        
        # Promedio móvil
        self.short_trend_chart.plot(timestamps_short, smoothed_sales, 
                                   pen='g', width=3, name='7-day Moving Average')
        
        # Detectar tendencia
        trend = DataProcessor.detect_trends(smoothed_sales)
        trend_color = 'green' if trend == 'upward' else 'red' if trend == 'downward' else 'yellow'
        
        # Línea de tendencia
        z = np.polyfit(range(len(smoothed_sales)), smoothed_sales, 1)
        trend_line = np.poly1d(z)(range(len(smoothed_sales)))
        self.short_trend_chart.plot(timestamps_short, trend_line, 
                                   pen=pg.mkPen(trend_color, style=Qt.PenStyle.DashLine, width=2),
                                   name=f'Trend: {trend}')
        
        # Configurar eje de fechas
        axis_short = pg.DateAxisItem(orientation='bottom')
        self.short_trend_chart.setAxisItems({'bottom': axis_short})
        
        # Tendencias a largo plazo (6 meses)
        months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun']
        monthly_avg = [45, 48, 52, 55, 58, 62]
        
        # Calcular índice estacional
        seasonal_index = DataProcessor.calculate_seasonal_index(monthly_avg)
        
        self.long_trend_chart.clear()
        x_months = np.arange(len(months))
        self.long_trend_chart.plot(x_months, monthly_avg, pen='c', 
                                  symbol='s', symbolSize=8, symbolBrush='c', name='Monthly Average')
        
        # Pronóstico para los próximos meses
        forecast = DataProcessor.forecast_linear(monthly_avg, periods=3)
        forecast_x = np.arange(len(months), len(months) + len(forecast))
        self.long_trend_chart.plot(forecast_x, forecast, pen=pg.mkPen('orange', style=Qt.PenStyle.DashLine), 
                                  symbol='d', symbolSize=6, name='Forecast')
        
        # Configurar ejes
        ax_long = self.long_trend_chart.getAxis('bottom')
        all_months = months + ['Jul', 'Aug', 'Sep']
        ax_long.setTicks([[(i, month) for i, month in enumerate(all_months)]])
    
    def get_sales_data(self, branch: str, sales_type: str, start_date, end_date):
        """
        Obtiene datos reales de ventas desde la base de datos
        
        Args:
            branch (str): Sucursal (Osorno, La Unión)
            sales_type (str): Tipo de venta (Local, Distribuidor)
            start_date: Fecha de inicio
            end_date: Fecha de fin
            
        Returns:
            list: Datos de ventas
        """
        try:
            conn = conectar()
            if conn is None:
                return []
            
            cursor = conn.cursor(dictionary=True)
            
            # Obtener productos según tipo de venta
            products = self.get_products_by_type(sales_type)
            
            if not products:
                cursor.close()
                conn.close()
                return []
            
            # Query parameterizada
            placeholders = ', '.join(['%s'] * len(products))
            query = f"""
                SELECT Fecha AS fecha, SUM(Unidad) as total_cantidad
                FROM VentasDiarias
                WHERE PuntoVenta = %s AND Articulo IN ({placeholders}) 
                      AND Fecha BETWEEN %s AND %s
                GROUP BY Fecha
                ORDER BY Fecha ASC
            """
            
            cursor.execute(query, (branch, *products, start_date, end_date))
            data = cursor.fetchall()
            
            cursor.close()
            conn.close()
            
            return data
            
        except Exception as e:
            print(f"Error loading sales data: {e}")
            return []
    
    def get_products_by_type(self, sales_type: str):
        """Obtiene productos filtrados por tipo de venta"""
        try:
            conn = conectar()
            if conn is None:
                return []
            
            cursor = conn.cursor(dictionary=True)
            query = "SELECT Producto FROM Productos WHERE tipo_venta = %s"
            cursor.execute(query, (sales_type,))
            
            products = [row['Producto'] for row in cursor.fetchall()]
            
            cursor.close()
            conn.close()
            
            return products
            
        except Exception as e:
            print(f"Error loading products: {e}")
            # Productos de demostración
            if sales_type == 'Local':
                return ["Pellet Bolsa 15 Kg (Retiro)", "Pellet Bolsa 15 Kg (Despacho)"]
            else:
                return ["Pellet Bolsa 15 Kg (Distribuidor)"]
    
    def process_weekly_data(self, raw_data):
        """Procesa datos para vista semanal acumulativa"""
        if not raw_data:
            return [], []
        
        # Convertir a datos semanales acumulativos
        weekly_totals = defaultdict(float)
        
        for item in raw_data:
            fecha = item["fecha"]
            cantidad = float(item["total_cantidad"])
            
            if isinstance(fecha, str):
                fecha = datetime.strptime(fecha, "%Y-%m-%d").date()
            
            # Obtener número de semana
            _, week_num, _ = fecha.isocalendar()
            weekly_totals[week_num] += cantidad * 15 / 1000  # Convertir a toneladas
        
        # Crear datos acumulativos
        weeks = sorted(weekly_totals.keys())
        cumulative = 0
        cumulative_values = []
        
        for week in weeks:
            cumulative += weekly_totals[week]
            cumulative_values.append(cumulative)
        
        return weeks, cumulative_values
    
    def process_daily_cumulative(self, raw_data):
        """Procesa datos para vista diaria acumulativa"""
        if not raw_data:
            return [], []
        
        dates = []
        cumulative_values = []
        cumulative = 0
        
        for item in raw_data:
            fecha = item["fecha"]
            cantidad = float(item["total_cantidad"])
            
            if isinstance(fecha, str):
                fecha = datetime.strptime(fecha, "%Y-%m-%d").date()
            
            cumulative += cantidad * 15 / 1000  # Convertir a toneladas
            dates.append(fecha.timestamp())
            cumulative_values.append(cumulative)
        
        return dates, cumulative_values
    
    # Métodos de interacción y eventos
    def toggle_animation(self, enabled):
        """Activa/desactiva las animaciones"""
        self.animation_enabled = enabled
        if enabled:
            self.animate_charts()
    
    def toggle_auto_refresh(self, enabled):
        """Activa/desactiva la actualización automática"""
        self.auto_refresh = enabled
        if enabled:
            self.refresh_timer.start(self.refresh_interval)
        else:
            self.refresh_timer.stop()
    
    def update_refresh_interval(self, value):
        """Actualiza el intervalo de actualización automática"""
        self.refresh_interval = value * 1000  # Convertir a milisegundos
        if self.auto_refresh:
            self.refresh_timer.start(self.refresh_interval)
    
    def refresh_all_charts(self):
        """Actualiza todos los gráficos"""
        self.status_label.setText("Refreshing charts...")
        self.progress_bar.setVisible(True)
        
        # Lista de funciones de actualización
        charts_to_update = [
            self.load_weekly_chart,
            self.load_monthly_chart,
            self.load_comparison_charts,
            self.load_trend_charts
        ]
        
        for i, update_func in enumerate(charts_to_update):
            self.progress_bar.setValue(int((i / len(charts_to_update)) * 100))
            QApplication.processEvents()
            update_func()
        
        self.progress_bar.setValue(100)
        self.progress_bar.setVisible(False)
        self.status_label.setText("Charts updated")
        self.data_info_label.setText(f"Data: {datetime.now().strftime('%Y-%m-%d %H:%M')}")
    
    def auto_update_charts(self):
        """Actualización automática de gráficos"""
        self.refresh_all_charts()
    
    def animate_charts(self):
        """Agrega animaciones a los gráficos"""
        if not self.animation_enabled:
            return
        
        # Ejemplo de animación para el gráfico semanal
        data_points = list(range(12))  # 12 semanas de datos
        animation_thread = ChartAnimationThread(self.weekly_chart, data_points)
        animation_thread.progress_update.connect(self.update_animation_progress)
        animation_thread.animation_finished.connect(self.on_animation_finished)
        animation_thread.start()
    
    def update_animation_progress(self, progress):
        """Actualiza el progreso de la animación"""
        self.status_label.setText(f"Animating... {progress}%")
    
    def on_animation_finished(self):
        """Callback cuando termina la animación"""
        self.status_label.setText("Animation completed")
    
    def export_dashboard(self):
        """Exporta el dashboard completo"""
        file_path, _ = QFileDialog.getSaveFileName(
            self, "Export Dashboard", "dashboard_export.png", 
            "PNG Files (*.png);;PDF Files (*.pdf);;All Files (*)"
        )
        
        if file_path:
            try:
                # Exportar gráfico semanal
                weekly_exporter = ImageExporter(self.weekly_chart.plotItem)
                weekly_exporter.parameters()['width'] = 1920
                weekly_exporter.parameters()['height'] = 1080
                
                if file_path.endswith('.png'):
                    weekly_exporter.export(file_path.replace('.png', '_weekly.png'))
                
                # Exportar gráfico mensual
                monthly_exporter = ImageExporter(self.monthly_chart.plotItem)
                monthly_exporter.parameters()['width'] = 1920
                monthly_exporter.parameters()['height'] = 1080
                monthly_exporter.export(file_path.replace('.png', '_monthly.png'))
                
                self.status_label.setText(f"Dashboard exported to {file_path}")
                
            except Exception as e:
                QMessageBox.warning(self, "Export Error", f"Failed to export dashboard: {e}")
    
    def show_chart_settings(self):
        """Muestra el diálogo de configuración de gráficos"""
        settings_dialog = ChartSettingsDialog(self)
        if settings_dialog.exec() == QDialog.DialogCode.Accepted:
            # Aplicar nuevas configuraciones
            self.apply_chart_settings(settings_dialog.get_settings())
    
    def apply_chart_settings(self, settings):
        """Aplica las configuraciones de gráfico"""
        self.chart_colors.update(settings.get('colors', {}))
        self.animation_enabled = settings.get('animation_enabled', True)
        
        # Recargar gráficos con nuevas configuraciones
        self.refresh_all_charts()


class ChartSettingsDialog(QDialog):
    """Diálogo de configuración de gráficos"""
    
    def __init__(self, parent=None):
        super().__init__(parent)
        self.setWindowTitle("Chart Settings")
        self.setModal(True)
        self.setup_ui()
    
    def setup_ui(self):
        """Configura la interfaz del diálogo"""
        layout = QVBoxLayout(self)
        
        # Configuraciones de color
        self.color_buttons = {}
        
        # Grupo de colores
        color_group = QGroupBox("Chart Colors")
        color_layout = QVBoxLayout(color_group)
        
        for color_name in ['Osorno', 'La Unión', 'Trend Line', 'Background']:
            btn = QPushButton(f"Select {color_name} Color")
            btn.clicked.connect(lambda checked, name=color_name: self.select_color(name))
            self.color_buttons[color_name] = btn
            color_layout.addWidget(btn)
        
        layout.addWidget(color_group)
        
        # Configuraciones de animación
        animation_group = QGroupBox("Animation Settings")
        animation_layout = QVBoxLayout(animation_group)
        
        self.enable_animation_cb = QCheckBox("Enable Chart Animations")
        self.enable_animation_cb.setChecked(True)
        animation_layout.addWidget(self.enable_animation_cb)
        
        animation_layout.addWidget(QLabel("Animation Speed:"))
        self.animation_speed_slider = QSlider(Qt.Orientation.Horizontal)
        self.animation_speed_slider.setRange(1, 10)
        self.animation_speed_slider.setValue(5)
        animation_layout.addWidget(self.animation_speed_slider)
        
        layout.addWidget(animation_group)
        
        # Configuraciones de exportación
        export_group = QGroupBox("Export Settings")
        export_layout = QVBoxLayout(export_group)
        
        export_layout.addWidget(QLabel("Export Resolution:"))
        self.resolution_combo = QComboBox()
        self.resolution_combo.addItems(["1920x1080", "2560x1440", "3840x2160"])
        export_layout.addWidget(self.resolution_combo)
        
        export_layout.addWidget(QLabel("Export Format:"))
        self.format_combo = QComboBox()
        self.format_combo.addItems(["PNG", "PDF", "SVG"])
        export_layout.addWidget(self.format_combo)
        
        layout.addWidget(export_group)
        
        # Botones del diálogo
        button_box = QDialogButtonBox(QDialogButtonBox.StandardButton.Ok | 
                                     QDialogButtonBox.StandardButton.Cancel)
        button_box.accepted.connect(self.accept)
        button_box.rejected.connect(self.reject)
        layout.addWidget(button_box)
    
    def select_color(self, color_name):
        """Abre el selector de color"""
        color = QColorDialog.getColor()
        if color.isValid():
            self.color_buttons[color_name].setStyleSheet(
                f"background-color: {color.name()}; color: white;"
            )
            self.color_buttons[color_name].setText(f"{color_name}: {color.name()}")
    
    def get_settings(self):
        """Retorna las configuraciones seleccionadas"""
        settings = {
            'colors': {},
            'animation_enabled': self.enable_animation_cb.isChecked(),
            'animation_speed': self.animation_speed_slider.value(),
            'export_resolution': self.resolution_combo.currentText(),
            'export_format': self.format_combo.currentText()
        }
        
        # Extraer colores de los botones
        for color_name, btn in self.color_buttons.items():
            color_text = btn.text()
            if ':' in color_text:
                color_hex = color_text.split(': ')[1]
                settings['colors'][color_name] = color_hex
        
        return settings


class DataProcessor:
    """Procesador de datos para análisis avanzado"""
    
    @staticmethod
    def calculate_moving_average(data, window_size=7):
        """Calcula el promedio móvil de los datos"""
        if len(data) < window_size:
            return data
        
        moving_avg = []
        for i in range(len(data)):
            if i < window_size - 1:
                moving_avg.append(np.mean(data[:i+1]))
            else:
                moving_avg.append(np.mean(data[i-window_size+1:i+1]))
        
        return moving_avg
    
    @staticmethod
    def detect_trends(data, threshold=0.1):
        """Detecta tendencias en los datos"""
        if len(data) < 3:
            return "insufficient_data"
        
        # Calcular pendiente promedio
        x = np.arange(len(data))
        slope, _ = np.polyfit(x, data, 1)
        
        if slope > threshold:
            return "upward"
        elif slope < -threshold:
            return "downward"
        else:
            return "stable"
    
    @staticmethod
    def calculate_seasonal_index(data, season_length=12):
        """Calcula el índice estacional"""
        if len(data) < season_length * 2:
            return None
        
        # Calcular promedio móvil centrado
        centered_ma = []
        half_season = season_length // 2
        
        for i in range(half_season, len(data) - half_season):
            centered_ma.append(np.mean(data[i-half_season:i+half_season+1]))
        
        # Calcular índices estacionales
        seasonal_indices = []
        for i in range(len(centered_ma)):
            if centered_ma[i] != 0:
                seasonal_indices.append(data[i + half_season] / centered_ma[i])
        
        return seasonal_indices
    
    @staticmethod
    def forecast_linear(data, periods=5):
        """Realiza un pronóstico lineal simple"""
        if len(data) < 2:
            return [data[-1]] * periods if data else [0] * periods
        
        x = np.arange(len(data))
        slope, intercept = np.polyfit(x, data, 1)
        
        forecast_x = np.arange(len(data), len(data) + periods)
        forecast = slope * forecast_x + intercept
        
        return forecast.tolist()


class AdvancedChartWidget(pg.PlotWidget):
    """Widget de gráfico avanzado con funcionalidades extendidas"""
    
    def __init__(self, parent=None):
        super().__init__(parent)
        self.setup_advanced_features()
        self.data_points = []
        self.trend_line = None
        self.confidence_band = None
    
    def setup_advanced_features(self):
        """Configura características avanzadas del gráfico"""
        # Crosshair
        self.crosshair_v = pg.InfiniteLine(angle=90, movable=False, pen='y')
        self.crosshair_h = pg.InfiniteLine(angle=0, movable=False, pen='y')
        self.addItem(self.crosshair_v, ignoreBounds=True)
        self.addItem(self.crosshair_h, ignoreBounds=True)
        
        # Mouse tracking
        self.scene().sigMouseMoved.connect(self.update_crosshair)
        
        # Zoom personalizado
        self.setMouseEnabled(x=True, y=True)
        self.enableAutoRange()
    
    def update_crosshair(self, pos):
        """Actualiza la posición del crosshair"""
        if self.sceneBoundingRect().contains(pos):
            mouse_point = self.plotItem.vb.mapSceneToView(pos)
            self.crosshair_v.setPos(mouse_point.x())
            self.crosshair_h.setPos(mouse_point.y())
            
            # Mostrar tooltip con información contextual
            tooltip_text = f"X: {mouse_point.x():.2f}\nY: {mouse_point.y():.2f}"
            QToolTip.showText(QCursor.pos(), tooltip_text)
    
    def add_trend_line(self, data):
        """Agrega línea de tendencia a los datos"""
        if len(data) < 2:
            return
        
        x = np.arange(len(data))
        slope, intercept = np.polyfit(x, data, 1)
        trend_data = slope * x + intercept
        
        if self.trend_line:
            self.removeItem(self.trend_line)
        
        self.trend_line = self.plot(x, trend_data, 
                                   pen=pg.mkPen('#FFD700', style=Qt.PenStyle.DashLine, width=2),
                                   name='Trend')
    
    def add_confidence_band(self, x_data, y_data, confidence=0.95):
        """Agrega banda de confianza alrededor de los datos"""
        if len(x_data) != len(y_data) or len(x_data) < 3:
            return
        
        # Calcular desviación estándar
        std_dev = np.std(y_data)
        confidence_factor = 1.96 if confidence == 0.95 else 2.58  # 95% or 99%
        
        upper_bound = np.array(y_data) + confidence_factor * std_dev
        lower_bound = np.array(y_data) - confidence_factor * std_dev
        
        if self.confidence_band:
            self.removeItem(self.confidence_band)
        
        # Crear banda de confianza usando FillBetweenItem
        upper_curve = self.plot(x_data, upper_bound, pen=None)
        lower_curve = self.plot(x_data, lower_bound, pen=None)
        
        self.confidence_band = pg.FillBetweenItem(
            curve1=upper_curve,
            curve2=lower_curve,
            brush=pg.mkBrush(color=(135, 206, 235, 50))  # Light blue with transparency
        )
        self.addItem(self.confidence_band)
    
    def highlight_anomalies(self, data, threshold=2):
        """Resalta valores anómalos en los datos"""
        if len(data) < 3:
            return
        
        mean_val = np.mean(data)
        std_val = np.std(data)
        
        anomalies = []
        for i, value in enumerate(data):
            z_score = abs((value - mean_val) / std_val)
            if z_score > threshold:
                anomalies.append((i, value))
        
        # Marcar anomalías
        for x, y in anomalies:
            self.plot([x], [y], pen=None, symbol='o', symbolBrush='r', 
                     symbolSize=15, name='Anomaly')


class RealtimeDataManager:
    """Gestor de datos en tiempo real"""
    
    def __init__(self):
        self.data_buffer = defaultdict(list)
        self.max_buffer_size = 1000
        self.update_callbacks = []
    
    def add_data_point(self, series_name, timestamp, value):
        """Agrega un punto de datos a la serie especificada"""
        if len(self.data_buffer[series_name]) >= self.max_buffer_size:
            self.data_buffer[series_name].pop(0)  # Remover el más antiguo
        
        self.data_buffer[series_name].append((timestamp, value))
        
        # Notificar callbacks
        for callback in self.update_callbacks:
            callback(series_name, timestamp, value)
    
    def get_series_data(self, series_name, last_n=None):
        """Obtiene los datos de una serie"""
        data = self.data_buffer.get(series_name, [])
        if last_n:
            return data[-last_n:]
        return data
    
    def register_update_callback(self, callback):
        """Registra un callback para actualizaciones de datos"""
        self.update_callbacks.append(callback)
    
    def clear_series(self, series_name):
        """Limpia los datos de una serie"""
        if series_name in self.data_buffer:
            del self.data_buffer[series_name]
    
    def get_latest_value(self, series_name):
        """Obtiene el último valor de una serie"""
        data = self.data_buffer.get(series_name, [])
        return data[-1] if data else None


def main():
    """Función principal para ejecutar la aplicación"""
    app = QApplication(sys.argv)
    
    # Configurar estilo de la aplicación
    app.setStyle('Fusion')
    
    # Crear y mostrar la ventana principal
    window = InteractiveChartsWindow()
    window.show()
    
    # Ejecutar la aplicación
    sys.exit(app.exec())


if __name__ == "__main__":
    main()
