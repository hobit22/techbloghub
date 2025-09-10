import { NextResponse } from 'next/server'

/**
 * Frontend Health Check API
 * 백엔드 API(/api/*)와 충돌하지 않는 프론트엔드 전용 헬스체크 엔드포인트
 */
export async function GET() {
  try {
    return NextResponse.json({
      status: 'healthy',
      service: 'techbloghub-frontend',
      timestamp: new Date().toISOString(),
      version: process.env.npm_package_version || '1.0.0',
      environment: process.env.NODE_ENV || 'production'
    }, { status: 200 })
  } catch (error) {
    console.error('Health check failed:', error)
    
    return NextResponse.json({
      status: 'unhealthy',
      service: 'techbloghub-frontend',
      error: error instanceof Error ? error.message : 'Unknown error',
      timestamp: new Date().toISOString()
    }, { status: 500 })
  }
}

// OPTIONS method for CORS preflight requests (if needed)
export async function OPTIONS() {
  return NextResponse.json({}, { status: 200 })
}