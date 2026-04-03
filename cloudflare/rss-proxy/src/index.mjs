export default {
  async fetch(request) {
    if (request.method === 'OPTIONS') {
      return new Response(null, {
        headers: corsHeaders(),
      });
    }

    if (request.method !== 'GET') {
      return new Response('Method Not Allowed', {
        status: 405,
        headers: { ...corsHeaders(), Allow: 'GET' },
      });
    }

    const requestUrl = new URL(request.url);
    const targetUrl = requestUrl.searchParams.get('url');

    if (!targetUrl) {
      return jsonError('Missing url query parameter', 400);
    }

    let parsedTargetUrl;
    try {
      parsedTargetUrl = new URL(targetUrl);
    } catch {
      return jsonError('Invalid target URL', 400);
    }

    if (!['http:', 'https:'].includes(parsedTargetUrl.protocol)) {
      return jsonError('Only http and https URLs are supported', 400);
    }

    const upstreamRequest = new Request(parsedTargetUrl.toString(), {
      method: 'GET',
      headers: {
        'accept': 'application/rss+xml, application/atom+xml, application/xml, text/xml;q=0.9, text/html;q=0.8, */*;q=0.7',
        'accept-language': 'ko-KR,ko;q=0.9,en;q=0.8',
        'cache-control': 'no-cache',
        'referer': 'https://www.google.com/',
        'user-agent': 'Mozilla/5.0 (compatible; TechBlogHubRSSProxy/1.0; +https://github.com/hobit22/techbloghub)',
      },
      redirect: 'follow',
    });

    try {
      const upstreamResponse = await fetch(upstreamRequest);
      const responseHeaders = new Headers(upstreamResponse.headers);
      applyCorsHeaders(responseHeaders);
      responseHeaders.set('x-techbloghub-proxy', 'cloudflare-worker');
      responseHeaders.set('x-techbloghub-upstream-status', String(upstreamResponse.status));

      return new Response(upstreamResponse.body, {
        status: upstreamResponse.status,
        statusText: upstreamResponse.statusText,
        headers: responseHeaders,
      });
    } catch (error) {
      return jsonError(
        error instanceof Error ? error.message : 'Upstream fetch failed',
        502,
      );
    }
  },
};

function jsonError(message, status) {
  return new Response(JSON.stringify({ error: message }), {
    status,
    headers: {
      ...corsHeaders(),
      'content-type': 'application/json; charset=utf-8',
    },
  });
}

function corsHeaders() {
  return {
    'access-control-allow-origin': '*',
    'access-control-allow-methods': 'GET, OPTIONS',
  };
}

function applyCorsHeaders(headers) {
  headers.set('access-control-allow-origin', '*');
  headers.set('access-control-allow-methods', 'GET, OPTIONS');
}
