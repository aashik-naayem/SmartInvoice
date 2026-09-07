import client from "./client";

export function getDashboard() {
  return client.get("/analytics/dashboard").then((res) => res.data);
}
