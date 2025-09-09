/**
 * CalendarView Component - Advanced Calendar Interface
 * 
 * A sophisticated calendar component built with react-big-calendar featuring:
 * - Multiple view modes (month, week, day, agenda)
 * - Interactive event handling with mouse wheel navigation
 * - Dynamic styling based on event categories and departments
 * - Holiday highlighting and special day marking
 * - Responsive design with custom event cards
 * - Internationalization support
 * 
 * Key Technical Features:
 * - Performance optimized with useMemo and useCallback
 * - Custom event styling with CSS-in-JS
 * - Advanced date manipulation with dayjs
 * - Type-safe event handling with TypeScript
 * - Accessibility considerations for keyboard navigation
 * 
 * @author [Daniel Jara]
 * @version 1.0.0
 * @requires react-big-calendar, dayjs
 */

import { useMemo, useState, useCallback } from 'react';
import { Calendar, dayjsLocalizer, View, NavigateAction } from 'react-big-calendar';
import dayjs from 'dayjs';
import 'dayjs/locale/en';
import 'react-big-calendar/lib/css/react-big-calendar.css';

// Types
import { IEvent } from '../types';

// Components
import { CustomEventCard } from './CustomEventCard';

// Internationalization
import { messagesEn } from '../i18n/messagesEn';

// Configure dayjs localizer for international support
dayjs.locale('en');
const localizer = dayjsLocalizer(dayjs);

// ================ INTERFACES ================

/**
 * Props interface for CalendarView component
 */
interface ICalendarViewProps {
  /** Array of events to display on the calendar */
  events: IEvent[];
  /** Current date being displayed */
  date: Date;
  /** Callback for date navigation changes */
  onDateChange: (newDate: Date) => void;
  /** Optional callback for event selection */
  onSelectEvent?: (event: IEvent) => void;
  /** Optional callback for empty slot selection */
  onSelectSlot?: (slotInfo: { start: Date }) => void;
  /** Default calendar view mode */
  defaultView?: View;
  /** Currently selected date for highlighting */
  selectedDate?: Date;
}

/**
 * Extended event interface for react-big-calendar compatibility
 */
interface CalendarEvent extends IEvent {
  start: Date;
  end: Date;
  title: string;
}

// ================ CONSTANTS ================

/**
 * Department color scheme for visual categorization
 * Carefully chosen colors for accessibility and professional appearance
 */
const DEPARTMENT_COLORS = {
  1: '#1E3A8A',  // Deep Blue - IT Department
  2: '#F1FAEE',  // Light Mint - Management
  3: '#457B9D',  // Steel Blue - Administration
  4: '#F4A261',  // Warm Orange - Human Resources
  5: '#2A9D8F',  // Teal - Accounting
  6: '#E76F51',  // Coral - Procurement
  7: '#264653',  // Dark Green - Legal & Regulation
  8: '#E9C46A',  // Golden Yellow - Operations
  9: '#2ECC71',  // Emerald Green - Forestry
  10: '#D4A017', // Gold - Harvest
  11: '#1ABC9C', // Turquoise - Transportation
  12: '#C0392B', // Crimson - Roads
  13: '#3498DB', // Sky Blue - Planning & Cartography
  14: '#9B59B6', // Purple - Production & Sales
  15: '#34495E', // Slate Gray - TSA
  16: '#F39C12', // Orange - Projects
} as const;

/**
 * Holiday event color - High contrast red for visibility
 */
const HOLIDAY_COLOR = '#E63946';

// ================ STYLE DEFINITIONS ================

/**
 * CSS-in-JS styles for calendar customization
 * Ensures consistent styling across different browsers and devices
 */
const CALENDAR_STYLES = `
  /* Base event styling */
  .rbc-event {
    padding: 2px 4px !important;
    border-radius: 4px !important;
    border: none !important;
    display: flex !important;
    align-items: center !important;
    min-height: 20px !important;
    font-weight: 500 !important;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1) !important;
  }

  /* Event content typography */
  .rbc-event-content {
    font-size: 12px !important;
    line-height: 1.2 !important;
    white-space: normal !important;
    overflow: hidden !important;
    text-overflow: ellipsis !important;
    display: -webkit-box !important;
    -webkit-line-clamp: 2 !important;
    -webkit-box-orient: vertical !important;
  }

  /* Event title styling */
  .event-title {
    font-size: 12px !important;
    line-height: 1.2 !important;
    white-space: normal !important;
    font-weight: 600 !important;
  }

  /* Holiday event special styling */
  .holiday-event {
    background-color: ${HOLIDAY_COLOR} !important;
    color: #fff !important;
    border: 1px dashed #fff !important;
    font-weight: 700 !important;
    animation: holidayPulse 2s ease-in-out infinite alternate;
  }

  @keyframes holidayPulse {
    from { opacity: 0.9; }
    to { opacity: 1; }
  }

  /* Department-specific color classes */
  ${Object.entries(DEPARTMENT_COLORS).map(([id, color]) => `
    .department-${id} {
      background-color: ${color} !important;
      color: ${color === '#F1FAEE' ? '#000' : '#fff'} !important;
      border: 1px solid ${color === '#F1FAEE' ? '#ddd' : color} !important;
    }
  `).join('')}

  /* Selected event highlighting */
  .rbc-selected-event {
    border: 2px solid #000 !important;
    box-shadow: 0 0 0 2px rgba(0, 0, 0, 0.2) !important;
  }

  /* Month view specific adjustments */
  .rbc-month-view .rbc-event {
    padding: 1px 2px !important;
    min-height: 18px !important;
  }

  .rbc-month-view .rbc-event-content {
    font-size: 10px !important;
    -webkit-line-clamp: 1 !important;
  }

  .rbc-month-view .event-title {
    font-size: 11px !important;
  }

  /* Week and day view adjustments */
  .rbc-time-view .rbc-event {
    border-left: 3px solid rgba(255, 255, 255, 0.8) !important;
  }

  /* Accessibility improvements */
  .rbc-event:focus {
    outline: 2px solid #4A90E2 !important;
    outline-offset: 2px !important;
  }

  /* Responsive design for mobile devices */
  @media (max-width: 768px) {
    .rbc-event {
      min-height: 16px !important;
      padding: 1px 2px !important;
    }
    
    .rbc-event-content,
    .event-title {
      font-size: 10px !important;
    }
  }
`;

// ================ MAIN COMPONENT ================

/**
 * CalendarView Component
 * 
 * Advanced calendar interface with comprehensive event management capabilities.
 * Demonstrates proficiency in:
 * - Complex React component architecture
 * - Performance optimization techniques
 * - Advanced user interaction patterns
 * - Responsive design principles
 * - Accessibility standards
 */
const CalendarView: React.FC<ICalendarViewProps> = ({
  events,
  date,
  onDateChange,
  onSelectEvent,
  onSelectSlot,
  defaultView = 'month',
  selectedDate,
}) => {
  // ================ STATE MANAGEMENT ================
  
  const [currentView, setCurrentView] = useState<View>(defaultView);

  // ================ MEMOIZED COMPUTATIONS ================

  /**
   * Transform events for react-big-calendar compatibility
   * Optimized with useMemo to prevent unnecessary re-computations
   */
  const calendarEvents = useMemo((): CalendarEvent[] => {
    return events.map((event) => ({
      ...event,
      start: new Date(event.fecha_inicio),
      end: new Date(event.fecha_fin),
      title: event.titulo,
    }));
  }, [events]);

  // ================ EVENT HANDLERS ================

  /**
   * Mouse wheel navigation for month view
   * Provides smooth navigation experience similar to modern calendar apps
   */
  const handleWheelNavigation = useCallback(
    (e: React.WheelEvent<HTMLDivElement>) => {
      // Only enable wheel navigation in month view
      if (currentView !== 'month') return;
      
      e.preventDefault();
      
      const direction = e.deltaY < 0 ? -1 : 1;
      const newDate = dayjs(date).add(direction, 'month').toDate();
      onDateChange(newDate);
    },
    [date, onDateChange, currentView]
  );

  /**
   * Dynamic event styling based on event properties
   * Implements visual categorization for better user experience
   */
  const getEventStyle = useCallback(
    (event: IEvent, _start: Date, _end: Date, isSelected: boolean) => {
      // Holiday events get special styling
      if (event.isFeriado || event.tipo === 'feriado') {
        return {
          className: 'holiday-event' + (isSelected ? ' rbc-selected-event' : ''),
        };
      }

      // Department-based styling
      const departmentClass = event.area_id ? `department-${event.area_id}` : 'department-1';
      
      return {
        className: departmentClass + (isSelected ? ' rbc-selected-event' : ''),
      };
    },
    []
  );

  /**
   * Custom day cell styling for special days
   * Highlights holidays, weekends, and selected dates
   */
  const getDayProps = useCallback(
    (day: Date) => {
      const isSunday = day.getDay() === 0;
      const isHoliday = events.some(
        (event) =>
          (event.isFeriado || event.tipo === 'feriado') &&
          new Date(event.fecha_inicio).toDateString() === day.toDateString()
      );
      const isSelected = selectedDate && day.toDateString() === selectedDate.toDateString();

      const dayStyles: React.CSSProperties = {};

      // Style weekends and holidays
      if (isSunday || isHoliday) {
        dayStyles.backgroundColor = '#ffe5e5';
        dayStyles.color = '#d32f2f';
        dayStyles.fontWeight = '600';
      }

      // Highlight selected date
      if (isSelected) {
        dayStyles.border = '2px solid #3b82f6';
        dayStyles.borderRadius = '8px';
        dayStyles.backgroundColor = isHoliday || isSunday 
          ? '#e3f2fd' 
          : '#f3f4f6';
      }

      return { style: dayStyles };
    },
    [events, selectedDate]
  );

  /**
   * Navigation handler with date validation
   */
  const handleNavigate = useCallback(
    (newDate: Date, view: View, action: NavigateAction) => {
      // Validate date range if needed (e.g., prevent navigation too far in the past/future)
      const minDate = dayjs().subtract(5, 'years').toDate();
      const maxDate = dayjs().add(5, 'years').toDate();
      
      if (newDate >= minDate && newDate <= maxDate) {
        onDateChange(newDate);
      }
    },
    [onDateChange]
  );

  /**
   * View change handler with analytics potential
   */
  const handleViewChange = useCallback((view: View) => {
    setCurrentView(view);
    // Could add analytics tracking here
    console.log(`Calendar view changed to: ${view}`);
  }, []);

  // ================ RENDER ================

  return (
    <div
      className="calendar-container h-full"
      style={{ 
        backgroundColor: 'white', 
        color: '#000', 
        padding: '1rem',
        position: 'relative'
      }}
      onWheel={handleWheelNavigation}
      role="application"
      aria-label="Interactive Event Calendar"
    >
      {/* Inject custom styles */}
      <style>{CALENDAR_STYLES}</style>

      {/* Main Calendar Component */}
      <Calendar
        // Core configuration
        localizer={localizer}
        events={calendarEvents}
        date={date}
        onNavigate={handleNavigate}
        
        // Date accessors
        startAccessor="start"
        endAccessor="end"
        titleAccessor="title"
        
        // View configuration
        views={['month', 'week', 'day', 'agenda']}
        defaultView={defaultView}
        view={currentView}
        onView={handleViewChange}
        
        // Localization
        culture="en"
        messages={messagesEn}
        
        // Event handlers
        onSelectEvent={(event) => onSelectEvent?.(event as IEvent)}
        onSelectSlot={onSelectSlot}
        selectable
        
        // Custom components and styling
        components={{
          event: CustomEventCard,
        }}
        eventPropGetter={getEventStyle}
        dayPropGetter={getDayProps}
        
        // Accessibility
        step={15} // 15-minute intervals
        timeslots={4} // 4 slots per hour
        min={new Date(0, 0, 0, 6, 0, 0)} // Start at 6 AM
        max={new Date(0, 0, 0, 22, 0, 0)} // End at 10 PM
        
        // Additional features
        popup
        popupOffset={30}
        showMultiDayTimes
        rtl={false}
      />
      
      {/* Loading overlay could be added here if needed */}
      {/* Accessibility announcements could be added here */}
    </div>
  );
};

export default CalendarView;
