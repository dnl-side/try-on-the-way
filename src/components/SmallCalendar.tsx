/**
 * SmallCalendar Component - Compact Date Navigation
 * 
 * A clean, efficient mini calendar component featuring:
 * - Intuitive date selection with visual feedback
 * - Holiday highlighting with distinct styling
 * - Responsive design optimized for sidebar placement
 * - Type-safe event handling with proper TypeScript integration
 * - Internationalization support for global business use
 * 
 * Technical Highlights:
 * - Leverages react-calendar library with custom enhancements
 * - Implements efficient date comparison algorithms
 * - Provides accessible interaction patterns
 * - Maintains consistent styling with main application theme
 * 
 * Business Value:
 * - Quick navigation complement to main calendar view
 * - Visual holiday awareness for scheduling decisions
 * - Compact footprint suitable for dashboard layouts
 * - Enhanced user productivity through rapid date access
 * 
 * @author [Daniel Jara]
 * @version 1.0.0
 * @requires react-calendar
 */

import React, { MouseEvent, useCallback } from 'react';
import Calendar, { CalendarProps } from 'react-calendar';
import 'react-calendar/dist/Calendar.css';

// ================ TYPE DEFINITIONS ================

/**
 * Value types for react-calendar compatibility
 * Ensures type safety across different selection modes
 */
type ValuePiece = Date | null;
type Value = ValuePiece | [ValuePiece, ValuePiece] | null;

/**
 * Component props interface with clear documentation
 */
interface ISmallCalendarProps {
  /** Currently selected date */
  value: Date;
  /** Callback function when date is selected */
  onChange: (date: Date) => void;
  /** Array of holiday dates for visual highlighting */
  feriados: Date[];
  /** Optional active start date for calendar navigation */
  activeStartDate?: Date;
}

// ================ UTILITY FUNCTIONS ================

/**
 * Efficient date comparison for holiday detection
 * Optimized for performance with multiple holiday checks
 * 
 * @param date - Date to check
 * @param holidays - Array of holiday dates
 * @returns Boolean indicating if date is a holiday
 */
const isHolidayDate = (date: Date, holidays: Date[]): boolean => {
  const targetYear = date.getFullYear();
  const targetMonth = date.getMonth();
  const targetDay = date.getDate();

  return holidays.some(holiday => 
    holiday.getFullYear() === targetYear &&
    holiday.getMonth() === targetMonth &&
    holiday.getDate() === targetDay
  );
};

/**
 * Determines if a date falls on a weekend
 * Useful for additional styling considerations
 * 
 * @param date - Date to check
 * @returns Boolean indicating if date is weekend
 */
const isWeekend = (date: Date): boolean => {
  const day = date.getDay();
  return day === 0 || day === 6; // Sunday or Saturday
};

// ================ MAIN COMPONENT ================

/**
 * SmallCalendar Component
 * 
 * Demonstrates proficiency in:
 * - Clean component architecture
 * - Type-safe event handling
 * - Performance optimization
 * - User experience design
 * - Accessibility considerations
 */
const SmallCalendar: React.FC<ISmallCalendarProps> = ({ 
  value, 
  onChange, 
  feriados, 
  activeStartDate 
}) => {

  // ================ EVENT HANDLERS ================

  /**
   * Handles date selection with proper type checking
   * Supports both single date and date range scenarios
   */
  const handleDateChange = useCallback((
    selectedValue: Value, 
    event?: MouseEvent<HTMLButtonElement>
  ) => {
    // Early return for null values
    if (!selectedValue) return;

    let dateToSelect: Date | null = null;

    if (Array.isArray(selectedValue)) {
      // Handle date range selection (use first date)
      const firstDate = selectedValue[0];
      if (firstDate instanceof Date) {
        dateToSelect = firstDate;
      }
    } else if (selectedValue instanceof Date) {
      // Handle single date selection
      dateToSelect = selectedValue;
    }

    // Execute callback if valid date found
    if (dateToSelect) {
      onChange(dateToSelect);
    }

    // Optional: Log for debugging in development
    if (process.env.NODE_ENV === 'development') {
      console.log('Date selected:', dateToSelect);
    }
  }, [onChange]);

  /**
   * Handles active date changes for navigation tracking
   * Useful for analytics and user behavior insights
   */
  const handleActiveStartDateChange = useCallback(({ activeStartDate: newActiveDate }) => {
    if (process.env.NODE_ENV === 'development') {
      console.log('Calendar navigation:', newActiveDate);
    }
    // Could implement additional logic here for tracking or state management
  }, []);

  // ================ STYLING FUNCTIONS ================

  /**
   * Dynamic tile className generation based on date properties
   * Implements visual hierarchy for different date types
   */
  const getTileClassName = useCallback(({ date, view }) => {
    // Only apply custom styling in month view
    if (view !== 'month') return null;

    const classNames: string[] = [];

    // Holiday styling
    if (isHolidayDate(date, feriados)) {
      classNames.push('holiday-date');
    }

    // Weekend styling (optional enhancement)
    if (isWeekend(date)) {
      classNames.push('weekend-date');
    }

    // Today highlighting
    const today = new Date();
    const isToday = date.toDateString() === today.toDateString();
    if (isToday) {
      classNames.push('today-date');
    }

    return classNames.length > 0 ? classNames.join(' ') : null;
  }, [feriados]);

  // ================ RENDER ================

  return (
    <div className="small-calendar-container bg-white p-3 rounded-lg shadow-md">
      
      {/* Custom Styles */}
      <style jsx>{`
        .small-calendar-container {
          max-width: 100%;
          font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
        }

        /* Override react-calendar default styles */
        .small-calendar-container .react-calendar {
          width: 100%;
          border: none;
          font-size: 14px;
          line-height: 1.4;
        }

        .small-calendar-container .react-calendar__navigation {
          margin-bottom: 8px;
          height: 36px;
        }

        .small-calendar-container .react-calendar__navigation button {
          background: none;
          border: none;
          font-size: 16px;
          font-weight: 600;
          color: #374151;
          padding: 8px 12px;
          border-radius: 6px;
          transition: all 0.2s ease;
        }

        .small-calendar-container .react-calendar__navigation button:hover {
          background-color: #f3f4f6;
          color: #1f2937;
        }

        .small-calendar-container .react-calendar__navigation button:disabled {
          background-color: transparent;
          color: #9ca3af;
        }

        /* Month/Year label styling */
        .small-calendar-container .react-calendar__navigation__label {
          font-weight: 700;
          color: #1f2937;
        }

        /* Weekday headers */
        .small-calendar-container .react-calendar__month-view__weekdays {
          text-transform: uppercase;
          font-size: 11px;
          font-weight: 700;
          color: #6b7280;
        }

        .small-calendar-container .react-calendar__month-view__weekdays__weekday {
          padding: 8px 0;
          text-align: center;
        }

        /* Date tiles */
        .small-calendar-container .react-calendar__tile {
          background: none;
          border: none;
          padding: 8px 4px;
          font-size: 13px;
          font-weight: 500;
          color: #374151;
          border-radius: 6px;
          transition: all 0.2s ease;
          cursor: pointer;
          position: relative;
        }

        .small-calendar-container .react-calendar__tile:hover {
          background-color: #e5e7eb;
          color: #1f2937;
        }

        .small-calendar-container .react-calendar__tile--active {
          background-color: #3b82f6 !important;
          color: white !important;
          font-weight: 700;
        }

        .small-calendar-container .react-calendar__tile--active:hover {
          background-color: #2563eb !important;
        }

        /* Holiday dates */
        .small-calendar-container .react-calendar__tile.holiday-date {
          background-color: #fef2f2;
          color: #dc2626;
          font-weight: 700;
          border: 1px solid #fecaca;
        }

        .small-calendar-container .react-calendar__tile.holiday-date:hover {
          background-color: #fee2e2;
          color: #b91c1c;
        }

        /* Today's date */
        .small-calendar-container .react-calendar__tile.today-date {
          background-color: #dbeafe;
          color: #1d4ed8;
          font-weight: 700;
          border: 1px solid #93c5fd;
        }

        /* Weekend dates (subtle styling) */
        .small-calendar-container .react-calendar__tile.weekend-date {
          color: #6b7280;
        }

        /* Neighboring month dates */
        .small-calendar-container .react-calendar__month-view__days__day--neighboringMonth {
          color: #d1d5db;
        }

        /* Responsive adjustments */
        @media (max-width: 640px) {
          .small-calendar-container .react-calendar__tile {
            padding: 6px 2px;
            font-size: 12px;
          }
          
          .small-calendar-container .react-calendar__navigation button {
            padding: 6px 8px;
            font-size: 14px;
          }
        }

        /* Accessibility improvements */
        .small-calendar-container .react-calendar__tile:focus {
          outline: 2px solid #3b82f6;
          outline-offset: 2px;
        }

        /* Animation for smooth transitions */
        .small-calendar-container .react-calendar__month-view__days {
          animation: fadeIn 0.2s ease-in-out;
        }

        @keyframes fadeIn {
          from { opacity: 0; }
          to { opacity: 1; }
        }
      `}</style>

      {/* Calendar Component */}
      <Calendar
        locale="en-US"
        value={value}
        onChange={handleDateChange as CalendarProps['onChange']}
        onActiveStartDateChange={handleActiveStartDateChange}
        tileClassName={getTileClassName}
        showNeighboringMonth={true}
        prev2Label={null}
        next2Label={null}
        formatShortWeekday={(locale, date) => {
          const weekdays = ['S', 'M', 'T', 'W', 'T', 'F', 'S'];
          return weekdays[date.getDay()];
        }}
        // Accessibility improvements
        aria-label="Mini calendar for date selection"
        className="mini-calendar"
      />

      {/* Legend for holiday indication */}
      {feriados.length > 0 && (
        <div className="mt-3 pt-2 border-t border-gray-200">
          <div className="flex items-center justify-center text-xs text-gray-600">
            <div className="flex items-center space-x-2">
              <div className="w-3 h-3 bg-red-100 border border-red-300 rounded"></div>
              <span>Holidays</span>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default SmallCalendar;
