/**
 * Calendar Management System - React TypeScript Component
 * 
 * A comprehensive calendar application featuring:
 * - Event management with CRUD operations
 * - Multiple view modes (Calendar & Timeline)
 * - Recurring events support
 * - Holiday integration
 * - Room and user management
 * - Responsive design with mobile support
 * 
 * Tech Stack: React, TypeScript, date-fns, React Icons
 * 
 * @author [Daniel Jara]
 * @version 1.0.0
 */

import { useEffect, useState } from 'react';
import { eachDayOfInterval } from 'date-fns';
import { Link } from "react-router-dom";
import dayjs from 'dayjs';

// Icons
import { 
  FaCalendarAlt, 
  FaClock, 
  FaBuilding, 
  FaUser, 
  FaDoorOpen, 
  FaSyncAlt, 
  FaRegStickyNote, 
  FaTrashAlt 
} from 'react-icons/fa';
import { AiOutlineClose } from 'react-icons/ai';

// API Services (would be implemented separately)
import { getEventos, createEvento, deleteEvento } from '../services/eventsApi';
import { getFeriados } from '../services/feriadosApi';
import { getSalas } from '../services/salasApi';
import { getUsers } from '../services/usersApi';

// Types
import { ICreateEventDTO, IEvent, ISala, IUser } from '../types';

// Components
import CalendarView from '../components/CalendarView';
import TimelineView from '../components/TimelineView';
import SmallCalendar from '../components/SmallCalendar';
import EventFormModal from '../components/EventFormModal';

// Hooks
import { useAuth } from '../contexts/AuthHooks';

// ================ CONSTANTS & CONFIGURATION ================

/**
 * Color mapping for different departments/areas
 * Used to visually distinguish events by organizational units
 */
const AREA_COLOR_MAP: { [key: number]: string } = {
  1: 'border-blue-600',    // IT Department
  2: 'border-green-600',   // Management
  3: 'border-red-600',     // Administration
  4: 'border-yellow-600',  // Human Resources
  5: 'border-teal-600',    // Accounting
  6: 'border-orange-600',  // Procurement
  7: 'border-purple-600',  // Legal & Regulation
  8: 'border-emerald-600', // Operations
  9: 'border-lime-600',    // Forestry
  10: 'border-amber-600',  // Harvest
  11: 'border-cyan-600',   // Transportation
  12: 'border-rose-600',   // Roads
  13: 'border-indigo-600', // Planning & Cartography
  14: 'border-fuchsia-600',// Production & Sales
  15: 'border-stone-600',  // TSA
  16: 'border-pink-600',   // Projects
};

/**
 * Department name mapping
 * Bilingual support for internationalization
 */
const AREA_NAMES: { [key: number]: string } = {
  1: 'IT Department',
  2: 'Management',
  3: 'Administration',
  4: 'Human Resources',
  5: 'Accounting',
  6: 'Procurement',
  7: 'Legal & Regulation',
  8: 'Operations',
  9: 'Forestry',
  10: 'Harvest',
  11: 'Transportation',
  12: 'Roads',
  13: 'Planning & Cartography',
  14: 'Production & Sales',
  15: 'TSA',
  16: 'Projects',
};

/**
 * Day codes for recurring events (RFC standard)
 */
const DAY_CODE_MAP: { [key: string]: string } = {
  MO: 'Monday',
  TU: 'Tuesday',
  WE: 'Wednesday',
  TH: 'Thursday',
  FR: 'Friday',
  SA: 'Saturday',
  SU: 'Sunday',
};

// ================ UTILITY FUNCTIONS ================

/**
 * Gets the appropriate color class for an event based on its area
 * @param event - The event object
 * @returns CSS class string for border color
 */
const getAreaColorClass = (event: IEvent): string => {
  const areaId = event.area_id;
  return areaId && AREA_COLOR_MAP[areaId] ? AREA_COLOR_MAP[areaId] : 'border-blue-600';
};

/**
 * Parses recurring day codes into readable format
 * @param codes - Comma-separated day codes (e.g., "MO,TU,WE")
 * @returns Human-readable day names
 */
const parseRecurrenceDays = (codes: string): string => {
  return codes
    .split(',')
    .map((code) => DAY_CODE_MAP[code.trim()] || code.trim())
    .join(', ');
};

/**
 * Gets department name from ID
 * @param areaId - Department ID
 * @returns Department name or 'All' if not found
 */
const getAreaNameFromId = (areaId?: number): string => {
  return AREA_NAMES[areaId ?? 0] ?? 'All Departments';
};

/**
 * Parses date string as local date (avoiding timezone issues)
 * @param dateStr - Date string in YYYY-MM-DD format
 * @returns Date object in local timezone
 */
const parseDateAsLocal = (dateStr: string): Date => {
  const [year, month, day] = dateStr.split('-').map(Number);
  return new Date(year, month - 1, day); // JavaScript months are 0-based
};

/**
 * Expands recurring events into individual event instances
 * This function handles the complex logic of creating multiple event instances
 * from a single recurring event definition
 * 
 * @param eventos - Array of events, some potentially recurring
 * @returns Expanded array with individual instances of recurring events
 */
const expandRecurringEvents = (eventos: IEvent[]): IEvent[] => {
  const expandedEvents: IEvent[] = [];

  eventos.forEach((evento) => {
    if (evento.es_recurrente && evento.dias_recurrencia) {
      const diasRecurrencia = evento.dias_recurrencia.split(',').map((dia) => dia.trim());
      const startDate = new Date(evento.fecha_inicio);
      const endDate = new Date(evento.fecha_fin);

      // Extend end date by one day to include the last day in the interval
      const adjustedEndDate = new Date(endDate);
      adjustedEndDate.setDate(endDate.getDate() + 1);

      // Get all dates in the interval
      const allDates = eachDayOfInterval({ start: startDate, end: adjustedEndDate });

      // Map weekdays to day codes (MO, TU, etc.)
      const dayMap: { [key: string]: string } = {
        1: "MO", // Monday
        2: "TU", // Tuesday
        3: "WE", // Wednesday
        4: "TH", // Thursday
        5: "FR", // Friday
        6: "SA", // Saturday
        0: "SU", // Sunday
      };

      allDates.forEach((date) => {
        const weekdayNum = date.getDay();
        const weekday = dayMap[weekdayNum];

        if (diasRecurrencia.includes(weekday)) {
          // Create new dates for the event on this day
          const newStartDate = new Date(date);
          newStartDate.setHours(startDate.getHours(), startDate.getMinutes(), 0, 0);

          // Fixed event duration (can be made configurable)
          const newEndDate = new Date(date);
          newEndDate.setHours(startDate.getHours() + 3, startDate.getMinutes(), 0, 0);

          // Generate unique ID combining original ID with date
          const dateStr = date.toISOString().split('T')[0];
          const uniqueId = `${evento.id}-${dateStr}`;

          expandedEvents.push({
            ...evento,
            id: uniqueId,
            fecha_inicio: newStartDate.toISOString(),
            fecha_fin: newEndDate.toISOString(),
          });
        }
      });
    } else {
      // Non-recurring event, add as-is
      expandedEvents.push(evento);
    }
  });

  return expandedEvents;
};

// ================ MODAL COMPONENTS ================

/**
 * Event Detail Modal Component
 * Displays comprehensive event information with action capabilities
 */
interface EventDetailModalProps {
  event: IEvent | null;
  onClose: () => void;
  salas: ISala[];
  usuarios: IUser[];
  onDelete?: (id: number | string) => void;
}

const EventDetailModal: React.FC<EventDetailModalProps> = ({
  event,
  onClose,
  salas,
  usuarios,
  onDelete,
}) => {
  const { user } = useAuth();

  if (!event) return null;

  // Derived data for display
  const areaNombre = getAreaNameFromId(event.area_id);
  const salaNombre = salas.find(s => s.id === event.sala_id)?.nombre ?? 'No room assigned';
  const salaUbicacion = salas.find(s => s.id === event.sala_id)?.ubicacion ?? '';
  const organizadorNombre = usuarios.find(u => u.id === event.organizador_id)?.nombre ?? 'Unknown';
  const usuarioActualId = user?.id;

  /**
   * Handles event deletion with confirmation
   */
  const handleDelete = () => {
    const realId = typeof event.id === 'string' && event.id.includes('-')
      ? parseInt(event.id.toString().split('-')[0])
      : Number(event.id);
    
    onDelete?.(realId);
  };

  return (
    <div className="fixed inset-0 flex items-center justify-center backdrop-blur-sm bg-black/40 z-50">
      <div className={`bg-white text-gray-800 p-4 rounded-xl w-[22rem] sm:w-[26rem] relative shadow-2xl border-t-4 ${getAreaColorClass(event)}`}>
        
        {/* Header */}
        <h2 className="text-xl font-bold mb-3 flex items-center justify-between gap-2">
          <span className="flex items-center gap-2">
            <FaCalendarAlt className="text-blue-600" />
            {event.titulo}
          </span>
          <button
            onClick={onClose}
            aria-label="Close"
            className="text-neutral-500 hover:text-neutral-800 hover:bg-neutral-100 p-1 rounded-full transition leading-none"
          >
            <AiOutlineClose className="text-sm" />
          </button>
        </h2>

        {/* Event Details */}
        <div className="space-y-2">
          <DetailRow 
            icon={<FaRegStickyNote className="text-gray-600" />}
            label="Description"
            value={event.descripcion || 'No description'}
          />

          <DetailRow 
            icon={<FaClock className="text-green-600" />}
            label="Start"
            value={formatEventDateTime(event.fecha_inicio, event.tipo)}
          />

          <DetailRow 
            icon={<FaClock className="text-red-600" />}
            label="End"
            value={formatEventDateTime(event.fecha_fin, event.tipo)}
          />

          <DetailRow 
            icon={<FaBuilding className="text-purple-600" />}
            label="Department"
            value={areaNombre}
          />

          {event.sala_id && (
            <DetailRow 
              icon={<FaDoorOpen className="text-yellow-600" />}
              label="Room"
              value={`${salaNombre}${salaUbicacion ? ` (Location: ${salaUbicacion})` : ''}`}
            />
          )}

          {event.organizador_id && (
            <DetailRow 
              icon={<FaUser className="text-indigo-600" />}
              label="Organizer"
              value={organizadorNombre}
            />
          )}

          {event.es_recurrente && (
            <DetailRow 
              icon={<FaSyncAlt className="text-pink-600" />}
              label="Recurring"
              value={event.dias_recurrencia 
                ? parseRecurrenceDays(event.dias_recurrencia)
                : 'Recurrence not specified'
              }
            />
          )}
        </div>

        {/* Actions */}
        {event.organizador_id === usuarioActualId && (
          <button
            onClick={handleDelete}
            className="mt-4 flex items-center gap-2 bg-red-600 text-white py-2 px-4 rounded hover:bg-red-700 transition"
          >
            <FaTrashAlt />
            Cancel Event
          </button>
        )}
      </div>
    </div>
  );
};

/**
 * Reusable component for displaying event detail rows
 */
interface DetailRowProps {
  icon: React.ReactNode;
  label: string;
  value: string;
}

const DetailRow: React.FC<DetailRowProps> = ({ icon, label, value }) => (
  <p className="text-sm mb-2 flex items-center gap-2">
    {icon}
    <span><strong>{label}:</strong> {value}</span>
  </p>
);

/**
 * Formats event date/time based on event type
 */
const formatEventDateTime = (dateStr: string, eventType?: string): string => {
  const date = new Date(dateStr);
  
  if (eventType === 'feriado') {
    return date.toLocaleDateString('en-US', {
      weekday: 'long',
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    });
  }
  
  return date.toLocaleString('en-US');
};

// ================ DAILY EVENTS SUMMARY ================

/**
 * Daily Events Summary Component
 * Shows events for the selected date in a compact format
 */
interface DailyEventsSummaryProps {
  events: IEvent[];
  selectedDate: Date;
}

const DailyEventsSummary: React.FC<DailyEventsSummaryProps> = ({ events, selectedDate }) => {
  const eventsForDay = events.filter((event) => {
    const eventStart = new Date(event.fecha_inicio);
    return (
      eventStart.getDate() === selectedDate.getDate() &&
      eventStart.getMonth() === selectedDate.getMonth() &&
      eventStart.getFullYear() === selectedDate.getFullYear()
    );
  });

  return (
    <div className="bg-white p-4 rounded-lg shadow-md mt-4">
      <h3 className="text-md font-semibold mb-3">
        Events for {selectedDate.toLocaleDateString('en-US', { day: 'numeric', month: 'long' })}
      </h3>
      
      {eventsForDay.length === 0 ? (
        <p className="text-gray-600 text-sm">No events for this day.</p>
      ) : (
        <ul className="space-y-2">
          {eventsForDay.map((event) => (
            <li
              key={event.id}
              className="border-l-4 border-blue-500 pl-3 py-2 bg-gray-50 rounded"
            >
              <h4 className="text-sm font-medium">{event.titulo}</h4>
              <p className="text-xs text-gray-600">{event.descripcion}</p>
              {event.tipo !== 'feriado' && (
                <p className="text-xs text-gray-500">
                  {new Date(event.fecha_inicio).toLocaleTimeString('en-US', {
                    hour: '2-digit',
                    minute: '2-digit',
                  })}
                  {' - '}
                  {new Date(event.fecha_fin).toLocaleTimeString('en-US', {
                    hour: '2-digit',
                    minute: '2-digit',
                  })}
                </p>
              )}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
};

// ================ MAIN COMPONENT ================

/**
 * Calendar Page Component
 * Main component that orchestrates the entire calendar functionality
 */
const CalendarPage: React.FC = () => {
  // ================ STATE MANAGEMENT ================
  const [eventos, setEventos] = useState<IEvent[]>([]);
  const [feriados, setFeriados] = useState<Date[]>([]);
  const [selectedEvent, setSelectedEvent] = useState<IEvent | null>(null);
  const [selectedDate, setSelectedDate] = useState<Date>(new Date());
  const [viewMode, setViewMode] = useState<'calendar' | 'timeline'>('calendar');
  const [showEventForm, setShowEventForm] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [salas, setSalas] = useState<ISala[]>([]);
  const [usuarios, setUsuarios] = useState<IUser[]>([]);

  // ================ DATA FETCHING ================

  /**
   * Fetches all necessary data from APIs
   * Combines events and holidays into a unified data structure
   */
  const fetchData = async (): Promise<void> => {
    try {
      setLoading(true);
      setError(null);

      // Parallel API calls for better performance
      const [evts, fers] = await Promise.all([getEventos(), getFeriados()]);

      // Convert holidays to event format for unified handling
      const feriadosComoEventos: IEvent[] = fers.map((f) => {
        const fecha = parseDateAsLocal(f.fecha);
        return {
          id: f.id * -1, // Negative ID to distinguish from regular events
          tipo: 'feriado',
          titulo: f.nombre_feriado,
          descripcion: 'National Holiday',
          fecha_inicio: fecha.toISOString(),
          fecha_fin: fecha.toISOString(),
          area_id: undefined,
          organizador_id: undefined,
          sala_id: undefined,
          es_recurrente: false,
          dias_recurrencia: '',
          isFeriado: true,
        };
      });

      // Combine all events and expand recurring ones
      const todosEventos = [
        ...evts.map((evt) => ({ ...evt, isFeriado: false })),
        ...feriadosComoEventos,
      ];
      setEventos(expandRecurringEvents(todosEventos));

      // Prepare holiday dates for mini calendar
      const feriadoDates = fers.map((f) =>
        dayjs(f.fecha).hour(12).minute(0).second(0).toDate()
      );
      setFeriados(feriadoDates);

    } catch (error) {
      console.error('Error fetching data:', error);
      setError('Error loading events and holidays. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  // ================ EVENT HANDLERS ================

  /**
   * Creates a new event
   */
  const handleCreateEvento = async (eventoData: ICreateEventDTO): Promise<void> => {
    console.log('Saving event:', eventoData);
    try {
      setLoading(true);
      setError(null);
      const newEvent = await createEvento(eventoData);
      setEventos((prev) => [...prev, { ...newEvent, isFeriado: false }]);
      setShowEventForm(false);
    } catch (error) {
      console.error('Error creating event:', error);
      setError('Error creating event. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  /**
   * Deletes an event with confirmation
   */
  const handleDeleteEvento = async (eventoId: number | string): Promise<void> => {
    if (!confirm('Are you sure you want to cancel this event?')) return;
    
    try {
      await deleteEvento(eventoId);
      setEventos((prev) => prev.filter((e) => e.id !== eventoId));
      setSelectedEvent(null);
    } catch (err) {
      console.error('Error deleting event:', err);
      alert('There was an error deleting the event.');
    }
  };

  /**
   * Handles event selection from calendar
   */
  const handleSelectEvent = (event: IEvent): void => {
    setSelectedEvent(event);
  };

  /**
   * Handles empty slot selection
   */
  const handleSelectSlot = (slotInfo: { start: Date }): void => {
    setSelectedDate(slotInfo.start);
  };

  /**
   * Closes the event detail modal
   */
  const closeModal = (): void => {
    setSelectedEvent(null);
  };

  /**
   * Handles date navigation in main calendar
   */
  const handleDateChange = (newDate: Date): void => {
    setSelectedDate(newDate);
  };

  /**
   * Handles date selection from mini calendar
   */
  const handleSmallCalendarChange = (date: Date): void => {
    setSelectedDate(date);
  };

  // ================ LIFECYCLE ================

  useEffect(() => {
    // Initialize data on component mount
    fetchData();
    getSalas().then(setSalas).catch(console.error);
    getUsers(true).then(setUsuarios).catch(console.error);
  }, []);

  // ================ RENDER ================

  return (
    <div className="min-h-screen bg-gray-100 text-gray-800 flex flex-col">
      
      {/* Header */}
      <header className="px-4 sm:px-6 py-4 flex justify-between items-center bg-white shadow">
        <div className="flex items-center space-x-4">
          <Link
            to="/home"
            className="py-2 px-3 sm:px-4 rounded text-sm sm:text-base bg-gray-600 text-white hover:bg-gray-700 transition-colors duration-200"
          >
            Back
          </Link>
          <h1 className="text-2xl sm:text-3xl font-bold">Event Calendar</h1>
        </div>

        <div className="flex space-x-2 sm:space-x-4">
          <button
            onClick={() => setViewMode('calendar')}
            className={`py-2 px-3 sm:px-4 rounded text-sm sm:text-base ${
              viewMode === 'calendar'
                ? 'bg-blue-600 text-white'
                : 'bg-white text-blue-600 border border-blue-600 hover:bg-blue-50'
            } transition-colors duration-200`}
          >
            Calendar View
          </button>
          <button
            onClick={() => setViewMode('timeline')}
            className={`py-2 px-3 sm:px-4 rounded text-sm sm:text-base ${
              viewMode === 'timeline'
                ? 'bg-blue-600 text-white'
                : 'bg-white text-blue-600 border border-blue-600 hover:bg-blue-50'
            } transition-colors duration-200`}
          >
            Timeline View
          </button>
          <button
            onClick={() => setShowEventForm(true)}
            className="py-2 px-3 sm:px-4 rounded text-sm sm:text-base bg-green-600 text-white hover:bg-green-700 transition-colors duration-200"
          >
            Create Event
          </button>
        </div>
      </header>

      {/* Loading and Error States */}
      {loading && (
        <div className="p-4 text-center">
          <p className="text-blue-600">Loading...</p>
        </div>
      )}
      
      {error && (
        <div className="p-4 text-center">
          <p className="text-red-600">{error}</p>
          <button
            onClick={fetchData}
            className="mt-2 py-1 px-3 bg-blue-600 text-white rounded hover:bg-blue-700"
          >
            Retry
          </button>
        </div>
      )}

      {/* Main Content */}
      {!loading && !error && (
        <div className="flex flex-1 flex-col lg:flex-row gap-4 p-4 sm:p-6">
          
          {/* Side Panel */}
          {(viewMode === 'calendar' || viewMode === 'timeline') && (
            <div className="lg:w-1/4 w-full flex flex-col">
              <SmallCalendar
                value={selectedDate}
                onChange={handleSmallCalendarChange}
                feriados={feriados}
                activeStartDate={selectedDate} 
              />
              <DailyEventsSummary events={eventos} selectedDate={selectedDate} />
            </div>
          )}

          {/* Main Panel */}
          <div
            className={`w-full ${
              (viewMode === 'calendar' || viewMode === 'timeline') ? 'lg:w-3/4' : 'lg:w-full'
            } bg-white rounded-lg p-2 sm:p-4 shadow-md flex-1`}
          >
            {/* Calendar View */}
            {viewMode === 'calendar' && (
              <div className="h-[70vh] sm:h-[80vh]">
                <CalendarView
                  events={eventos}
                  date={selectedDate}
                  selectedDate={selectedDate}
                  onDateChange={handleDateChange}
                  onSelectEvent={handleSelectEvent}
                  onSelectSlot={handleSelectSlot}
                />
              </div>
            )}

            {/* Timeline View */}
            {viewMode === 'timeline' && (
              <div className="h-[70vh] sm:h-[80vh] overflow-y-auto">
                <TimelineView events={eventos} selectedDate={selectedDate} />
              </div>
            )}
          </div>
        </div>
      )}

      {/* Modals */}
      <EventDetailModal
        event={selectedEvent}
        onClose={closeModal}
        salas={salas}
        usuarios={usuarios}
        onDelete={handleDeleteEvento}
      />

      {showEventForm && (
        <EventFormModal
          onSave={handleCreateEvento}
          onClose={() => {
            console.log('Closing form modal from CalendarPage');
            setShowEventForm(false);
          }}
        />
      )}
    </div>
  );
};

export default CalendarPage;
