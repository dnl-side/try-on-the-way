"""
Pellet Sales Management System - Enterprise Desktop Application

A comprehensive PyQt6-based sales management system featuring:
- Multi-window interface with professional styling and theming
- Role-based authentication with granular privilege management
- Real-time database synchronization (MySQL + SQLite fallback strategy)
- Advanced data visualization with interactive charts (PyQtGraph)
- Complex business logic for sales validation and financial calculations
- Professional reporting with Excel export and custom formatting
- Automatic update system with version control and FTP deployment
- Multi-language support with resource management

Technical Achievements:
- Advanced PyQt6 interface with custom CSS styling and gradients
- Dual database architecture with automatic failover mechanisms
- Context-managed database connections with proper resource cleanup
- Threading implementation for non-blocking UI operations
- Complex signal/slot architecture for event-driven programming
- Professional error handling with user-friendly feedback
- Modular design following separation of concerns principles
- Resource management with dynamic path resolution
- Advanced validation logic for business rules and data integrity

Business Value:
- Streamlined sales process management for pellet distribution
- Real-time inventory and sales tracking across multiple branches
- Automated financial calculations with tax handling (IVA)
- Comprehensive reporting for business intelligence and analytics
- Improved data accuracy through validation and duplicate prevention
- Enhanced productivity through intuitive user interface design

@author Daniel Jara
@version 0.3
@since Python 3.8+
"""

import sys
import os
from PyQt6.QtWidgets import (
    QApplication, QDialog, QVBoxLayout, QHBoxLayout, QPushButton, 
    QMessageBox, QMainWindow, QSizePolicy, QWidget, QLabel
)
from PyQt6.QtGui import QGuiApplication, QIcon, QPixmap 
from PyQt6.QtCore import QCoreApplication, Qt, QSize

# Core application modules
from login import LoginWindow
from ingreso_ventas import IngresoVentasWindow
from ventas_diarias import VentasDiariasWindow
from resumen_ventas import ResumenVentasWindow
from update_dialog import UpdateDialog

# Utility modules
from resource_path import resource_path
from mysql.connector import Error
from database_connection import conectar, cerrar_conexion


class MenuWindow(QMainWindow):
    """
    Main Application Window - Central Dashboard Controller
    
    Demonstrates advanced PyQt6 development patterns:
    - Professional window management with proper sizing and positioning
    - Dynamic privilege-based UI control and feature restriction
    - Corporate branding integration with scalable graphics
    - Custom styling with CSS gradients and hover effects
    - Modular navigation system with result-based flow control
    - Version management with database-driven update notifications
    - Resource management with proper cleanup and memory handling
    
    Features:
    - Role-based access control with granular permissions
    - Auto-update notification system with version checking
    - Professional corporate styling with custom themes
    - Responsive layout design for different screen sizes
    - Modal dialog management with proper parent-child relationships
    """
    
    class MenuResults:
        """
        Navigation result constants for clean flow control
        Provides type-safe navigation between application modules
        """
        SALES_ENTRY = 1
        DAILY_SALES = 2  
        UPDATE_SYSTEM = 3
        SALES_SUMMARY = 4

    def __init__(self, privilege_level: int, current_version: str):
        """
        Initialize main application window
        
        Args:
            privilege_level (int): User privilege level (10=standard, 11=admin)
            current_version (str): Current application version for update checking
        """
        super().__init__()
        
        self.privilege_level = privilege_level
        self.current_version = current_version
        
        # Configure window properties and branding
        self.setWindowIcon(QIcon(resource_path('icons/app_icon.PNG')))
        self.setWindowTitle("Pellet Sales Management System")
        self._setup_window_geometry()
        self._apply_corporate_styling()
        
        # Initialize UI components
        self._create_main_interface()
        self._configure_privilege_restrictions()
        
        # Navigation result tracking
        self.navigation_result = None

    def _setup_window_geometry(self):
        """
        Configure window size and positioning for optimal user experience
        Implements responsive design principles for different screen sizes
        """
        screen = QGuiApplication.primaryScreen()
        screen_geometry = screen.availableGeometry()
        
        # Calculate optimal window size (20% width, 30% height)
        window_width = int(screen_geometry.width() * 0.2)
        window_height = int(screen_geometry.height() * 0.3)
        
        # Center window on screen
        x_position = (screen_geometry.width() - window_width) // 2
        y_position = (screen_geometry.height() - window_height) // 2
        
        self.setGeometry(x_position, y_position, window_width, window_height)

    def _apply_corporate_styling(self):
        """
        Apply professional corporate styling with custom CSS
        Implements modern gradient themes with hover effects
        """
        self.setStyleSheet("""
            QMainWindow {
                background: qlineargradient(x1:0, y1:0, x2:0, y2:1, 
                    stop:0 #1a1a2e, stop:1 #16213e);
            }
        """)

    def _create_main_interface(self):
        """
        Create and configure the main user interface
        Implements professional layout with corporate branding
        """
        # Create central widget and layout
        main_widget = QWidget()
        main_layout = QHBoxLayout(main_widget)

        # Corporate branding section
        branding_layout = self._create_branding_section()
        
        # Navigation menu section  
        menu_layout = self._create_navigation_menu()
        
        # Version information section
        version_section = self._create_version_section()

        # Assemble main layout
        main_layout.addLayout(branding_layout)
        main_layout.addLayout(menu_layout)
        main_layout.addWidget(version_section, 
            alignment=Qt.AlignmentFlag.AlignRight | Qt.AlignmentFlag.AlignBottom)

        self.setCentralWidget(main_widget)

    def _create_branding_section(self) -> QHBoxLayout:
        """
        Create corporate branding section with scalable logo
        
        Returns:
            QHBoxLayout: Configured branding layout
        """
        branding_layout = QHBoxLayout()
        
        # Load and scale corporate logo
        logo_label = QLabel()
        logo_pixmap = QPixmap(resource_path('icons/company_logo.png'))
        
        # Apply high-quality scaling for professional appearance
        scaled_logo = logo_pixmap.scaled(
            200, 200, 
            Qt.AspectRatioMode.KeepAspectRatio, 
            Qt.TransformationMode.SmoothTransformation
        )
        
        logo_label.setPixmap(scaled_logo)
        logo_label.setFixedSize(200, 200)
        logo_label.setAlignment(Qt.AlignmentFlag.AlignCenter)
        logo_label.setScaledContents(True)
        
        branding_layout.addWidget(logo_label)
        return branding_layout

    def _create_navigation_menu(self) -> QVBoxLayout:
        """
        Create main navigation menu with professional styling
        
        Returns:
            QVBoxLayout: Configured menu layout
        """
        menu_layout = QVBoxLayout()
        
        # Define menu buttons with icons and standardized sizing
        button_config = {
            'width': 300,
            'height': 60,
            'icon_size': QSize(32, 32)
        }
        
        # Create navigation buttons
        self.btn_sales_entry = self._create_menu_button(
            "SALES ENTRY", "buttons/sales_entry.PNG", button_config
        )
        self.btn_daily_sales = self._create_menu_button(
            "DAILY SALES", "buttons/daily_sales.PNG", button_config
        )
        self.btn_sales_summary = self._create_menu_button(
            "SALES SUMMARY", "buttons/sales_summary.PNG", button_config
        )
        self.btn_system_update = self._create_menu_button(
            "SYSTEM UPDATE", "buttons/system_update.PNG", button_config
        )
        self.btn_exit = self._create_menu_button(
            "EXIT", "buttons/exit.PNG", button_config
        )
        
        # Configure button event handlers
        self._configure_button_events()
        
        # Apply professional styling to all buttons
        self._apply_button_styling([
            self.btn_sales_entry, self.btn_daily_sales, 
            self.btn_sales_summary, self.btn_system_update, self.btn_exit
        ])
        
        # Add buttons to layout
        for button in [self.btn_sales_entry, self.btn_daily_sales, 
                      self.btn_sales_summary, self.btn_system_update, self.btn_exit]:
            menu_layout.addWidget(button)

        return menu_layout

    def _create_menu_button(self, text: str, icon_path: str, config: dict) -> QPushButton:
        """
        Create standardized menu button with icon and styling
        
        Args:
            text (str): Button display text
            icon_path (str): Relative path to button icon
            config (dict): Button configuration parameters
            
        Returns:
            QPushButton: Configured button instance
        """
        button = QPushButton(text)
        button.setIcon(QIcon(resource_path(icon_path)))
        button.setIconSize(config['icon_size'])
        button.setFixedSize(config['width'], config['height'])
        return button

    def _configure_button_events(self):
        """
        Configure event handlers for navigation buttons
        Implements clean separation between UI and business logic
        """
        self.btn_sales_entry.clicked.connect(self._handle_sales_entry)
        self.btn_daily_sales.clicked.connect(self._handle_daily_sales)
        self.btn_sales_summary.clicked.connect(self._handle_sales_summary)
        self.btn_system_update.clicked.connect(self._handle_system_update)
        self.btn_exit.clicked.connect(self._handle_exit)

    def _apply_button_styling(self, buttons: list):
        """
        Apply professional styling to menu buttons
        Implements responsive DPI scaling and hover effects
        
        Args:
            buttons (list): List of QPushButton instances to style
        """
        # Calculate DPI-aware font sizing
        dpi = QGuiApplication.primaryScreen().logicalDotsPerInch()
        base_font_size = 20
        scaled_font_size = int(base_font_size * dpi / 100) if dpi > 100 else base_font_size

        # Professional button styling with gradients and transitions
        button_style = f"""
            QPushButton {{
                font-size: {scaled_font_size}px;
                font-family: 'Segoe UI', Arial, sans-serif;
                font-weight: bold;
                color: white;
                background: qlineargradient(x1:0, y1:0, x2:0, y2:1,
                    stop:0 #0f3460, stop:1 #53354a);
                border: 2px solid #53354a;
                border-radius: 8px;
                padding: 10px;
                margin-bottom: 10px;
            }}
            QPushButton:hover {{
                background: qlineargradient(x1:0, y1:0, x2:1, y2:0,
                    stop:0 #e94560, stop:1 #0f3460);
                border: 2px solid #e94560;
            }}
            QPushButton:pressed {{
                background-color: #53354a;
                border: 2px solid #53354a;
            }}
            QPushButton:disabled {{
                background: qlineargradient(x1:0, y1:0, x2:0, y2:1,
                    stop:0 #666666, stop:1 #444444);
                border: 2px solid #666666;
                color: #cccccc;
            }}
        """

        for button in buttons:
            button.setStyleSheet(button_style)

    def _create_version_section(self) -> QLabel:
        """
        Create version information display
        
        Returns:
            QLabel: Styled version label
        """
        version_label = QLabel(f"Version: {self.current_version}")
        version_label.setStyleSheet("""
            color: white; 
            font-size: 10px;
            font-family: 'Segoe UI', Arial, sans-serif;
        """)
        version_label.setAlignment(
            Qt.AlignmentFlag.AlignRight | Qt.AlignmentFlag.AlignBottom
        )
        return version_label

    def _configure_privilege_restrictions(self):
        """
        Apply privilege-based access control
        Implements role-based feature restriction for security
        """
        if self.privilege_level < 10:
            self.btn_sales_entry.setEnabled(False)
            self.btn_sales_entry.setToolTip("Insufficient privileges for sales entry")

    # ================ EVENT HANDLERS ================

    def _handle_sales_entry(self):
        """Handle sales entry navigation"""
        self.navigation_result = self.MenuResults.SALES_ENTRY

    def _handle_daily_sales(self):
        """Handle daily sales navigation"""
        self.navigation_result = self.MenuResults.DAILY_SALES

    def _handle_sales_summary(self):
        """Handle sales summary navigation"""
        self.navigation_result = self.MenuResults.SALES_SUMMARY
        
    def _handle_system_update(self):
        """Handle system update navigation"""
        self.navigation_result = self.MenuResults.UPDATE_SYSTEM

    def _handle_exit(self):
        """Handle application exit with confirmation"""
        self.close()

    def _show_custom_message_box(self, title: str, text: str, icon=QMessageBox.Icon.Information):
        """
        Show styled message box with corporate theme
        
        Args:
            title (str): Dialog title
            text (str): Dialog message
            icon: Message box icon type
            
        Returns:
            int: User response code
        """
        msg_box = QMessageBox(self)
        msg_box.setWindowTitle(title)
        msg_box.setText(text)
        msg_box.setIcon(icon)
        msg_box.setStandardButtons(
            QMessageBox.StandardButton.Ok | QMessageBox.StandardButton.Cancel
        )
        msg_box.setDefaultButton(QMessageBox.StandardButton.Ok)
    
        # Apply corporate styling to message box
        msg_box.setStyleSheet("""
            QMessageBox {
                background: qlineargradient(x1:0, y1:0, x2:0, y2:1, 
                    stop:0 #1a1a2e, stop:1 #16213e);
                color: white;
                border: 2px solid #53354a;
                border-radius: 5px;
            }
            QLabel {
                color: white;
                font-family: 'Segoe UI', Arial, sans-serif;
            }
            QPushButton {
                background: qlineargradient(x1:0, y1:0, x2:0, y2:1, 
                    stop:0 #0f3460, stop:1 #53354a);
                color: white;
                border: 2px solid #53354a;
                border-radius: 5px;
                padding: 5px 10px;
                font-family: 'Segoe UI', Arial, sans-serif;
            }
            QPushButton:hover {
                background: qlineargradient(x1:0, y1:0, x2:1, y2:0, 
                    stop:0 #e94560, stop:1 #0f3460);
            }
            QPushButton:pressed {
                background-color: #53354a;
            }
        """)
    
        return msg_box.exec()

    def closeEvent(self, event):
        """
        Handle window close event with user confirmation
        Implements graceful application shutdown
        """
        reply = self._show_custom_message_box(
            'Confirm Exit', 
            'Are you sure you want to exit the application?', 
            QMessageBox.Icon.Question
        )
        
        if reply == QMessageBox.StandardButton.Ok:
            event.accept()
        else:
            event.ignore()


def get_current_version() -> str:
    """
    Retrieve current application version from database
    Implements version checking for update notifications
    
    Returns:
        str: Current version string or None if unavailable
    """
    try:
        conn = conectar()
        if conn is None:
            raise Exception("Database connection failed")
            
        cursor = conn.cursor()

        # Query latest version from version control table
        version_query = "SELECT version FROM version_app ORDER BY id DESC LIMIT 1"
        cursor.execute(version_query)
        version_record = cursor.fetchone()

        return version_record[0] if version_record else None

    except Error as database_error:
        print(f"Database connection error: {database_error}")
        return None

    finally:
        if cursor:
            cursor.close()
        if conn:
            cerrar_conexion(conn)


def main():
    """
    Main application entry point
    
    Demonstrates professional application architecture:
    - Clean separation of authentication and main application
    - Database-driven version management
    - Graceful error handling with user feedback
    - Proper resource cleanup and memory management
    - Modal dialog management with result-based navigation
    """
    app = QApplication(sys.argv)
    
    # ================ AUTHENTICATION PHASE ================
    
    login_window = LoginWindow()
    if login_window.exec() != QDialog.DialogCode.Accepted:
        sys.exit(-1)  # Exit if authentication fails
    
    user_privilege = login_window.privilegio
    
    # ================ VERSION MANAGEMENT ================
    
    current_app_version = "0.3"  # Application version
    database_version = get_current_version()
    
    # ================ MAIN APPLICATION LOOP ================
    
    main_window = MenuWindow(user_privilege, current_app_version)
    
    # Check for available updates
    if database_version and database_version != current_app_version:
        main_window._show_information_message_box(
            "Update Available", 
            "A new version is available. Please update for the latest features."
        )
        main_window.btn_system_update.setEnabled(True)
    else:
        main_window.btn_system_update.setEnabled(False)
    
    main_window.show()

    # Main application event loop with navigation handling
    while main_window.isVisible():
        app.processEvents()
        
        # Handle navigation results
        if main_window.navigation_result == MenuWindow.MenuResults.SALES_ENTRY:
            sales_window = IngresoVentasWindow(user_privilege)
            sales_window.exec()
            main_window.navigation_result = None

        elif main_window.navigation_result == MenuWindow.MenuResults.DAILY_SALES:
            daily_sales_window = VentasDiariasWindow()
            daily_sales_window.exec()
            main_window.navigation_result = None
            
        elif main_window.navigation_result == MenuWindow.MenuResults.UPDATE_SYSTEM:
            update_window = UpdateDialog(current_app_version)
            update_window.exec()
            main_window.navigation_result = None

        elif main_window.navigation_result == MenuWindow.MenuResults.SALES_SUMMARY:
            summary_window = ResumenVentasWindow()
            summary_window.show()
            summary_window.activateWindow()
            summary_window.raise_()

            # Handle summary window event loop
            while summary_window.isVisible():
                app.processEvents()

            main_window.navigation_result = None

    # Ensure clean application shutdown
    QCoreApplication.instance().quit()


if __name__ == "__main__":
    main()
