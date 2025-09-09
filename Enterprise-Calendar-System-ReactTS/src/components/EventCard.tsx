/**
 * CustomEventCard Component - Event Display Renderer
 * 
 * A specialized event renderer for react-big-calendar integration featuring:
 * - Department-specific visual identification through icons
 * - Holiday event special formatting and styling
 * - Responsive text handling for various calendar views
 * - Consistent branding with organizational departments
 * - Accessibility-compliant event representation
 * 
 * Technical Features:
 * - Custom render component for react-big-calendar
 * - Type-safe event property handling
 * - Efficient department mapping with fallbacks
 * - Optimized for performance in calendar grid views
 * - Seamless integration with AreaIcon system
 * 
 * Business Value:
 * - Enhanced visual recognition of event categories
 * - Improved user experience through consistent iconography
 * - Streamlined event identification across calendar views
 * - Professional presentation of organizational events
 * 
 * @author [Daniel Jara]
 * @version 1.0.0
 * @requires react-big-calendar, AreaIcon component
 */

import React from 'react';
import { EventProps } from 'react-big-calendar';
import { FaCalendarDay } from 'react-icons/fa';
import { IEvent } from '../types';
import AreaIcon from './AreaIcon';

// ================ TYPE DEFINITIONS ================

/**
 * Extended event properties for custom rendering
 */
interface CustomEventProps extends EventProps<IEvent> {
  event: IEvent;
}

// ================ CONSTANTS ================

/**
 * Department mapping for consistent naming across the application
 * Maintains backward compatibility while supporting internationalization
 */
const DEPARTMENT_ID_MAP: { [key: number]: string } = {
  1: 'Management',
  2: 'Administration', 
  3: 'IT Department',
  4: 'Human Resources',
  5: 'Accounting',
  6: 'Procurement',
  7: 'Legal & Compliance',
  8: 'Operations',
  9: 'Forestry',
  10: 'Harvest',
  11: 'Transportation',
  12: 'Infrastructure',
  13: 'Planning & GIS',
  14: 'Production & Sales',
  15: 'Technical Support',
  16: 'Project Management',
} as const;

/**
 * Legacy Spanish department names for backward compatibility
 * Supports existing data while transitioning to international standards
 */
const LEGACY_DEPARTMENT_MAP: { [key: number]: string } = {
  1: 'Gerencia',
  2: 'Administración',
  3: 'Informática',
  4: 'Recursos Humanos',
  5: 'Contabilidad',
  6: 'Compras',
  7: 'Legal y Regulación',
  8: 'Operaciones',
  9: 'Silvícola',
  10: 'Cosecha',
  11: 'Transporte',
  12: 'Caminos',
  13: 'Planificación y Cartografía',
  14: 'Producción y Ventas',
  15: 'TSA',
  16: 'Proyectos',
} as const;

// ================ UTILITY FUNCTIONS ================

/**
 * Maps department ID to human-readable name
 * Provides fallback handling for unknown departments
 * 
 * @param departmentId - Numeric department identifier
 * @param useLegacyNames - Whether to use Spanish legacy names
 * @returns Department name string
 */
const getDepartmentNameFromId = (
  departmentId: number, 
  useLegacyNames: boolean = false
): string => {
  const map = useLegacyNames ? LEGACY_DEPARTMENT_MAP : DEPARTMENT_ID_MAP;
  return map[departmentId] || 'All Departments';
};

/**
 * Determines if an event is a holiday
 * Supports multiple holiday identification patterns
 * 
 * @param event - Event object to check
 * @returns Boolean indicating holiday status
 */
const isHolidayEvent = (event: IEvent): boolean => {
  return !!(event.isFeriado || event.tipo === 'feriado');
};

/**
 * Gets the appropriate department name for display
 * Handles various data formats and provides sensible defaults
 * 
 * @param event - Event object containing department information
 * @returns Department name for icon mapping
 */
const getEventDepartmentName = (event: IEvent): string => {
  if (typeof event.area_id === 'number') {
    return getDepartmentNameFromId(event.area_id);
  }
  
  // Handle string area_id (edge case)
  if (typeof event.area_id === 'string') {
    const numericId = parseInt(event.area_id, 10);
    if (!isNaN(numericId)) {
      return getDepartmentNameFromId(numericId);
    }
  }
  
  // Fallback to default
  return 'All Departments';
};

// ================ COMPONENTS ================

/**
 * Holiday Event Renderer
 * Specialized component for displaying holiday events with distinct styling
 */
const HolidayEventCard: React.FC<{ event: IEvent }> = ({ event }) => (
  <div 
    className="flex items-center space-x-1 holiday-event-card"
    role="button"
    tabIndex={0}
    aria-label={`Holiday: ${event.titulo}`}
  >
    <span className="holiday-icon" aria-hidden="true">
      <FaCalendarDay size={12} />
    </span>
    <span className="event-title truncate font-medium">
      {event.titulo}
    </span>
  </div>
);

/**
 * Department Event Renderer
 * Standard component for displaying departmental events with appropriate iconography
 */
const DepartmentEventCard: React.FC<{ event: IEvent }> = ({ event }) => {
  const departmentName = getEventDepartmentName(event);
  
  return (
    <div 
      className="flex items-center space-x-1 department-event-card"
      role="button"
      tabIndex={0}
      aria-label={`${departmentName} event: ${event.titulo}`}
    >
      <AreaIcon 
        areaName={departmentName} 
        size="sm"
        className="flex-shrink-0"
        aria-hidden="true"
      />
      <span className="event-title truncate font-medium">
        {event.titulo}
      </span>
    </div>
  );
};

// ================ MAIN COMPONENT ================

/**
 * CustomEventCard Component
 * 
 * Primary event renderer for react-big-calendar integration.
 * Demonstrates:
 * - Component composition patterns
 * - Conditional rendering strategies
 * - Performance-optimized event display
 * - Accessibility best practices
 * - Consistent visual language
 */
export const CustomEventCard: React.FC<CustomEventProps> = ({ event }) => {
  // Early return for holiday events
  if (isHolidayEvent(event)) {
    return <HolidayEventCard event={event} />;
  }
  
  // Standard department event rendering
  return <DepartmentEventCard event={event} />;
};

// ================ ALTERNATIVE RENDERERS ================

/**
 * Compact Event Card
 * Minimalist renderer for dense calendar views
 */
export const CompactEventCard: React.FC<CustomEventProps> = ({ event }) => {
  const isHoliday = isHolidayEvent(event);
  
  return (
    <div 
      className={`compact-event ${isHoliday ? 'compact-holiday' : 'compact-department'}`}
      title={event.titulo}
      aria-label={event.titulo}
    >
      {isHoliday ? (
        <FaCalendarDay size={10} />
      ) : (
        <AreaIcon 
          areaName={getEventDepartmentName(event)} 
          size="sm" 
        />
      )}
    </div>
  );
};

/**
 * Detailed Event Card
 * Rich renderer for expanded calendar views with additional information
 */
export const DetailedEventCard: React.FC<CustomEventProps> = ({ event }) => {
  const isHoliday = isHolidayEvent(event);
  const departmentName = getEventDepartmentName(event);
  
  return (
    <div className={`detailed-event ${isHoliday ? 'detailed-holiday' : 'detailed-department'}`}>
      <div className="flex items-start space-x-2">
        <div className="flex-shrink-0 mt-0.5">
          {isHoliday ? (
            <FaCalendarDay size={14} />
          ) : (
            <AreaIcon areaName={departmentName} size="md" />
          )}
        </div>
        <div className="flex-1 min-w-0">
          <h4 className="text-sm font-semibold truncate">
            {event.titulo}
          </h4>
          {event.descripcion && (
            <p className="text-xs text-gray-600 truncate mt-1">
              {event.descripcion}
            </p>
          )}
          {!isHoliday && (
            <p className="text-xs text-gray-500 mt-1">
              {departmentName}
            </p>
          )}
        </div>
      </div>
    </div>
  );
};

// ================ UTILITY EXPORTS ================

/**
 * Export department mapping for external use
 */
export { getDepartmentNameFromId, isHolidayEvent, getEventDepartmentName };

/**
 * Export department constants for validation
 */
export { DEPARTMENT_ID_MAP, LEGACY_DEPARTMENT_MAP };

export default CustomEventCard;
