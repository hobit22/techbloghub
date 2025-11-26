'use client';

import { useRef, KeyboardEvent } from 'react';
import { Search, X } from 'lucide-react';

interface UnifiedSearchInputProps {
  value: string;
  onValueChange: (value: string) => void;
  onSearch?: (query: string) => void;
  onClear?: () => void;
  placeholder?: string;
}

export default function UnifiedSearchInput({
  value,
  onValueChange,
  onSearch,
  onClear,
  placeholder = "검색어 입력..."
}: UnifiedSearchInputProps) {
  const inputRef = useRef<HTMLInputElement>(null);

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    onValueChange(e.target.value);
  };

  const handleKeyDown = (e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter' && onSearch) {
      onSearch(value);
    }
  };

  const handleSearchClick = () => {
    if (onSearch) {
      onSearch(value);
    }
  };

  const handleClear = () => {
    onValueChange('');
    if (onClear) {
      onClear();
    }
    inputRef.current?.focus();
  };

  return (
    <div className="relative w-full">
      <div className="relative flex items-center">
        <Search className="absolute left-4 h-5 w-5 text-slate-400 pointer-events-none" />
        <input
          ref={inputRef}
          type="text"
          value={value}
          onChange={handleInputChange}
          onKeyDown={handleKeyDown}
          placeholder={placeholder}
          className="w-full pl-11 pr-20 py-2.5 text-sm text-slate-900 placeholder:text-slate-500
                   bg-white border border-slate-200 rounded-xl
                   focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-400
                   transition-all duration-200"
        />
        <div className="absolute right-2 flex items-center space-x-1">
          {value && (
            <button
              onClick={handleClear}
              className="p-1.5 hover:bg-slate-100 rounded-lg transition-colors"
              aria-label="Clear search"
            >
              <X className="h-4 w-4 text-slate-400" />
            </button>
          )}
          <button
            onClick={handleSearchClick}
            className="p-1.5 bg-blue-600 hover:bg-blue-700 text-white rounded-lg transition-colors"
            aria-label="Search"
          >
            <Search className="h-4 w-4" />
          </button>
        </div>
      </div>
    </div>
  );
}
