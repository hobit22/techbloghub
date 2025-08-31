import { SkeletonGrid } from '@/components/SkeletonCard';

export default function Loading() {
  return (
    <div className="min-h-screen bg-slate-50">
      <div className="max-w-7xl mx-auto p-4 lg:p-6">
        {/* Header Skeleton */}
        <div className="mb-6 lg:mb-8">
          <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-4">
            <div className="space-y-2">
              <div className="h-8 bg-slate-200 rounded-md w-80 animate-pulse"></div>
              <div className="h-5 bg-slate-200 rounded-md w-60 animate-pulse"></div>
            </div>
          </div>
        </div>

        {/* Filter Bar Skeleton */}
        <div className="bg-white/95 backdrop-blur-sm border-b border-slate-200 -mx-4 lg:-mx-6 px-4 lg:px-6 py-4 mb-6">
          <div className="flex flex-wrap items-center gap-3">
            <div className="flex items-center space-x-3 mr-2">
              <div className="flex items-center space-x-2">
                <div className="w-8 h-8 bg-slate-200 rounded-lg animate-pulse"></div>
                <div className="h-5 bg-slate-200 rounded-md w-12 animate-pulse"></div>
              </div>
            </div>
            <div className="flex flex-wrap items-center gap-2">
              <div className="h-10 bg-slate-200 rounded-xl w-20 animate-pulse"></div>
              <div className="h-10 bg-slate-200 rounded-xl w-24 animate-pulse"></div>
              <div className="h-10 bg-slate-200 rounded-xl w-16 animate-pulse"></div>
            </div>
          </div>
        </div>

        {/* Content Skeleton */}
        <SkeletonGrid count={8} />
      </div>
    </div>
  );
}