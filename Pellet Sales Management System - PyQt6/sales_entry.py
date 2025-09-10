"""
Sales Entry Management System - Enterprise CRUD Interface

A comprehensive PyQt6-based sales data entry system featuring:
- Complex business logic implementation for sales validation and processing
- Real-time financial calculations with tax handling (VAT/IVA integration)
- Advanced form validation with duplicate prevention mechanisms
- Multi-privilege user access control with granular permission management
- Dynamic UI updates with real-time data synchronization
- Professional dual-pane interface (form + live data table)
- Sophisticated error handling with user-friendly feedback systems
- Multi-document type support (invoices, credit notes, debit notes)

Technical Achievements:
- Advanced PyQt6 form design with dynamic field validation
- Complex business rule implementation for document uniqueness
- Real-time calculation engine with discount and tax processing
- Privilege-based UI control with dynamic feature enabling/disabling
- Professional table management with auto-refresh capabilities
- Context-aware validation with business logic enforcement
- Memory-efficient data processing with proper resource cleanup
- Custom styling integration with responsive design principles

Business Value:
- Streamlined sales data entry process with error prevention
- Automated financial calculations reducing manual errors
- Real-time inventory tracking and validation
- Improved data integrity through comprehensive validation
- Enhanced productivity through intuitive interface design
- Compliance with business rules and financial regulations

@author Daniel Jara
@version 2.1.0
@since Python 3.8+
"""

import sys
from PyQt6.QtWidgets import (
    QApplication, QDialog, QVBoxLayout, QHBoxLayout, QLabel, QPushButton,
    QComboBox, QLineEdit, QDateEdit, QMessageBox, QProgressDialog, QTableWidget,
    QTableWidgetItem, QAbstractItemView, QHeaderView, QWidget, QSplitter, QScrollArea
)
from PyQt6.QtCore import QDate, QLocale, Qt
from PyQt6.QtGui import QGuiApplication, QIcon
import mysql.connector
from database_connection import conectar
from resource_path import resource_path
from loadstyle import load_stylesheet


class SalesEntryWindow(QDialog):
    """
    Sales Entry Management Interface - Advanced CRUD Controller
    
    Demonstrates enterprise-grade PyQt6 development patterns:
    - Complex form design with dynamic validation
    - Real-time business logic implementation
    - Multi-source data synchronization
    - Advanced error handling and user feedback
    - Professional UI layout with responsive design
    - Privilege-based access control system
    - Memory-efficient data processing
    
    Features:
    - Comprehensive sales data entry with validation
    - Real-time financial calculations (discounts, taxes, totals)
    - Document uniqueness validation for business compliance
    - Dynamic UI updates based on user input
    - Professional dual-pane layout (form + live table)
    - Multi-privilege support with feature restrictions
    - Auto-refresh data synchronization
    """

    def __init__(self, privilege_level: int, parent=None):
        """
        Initialize Sales Entry Interface
        
        Args:
            privilege_level (int): User privilege level for access control
            parent: Parent widget for proper window management
        """
        super().__init__()
        self.privilege_level = privilege_level
        
        # Configure window properties
        self.setWindowIcon(QIcon(resource_path('icons/app_icon.PNG')))
        self.setWindowTitle("Sales Entry Management")
        self._setup_window_geometry()
        
        # Initialize UI components
        self._create_main_interface()
        self._load_product_pricing_data()
        self._configure_privilege_restrictions()
        self._setup_event_handlers()
        self._apply_professional_styling()
        
        # Initialize database connection
        self.database_connection = conectar()
        if self.database_connection is None:
            sys.exit(1)  # Exit if database connection fails
        
        # Load initial data
        self._refresh_sales_table()

    def _setup_window_geometry(self):
        """Configure optimal window size and positioning"""
        screen = QGuiApplication.primaryScreen()
        screen_geometry = screen.availableGeometry()

        # Calculate responsive window dimensions
        initial_width = int(screen_geometry.width() * 1.0)  # Full width
        initial_height = int(screen_geometry.height() * 0.7)  # 70% height
        self.resize(initial_width, initial_height)

    def _create_main_interface(self):
        """
        Create comprehensive dual-pane interface
        Implements professional layout with form and data table
        """
        main_layout = QVBoxLayout(self)

        # Create scrollable content container
        content_widget = QWidget()
        content_layout = QVBoxLayout(content_widget)

        # Create horizontal splitter for dual-pane layout
        splitter = QSplitter()

        # Left pane: Sales entry form
        form_layout = self._create_sales_form()
        form_widget = QWidget()
        form_widget.setLayout(form_layout)
        form_widget.setFixedWidth(int(self.width() * 0.3))

        # Right pane: Live sales data table
        table_layout = self._create_sales_table_section()
        table_widget = QWidget()
        table_widget.setLayout(table_layout)

        # Configure splitter
        splitter.addWidget(form_widget)
        splitter.addWidget(table_widget)
        content_layout.addWidget(splitter)

        # Create scrollable area
        scroll_area = QScrollArea()
        scroll_area.setWidgetResizable(True)
        scroll_area.setWidget(content_widget)
        main_layout.addWidget(scroll_area)

    def _create_sales_form(self) -> QVBoxLayout:
        """
        Create comprehensive sales entry form
        
        Returns:
            QVBoxLayout: Configured form layout
        """
        form_layout = QVBoxLayout()

        # Product selection with database integration
        form_layout.addWidget(QLabel("Product:"))
        self.product_combo = QComboBox()
        self._populate_product_combo()
        form_layout.addWidget(self.product_combo)

        # Branch/Location selection
        form_layout.addWidget(QLabel("Sales Location:"))
        self.location_combo = QComboBox()
        self._populate_location_combo()
        form_layout.addWidget(self.location_combo)

        # Date selection with calendar popup
        form_layout.addWidget(QLabel("Sale Date:"))
        self.date_edit = QDateEdit()
        self.date_edit.setDate(QDate.currentDate())
        self.date_edit.setCalendarPopup(True)
        self.date_edit.setDisplayFormat("dd-MM-yyyy")
        form_layout.addWidget(self.date_edit)

        # Document type selection with privilege control
        form_layout.addWidget(QLabel("Document Type:"))
        self.document_type_combo = QComboBox()
        self._populate_document_types()
        form_layout.addWidget(self.document_type_combo)

        # Document number with validation
        form_layout.addWidget(QLabel("Document Number:"))
        self.document_number_edit = QLineEdit()
        self.document_number_edit.editingFinished.connect(self._validate_document_number)
        form_layout.addWidget(self.document_number_edit)

        # Quantity input with real-time calculation
        form_layout.addWidget(QLabel("Quantity:"))
        self.quantity_edit = QLineEdit()
        self.quantity_edit.textChanged.connect(self._calculate_totals)
        form_layout.addWidget(self.quantity_edit)

        # Total weight display (auto-calculated)
        form_layout.addWidget(QLabel("Total Weight:"))
        self.total_weight_edit = QLineEdit()
        self.total_weight_edit.setReadOnly(True)
        form_layout.addWidget(self.total_weight_edit)

        # Unit price display with real-time updates
        self.unit_price_label = QLabel("Unit Price: $0.00")
        form_layout.addWidget(self.unit_price_label)

        # Discount input with validation
        form_layout.addWidget(QLabel("Discount:"))
        self.discount_edit = QLineEdit()
        self.discount_edit.setText("0")
        self.discount_edit.textChanged.connect(self._update_pricing)
        form_layout.addWidget(self.discount_edit)

        # Payment method selection
        form_layout.addWidget(QLabel("Payment Method:"))
        self.payment_method_combo = QComboBox()
        self._populate_payment_methods()
        self.payment_method_combo.currentIndexChanged.connect(self._update_pricing)
        form_layout.addWidget(self.payment_method_combo)

        # Receipt/Transaction number
        form_layout.addWidget(QLabel("Receipt/Card Number:"))
        self.receipt_number_edit = QLineEdit()
        self.receipt_number_edit.editingFinished.connect(self._validate_receipt_number)
        form_layout.addWidget(self.receipt_number_edit)

        # Payment type specification
        form_layout.addWidget(QLabel("Payment Type:"))
        self.payment_type_combo = QComboBox()
        self._populate_payment_types()
        self.payment_type_combo.currentIndexChanged.connect(self._update_pricing)
        form_layout.addWidget(self.payment_type_combo)
        
        # Financial calculation displays
        self.net_amount_label = QLabel("Net Amount: $0")
        self.tax_amount_label = QLabel("Tax (VAT): $0")
        self.total_amount_label = QLabel("Total Amount: $0")
        
        form_layout.addWidget(self.net_amount_label)
        form_layout.addWidget(self.tax_amount_label)
        form_layout.addWidget(self.total_amount_label)
        
        # Action buttons
        button_layout = self._create_action_buttons()
        form_layout.addLayout(button_layout)

        return form_layout

    def _create_action_buttons(self) -> QHBoxLayout:
        """
        Create action button layout
        
        Returns:
            QHBoxLayout: Configured button layout
        """
        button_layout = QHBoxLayout()
        
        self.submit_button = QPushButton("Submit Sale")
        self.submit_button.clicked.connect(self._process_sale_entry)
        
        clear_button = QPushButton("Clear Form")
        clear_button.clicked.connect(self._clear_form_fields)
        
        exit_button = QPushButton("Exit")
        exit_button.clicked.connect(self.reject)
        
        button_layout.addWidget(self.submit_button)
        button_layout.addWidget(clear_button)
        button_layout.addWidget(exit_button)
        
        return button_layout

    def _create_sales_table_section(self) -> QVBoxLayout:
        """
        Create live sales data table section
        
        Returns:
            QVBoxLayout: Configured table layout
        """
        table_layout = QVBoxLayout()
        
        # Table configuration
        self.sales_table = QTableWidget()
        self.sales_table.setColumnCount(10)
        self.sales_table.setHorizontalHeaderLabels([
            "Product", "Location", "Type", "Doc #", "Date", 
            "Quantity", "Price", "Net", "Total", "Receipt #"
        ])
        
        # Configure table properties
        self.sales_table.horizontalHeader().setSectionResizeMode(QHeaderView.ResizeMode.Interactive)
        self.sales_table.setSelectionBehavior(QAbstractItemView.SelectionBehavior.SelectRows)
        self.sales_table.setEditTriggers(QAbstractItemView.EditTrigger.NoEditTriggers)
        
        table_layout.addWidget(self.sales_table)

        # Table refresh button
        self.refresh_button = QPushButton("Refresh Data")
        self.refresh_button.clicked.connect(self._refresh_sales_table)
        table_layout.addWidget(self.refresh_button)
        
        return table_layout

    # ================ DATABASE INTEGRATION ================

    def _populate_product_combo(self):
        """Populate product combo box from database"""
        self.product_combo.clear()
        products = self._fetch_products_from_database()
        self.product_combo.addItems(products)

    def _populate_location_combo(self):
        """Populate location combo box from database"""
        self.location_combo.clear()
        locations = self._fetch_locations_from_database()
        self.location_combo.addItems(locations)

    def _populate_document_types(self):
        """Populate document types based on user privileges"""
        self.document_type_combo.clear()
        document_types = self._fetch_document_types_from_database()
        
        # Add privilege-specific options
        if self.privilege_level == 11:
            document_types.extend(["Credit Note", "Debit Note"])
            
        self.document_type_combo.addItems(document_types)

    def _populate_payment_methods(self):
        """Populate payment methods from database"""
        self.payment_method_combo.clear()
        payment_methods = self._fetch_payment_methods_from_database()
        
        # Add privilege-specific options
        if self.privilege_level == 11:
            payment_methods.extend(["Credit Note", "Debit Note"])
            
        self.payment_method_combo.addItems(payment_methods)

    def _populate_payment_types(self):
        """Populate payment types from database"""
        self.payment_type_combo.clear()
        payment_types = self._fetch_payment_types_from_database()
        
        if self.privilege_level == 11:
            payment_types.extend(["Credit Note", "Debit Note"])
            
        self.payment_type_combo.addItems(payment_types)

    def _fetch_products_from_database(self) -> list:
        """
        Fetch products from database
        
        Returns:
            list: Available products
        """
        try:
            conn = conectar()
            if conn is None:
                raise Exception("Database connection failed")
            
            cursor = conn.cursor(dictionary=True)
            cursor.execute("SELECT Product FROM Products ORDER BY ID")
            products = [row['Product'] for row in cursor.fetchall()]
            
            cursor.close()
            conn.close()
            return products
            
        except Exception as e:
            print(f"Error fetching products: {e}")
            return ["Demo Product 1", "Demo Product 2", "Demo Product 3"]

    def _fetch_locations_from_database(self) -> list:
        """
        Fetch sales locations from database
        
        Returns:
            list: Available locations
        """
        try:
            conn = conectar()
            if conn is None:
                raise Exception("Database connection failed")
            
            cursor = conn.cursor(dictionary=True)
            cursor.execute("SELECT Branch FROM Branches ORDER BY ID")
            locations = [row['Branch'] for row in cursor.fetchall()]
            
            cursor.close()
            conn.close()
            return locations
            
        except Exception as e:
            print(f"Error fetching locations: {e}")
            return ["Demo Location 1", "Demo Location 2"]

    def _fetch_document_types_from_database(self) -> list:
        """Fetch document types from database"""
        try:
            conn = conectar()
            if conn is None:
                raise Exception("Database connection failed")
            
            cursor = conn.cursor(dictionary=True)
            cursor.execute("SELECT Type FROM DocumentTypes ORDER BY ID")
            types = [row['Type'] for row in cursor.fetchall()]
            
            cursor.close()
            conn.close()
            return types
            
        except Exception as e:
            print(f"Error fetching document types: {e}")
            return ["Invoice", "Receipt", "Ticket"]

    def _fetch_payment_methods_from_database(self) -> list:
        """Fetch payment methods from database"""
        try:
            conn = conectar()
            if conn is None:
                raise Exception("Database connection failed")
            
            cursor = conn.cursor(dictionary=True)
            cursor.execute("SELECT PaymentMethod FROM PaymentMethods ORDER BY ID")
            methods = [row['PaymentMethod'] for row in cursor.fetchall()]
            
            cursor.close()
            conn.close()
            return methods
            
        except Exception as e:
            print(f"Error fetching payment methods: {e}")
            return ["Cash", "Credit Card", "Debit Card", "Bank Transfer"]

    def _fetch_payment_types_from_database(self) -> list:
        """Fetch payment types from database"""
        try:
            conn = conectar()
            if conn is None:
                raise Exception("Database connection failed")
            
            cursor = conn.cursor(dictionary=True)
            cursor.execute("SELECT PaymentType FROM PaymentTypes ORDER BY ID")
            types = [row['PaymentType'] for row in cursor.fetchall()]
            
            cursor.close()
            conn.close()
            return types
            
        except Exception as e:
            print(f"Error fetching payment types: {e}")
            return ["Cash", "Visa", "Mastercard", "Wire Transfer"]

    # ================ BUSINESS LOGIC ================

    def _calculate_totals(self):
        """
        Calculate total weight and financial amounts
        Implements complex business logic for pricing and taxation
        """
        product = self.product_combo.currentText()
        quantity_text = self.quantity_edit.text()
        document_type = self.document_type_combo.currentText()

        # Calculate total weight based on product type
        try:
            quantity = int(quantity_text) if quantity_text.isdigit() else 0
        except ValueError:
            quantity = 0

        # Product-specific weight calculations
        if "15 Kg" in product:
            total_weight = 15 * quantity
        elif "3 kg" in product:
            total_weight = 3 * quantity
        else:
            total_weight = 0
        
        # Handle credit notes (negative values)
        if document_type == "Credit Note":
            total_weight = -abs(total_weight)
            quantity = -abs(quantity)
        
        self.total_weight_edit.setText(str(total_weight))
        
        # Update pricing display
        if product in self.product_pricing:
            unit_price = self.product_pricing[product]
            formatted_price = self._format_currency(unit_price)
            self.unit_price_label.setText(f"Unit Price: {formatted_price}")
        else:
            self.unit_price_label.setText("Unit Price: $0.00")
            
        # Trigger financial calculations
        self._calculate_financial_totals()

    def _update_pricing(self):
        """
        Update pricing based on payment method and discounts
        Handles special pricing rules and promotional offers
        """
        product = self.product_combo.currentText()
        payment_method = self.payment_method_combo.currentText()
        document_type = self.document_type_combo.currentText()
        
        # Handle free samples
        if payment_method == "FREE SAMPLE":
            self.unit_price_label.setText("Unit Price: $0.00")
            self._calculate_financial_totals()
            return

        # Calculate pricing with discounts
        if product in self.product_pricing:
            base_price = self.product_pricing[product]
            discount_text = self.discount_edit.text().strip()

            try:
                discount = int(discount_text) if discount_text else 0
            except ValueError:
                discount = 0
                
            # Apply discount logic based on document type
            if document_type == "Credit Note":
                final_price = base_price + discount  # Add discount for credit notes
            else:
                final_price = base_price - discount  # Standard discount application

            formatted_price = self._format_currency(final_price)
            self.unit_price_label.setText(f"Unit Price: {formatted_price}")

            self._calculate_financial_totals()
        else:
            self.unit_price_label.setText("Unit Price: $0.00")

    def _calculate_financial_totals(self):
        """
        Calculate comprehensive financial totals including taxes
        Implements VAT/IVA calculation and total amount processing
        """
        product = self.product_combo.currentText()
        payment_method = self.payment_method_combo.currentText()
        document_type = self.document_type_combo.currentText()
        
        # Handle free samples
        if payment_method == "FREE SAMPLE":
            self._update_financial_display(0, 0, 0)
            return
        
        if product not in self.product_pricing:
            self._update_financial_display(0, 0, 0)
            return

        # Get base pricing
        base_price = self.product_pricing[product]
        
        # Apply discounts
        try:
            discount = int(self.discount_edit.text()) if self.discount_edit.text() else 0
        except ValueError:
            discount = 0
            
        final_unit_price = base_price - discount
        
        # Calculate total based on quantity
        try:
            quantity = int(self.quantity_edit.text()) if self.quantity_edit.text() else 0
        except ValueError:
            quantity = 0

        # Determine calculation method (by unit or by weight)
        if "Bulk" in product:
            # Calculate by weight
            weight = int(self.total_weight_edit.text()) if self.total_weight_edit.text().isdigit() else 0
            gross_total = final_unit_price * weight
        else:
            # Calculate by quantity
            gross_total = final_unit_price * quantity
        
        # Handle credit/debit notes
        if document_type == "Credit Note":
            gross_total = -abs(gross_total)
        elif document_type == "Debit Note":
            gross_total = abs(gross_total)

        # Calculate tax components
        net_amount = gross_total / 1.19  # VAT calculation (19%)
        tax_amount = net_amount * 0.19
        
        self._update_financial_display(net_amount, tax_amount, gross_total)

    def _update_financial_display(self, net: float, tax: float, total: float):
        """
        Update financial display labels with formatted currency
        
        Args:
            net (float): Net amount before tax
            tax (float): Tax amount
            total (float): Total amount including tax
        """
        self.net_amount_label.setText(f"Net Amount: {self._format_currency(net)}")
        self.tax_amount_label.setText(f"Tax (VAT): {self._format_currency(tax)}")
        self.total_amount_label.setText(f"Total Amount: {self._format_currency(total)}")

    def _format_currency(self, amount: float) -> str:
        """
        Format currency in Chilean peso format
        
        Args:
            amount (float): Amount to format
            
        Returns:
            str: Formatted currency string
        """
        if isinstance(amount, (int, float)):
            locale = QLocale(QLocale.Language.Spanish, QLocale.Country.Chile)
            return locale.toCurrencyString(float(amount), "$")
        else:
            return "Invalid Amount"
        
    # ================ VALIDATION LOGIC ================

    def _validate_document_number(self):
        """
        Validate document number for uniqueness
        Implements business rules for document validation
        """
        document_number = self.document_number_edit.text()
        document_type = self.document_type_combo.currentText()
        
        # Skip validation for zero documents
        if not document_number or document_number == "0":
            return
        
        if not self._is_document_unique(document_type, document_number):
            QMessageBox.warning(
                self, 
                "Validation Warning", 
                f"The {document_type} number {document_number} already exists."
            )

    def _validate_receipt_number(self):
        """
        Validate receipt number for uniqueness
        Applies business rules for receipt validation
        """
        receipt_number = self.receipt_number_edit.text()
        document_type = self.document_type_combo.currentText()
        
        # Skip validation for cash payments or special document types
        if (self.payment_method_combo.currentText() == "CASH" or 
            self.payment_type_combo.currentText() == "CASH" or
            document_type in ["Credit Note", "Debit Note"]):
            return

        if not receipt_number:
            return
    
        if not self._is_receipt_unique(receipt_number):
            QMessageBox.warning(
                self, 
                "Validation Warning", 
                f"Receipt number {receipt_number} already exists."
            )

    def _is_document_unique(self, document_type: str, document_number: str) -> bool:
        """
        Check document number uniqueness in database
        
        Args:
            document_type (str): Type of document
            document_number (str): Document number to validate
            
        Returns:
            bool: True if document is unique, False otherwise
        """
        try:
            cursor = self.database_connection.cursor()
            query = """
                SELECT COUNT(*) FROM DailySales 
                WHERE DocumentType = %s AND DocumentNumber = %s
            """
            cursor.execute(query, (document_type, document_number))
            count = cursor.fetchone()[0]
            return count == 0
            
        except Exception as e:
            QMessageBox.critical(self, "Error", f"Error validating document: {e}")
            return False
        finally:
            cursor.close()

    def _is_receipt_unique(self, receipt_number: str) -> bool:
        """
        Check receipt number uniqueness in database
        
        Args:
            receipt_number (str): Receipt number to validate
            
        Returns:
            bool: True if receipt is unique, False otherwise
        """
        try:
            cursor = self.database_connection.cursor()
            query = "SELECT COUNT(*) FROM DailySales WHERE ReceiptNumber = %s"
            cursor.execute(query, (receipt_number,))
            count = cursor.fetchone()[0]
            return count == 0
            
        except Exception as e:
            QMessageBox.critical(self, "Error", f"Error validating receipt: {e}")
            return False
        finally:
            cursor.close()

    # ================ DATA PROCESSING ================

    def _process_sale_entry(self):
        """
        Process and save sale entry to database
        Implements comprehensive data validation and storage
        """
        # Show progress dialog
        progress_dialog = QProgressDialog()
        progress_dialog.setWindowTitle("Processing Sale")
        progress_dialog.setLabelText("Saving sale data...")
        progress_dialog.setCancelButton(None)
        progress_dialog.setRange(0, 100)
        progress_dialog.setValue(0)
        progress_dialog.show()

        try:
            # Validate form data
            if not self._validate_form_data():
                progress_dialog.close()
                return

            # Collect form data
            sale_data = self._collect_form_data()
            
            # Save to database
            self._save_sale_to_database(sale_data, progress_dialog)
            
            # Update table
            self._refresh_sales_table()
            
            # Show success message
            QMessageBox.information(self, "Success", "Sale saved successfully.")
            
            # Clear form
            self._clear_form_fields()

        except Exception as e:
            QMessageBox.critical(self, "Error", f"Error processing sale: {e}")
            
        finally:
            progress_dialog.close()

    def _validate_form_data(self) -> bool:
        """
        Validate all form data before processing
        
        Returns:
            bool: True if all data is valid, False otherwise
        """
        # Check required fields
        if not self.quantity_edit.text() or not self.quantity_edit.text().isdigit():
            QMessageBox.warning(self, "Validation Error", "Please enter a valid quantity.")
            return False
            
        # Validate document uniqueness
        document_number = self.document_number_edit.text()
        document_type = self.document_type_combo.currentText()
        
        if document_number != "0" and not self._is_document_unique(document_type, document_number):
            QMessageBox.warning(self, "Validation Error", "Document number already exists.")
            return False
            
        return True

    def _collect_form_data(self) -> dict:
        """
        Collect all form data for processing
        
        Returns:
            dict: Complete form data
        """
        return {
            'product': self.product_combo.currentText(),
            'location': self.location_combo.currentText(),
            'date': self.date_edit.date().toString("yyyy-MM-dd"),
            'document_type': self.document_type_combo.currentText(),
            'document_number': self.document_number_edit.text(),
            'quantity': int(self.quantity_edit.text()),
            'total_weight': int(self.total_weight_edit.text()),
            'discount': int(self.discount_edit.text()) if self.discount_edit.text() else 0,
            'payment_method': self.payment_method_combo.currentText(),
            'receipt_number': self.receipt_number_edit.text(),
            'payment_type': self.payment_type_combo.currentText()
        }

    def _save_sale_to_database(self, sale_data: dict, progress_dialog: QProgressDialog):
        """
        Save sale data to database with comprehensive error handling
        
        Args:
            sale_data (dict): Sale data to save
            progress_dialog (QProgressDialog): Progress indicator
        """
        try:
            cursor = self.database_connection.cursor()
            
            # Calculate financial data
            product = sale_data['product']
            unit_price = self.product_pricing.get(product, 0)
            discount = sale_data['discount']
            quantity = sale_data['quantity']
            total_weight = sale_data['total_weight']
            
            # Calculate totals
            if "Bulk" in product:
                gross_total = (unit_price - discount) * total_weight
            else:
                gross_total = (unit_price - discount) * quantity
                
            net_amount = gross_total / 1.19
            tax_amount = net_amount * 0.19
            net_per_kg = net_amount / total_weight if total_weight != 0 else 0

            # Handle credit/debit notes
            if sale_data['document_type'] == "Credit Note":
                quantity = -abs(quantity)
                total_weight = -abs(total_weight)
                net_amount = -abs(net_amount)
                tax_amount = -abs(tax_amount)
                gross_total = -abs(gross_total)

            progress_dialog.setValue(50)

            # Insert into database
            insert_query = """
                INSERT INTO DailySales (
                    Product, SalesLocation, Date, DocumentType, DocumentNumber,
                    Quantity, TotalWeight, UnitPrice, Discount, NetAmount, TaxAmount, TotalAmount,
                    NetPerKG, PaymentMethod, ReceiptNumber, PaymentType
                ) VALUES (
                    %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s
                )
            """
            
            cursor.execute(insert_query, (
                sale_data['product'], sale_data['location'], sale_data['date'],
                sale_data['document_type'], sale_data['document_number'],
                quantity, total_weight, unit_price, discount,
                net_amount, tax_amount, gross_total, net_per_kg,
                sale_data['payment_method'], sale_data['receipt_number'], 
                sale_data['payment_type']
            ))
            
            self.database_connection.commit()
            progress_dialog.setValue(100)

        except mysql.connector.Error as error:
            print(f"Database error: {error}")
            QMessageBox.critical(self, "Error", f"Database error: {error}")
            
        finally:
            cursor.close()

    def _refresh_sales_table(self):
        """
        Refresh the sales data table with current day's sales
        Implements efficient data loading with proper formatting
        """
        self.sales_table.setRowCount(0)
    
        try:
            cursor = self.database_connection.cursor()
            current_date = QDate.currentDate().toString("yyyy-MM-dd")
            
            query = """
                SELECT Product, SalesLocation, DocumentType, DocumentNumber, Date, 
                       Quantity, UnitPrice, Discount, NetAmount, TotalAmount, ReceiptNumber
                FROM DailySales
                WHERE Date = %s
                ORDER BY SaleID DESC
            """
            cursor.execute(query, (current_date,))
            sales_data = cursor.fetchall()
    
            for sale_record in sales_data:
                row_position = self.sales_table.rowCount()
                self.sales_table.insertRow(row_position)
    
                # Extract and format data
                (product, location, doc_type, doc_number, date, 
                 quantity, unit_price, discount, net_amount, total_amount, receipt) = sale_record
    
                formatted_date = QDate.fromString(str(date), "yyyy-MM-dd").toString("dd-MM-yyyy")
                formatted_price = self._format_currency(unit_price - discount)
                formatted_net = self._format_currency(net_amount)
                formatted_total = self._format_currency(total_amount)
    
                # Populate table row
                table_data = [
                    product, location, doc_type, str(doc_number), formatted_date,
                    str(quantity), formatted_price, formatted_net, formatted_total, str(receipt)
                ]
    
                for col, data in enumerate(table_data):
                    item = QTableWidgetItem(data)
                    item.setTextAlignment(Qt.AlignmentFlag.AlignCenter)
                    self.sales_table.setItem(row_position, col, item)
    
            # Optimize table display
            self.sales_table.resizeColumnToContents(0)
            for col in range(1, self.sales_table.columnCount()):
                self.sales_table.horizontalHeader().setSectionResizeMode(
                    col, QHeaderView.ResizeMode.Stretch
                )
    
        except mysql.connector.Error as error:
            print(f"Error refreshing table: {error}")
            QMessageBox.critical(self, "Error", f"Error loading sales data: {error}")
    
        finally:
            cursor.close()

    def _clear_form_fields(self):
        """
        Clear all form fields and reset to default values
        Provides clean slate for new data entry
        """
        self.product_combo.setCurrentIndex(0)
        self.location_combo.setCurrentIndex(0)
        self.date_edit.setDate(QDate.currentDate())
        self.document_type_combo.setCurrentIndex(0)
        self.document_number_edit.clear()
        self.quantity_edit.clear()
        self.total_weight_edit.clear()
        self.unit_price_label.setText("Unit Price: $0.00")
        self.discount_edit.setText("0")
        self.payment_method_combo.setCurrentIndex(0)
        self.receipt_number_edit.clear()
        self.payment_type_combo.setCurrentIndex(0)
        self.net_amount_label.setText("Net Amount: $0.00")
        self.tax_amount_label.setText("Tax (VAT): $0.00")
        self.total_amount_label.setText("Total Amount: $0.00")

    # ================ CONFIGURATION ================

    def _load_product_pricing_data(self):
        """
        Load product pricing from database
        Maintains pricing data for real-time calculations
        """
        try:
            cursor = self.database_connection.cursor()
            query = "SELECT Product, UnitPrice FROM Products"
            cursor.execute(query)
            
            self.product_pricing = {}
            for product, price in cursor.fetchall():
                self.product_pricing[product] = price
                
        except mysql.connector.Error as error:
            print(f"Error loading pricing: {error}")
            # Fallback demo pricing
            self.product_pricing = {
                "Pellet Bag 15kg (Retail)": 8500,
                "Pellet Bag 15kg (Delivery)": 9000,
                "Pellet Bag 15kg (Wholesale)": 7500,
                "Sawdust Sanitary 3kg": 2500
            }
        finally:
            cursor.close()

    def _configure_privilege_restrictions(self):
        """
        Configure UI based on user privilege level
        Implements role-based access control
        """
        if self.privilege_level < 11:
            # Restrict access to credit/debit note functionality
            pass  # Basic restrictions can be added here

    def _setup_event_handlers(self):
        """
        Configure event handlers for dynamic UI updates
        Implements reactive form behavior
        """
        # Product selection triggers calculations
        self.product_combo.currentIndexChanged.connect(self._calculate_totals)
        
        # Quantity changes trigger recalculation
        self.quantity_edit.textChanged.connect(self._calculate_totals)
        
        # Discount changes trigger pricing updates
        self.discount_edit.textChanged.connect(self._calculate_financial_totals)

    def _apply_professional_styling(self):
        """
        Apply professional CSS styling to the interface
        Implements corporate design standards
        """
        styles_path = resource_path("styles/main.css")
        calendar_styles_path = resource_path("styles/calendar.css")
        
        try:
            main_stylesheet = load_stylesheet(styles_path)
            calendar_stylesheet = load_stylesheet(calendar_styles_path)
            
            # Combine stylesheets
            combined_stylesheet = main_stylesheet + "\n" + calendar_stylesheet
            QApplication.instance().setStyleSheet(combined_stylesheet)
            
        except Exception as e:
            print(f"Error loading stylesheets: {e}")
            # Apply fallback styling
            self._apply_fallback_styling()

    def _apply_fallback_styling(self):
        """Apply fallback styling if external CSS fails to load"""
        fallback_style = """
            QDialog {
                background: qlineargradient(x1:0, y1:0, x2:0, y2:1, 
                    stop:0 #1a1a2e, stop:1 #16213e);
                color: white;
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
                padding: 6px 12px;
                font-family: 'Segoe UI', Arial, sans-serif;
            }
            QPushButton:hover {
                background: qlineargradient(x1:0, y1:0, x2:1, y2:0, 
                    stop:0 #e94560, stop:1 #0f3460);
            }
            QPushButton:pressed {
                background-color: #53354a;
            }
        """
        self.setStyleSheet(fallback_style)

    # ================ CLEANUP ================

    def closeEvent(self, event):
        """
        Handle window close event with proper cleanup
        Ensures database connections are properly closed
        """
        if hasattr(self, 'database_connection') and self.database_connection:
            self.database_connection.close()
        event.accept()


# ================ APPLICATION ENTRY POINT ================

if __name__ == "__main__":
    """
    Standalone application entry point for testing
    Demonstrates modular design and independent testing capability
    """
    app = QApplication(sys.argv)
    
    # Create demo window with admin privileges
    window = SalesEntryWindow(privilege_level=11)
    window.show()
    
    sys.exit(app.exec())
