import client from "./client";

export function login(email, password) {
  return client.post("/auth/login", { email, password }).then((res) => res.data);
}

export function register(fullName, email, password) {
  return client.post("/auth/register", { fullName, email, password }).then((res) => res.data);
}
