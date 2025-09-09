/**
 * AreaIcon Component - Department Visual Identification System
 * 
 * A centralized icon mapping system for organizational departments featuring:
 * - Consistent visual language across the application
 * - Scalable icon management with type safety
 * - Intuitive department recognition through iconography
 * - Fallback handling for unknown departments
 * - Accessible design with proper semantic markup
 * 
 * Technical Features:
 * - TypeScript interface for type safety
 * - React-icons integration for consistent styling
 * - Centralized icon mapping for maintainability
 * - Performance optimized with static mapping
 * - Extensible design for organizational growth
 * 
 * Business Value:
 * - Enhances user experience through visual recognition
 * - Reduces cognitive load in department identification
 * - Supports scalable organizational structure
 * - Maintains brand consistency across interfaces
 * 
 * @author [Daniel Jara]
 * @version 1.0.0
 * @requires react-icons
 */

import React, { JSX } from 'react';

// Icon imports from react-icons
import { FaUsers } from 'react-icons/fa';
import { FaTruckFront } from 'react-icons/fa6';
import { 
  MdOutlineTempleBuddhist, 
  MdBusiness, 
  MdPeopleOutline, 
  MdSettings, 
  MdDirectionsBoatFilled, 
  MdLightbulbOutline 
} from 'react-icons/md';
import { TfiMicrosoftAlt } from 'react-icons/tfi';
import { TbCashRegister } from 'react-icons/tb';
import { FiShoppingCart } from 'react-icons/fi';
import { BsCardChecklist } from 'react-icons/bs';
import { PiPlantDuotone, PiTruckTrailerFill } from 'react-icons/pi';
import { GiWoodPile, GiRoad } from 'react-icons/gi';
import { IoBarChartOutline } from 'react-icons/io5';

// ================ TYPE DEFINITIONS ================

/**
 * Props interface for AreaIcon component
 */
interface IAreaIconProps {
  /** Department name for icon mapping */
  areaName: string;
  /** Optional size override (default: 'lg') */
  size?: 'sm' | 'md' | 'lg' | 'xl';
  /** Optional color override */
  color?: string;
  /** Optional className for additional styling */
  className?: string;
}

/**
 * Department names enum for type safety and documentation
 */
enum DepartmentNames {
  ALL = 'All Departments',
  MANAGEMENT = 'Management',
  ADMINISTRATION = 'Administration', 
  IT = 'IT Department',
  HR = 'Human Resources',
  ACCOUNTING = 'Accounting',
  PROCUREMENT = 'Procurement',
  LEGAL = 'Legal & Compliance',
  OPERATIONS = 'Operations',
  FORESTRY = 'Forestry',
  HARVEST = 'Harvest',
  TRANSPORTATION = 'Transportation',
  INFRASTRUCTURE = 'Infrastructure',
  PLANNING = 'Planning & GIS',
  PRODUCTION = 'Production & Sales',
  TECHNICAL_SUPPORT = 'Technical Support',
  PROJECTS = 'Project Management',
}

// ================ ICON MAPPING ================

/**
 * Comprehensive department icon mapping
 * Each icon is carefully selected to represent the department's function
 * Maintains visual consistency and intuitive recognition
 */
const DEPARTMENT_ICON_MAP: { [key: string]: JSX.Element } = {
  // English department names (primary)
  [DepartmentNames.ALL]: <FaUsers />,
  [DepartmentNames.MANAGEMENT]: <MdOutlineTempleBuddhist />,
  [DepartmentNames.ADMINISTRATION]: <MdBusiness />,
  [DepartmentNames.IT]: <TfiMicrosoftAlt />,
  [DepartmentNames.HR]: <MdPeopleOutline />,
  [DepartmentNames.ACCOUNTING]: <TbCashRegister />,
  [DepartmentNames.PROCUREMENT]: <FiShoppingCart />,
  [DepartmentNames.LEGAL]: <BsCardChecklist />,
  [DepartmentNames.OPERATIONS]: <MdSettings />,
  [DepartmentNames.FORESTRY]: <PiPlantDuotone />,
  [DepartmentNames.HARVEST]: <GiWoodPile />,
  [DepartmentNames.TRANSPORTATION]: <FaTruckFront />,
  [DepartmentNames.INFRASTRUCTURE]: <GiRoad />,
  [DepartmentNames.PLANNING]: <IoBarChartOutline />,
  [DepartmentNames.PRODUCTION]: <MdDirectionsBoatFilled />,
  [DepartmentNames.TECHNICAL_SUPPORT]: <PiTruckTrailerFill />,
  [DepartmentNames.PROJECTS]: <MdLightbulbOutline />,

  // Legacy Spanish names for backward compatibility
  'Todas': <FaUsers />,
  'Gerencia': <MdOutlineTempleBuddhist />,
  'Administración': <MdBusiness />,
  'Informática': <TfiMicrosoftAlt />,
  'Recursos Humanos': <MdPeopleOutline />,
  'Contabilidad': <TbCashRegister />,
  'Compras': <FiShoppingCart />,
  'Legal y Regulación': <BsCardChecklist />,
  'Operaciones': <MdSettings />,
  'Silvícola': <PiPlantDuotone />,
  'Cosecha': <GiWoodPile />,
  'Transporte': <FaTruckFront />,
  'Caminos': <GiRoad />,
  'Planificación y Cartografía': <IoBarChartOutline />,
  'Producción y Ventas': <MdDirectionsBoatFilled />,
  'TSA': <PiTruckTrailerFill />,
  'Proyectos': <MdLightbulbOutline />,
};

/**
 * Size mapping for consistent icon scaling
 */
const SIZE_MAP = {
  sm: 'text-sm',
  md: 'text-base', 
  lg: 'text-lg',
  xl: 'text-xl',
} as const;

// ================ UTILITY FUNCTIONS ================

/**
 * Normalizes department name for consistent matching
 * Handles case sensitivity and common variations
 * 
 * @param name - Raw department name
 * @returns Normalized department name
 */
const normalizeDepartmentName = (name: string): string => {
  if (!name) return '';
  
  // Trim whitespace and normalize case
  const normalized = name.trim();
  
  // Check for exact matches first (case-sensitive)
  if (DEPARTMENT_ICON_MAP[normalized]) {
    return normalized;
  }
  
  // Case-insensitive fallback
  const keys = Object.keys(DEPARTMENT_ICON_MAP);
  const match = keys.find(key => key.toLowerCase() === normalized.toLowerCase());
  
  return match || '';
};

/**
 * Gets appropriate icon for department with fallback
 * 
 * @param departmentName - Department name to map
 * @returns JSX element for the icon
 */
const getDepartmentIcon = (departmentName: string): JSX.Element => {
  const normalizedName = normalizeDepartmentName(departmentName);
  return DEPARTMENT_ICON_MAP[normalizedName] || DEPARTMENT_ICON_MAP[DepartmentNames.ALL];
};

// ================ MAIN COMPONENT ================

/**
 * AreaIcon Component
 * 
 * Demonstrates:
 * - Clean component architecture
 * - Type-safe props handling
 * - Flexible styling system
 * - Graceful fallback handling
 * - Performance optimization
 */
const AreaIcon: React.FC<IAreaIconProps> = ({ 
  areaName, 
  size = 'lg', 
  color,
  className = '' 
}) => {
  
  // Get the appropriate icon for the department
  const icon = getDepartmentIcon(areaName);
  
  // Build CSS classes
  const sizeClass = SIZE_MAP[size];
  const colorStyle = color ? { color } : {};
  const combinedClassName = `inline-flex items-center justify-center ${sizeClass} ${className}`.trim();
  
  // Log for debugging in development
  if (process.env.NODE_ENV === 'development' && !DEPARTMENT_ICON_MAP[normalizeDepartmentName(areaName)]) {
    console.warn(`AreaIcon: No icon found for department "${areaName}". Using fallback icon.`);
  }
  
  return (
    <span 
      className={combinedClassName}
      style={colorStyle}
      title={areaName || 'Department Icon'}
      aria-label={`${areaName || 'Unknown'} department icon`}
      role="img"
    >
      {icon}
    </span>
  );
};

// ================ HELPER COMPONENTS ================

/**
 * Specialized component for department headers
 */
export const DepartmentHeader: React.FC<{ 
  departmentName: string; 
  children?: React.ReactNode;
}> = ({ departmentName, children }) => (
  <div className="flex items-center gap-3 p-3 bg-gray-50 rounded-lg border">
    <AreaIcon areaName={departmentName} size="xl" />
    <div className="flex-1">
      <h3 className="font-semibold text-gray-800">{departmentName}</h3>
      {children}
    </div>
  </div>
);

/**
 * Compact department indicator for lists
 */
export const DepartmentIndicator: React.FC<{ 
  departmentName: string;
  showLabel?: boolean;
}> = ({ departmentName, showLabel = true }) => (
  <div className="flex items-center gap-2">
    <AreaIcon areaName={departmentName} size="sm" />
    {showLabel && (
      <span className="text-sm text-gray-600 truncate">{departmentName}</span>
    )}
  </div>
);

// ================ UTILITY EXPORTS ================

/**
 * Get all available department names
 * Useful for dropdowns and validation
 */
export const getAvailableDepartments = (): string[] => {
  return Object.values(DepartmentNames);
};

/**
 * Check if a department name has an associated icon
 */
export const hasDepartmentIcon = (departmentName: string): boolean => {
  const normalizedName = normalizeDepartmentName(departmentName);
  return !!DEPARTMENT_ICON_MAP[normalizedName];
};

/**
 * Get department icon as React element (for external use)
 */
export const getDepartmentIconElement = (departmentName: string): JSX.Element => {
  return getDepartmentIcon(departmentName);
};

export default AreaIcon;
