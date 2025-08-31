'use client';

export default function SkeletonCard() {
  return (
    <article className="bg-white rounded-lg border border-slate-200 p-4 animate-pulse">
      <div className="flex items-start space-x-4">
        {/* Left side - Company Logo and Info */}
        <div className="flex-shrink-0">
          <div className="w-12 h-12 bg-slate-200 rounded-lg mb-2"></div>
          <div className="text-center space-y-1">
            <div className="h-3 bg-slate-200 rounded-md w-16"></div>
            <div className="h-2.5 bg-slate-200 rounded-md w-12"></div>
          </div>
        </div>

        {/* Main Content */}
        <div className="flex-1 min-w-0">
          <div className="flex items-start justify-between mb-2">
            <div className="flex-1 space-y-2 pr-4">
              <div className="h-5 bg-slate-200 rounded-md w-3/4"></div>
              <div className="h-4 bg-slate-200 rounded-md w-1/2"></div>
            </div>
            
            {/* Status and Arrow */}
            <div className="flex items-center space-x-2 flex-shrink-0">
              <div className="h-6 bg-slate-200 rounded-full w-12"></div>
              <div className="h-4 w-4 bg-slate-200 rounded"></div>
            </div>
          </div>

          {/* Content Preview */}
          <div className="space-y-2 mb-3">
            <div className="h-3.5 bg-slate-200 rounded-md w-full"></div>
            <div className="h-3.5 bg-slate-200 rounded-md w-5/6"></div>
          </div>

          {/* Tags and Categories */}
          <div className="flex flex-wrap gap-2 mb-3">
            <div className="h-6 bg-slate-200 rounded-md w-16"></div>
            <div className="h-6 bg-slate-200 rounded-md w-20"></div>
            <div className="h-6 bg-slate-200 rounded-md w-12"></div>
            <div className="h-6 bg-slate-200 rounded-md w-24"></div>
          </div>

          {/* Footer */}
          <div className="flex items-center justify-between text-sm">
            <div className="flex items-center space-x-4">
              <div className="flex items-center space-x-1">
                <div className="h-3.5 w-3.5 bg-slate-200 rounded"></div>
                <div className="h-3 bg-slate-200 rounded-md w-16"></div>
              </div>
              <div className="flex items-center space-x-1">
                <div className="h-3.5 w-3.5 bg-slate-200 rounded"></div>
                <div className="h-3 bg-slate-200 rounded-md w-20"></div>
              </div>
            </div>
            
            <div className="flex items-center space-x-2">
              <div className="h-3.5 w-3.5 bg-slate-200 rounded"></div>
            </div>
          </div>
        </div>
      </div>
    </article>
  );
}

export function SkeletonGrid({ count = 6 }: { count?: number }) {
  return (
    <div className="space-y-4">
      {Array.from({ length: count }).map((_, index) => (
        <SkeletonCard key={index} />
      ))}
    </div>
  );
}

// 컴팩트한 버전 (헤더용)
export function SkeletonCardCompact() {
  return (
    <div className="bg-white rounded-lg border border-slate-200 p-4 animate-pulse">
      <div className="flex items-start space-x-4">
        <div className="w-12 h-12 bg-slate-200 rounded-lg"></div>
        <div className="flex-1 space-y-2">
          <div className="h-5 bg-slate-200 rounded-md w-3/4"></div>
          <div className="h-4 bg-slate-200 rounded-md w-1/2"></div>
          <div className="flex gap-2 mt-2">
            <div className="h-6 bg-slate-200 rounded-md w-16"></div>
            <div className="h-6 bg-slate-200 rounded-md w-20"></div>
          </div>
        </div>
      </div>
    </div>
  );
}

export function SkeletonGridCompact({ count = 8 }: { count?: number }) {
  return (
    <div className="space-y-4">
      {Array.from({ length: count }).map((_, index) => (
        <SkeletonCardCompact key={index} />
      ))}
    </div>
  );
}