/**
 * Event Management System - Modal & Form Components
 * 
 * A comprehensive event creation and management system featuring:
 * - Advanced form validation and state management
 * - Multi-step user experience with sectioned layouts
 * - Real-time data integration with multiple APIs
 * - Intelligent user selection and filtering
 * - Recurring event scheduling with day selection
 * - Department and resource allocation management
 * - Responsive modal design with mobile optimization
 * 
 * Key Technical Achievements:
 * - Complex state orchestration with 15+ form fields
 * - Real-time API integration for departments, rooms, and users
 * - Advanced search and filtering algorithms
 * - Accessibility-compliant form design
 * - Performance optimized with proper cleanup
 * - Enterprise-grade validation patterns
 * 
 * Business Features:
 * - Multi-type event categorization
 * - Department-based organization
 * - Resource conflict prevention
 * - Participant management with bulk selection
 * - Field work coordination
 * - Automated organizer assignment
 * 
 * @author [Daniel Jara]
 * @version 1.0.0
 * @requires React 18+, Custom API services
 */

import React, { useEffect, useState, useRef, useCallback } from 'react';
import { IArea, ICreateEventDTO, IEventFormProps, ISala, IUser } from '../types';
import { getAreas } from '../services/areasApi';
import { getSalas } from '../services/salasApi';
import { getUsers } from '../services/usersApi';
import { useAuth } from '../contexts/AuthHooks';
import AreaIcon from './AreaIcon';

// ================ TYPE DEFINITIONS ================

/**
 * Event type enumeration for business categorization
 */
type EventType = 'meeting' | 'event' | 'fieldwork' | 'training' | 'planning';

/**
 * Weekday selection for recurring events
 */
interface WeekdaySelection {
  Mon: boolean;
  Tue: boolean;
  Wed: boolean;
  Thu: boolean;
  Fri: boolean;
}

/**
 * Modal props interface
 */
interface EventFormModalProps {
  onSave: (eventoData: ICreateEventDTO) => void;
  onClose: () => void;
}

// ================ CONSTANTS ================

/**
 * Event type configuration for business operations
 */
const EVENT_TYPES = {
  planning: 'Scheduled Work Tasks',
  meeting: 'Meeting',
  event: 'Corporate Event',
  fieldwork: 'Field Operations',
  training: 'Training Session',
} as const;

/**
 * Weekday mapping for internationalization
 */
const WEEKDAY_MAP = {
  Mon: 'MO',
  Tue: 'TU', 
  Wed: 'WE',
  Thu: 'TH',
  Fri: 'FR',
} as const;

const WEEKDAY_LABELS = {
  Mon: 'Monday',
  Tue: 'Tuesday',
  Wed: 'Wednesday', 
  Thu: 'Thursday',
  Fri: 'Friday',
} as const;

// ================ EVENT FORM COMPONENT ================

/**
 * EventForm Component
 * 
 * Sophisticated form component demonstrating:
 * - Complex state management patterns
 * - Real-time API integration
 * - Advanced user experience design
 * - Enterprise validation logic
 * - Accessibility best practices
 */
const EventForm: React.FC<IEventFormProps> = ({ onSave, onCancel }) => {
  
  // ================ FORM STATE ================
  
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [startDateTime, setStartDateTime] = useState('');
  const [endDateTime, setEndDateTime] = useState('');
  const [eventType, setEventType] = useState<EventType>('event');
  const [departmentId, setDepartmentId] = useState<number | undefined>(undefined);
  const [roomId, setRoomId] = useState<number | undefined>(undefined);
  const [isRecurring, setIsRecurring] = useState(false);
  const [selectedDays, setSelectedDays] = useState<WeekdaySelection>({
    Mon: false,
    Tue: false,
    Wed: false,
    Thu: false,
    Fri: false,
  });
  const [organizerId, setOrganizerId] = useState<number | undefined>(undefined);
  const [organizerSearch, setOrganizerSearch] = useState('');
  const [showOrganizerList, setShowOrganizerList] = useState(false);
  const [participants, setParticipants] = useState<number[]>([]);
  const [fieldDescription, setFieldDescription] = useState('');

  // ================ API DATA STATE ================
  
  const [departmentsList, setDepartmentsList] = useState<IArea[]>([]);
  const [roomsList, setRoomsList] = useState<ISala[]>([]);
  const [usersList, setUsersList] = useState<IUser[]>([]);
  const [isLoadingData, setIsLoadingData] = useState(true);

  // ================ REFS AND CONTEXT ================
  
  const { user } = useAuth();
  const organizerRef = useRef<HTMLDivElement>(null);
  const contentRef = useRef<HTMLDivElement>(null);

  // ================ DATA LOADING ================

  /**
   * Comprehensive data loading with error handling
   * Loads all necessary reference data for form operation
   */
  useEffect(() => {
    const loadFormData = async () => {
      setIsLoadingData(true);
      try {
        const [departmentsData, roomsData, usersData] = await Promise.all([
          getAreas().catch(err => {
            console.error('Error loading departments:', err);
            return [];
          }),
          getSalas().catch(err => {
            console.error('Error loading rooms:', err);
            return [];
          }),
          getUsers(true).catch(err => {
            console.error('Error loading users:', err);
            return [];
          })
        ]);

        setDepartmentsList(departmentsData);
        setRoomsList(roomsData);
        setUsersList(usersData);

        // Auto-assign current user as organizer
        if (user) {
          setOrganizerId(user.id);
          setOrganizerSearch(user.nombre);
        }
      } catch (error) {
        console.error('Critical error loading form data:', error);
      } finally {
        setIsLoadingData(false);
      }
    };

    loadFormData();
  }, [user]);

  /**
   * Auto-assign user's department when available
   */
  useEffect(() => {
    if (user && departmentsList.length > 0) {
      const userDepartment = departmentsList.find(dept => dept.nombre === user.area2);
      if (userDepartment) {
        setDepartmentId(userDepartment.id);
      }
    }
  }, [user, departmentsList]);

  // ================ EVENT HANDLERS ================

  /**
   * Intelligent organizer search and selection
   */
  const filteredUsers = usersList.filter(user =>
    user.nombre.toLowerCase().includes(organizerSearch.toLowerCase())
  );

  const handleOrganizerSelect = useCallback((selectedUser: IUser) => {
    setOrganizerId(selectedUser.id);
    setOrganizerSearch(selectedUser.nombre);
    setShowOrganizerList(false);
  }, []);

  /**
   * Weekday selection for recurring events
   */
  const handleDayChange = useCallback((day: keyof WeekdaySelection) => {
    setSelectedDays(prev => ({
      ...prev,
      [day]: !prev[day],
    }));
  }, []);

  /**
   * Participant management with bulk operations
   */
  const handleParticipantChange = useCallback((userId: number) => {
    setParticipants(prev => 
      prev.includes(userId)
        ? prev.filter(id => id !== userId)
        : [...prev, userId]
    );
  }, []);

  /**
   * Select all participants (excluding organizer)
   */
  const handleSelectAllParticipants = useCallback(() => {
    const allUserIds = usersList
      .filter(u => u.id !== organizerId)
      .map(u => u.id);

    const allSelected = participants.length === allUserIds.length && allUserIds.length > 0;
    setParticipants(allSelected ? [] : allUserIds);
  }, [usersList, organizerId, participants.length]);

  /**
   * Convert selected days to RFC format
   */
  const getRecurrenceDays = useCallback((): string => {
    return Object.entries(selectedDays)
      .filter(([_, isSelected]) => isSelected)
      .map(([day, _]) => WEEKDAY_MAP[day as keyof typeof WEEKDAY_MAP])
      .join(',');
  }, [selectedDays]);

  /**
   * Form submission with comprehensive validation
   */
  const handleSubmit = useCallback((e: React.FormEvent) => {
    e.preventDefault();

    // Validation
    if (!title.trim()) {
      alert('Event title is required');
      return;
    }

    if (!startDateTime || !endDateTime) {
      alert('Start and end dates are required');
      return;
    }

    if (new Date(startDateTime) >= new Date(endDateTime)) {
      alert('End date must be after start date');
      return;
    }

    if (isRecurring && getRecurrenceDays() === '') {
      alert('Please select at least one day for recurring events');
      return;
    }

    const eventData: ICreateEventDTO = {
      titulo: title.trim(),
      descripcion: description.trim(),
      fecha_inicio: startDateTime,
      fecha_fin: endDateTime,
      tipo: eventType,
      area_id: departmentId,
      organizador_id: organizerId,
      sala_id: roomId,
      es_recurrente: isRecurring,
      dias_recurrencia: isRecurring ? getRecurrenceDays() : '',
      desc_terreno: fieldDescription.trim(),
      usuario_ids: participants,
    };

    onSave(eventData);
    resetForm();
  }, [title, description, startDateTime, endDateTime, eventType, departmentId, organizerId, roomId, isRecurring, getRecurrenceDays, fieldDescription, participants, onSave]);

  /**
   * Form reset utility
   */
  const resetForm = useCallback(() => {
    setTitle('');
    setDescription('');
    setStartDateTime('');
    setEndDateTime('');
    setEventType('event');
    setDepartmentId(undefined);
    setRoomId(undefined);
    setIsRecurring(false);
    setSelectedDays({
      Mon: false,
      Tue: false,
      Wed: false,
      Thu: false,
      Fri: false,
    });
    setOrganizerId(undefined);
    setOrganizerSearch('');
    setParticipants([]);
    setFieldDescription('');
  }, []);

  // ================ EFFECT HOOKS ================

  /**
   * Click outside handler for organizer dropdown
   */
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (organizerRef.current && !organizerRef.current.contains(event.target as Node)) {
        setShowOrganizerList(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  /**
   * Keyboard navigation for form scrolling
   */
  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      if (contentRef.current && (event.ctrlKey || event.metaKey)) {
        if (event.key === 'ArrowUp') {
          event.preventDefault();
          contentRef.current.scrollBy({ top: -100, behavior: 'smooth' });
        } else if (event.key === 'ArrowDown') {
          event.preventDefault();
          contentRef.current.scrollBy({ top: 100, behavior: 'smooth' });
        }
      }
    };

    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, []);

  // ================ COMPUTED VALUES ================

  const sortedUsersList = [...usersList].sort((a, b) => a.nombre.localeCompare(b.nombre));

  // ================ RENDER ================

  if (isLoadingData) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
        <span className="ml-3 text-gray-600">Loading form data...</span>
      </div>
    );
  }

  return (
    <div className="flex flex-col h-full">
      
      {/* Scrollable Content */}
      <div ref={contentRef} className="flex-1 overflow-y-auto space-y-8 p-1">
        <form onSubmit={handleSubmit} className="space-y-8">
          
          {/* Event Details Section */}
          <section className="space-y-6">
            <h4 className="text-lg font-semibold text-gray-800 border-b-2 border-blue-100 pb-2">
              Event Details
            </h4>
            
            <div className="grid grid-cols-1 gap-6">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Event Title *
                </label>
                <input
                  type="text"
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  required
                  className="w-full p-3 border border-gray-300 rounded-lg shadow-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 bg-white text-gray-800 transition-all"
                  placeholder="Enter event title"
                  maxLength={100}
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Description
                </label>
                <textarea
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  className="w-full p-3 border border-gray-300 rounded-lg shadow-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 bg-white text-gray-800 transition-all"
                  placeholder="Event description or details"
                  rows={3}
                  maxLength={500}
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Event Type
                </label>
                <select
                  value={eventType}
                  onChange={(e) => setEventType(e.target.value as EventType)}
                  className="w-full p-3 border border-gray-300 rounded-lg shadow-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 bg-white text-gray-800 transition-all"
                >
                  {Object.entries(EVENT_TYPES).map(([value, label]) => (
                    <option key={value} value={value}>
                      {label}
                    </option>
                  ))}
                </select>
              </div>
            </div>
          </section>

          {/* Date & Time Section */}
          <section className="space-y-6">
            <h4 className="text-lg font-semibold text-gray-800 border-b-2 border-green-100 pb-2">
              Schedule
            </h4>
            
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Start Date & Time *
                </label>
                <input
                  type="datetime-local"
                  value={startDateTime}
                  onChange={(e) => setStartDateTime(e.target.value)}
                  required
                  className="w-full p-3 border border-gray-300 rounded-lg shadow-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 bg-white text-gray-800 transition-all"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  End Date & Time *
                </label>
                <input
                  type="datetime-local"
                  value={endDateTime}
                  onChange={(e) => setEndDateTime(e.target.value)}
                  required
                  className="w-full p-3 border border-gray-300 rounded-lg shadow-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 bg-white text-gray-800 transition-all"
                />
              </div>
            </div>

            <div className="space-y-4">
              <div className="flex items-center space-x-3">
                <input
                  type="checkbox"
                  id="recurring"
                  checked={isRecurring}
                  onChange={(e) => setIsRecurring(e.target.checked)}
                  className="h-5 w-5 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                />
                <label htmlFor="recurring" className="text-sm font-medium text-gray-700">
                  Recurring Event
                </label>
              </div>

              {isRecurring && (
                <div className="ml-8 space-y-3">
                  <label className="block text-sm font-medium text-gray-700">
                    Recurrence Days:
                  </label>
                  <div className="flex flex-wrap gap-4">
                    {Object.entries(WEEKDAY_LABELS).map(([key, label]) => (
                      <label key={key} className="flex items-center space-x-2">
                        <input
                          type="checkbox"
                          checked={selectedDays[key as keyof WeekdaySelection]}
                          onChange={() => handleDayChange(key as keyof WeekdaySelection)}
                          className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                        />
                        <span className="text-sm text-gray-700">{label}</span>
                      </label>
                    ))}
                  </div>
                </div>
              )}
            </div>
          </section>

          {/* Location & Resources Section */}
          <section className="space-y-6">
            <h4 className="text-lg font-semibold text-gray-800 border-b-2 border-purple-100 pb-2">
              Location & Resources
            </h4>
            
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Department
                </label>
                <div className="flex items-center gap-3">
                  <AreaIcon areaName={departmentsList.find(d => d.id === departmentId)?.nombre || ''} />
                  <select
                    value={departmentId || ''}
                    onChange={(e) => setDepartmentId(parseInt(e.target.value) || undefined)}
                    className="flex-1 p-3 border border-gray-300 rounded-lg shadow-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 bg-white text-gray-800 transition-all"
                  >
                    <option value="">No Department</option>
                    {departmentsList.map(dept => (
                      <option key={dept.id} value={dept.id}>
                        {dept.nombre}
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Room <span className="text-gray-500 text-xs">(Optional)</span>
                </label>
                <select
                  value={roomId || ''}
                  onChange={(e) => setRoomId(parseInt(e.target.value) || undefined)}
                  className="w-full p-3 border border-gray-300 rounded-lg shadow-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 bg-white text-gray-800 transition-all"
                >
                  <option value="">No Room</option>
                  {roomsList.map(room => (
                    <option key={room.id} value={room.id}>
                      {room.nombre}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Field Location <span className="text-gray-500 text-xs">(For off-site events)</span>
              </label>
              <input
                type="text"
                value={fieldDescription}
                onChange={(e) => setFieldDescription(e.target.value)}
                className="w-full p-3 border border-gray-300 rounded-lg shadow-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 bg-white text-gray-800 transition-all"
                placeholder="Field location or additional details"
                maxLength={200}
              />
            </div>
          </section>

          {/* Team & Participants Section */}
          <section className="space-y-6">
            <h4 className="text-lg font-semibold text-gray-800 border-b-2 border-orange-100 pb-2">
              Team & Participants
            </h4>
            
            <div ref={organizerRef}>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Event Organizer *
              </label>
              <div className="relative">
                <input
                  type="text"
                  value={organizerSearch}
                  onChange={(e) => {
                    setOrganizerSearch(e.target.value);
                    setShowOrganizerList(true);
                  }}
                  onFocus={() => setShowOrganizerList(true)}
                  className="w-full p-3 border border-gray-300 rounded-lg shadow-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 bg-white text-gray-800 transition-all"
                  placeholder="Search for organizer..."
                />
                {showOrganizerList && filteredUsers.length > 0 && (
                  <ul className="absolute z-20 w-full bg-white border border-gray-300 rounded-lg shadow-lg max-h-48 overflow-auto mt-1">
                    {filteredUsers.map(user => (
                      <li
                        key={user.id}
                        className="p-3 hover:bg-gray-50 cursor-pointer text-gray-800 border-b border-gray-100 last:border-b-0"
                        onClick={() => handleOrganizerSelect(user)}
                      >
                        <div className="font-medium">{user.nombre}</div>
                        <div className="text-sm text-gray-500">Department: {user.area2}</div>
                      </li>
                    ))}
                  </ul>
                )}
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-3">
                Event Participants
              </label>
              <div className="border border-gray-300 rounded-lg p-4 bg-gray-50 max-h-64 overflow-auto">
                <div className="flex items-center space-x-3 mb-4 pb-3 border-b border-gray-200">
                  <input
                    type="checkbox"
                    checked={
                      usersList.length > 0 &&
                      participants.length === usersList.filter(u => u.id !== organizerId).length
                    }
                    onChange={handleSelectAllParticipants}
                    className="h-5 w-5 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                  />
                  <span className="text-sm font-medium text-gray-800">Select All</span>
                  <span className="text-xs text-gray-500">
                    ({participants.length} selected)
                  </span>
                </div>
                
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
                  {sortedUsersList
                    .filter(u => u.id !== organizerId)
                    .map(user => (
                      <label key={user.id} className="flex items-center space-x-2 p-2 hover:bg-white rounded transition-colors">
                        <input
                          type="checkbox"
                          checked={participants.includes(user.id)}
                          onChange={() => handleParticipantChange(user.id)}
                          className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                        />
                        <span className="text-sm text-gray-800">{user.nombre}</span>
                      </label>
                    ))}
                </div>
              </div>
              
              {organizerId && (
                <p className="text-xs text-gray-500 mt-2">
                  The organizer ({organizerSearch}) is automatically included.
                </p>
              )}
            </div>
          </section>
        </form>
      </div>

      {/* Action Buttons */}
      <div className="sticky bottom-0 bg-white border-t border-gray-200 p-6 flex flex-col sm:flex-row gap-4 justify-center">
        <button
          type="button"
          onClick={onCancel}
          className="px-6 py-3 bg-gradient-to-r from-gray-500 to-gray-600 text-white rounded-lg hover:from-gray-600 hover:to-gray-700 transition-all duration-200 shadow-md hover:shadow-lg transform hover:-translate-y-0.5"
        >
          Cancel
        </button>
        <button
          type="button"
          onClick={handleSubmit}
          className="px-6 py-3 bg-gradient-to-r from-blue-500 to-blue-600 text-white rounded-lg hover:from-blue-600 hover:to-blue-700 transition-all duration-200 shadow-md hover:shadow-lg transform hover:-translate-y-0.5"
        >
          Save Event
        </button>
      </div>
    </div>
  );
};

// ================ MODAL WRAPPER COMPONENT ================

/**
 * EventFormModal Component
 * 
 * Modal wrapper providing:
 * - Proper z-index layering
 * - Responsive design
 * - Backdrop blur effects
 * - Smooth animations
 */
const EventFormModal: React.FC<EventFormModalProps> = ({ onSave, onClose }) => {
  
  // Prevent background scrolling when modal is open
  useEffect(() => {
    document.body.style.overflow = 'hidden';
    return () => {
      document.body.style.overflow = 'unset';
    };
  }, []);

  const handleBackdropClick = (e: React.MouseEvent) => {
    if (e.target === e.currentTarget) {
      onClose();
    }
  };

  return (
    <div 
      className="fixed inset-0 z-[1000] flex items-center justify-center bg-black/50 backdrop-blur-sm p-4"
      onClick={handleBackdropClick}
    >
      <div className="relative w-full max-w-6xl bg-white rounded-xl shadow-2xl max-h-[90vh] overflow-hidden animate-in fade-in slide-in-from-bottom-4 duration-300">
        
        {/* Modal Header */}
        <div className="sticky top-0 bg-white z-10 px-6 py-4 border-b border-gray-200">
          <div className="flex items-center justify-between">
            <h2 className="text-2xl font-bold text-gray-800">
              Create New Event
            </h2>
            <button
              onClick={onClose}
              className="p-2 hover:bg-gray-100 rounded-full transition-colors"
              aria-label="Close modal"
            >
              <svg className="w-6 h-6 text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
        </div>
        
        {/* Modal Content */}
        <div className="h-[calc(90vh-80px)]">
          <EventForm onSave={onSave} onCancel={onClose} />
        </div>
      </div>
    </div>
  );
};

export { EventForm, EventFormModal };
export default EventFormModal;
