import { useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { apiErrorMessage } from "../api/client";
import Button from "../components/Button";
import { Field, Input } from "../components/FormFields";
import { useAuth } from "../context/AuthContext";
import AuthLayout from "../layouts/AuthLayout";

export default function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setLoading(true);
    try {
      await login(email, password);
      navigate(location.state?.from ?? "/", { replace: true });
    } catch (err) {
      setError(apiErrorMessage(err, "Could not sign in. Check your email and password."));
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthLayout title="Sign in" subtitle="Welcome back — pick up where you left off.">
      <form className="flex flex-col gap-4" onSubmit={handleSubmit}>
        <Field label="Email" htmlFor="email">
          <Input
            id="email"
            type="email"
            autoComplete="email"
            required
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />
        </Field>
        <Field label="Password" htmlFor="password">
          <Input
            id="password"
            type="password"
            autoComplete="current-password"
            required
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
        </Field>

        {error && <p className="text-sm text-[color:var(--color-overdue)]">{error}</p>}

        <Button type="submit" disabled={loading} className="mt-2 w-full">
          {loading ? "Signing in…" : "Sign in"}
        </Button>
      </form>

      <p className="mt-6 text-sm text-[color:var(--color-ink-muted)]">
        New to SmartInvoice?{" "}
        <Link to="/register" className="text-[color:var(--color-pine)] underline underline-offset-4">
          Create an account
        </Link>
      </p>
    </AuthLayout>
  );
}
