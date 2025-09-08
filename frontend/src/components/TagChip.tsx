'use client';

import { X } from 'lucide-react';
import { forwardRef } from 'react';

interface TagChipProps {
  tag: string;
  onRemove: () => void;
  compact?: boolean;
  isFocused?: boolean;
  onFocus?: () => void;
  onKeyDown?: (e: React.KeyboardEvent) => void;
}

const TagChip = forwardRef<HTMLDivElement, TagChipProps>(
  ({ tag, onRemove, compact = false, isFocused = false, onFocus, onKeyDown }, ref) => {
    const handleKeyDown = (e: React.KeyboardEvent) => {
      if (e.key === 'Enter' || e.key === ' ') {
        e.preventDefault();
        onRemove();
      } else if (onKeyDown) {
        onKeyDown(e);
      }
    };

    if (compact) {
      return (
        <div
          ref={ref}
          tabIndex={0}
          onFocus={onFocus}
          onKeyDown={handleKeyDown}
          className={`inline-flex items-center text-xs font-medium px-2 py-1 rounded-md border flex-shrink-0 cursor-pointer transition-all duration-200 ${
            isFocused
              ? 'bg-blue-600 text-white border-blue-700 ring-2 ring-blue-300 ring-offset-1'
              : 'bg-blue-500 text-white border-blue-600 hover:bg-blue-600'
          }`}
        >
          <span className="mr-1">#</span>
          <span className="max-w-16 truncate">{tag}</span>
          <button
            onClick={onRemove}
            className="ml-1 hover:bg-blue-700 rounded-full p-0.5 transition-colors flex-shrink-0"
            type="button"
            title={`${tag} 태그 제거`}
          >
            <X className="h-2.5 w-2.5" />
          </button>
        </div>
      );
    }

    return (
      <div
        ref={ref}
        tabIndex={0}
        onFocus={onFocus}
        onKeyDown={handleKeyDown}
        className={`inline-flex items-center text-sm font-medium px-3 py-1 rounded-full border max-w-full cursor-pointer transition-all duration-200 ${
          isFocused
            ? 'bg-blue-200 text-blue-900 border-blue-300 ring-2 ring-blue-300 ring-offset-1'
            : 'bg-blue-100 text-blue-800 border-blue-200 hover:bg-blue-200'
        }`}
      >
        <span className="mr-1 flex-shrink-0">#</span>
        <span className="truncate">{tag}</span>
        <button
          onClick={onRemove}
          className="ml-2 hover:bg-blue-300 rounded-full p-0.5 transition-colors flex-shrink-0"
          type="button"
          title={`${tag} 태그 제거`}
        >
          <X className="h-3 w-3" />
        </button>
      </div>
    );
  }
);

TagChip.displayName = 'TagChip';

export default TagChip;