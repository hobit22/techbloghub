'use client';

import { useRouter } from 'next/navigation';

interface ClickableTagChipProps {
  tag: string;
  onClick?: (e: React.MouseEvent, tag: string) => void;
}

export default function ClickableTagChip({ tag, onClick }: ClickableTagChipProps) {
  const router = useRouter();

  const handleClick = (e: React.MouseEvent) => {
    e.stopPropagation();

    if (onClick) {
      onClick(e, tag);
    } else {
      // Default behavior: navigate to home with tag filter
      router.push(`/?tags=${encodeURIComponent(tag)}`);
    }
  };

  return (
    <span
      onClick={handleClick}
      className="inline-flex items-center px-2 py-1 text-xs font-medium text-slate-600
                 bg-slate-100 hover:bg-slate-200 rounded-md transition-colors cursor-pointer"
    >
      #{tag}
    </span>
  );
}