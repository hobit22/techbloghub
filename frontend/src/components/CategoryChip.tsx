'use client';

import { useRouter } from 'next/navigation';

interface CategoryChipProps {
  category: string;
  onClick?: (e: React.MouseEvent, category: string) => void;
}

export default function CategoryChip({ category, onClick }: CategoryChipProps) {
  const router = useRouter();

  const handleClick = (e: React.MouseEvent) => {
    e.stopPropagation();

    if (onClick) {
      onClick(e, category);
    } else {
      // Default behavior: navigate to home with category filter
      router.push(`/?categories=${encodeURIComponent(category)}`);
    }
  };

  return (
    <span
      onClick={handleClick}
      className="inline-flex items-center px-2 py-1 text-xs font-medium text-emerald-700
                 bg-emerald-50 border border-emerald-200 rounded-md cursor-pointer
                 hover:bg-emerald-100 transition-colors"
    >
      {category}
    </span>
  );
}