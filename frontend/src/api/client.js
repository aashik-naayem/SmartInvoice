import axios from "axios";

export const TOKEN_KEY = "smartinvoice.token";

const client = axios.create({
  baseURL: "/api/v1",
});

client.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

client.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem(TOKEN_KEY);
      if (!window.location.pathname.startsWith("/login")) {
        window.location.href = "/login";
      }
    }
    return Promise.reject(error);
  },
);

/** Pulls a readable message out of the backend's error shape, falling back to a generic one. */
export function apiErrorMessage(error, fallback = "Something went wrong. Please try again.") {
  const data = error?.response?.data;
  if (!data) return fallback;
  if (typeof data.message === "string") return data.message;
  if (data.errors && typeof data.errors === "object") {
    const first = Object.values(data.errors)[0];
    if (first) return String(first);
  }
  return fallback;
}

export default client;
