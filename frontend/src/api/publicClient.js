import axios from "axios";

// Deliberately separate from ./client: this one never attaches a login token and never
// redirects to /login on a 401, since the pages that use it (the client-facing invoice
// view) have no concept of being logged in at all - access is gated by the link's token.
const publicClient = axios.create({
  baseURL: "/api/v1/public",
});

export default publicClient;
