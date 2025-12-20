'use client';

import { memo } from 'react';
import { ChevronDown, Search } from 'lucide-react';

interface FilterDropdownProps {
  title: string;
  icon: React.ComponentType<{ className?: string }>;
  count: number;
  dropdownKey: string;
  children: React.ReactNode;
  hasSearch?: boolean;
  isOpen: boolean;
  searchValue?: string;
  onToggle: () => void;
  onSearchChange?: (value: string) => void;
  onWheel?: (e: React.WheelEvent) => void;
  dropdownRef?: (el: HTMLDivElement | null) => void;
}

export const FilterDropdown = memo<FilterDropdownProps>(({
  title,
  icon: Icon,
  count,
  dropdownKey,
  children,
  hasSearch = false,
  isOpen,
  searchValue = '',
  onToggle,
  onSearchChange,
  onWheel,
  dropdownRef,
}) => (
  <div className="relative">
    <button
      data-dropdown={dropdownKey}
      onClick={onToggle}
      className={`flex items-center space-x-2 px-4 py-2.5 border rounded-xl
                 text-sm font-medium transition-all duration-200 ${
        count > 0
          ? 'bg-blue-50 border-blue-200 text-blue-700 hover:bg-blue-100'
          : 'bg-white border-slate-200 text-slate-700 hover:bg-slate-50 hover:border-slate-300'
      }`}
    >
      <Icon className={`h-4 w-4 ${count > 0 ? 'text-blue-600' : 'text-slate-500'}`} />
      <span>{title}</span>
      {count > 0 && (
        <span className="bg-blue-600 text-white text-xs px-2 py-1 rounded-full font-medium min-w-[1.5rem] text-center">
          {count}
        </span>
      )}
      <ChevronDown
        className={`h-4 w-4 text-slate-400 transition-transform duration-200 ${
          isOpen ? 'rotate-180' : ''
        }`}
      />
    </button>

    {isOpen && (
      <div
        ref={dropdownRef}
        onWheel={onWheel}
        className="absolute top-full left-0 mt-2 w-96 bg-white border border-slate-200
                 rounded-xl shadow-xl z-50 animate-in fade-in slide-in-from-top-2 duration-200"
      >
        {hasSearch && (
          <div className="p-3 border-b border-slate-100">
            <div className="relative">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-slate-400" />
              <input
                type="text"
                placeholder={`${title} 검색...`}
                value={searchValue}
                onChange={(e) => onSearchChange?.(e.target.value)}
                className="w-full pl-10 pr-4 py-2 text-sm text-slate-800 placeholder:text-slate-500 border border-slate-200 rounded-lg
                         focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-400"
                onClick={(e) => e.stopPropagation()}
              />
            </div>
          </div>
        )}
        <div className="p-2 max-h-72 overflow-y-auto">{children}</div>
      </div>
    )}
  </div>
));

FilterDropdown.displayName = 'FilterDropdown';
