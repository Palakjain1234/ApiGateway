import client from './client'

export const getOrganizations    = ()      => client.get('/organizations')
export const getOrganization     = id      => client.get(`/organizations/${id}`)
export const createOrganization  = data    => client.post('/organizations', data)
export const activateOrg         = id      => client.put(`/organizations/${id}/activate`)
export const suspendOrg          = id      => client.put(`/organizations/${id}/suspend`)
export const deleteOrg           = id      => client.delete(`/organizations/${id}`)
export const createTenantAdmin   = (id, data) => client.post(`/organizations/${id}/admin`, data)
