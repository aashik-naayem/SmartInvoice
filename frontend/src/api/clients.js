import client from "./client";

export function listClients() {
  return client.get("/clients").then((res) => res.data);
}

export function getClient(id) {
  return client.get(`/clients/${id}`).then((res) => res.data);
}

export function createClient(payload) {
  return client.post("/clients", payload).then((res) => res.data);
}

export function updateClient(id, payload) {
  return client.put(`/clients/${id}`, payload).then((res) => res.data);
}

export function deleteClient(id) {
  return client.delete(`/clients/${id}`);
}
