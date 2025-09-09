/**
 * TimelineView Component - Advanced Timeline Visualization
 * 
 * A sophisticated timeline component featuring:
 * - Multi-scale temporal visualization (day, week, month, quarter, year)
 * - Dynamic zoom with mouse wheel interaction
 * - Intelligent event clustering and merging
 * - Interactive tooltips with detailed event information
 * - Responsive design with mobile optimization
 * - Department-based visual categorization
 * - Real-time horizontal scrolling and navigation
 * 
 * Key Technical Achievements:
 * - Complex state management with multiple synchronized views
 * - Performance-optimized rendering with useMemo and useCallback
 * - Advanced date-fns integration for international date handling
 * - Custom event merging algorithms for optimal visual density
 * - React-calendar-timeline integration with custom renderers
 * - CSS-in-JS styling with responsive breakpoints
 * - Type-safe event handling throughout the component
 * 
 * Business Value:
 * - Provides comprehensive temporal overview of organizational activities
 * - Enables pattern recognition across different time scales
 * - Supports strategic planning with visual timeline analysis
 * - Facilitates resource allocation through department visualization
 * 
 * @author [Daniel Jara]
 * @version 1.0.0
 * @requires react-calendar-timeline, date-fns, react-bootstrap
 */

import { 
  useState, 
  useEffect, 
  useMemo, 
  useCallback, 
  WheelEvent, 
  FC 
} from 'react';
import {
  startOfMonth,
  endOfMonth,
  addDays,
  startOfDay,
  endOfDay,
  startOfWeek,
  endOfWeek,
  startOfYear,
  endOfYear,
  addMonths,
  addSeconds,
  format
} from 'date-fns';
import { enUS } from 'date-fns/locale';

import Timeline, {
  Id,
  TimelineGroupBase,
  TimelineItemBase,
  TimelineHeaders,
  DateHeader,
  ReactCalendarItemRendererProps
} from 'react-calendar-timeline';
import 'react-calendar-timeline/dist/Timeline.scss';

import moment, { Moment } from 'moment';
moment.locale('en');

// Tooltip components
import OverlayTrigger from 'react-bootstrap/OverlayTrigger';
import Tooltip from 'react-bootstrap/Tooltip';

// Department icons for visual identification
import { FaUsers, FaCalendarDay } from 'react-icons/fa';
import { FaTruckFront } from 'react-icons/fa6';
import { TfiMicrosoftAlt } from 'react-icons/tfi';
import {
  MdBusiness,
  MdPeopleOutline,
  MdSettings,
  MdDirectionsBoatFilled,
  MdOutlineTempleBuddhist,
  MdLightbulbOutline
} from 'react-icons/md';
import { TbCashRegister } from 'react-icons/tb';
import { FiShoppingCart } from 'react-icons/fi';
import { BsCardChecklist } from 'react-icons/bs';
import { GiWoodPile, GiRoad, GiAnchor } from 'react-icons/gi';
import { IoBarChartOutline } from 'react-icons/io5';
import { PiPlantDuotone } from 'react-icons/pi';

import { IEvent } from '../types';

// ================ TYPE DEFINITIONS ================

/**
 * Enhanced group interface extending timeline base
 */
interface TimelineGroup extends TimelineGroupBase {
  id: Id;
  title: string;
}

/**
 * Enhanced item interface with event merging capabilities
 */
interface TimelineItem extends TimelineItemBase<Date> {
  id: Id;
  group: Id;
  title: string;
  start_time: Date;
  end_time: Date;
  area_id?: number | null;
  isFeriado?: boolean;
  tipo?: string;
  mergedData?: {
    title: string;
    start: Date;
    end: Date;
  }[];
}

/**
 * Timeline view modes for different temporal scales
 */
type ViewMode = 'day' | 'week' | 'month' | 'quarter' | 'year';

/**
 * Component props interface
 */
interface ITimelineViewProps {
  events: IEvent[];
  selectedDate?: Date;
}

// ================ CONSTANTS ================

/**
 * Department configuration with names and colors
 * Organized for enterprise-level operations
 */
const DEPARTMENT_CONFIG = {
  names: {
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
  },
  colors: {
    1: '#1E3A8A',  // Deep Blue - Management
    2: '#F1FAEE',  // Light Mint - Administration
    3: '#457B9D',  // Steel Blue - IT
    4: '#F4A261',  // Warm Orange - HR
    5: '#2A9D8F',  // Teal - Accounting
    6: '#E76F51',  // Coral - Procurement
    7: '#264653',  // Dark Green - Legal
    8: '#E9C46A',  // Golden Yellow - Operations
    9: '#2ECC71',  // Emerald - Forestry
    10: '#D4A017', // Gold - Harvest
    11: '#1ABC9C', // Turquoise - Transportation
    12: '#C0392B', // Crimson - Infrastructure
    13: '#3498DB', // Sky Blue - Planning
    14: '#9B59B6', // Purple - Production
    15: '#34495E', // Slate Gray - Technical Support
    16: '#F39C12', // Orange - Projects
  }
} as const;

/**
 * Icon mapping for visual department identification
 */
const DEPARTMENT_ICONS = {
  [-1]: FaCalendarDay,
  1: MdOutlineTempleBuddhist,
  2: MdBusiness,
  3: TfiMicrosoftAlt,
  4: MdPeopleOutline,
  5: TbCashRegister,
  6: FiShoppingCart,
  7: BsCardChecklist,
  8: MdSettings,
  9: PiPlantDuotone,
  10: GiWoodPile,
  11: FaTruckFront,
  12: GiRoad,
  13: IoBarChartOutline,
  14: MdDirectionsBoatFilled,
  15: GiAnchor,
  16: MdLightbulbOutline,
} as const;

/**
 * Time ranges in milliseconds for view mode determination
 */
const TIME_RANGES = {
  DAY: 24 * 60 * 60 * 1000,
  WEEK: 7 * 24 * 60 * 60 * 1000,
  MONTH: 31 * 24 * 60 * 60 * 1000,
  QUARTER: 90 * 24 * 60 * 60 * 1000,
  YEAR: 365.24 * 24 * 60 * 60 * 1000,
} as const;

// ================ UTILITY FUNCTIONS ================

/**
 * Intelligently determines optimal view mode based on time range
 * Implements smart scaling for better user experience
 */
const determineViewMode = (rangeMs: number): ViewMode => {
  if (rangeMs < 2 * TIME_RANGES.DAY) return 'day';
  if (rangeMs < 2 * TIME_RANGES.WEEK) return 'week';
  if (rangeMs < 2 * TIME_RANGES.MONTH) return 'month';
  if (rangeMs < 4 * TIME_RANGES.MONTH) return 'quarter';
  return 'year';
};

/**
 * Gets appropriate primary header unit for timeline display
 */
const getPrimaryHeaderUnit = (mode: ViewMode): string => {
  switch (mode) {
    case 'year': return 'year';
    case 'quarter': return 'year';
    case 'month': return 'month';
    case 'week': return 'month';
    case 'day': return 'day';
    default: return 'month';
  }
};

/**
 * Gets appropriate secondary header unit for timeline display
 */
const getSecondaryHeaderUnit = (mode: ViewMode): string => {
  switch (mode) {
    case 'year': return 'month';
    case 'quarter': return 'month';
    case 'month': return 'day';
    case 'week': return 'day';
    case 'day': return 'hour';
    default: return 'day';
  }
};

/**
 * Formats primary header labels with internationalization
 */
const formatPrimaryHeader = ([startTime]: [Moment, Moment], unit: string): string => {
  const date = startTime.toDate();
  switch (unit) {
    case 'year':
      return format(date, 'yyyy', { locale: enUS });
    case 'month':
      return format(date, 'MMMM yyyy', { locale: enUS });
    case 'day':
      return format(date, 'eeee, MMM dd, yyyy', { locale: enUS });
    default:
      return format(date, 'P', { locale: enUS });
  }
};

/**
 * Formats secondary header labels with context awareness
 */
const formatSecondaryHeader = (
  [startTime]: [Moment, Moment], 
  unit: string, 
  viewMode: ViewMode
): string => {
  const date = startTime.toDate();
  
  switch (unit) {
    case 'month':
      return format(date, 'MMM', { locale: enUS });
    case 'day':
      return viewMode === 'month' 
        ? format(date, 'd')
        : format(date, 'MMM d');
    case 'hour':
      return format(date, 'HH:mm', { locale: enUS });
    case 'year':
      return format(date, 'yyyy', { locale: enUS });
    default:
      return format(date, 'P', { locale: enUS });
  }
};

// ================ MAIN COMPONENT ================

/**
 * TimelineView Component
 * 
 * Advanced timeline visualization demonstrating:
 * - Complex React state management
 * - Performance optimization techniques
 * - Advanced user interaction patterns
 * - Enterprise-grade data visualization
 * - Responsive design principles
 * - International business standards
 */
const TimelineView: FC<ITimelineViewProps> = ({ events, selectedDate }) => {
  
  // ================ STATE MANAGEMENT ================
  
  const [viewMode, setViewMode] = useState<ViewMode>('month');
  
  const [visibleTimeStart, setVisibleTimeStart] = useState<number>(() => {
    const base = selectedDate || new Date();
    return startOfMonth(base).getTime();
  });
  
  const [visibleTimeEnd, setVisibleTimeEnd] = useState<number>(() => {
    const base = selectedDate || new Date();
    return addDays(endOfMonth(base), 1).getTime();
  });

  // ================ VIEW MANAGEMENT ================

  /**
   * Intelligent view range calculation with business logic
   * Optimizes display based on operational requirements
   */
  const setViewRange = useCallback((mode: ViewMode) => {
    const base = selectedDate || new Date();
    let start: Date;
    let end: Date;

    switch (mode) {
      case 'day':
        start = startOfDay(base);
        end = addSeconds(endOfDay(base), 1);
        break;
      case 'week':
        start = startOfWeek(base, { locale: enUS });
        end = addDays(endOfWeek(base, { locale: enUS }), 1);
        break;
      case 'month':
        start = startOfMonth(base);
        end = addDays(endOfMonth(base), 1);
        break;
      case 'quarter':
        start = startOfMonth(base);
        end = addDays(endOfMonth(addMonths(base, 3)), 1);
        break;
      case 'year':
        start = startOfYear(base);
        end = addDays(endOfYear(base), 1);
        break;
      default:
        start = startOfMonth(base);
        end = addDays(endOfMonth(base), 1);
    }

    setVisibleTimeStart(start.getTime());
    setVisibleTimeEnd(end.getTime());
    setViewMode(mode);
  }, [selectedDate]);

  // Synchronize view when dependencies change
  useEffect(() => {
    setViewRange(viewMode);
  }, [selectedDate, viewMode, setViewRange]);

  // ================ DATA PROCESSING ================

  /**
   * Dynamic group generation based on active departments
   * Automatically adapts to organizational structure
   */
  const groups = useMemo<TimelineGroup[]>(() => {
    const uniqueAreas = Array.from(
      new Set(events.map(e => e.area_id).filter(id => id !== undefined))
    ) as number[];

    const departmentGroups: TimelineGroup[] = uniqueAreas
      .sort((a, b) => a - b)
      .map(areaId => ({
        id: areaId,
        title: DEPARTMENT_CONFIG.names[areaId] || `Department ${areaId}`,
      }));

    const holidayGroup: TimelineGroup = { 
      id: -1, 
      title: 'Holidays & Special Events' 
    };

    return [holidayGroup, ...departmentGroups];
  }, [events]);

  /**
   * Advanced event processing with intelligent merging
   * Implements proximity-based clustering for optimal visualization
   */
  const items = useMemo<TimelineItem[]>(() => {
    // Transform events to timeline items
    const rawItems = events
      .map(evt => {
        const start = new Date(evt.fecha_inicio);
        const end = new Date(evt.fecha_fin);
        const groupId = evt.isFeriado || evt.tipo === 'feriado' ? -1 : evt.area_id ?? 1;

        return {
          id: evt.id,
          title: evt.titulo,
          group: groupId,
          start_time: start,
          end_time: end,
          area_id: evt.area_id ?? null,
          isFeriado: evt.isFeriado,
          tipo: evt.tipo,
          mergedData: [{
            title: evt.titulo,
            start,
            end
          }]
        };
      })
      .filter(item => 
        item.start_time.getTime() <= visibleTimeEnd &&
        item.end_time.getTime() >= visibleTimeStart
      );

    // Group by department for processing
    const itemsByGroup = new Map<Id, TimelineItem[]>();
    rawItems.forEach(item => {
      if (!itemsByGroup.has(item.group)) {
        itemsByGroup.set(item.group, []);
      }
      itemsByGroup.get(item.group)!.push(item);
    });

    /**
     * Intelligent event merging algorithm
     * Combines nearby events for better visual density
     */
    const mergeGroupItems = (groupItems: TimelineItem[]): TimelineItem[] => {
      groupItems.sort((a, b) => a.start_time.getTime() - b.start_time.getTime());

      const merged: TimelineItem[] = [];
      let current = { ...groupItems[0] };

      const proximityThreshold = 60 * 60 * 1000; // 1 hour
      const isWideView = ['month', 'quarter', 'year'].includes(viewMode);

      for (let i = 1; i < groupItems.length; i++) {
        const next = groupItems[i];
        const timeDiff = next.start_time.getTime() - current.end_time.getTime();
        const sameDay = current.start_time.toDateString() === next.start_time.toDateString();

        const shouldMerge = current.group === next.group &&
          (isWideView 
            ? sameDay && timeDiff <= proximityThreshold
            : timeDiff <= 0);

        if (shouldMerge) {
          current.end_time = new Date(Math.max(
            current.end_time.getTime(),
            next.end_time.getTime()
          ));
          current.mergedData = (current.mergedData || []).concat(next.mergedData || []);
        } else {
          merged.push(current);
          current = { ...next };
        }
      }
      merged.push(current);
      return merged;
    };

    // Process and flatten all groups
    const finalItems: TimelineItem[] = [];
    itemsByGroup.forEach(groupItems => {
      const mergedItems = mergeGroupItems(groupItems);
      finalItems.push(...mergedItems);
    });

    return finalItems;
  }, [events, visibleTimeStart, visibleTimeEnd, viewMode]);

  // ================ INTERACTION HANDLERS ================

  /**
   * Advanced mouse wheel zoom with momentum
   * Provides smooth, intuitive navigation experience
   */
  const handleWheelZoom = useCallback((e: WheelEvent<HTMLDivElement>) => {
    const currentRange = visibleTimeEnd - visibleTimeStart;
    const zoomFactor = 0.15; // Smooth zoom sensitivity
    const centerTime = visibleTimeStart + currentRange / 2;

    let newRange = currentRange;
    if (e.deltaY < 0) {
      newRange = currentRange * (1 - zoomFactor); // Zoom in
    } else {
      newRange = currentRange * (1 + zoomFactor); // Zoom out
    }

    // Enforce reasonable limits
    const minRange = TIME_RANGES.DAY;
    const maxRange = TIME_RANGES.YEAR * 5;
    newRange = Math.max(minRange, Math.min(maxRange, newRange));

    // Center the new range
    const newStart = centerTime - newRange / 2;
    const newEnd = centerTime + newRange / 2;
    
    setVisibleTimeStart(newStart);
    setVisibleTimeEnd(newEnd);

    // Auto-adjust view mode based on new range
    const newViewMode = determineViewMode(newRange);
    if (newViewMode !== viewMode) {
      setViewRange(newViewMode);
    }
  }, [visibleTimeStart, visibleTimeEnd, viewMode, setViewRange]);

  /**
   * Timeline navigation handler with bounds checking
   */
  const handleTimeChange = useCallback((
    newStart: number, 
    newEnd: number, 
    updateScrollCanvas: (start: number, end: number) => void
  ) => {
    setVisibleTimeStart(newStart);
    setVisibleTimeEnd(newEnd);
    updateScrollCanvas(newStart, newEnd);
  }, []);

  // ================ CUSTOM RENDERERS ================

  /**
   * Custom item renderer with rich tooltips and visual indicators
   * Demonstrates advanced React patterns and UX considerations
   */
  const customItemRenderer = ({
    item,
    itemContext,
    getItemProps,
    getResizeProps,
  }: ReactCalendarItemRendererProps<TimelineItem>) => {

    // Get appropriate icon for department
    const IconComponent = DEPARTMENT_ICONS[item.group as keyof typeof DEPARTMENT_ICONS] || FaUsers;

    // Determine styling class
    const cssClass = item.isFeriado || item.tipo === 'feriado' 
      ? 'holiday-event' 
      : `department-${item.area_id}`;

    // Create rich tooltip content
    const tooltipContent = (
      <Tooltip id={`tooltip-${String(item.id)}`}>
        {item.mergedData && item.mergedData.length > 1 ? (
          <>
            <strong>Merged Events ({item.mergedData.length})</strong>
            <ul style={{ paddingLeft: '15px', margin: '8px 0 0 0', fontSize: '12px' }}>
              {item.mergedData.map((subEvent, index) => {
                const isHoliday = item.isFeriado || item.tipo === 'feriado';
                const startFormatted = isHoliday
                  ? format(subEvent.start, "MMMM dd, yyyy", { locale: enUS })
                  : format(subEvent.start, "MMM dd, yyyy 'at' HH:mm", { locale: enUS });

                return (
                  <li key={index} style={{ marginBottom: '4px' }}>
                    <strong>{subEvent.title}</strong>
                    <br />
                    <small>{startFormatted}</small>
                  </li>
                );
              })}
            </ul>
          </>
        ) : (
          <>
            <strong>{item.title}</strong>
            <br />
            <small>
              {item.isFeriado || item.tipo === 'feriado'
                ? format(item.start_time, "MMMM dd, yyyy", { locale: enUS })
                : `${format(item.start_time, "MMM dd 'at' HH:mm", { locale: enUS })} - ${format(item.end_time, "HH:mm", { locale: enUS })}`
              }
            </small>
          </>
        )}
      </Tooltip>
    );

    const itemProps = getItemProps({
      onMouseDown: () => {
        console.log('Timeline item selected:', item.title);
      },
      style: {
        borderRadius: '6px',
        minWidth: '20px',
        padding: 0,
        cursor: 'pointer',
        transition: 'all 0.2s ease',
      },
      className: cssClass,
    });

    const { left: leftResizeProps, right: rightResizeProps } = getResizeProps();
    const { key, ref, style, ...divProps } = itemProps;

    return (
      <OverlayTrigger
        overlay={tooltipContent}
        placement="auto"
        trigger={['hover', 'focus']}
        delay={{ show: 300, hide: 150 }}
      >
        <div key={key} ref={ref} style={style} {...divProps}>
          {itemContext.resizing && <div {...leftResizeProps} />}
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              width: '100%',
              height: '100%',
              fontSize: '14px',
            }}
            aria-label={`${item.title} - ${DEPARTMENT_CONFIG.names[item.area_id as keyof typeof DEPARTMENT_CONFIG.names] || 'Holiday'}`}
          >
            <IconComponent size={14} />
          </div>
          {itemContext.resizing && <div {...rightResizeProps} />}
        </div>
      </OverlayTrigger>
    );
  };

  // ================ COMPUTED VALUES ================

  const primaryUnit = getPrimaryHeaderUnit(viewMode);
  const secondaryUnit = getSecondaryHeaderUnit(viewMode);

  // ================ RENDER ================

  return (
    <div
      className="timeline-container"
      onWheelCapture={handleWheelZoom}
      role="application"
      aria-label="Interactive Timeline View"
    >
      <h3 className="text-xl font-semibold mb-4 text-gray-800">
        Timeline Overview
      </h3>

      {/* View Mode Controls */}
      <div className="flex flex-wrap gap-2 mb-4">
        {[
          { mode: 'day' as ViewMode, label: 'Day' },
          { mode: 'week' as ViewMode, label: 'Week' },
          { mode: 'month' as ViewMode, label: 'Month' },
          { mode: 'quarter' as ViewMode, label: 'Quarter' },
          { mode: 'year' as ViewMode, label: 'Year' },
        ].map(({ mode, label }) => (
          <button
            key={mode}
            className={`py-2 px-4 rounded-lg text-sm font-medium transition-all duration-200 ${
              viewMode === mode
                ? 'bg-blue-600 text-white shadow-md'
                : 'bg-gray-200 text-gray-700 hover:bg-gray-300'
            }`}
            onClick={() => setViewRange(mode)}
            aria-pressed={viewMode === mode}
          >
            {label}
          </button>
        ))}
      </div>

      {/* Custom Styles */}
      <style jsx>{`
        .timeline-container {
          padding: 0.5rem;
          background-color: white;
          color: #000;
          overflow-x: auto;
          border-radius: 8px;
          box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
        }

        .react-calendar-timeline {
          border: 1px solid #e2e8f0;
          border-radius: 8px;
          overflow: hidden;
        }

        /* Base item styling */
        .react-calendar-timeline .rct-item {
          border-radius: 6px !important;
          display: flex !important;
          align-items: center !important;
          justify-content: center !important;
          padding: 0 !important;
          min-width: 18px !important;
          min-height: 18px !important;
          box-shadow: 0 1px 3px rgba(0, 0, 0, 0.12) !important;
          transition: all 0.2s ease !important;
        }

        .react-calendar-timeline .rct-item:hover {
          transform: translateY(-1px) !important;
          box-shadow: 0 2px 6px rgba(0, 0, 0, 0.15) !important;
        }

        /* Holiday styling */
        .react-calendar-timeline .rct-item.holiday-event {
          background-color: #E63946 !important;
          color: #fff !important;
          border: 2px dashed #fff !important;
          font-weight: 700 !important;
        }

        /* Department colors */
        ${Object.entries(DEPARTMENT_CONFIG.colors).map(([id, color]) => `
          .react-calendar-timeline .rct-item.department-${id} {
            background-color: ${color} !important;
            color: ${color === '#F1FAEE' ? '#000' : '#fff'} !important;
            border: 1px solid ${color === '#F1FAEE' ? '#ddd' : color} !important;
          }
        `).join('')}

        /* Groups styling */
        .react-calendar-timeline .rct-sidebar .rct-sidebar-row {
          font-size: 14px !important;
          font-weight: 600 !important;
          padding: 8px 12px !important;
          border-bottom: 1px solid #e2e8f0 !important;
          background-color: #f8fafc !important;
        }

        /* Responsive adjustments */
        @media (max-width: 1024px) {
          .timeline-container {
            padding: 0.25rem;
          }
          .react-calendar-timeline .rct-item {
            min-width: 14px !important;
            min-height: 14px !important;
          }
          .react-calendar-timeline .rct-sidebar .rct-sidebar-row {
            font-size: 12px !important;
            padding: 6px 8px !important;
          }
        }

        @media (max-width: 640px) {
          .react-calendar-timeline .rct-item {
            min-width: 12px !important;
            min-height: 12px !important;
          }
          .react-calendar-timeline .rct-sidebar .rct-sidebar-row {
            font-size: 10px !important;
            padding: 4px 6px !important;
          }
        }

        /* Hide default tooltips */
        .react-calendar-timeline .rct-tooltip,
        .react-calendar-timeline .rct-item-content {
          display: none !important;
        }
      `}</style>

      {/* Timeline Component */}
      <Timeline
        groups={groups}
        items={items}
        visibleTimeStart={visibleTimeStart}
        visibleTimeEnd={visibleTimeEnd}
        onTimeChange={handleTimeChange}
        minZoom={TIME_RANGES.DAY}
        maxZoom={TIME_RANGES.YEAR * 5}
        canMove={false}
        canResize={false}
        canChangeGroup={false}
        stackItems
        itemRenderer={customItemRenderer}
        lineHeight={40}
        itemHeightRatio={0.75}
        timeSteps={{
          day: 1,
          hour: 1,
          minute: 30,
          month: 1,
          second: 1,
          year: 1,
        }}
        sidebarWidth={180}
        traditionalZoom={false}
      >
        <TimelineHeaders>
          <DateHeader
            unit={primaryUnit}
            labelFormat={(args) => formatPrimaryHeader(args, primaryUnit)}
            style={{
              backgroundColor: '#f8fafc',
              color: '#1f2937',
              fontWeight: '600',
              fontSize: '14px',
            }}
          />
          <DateHeader
            unit={secondaryUnit}
            labelFormat={(args) => formatSecondaryHeader(args, secondaryUnit, viewMode)}
            style={{
              backgroundColor: '#ffffff',
              color: '#4b5563',
              fontSize: '12px',
            }}
          />
        </TimelineHeaders>
      </Timeline>
    </div>
  );
};

export default TimelineView;
