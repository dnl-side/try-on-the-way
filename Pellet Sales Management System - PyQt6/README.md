# Sales Analytics & Management System

> Professional sales data management system built with Python and PyQt6

![Python](https://img.shields.io/badge/Python-3.8+-blue.svg)
![PyQt6](https://img.shields.io/badge/PyQt6-6.4+-green.svg)
![MySQL](https://img.shields.io/badge/MySQL-8.0+-orange.svg)

## Overview

A comprehensive desktop application for sales data management and analysis, featuring real-time data visualization, advanced filtering capabilities, and professional reporting tools. Built with modern Python technologies and enterprise-grade architecture patterns.

## System Architecture

```
Project Structure
├── main.py                    # Application entry point & main menu
├── sales_entry.py             # Sales data input interface  
├── sales_dashboard.py         # Main analytics dashboard
├── interactive_charts.py      # Data visualization components
├── data_processor.py          # Business logic & data processing
└── advanced_search_system.py  # Advanced search & reporting
```

## Core Features

### Sales Data Management
- **Data Entry Interface**: Professional forms for sales transaction input
- **Validation System**: Real-time data validation with error handling
- **Multi-branch Support**: Handle multiple store locations and sales channels
- **Document Types**: Support for invoices, receipts, credit notes, and debit notes

### Advanced Analytics Dashboard
- **Interactive Charts**: Real-time visualization using PyQtGraph
- **Multi-dimensional Analysis**: Sales performance by branch, product, and time period
- **Trend Analysis**: Moving averages, growth rates, and forecasting
- **KPI Metrics**: Revenue, volume, efficiency, and performance indicators

### Professional Search & Filtering
- **Multi-criteria Search**: Filter by date range, branch, product, document type
- **Dynamic SQL Generation**: Secure parameterized queries with injection prevention
- **Real-time Results**: Instant search with performance optimization
- **Export Capabilities**: Professional Excel reports with corporate styling

### Data Visualization
- **Interactive Charts**: Mouse tooltips, zoom, and pan functionality  
- **Branch Comparison**: Side-by-side performance analysis
- **Time Series Analysis**: Weekly and monthly trend visualization
- **Custom Date Ranges**: Flexible period selection with calendar widgets

### Business Intelligence
- **Financial Calculations**: Tax calculations, currency conversion, profit margins
- **Performance Metrics**: Branch efficiency, product performance, sales trends
- **Executive Reporting**: Summary dashboards with key business indicators
- **Data Export**: Professional Excel reports with formatting and charts

## Technical Highlights

### Modern Python Architecture
- **MVC Pattern**: Clear separation between data, business logic, and presentation
- **Component-Based Design**: Modular, reusable components
- **Error Handling**: Comprehensive exception handling with logging
- **Type Hints**: Full type annotation for better code maintainability

### Database Integration
- **MySQL Connectivity**: Robust database connection management
- **Parameterized Queries**: SQL injection prevention
- **Transaction Management**: Data integrity with proper transaction handling
- **Connection Pooling**: Optimized database performance

### Professional UI/UX
- **PyQt6 Framework**: Modern desktop interface with native look and feel
- **Responsive Layouts**: Adaptive interface for different screen sizes
- **Professional Styling**: Corporate-grade CSS styling and themes
- **Accessibility**: Keyboard shortcuts and screen reader support

### Data Processing
- **Decimal Precision**: Accurate financial calculations using Python Decimal
- **Performance Optimization**: Efficient algorithms for large datasets
- **Memory Management**: Optimized data structures for scalability
- **Caching System**: Intelligent caching for improved performance

## Key Technical Components

### Sales Dashboard (`sales_dashboard.py`)
Main orchestration component featuring:
- Calendar-based date range selection
- Multi-table data presentation
- Real-time chart updates
- Professional Excel export functionality

### Interactive Charts (`interactive_charts.py`)
Advanced visualization engine with:
- PyQtGraph-based interactive charts
- Mouse hover tooltips with formatted data
- Multi-series data comparison
- Trend analysis and statistical overlays

### Data Processor (`data_processor.py`)
Business logic engine handling:
- Complex financial calculations
- Multi-dimensional data aggregation
- Performance metrics computation
- Data validation and integrity checks

### Advanced Search System (`advanced_search_system.py`)
Professional search interface featuring:
- Multi-criteria filtering capabilities
- Dynamic SQL query construction
- Professional Excel export with styling
- Real-time search with debouncing

## Installation & Setup

### Prerequisites
```bash
Python 3.8+
MySQL 8.0+
PyQt6
Required Python packages (see requirements.txt)
```

### Installation Steps
```bash
1. Clone the repository
2. Install dependencies: pip install -r requirements.txt
3. Configure database connection in conexión.py
4. Run the application: python main.py
```

## Database Schema

The system works with a normalized database structure including:
- **Sales transactions** with full audit trail
- **Product catalog** with categorization
- **Branch/location management**
- **User authentication** and permissions
- **Document types** and payment methods

## Security Features

- **Input Validation**: Comprehensive data sanitization
- **SQL Injection Prevention**: Parameterized queries throughout
- **User Authentication**: Role-based access control
- **Data Integrity**: Transaction-based operations
- **Error Logging**: Comprehensive audit trail

## Performance Optimizations

- **Query Optimization**: Efficient database queries with proper indexing
- **Memory Management**: Optimized data structures for large datasets
- **UI Responsiveness**: Asynchronous operations for smooth user experience
- **Caching Strategy**: Intelligent data caching for frequently accessed information

## Professional Features

### Excel Integration
- Automated report generation with corporate styling
- Conditional formatting for data analysis
- Multi-sheet workbooks with summary statistics
- Professional charts and visualizations

### Multi-language Support
- Internationalization support for Spanish/English
- Proper currency formatting (Chilean Peso)
- Date/time localization
- Cultural-specific business rules

### Scalability Considerations
- Modular architecture for easy feature additions
- Database abstraction for multiple database support
- Plugin-ready architecture for custom extensions
- API-ready structure for future web integration

## Development Approach

This project demonstrates professional software development practices:

- **Clean Architecture**: Separation of concerns with clear layer boundaries
- **SOLID Principles**: Object-oriented design following SOLID principles
- **Design Patterns**: Implementation of common enterprise patterns
- **Code Quality**: Comprehensive documentation and type hints
- **Error Handling**: Robust exception handling throughout the application

## Portfolio Highlights

This system showcases:

### Technical Skills
- Advanced Python programming with modern frameworks
- Desktop application development with PyQt6
- Database design and optimization
- Professional UI/UX design principles

### Software Engineering
- Enterprise architecture patterns
- Security best practices implementation
- Performance optimization techniques
- Professional documentation and code organization

### Business Domain Knowledge
- Sales and inventory management systems
- Financial calculations and reporting
- Multi-branch business operations
- Regulatory compliance (tax calculations, document management)


---

**Developed by**: Daniel Jara  
**Contact**: [dnl.side@gmail.com]

---


**Note**: This is a demonstration project showcasing professional software development capabilities. All sensitive business data has been anonymized for portfolio purposes.
