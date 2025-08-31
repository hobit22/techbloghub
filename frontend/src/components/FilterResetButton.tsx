'use client';

import { RotateCcw } from 'lucide-react';

interface FilterResetButtonProps {
  onReset: () => void;
  hasFilters: boolean;
}

export default function FilterResetButton({ onReset, hasFilters }: FilterResetButtonProps) {
  if (!hasFilters) return null;

  return (
    <button
      onClick={onReset}
      className="flex items-center space-x-2 px-4 py-2 text-sm text-gray-600 hover:text-gray-900 hover:bg-gray-100 rounded-lg transition-colors"
    >
      <RotateCcw className="h-4 w-4" />
      <span>필터 초기화</span>
    </button>
  );
}
