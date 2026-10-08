import client from './client'

export const getPlatformStats = ()                   => client.get('/analytics/platform')
export const getOrgStats      = (slug, from, to)     => client.get(`/analytics/org/${slug}?from=${from}&to=${to}`)
export const getRouteStats    = (routeId, from, to)  => client.get(`/analytics/route/${routeId}?from=${from}&to=${to}`)
