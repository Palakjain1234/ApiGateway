import client from './client'

export const getRoutes       = orgId        => client.get(`/organizations/${orgId}/routes`)
export const getRoute        = (orgId, rid) => client.get(`/organizations/${orgId}/routes/${rid}`)
export const createRoute     = (orgId, data)=> client.post(`/organizations/${orgId}/routes`, data)
export const updateRoute     = (orgId, rid, data) => client.put(`/organizations/${orgId}/routes/${rid}`, data)
export const activateRoute   = (orgId, rid) => client.put(`/organizations/${orgId}/routes/${rid}/activate`)
export const deactivateRoute = (orgId, rid) => client.put(`/organizations/${orgId}/routes/${rid}/deactivate`)
export const deleteRoute     = (orgId, rid) => client.delete(`/organizations/${orgId}/routes/${rid}`)
export const getRouteHistory = (orgId, rid) => client.get(`/organizations/${orgId}/routes/${rid}/history`)
