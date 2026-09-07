import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { apiErrorMessage } from "../api/client";
import Button from "../components/Button";
import { Field, Input } from "../components/FormFields";
import { useAuth } from "../context/AuthContext";
import AuthLayout from "../layouts/AuthLayout";

export default function RegisterPage() {
  const { register } = useAuth();
  const navigate = useNavigate();
  const [fullName, setFullName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setLoading(true);
    try {
      await register(fullName, email, password);
      navigate("/", { replace: true });
    } catch (err) {
      setError(apiErrorMessage(err, "Could not create your account."));
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthLayout title="Create your account" subtitle="Set up billing for your business in a minute.">
      <form className="flex flex-col gap-4" onSubmit={handleSubmit}>
        <Field label="Full name" htmlFor="fullName">
          <Input
            id="fullName"
            autoComplete="name"
            required
            value={fullName}
            onChange={(e) => setFullName(e.target.value)}
          />
        </Field>
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
        <Field label="Password" htmlFor="password" hint="At least 8 characters.">
          <Input
            id="password"
            type="password"
            autoComplete="new-password"
            minLength={8}
            required
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
        </Field>

        {error && <p className="text-sm text-[color:var(--color-overdue)]">{error}</p>}

        <Button type="submit" disabled={loading} className="mt-2 w-full">
          {loading ? "Creating account…" : "Create account"}
        </Button>
      </form>

      <p className="mt-6 text-sm text-[color:var(--color-ink-muted)]">
        Already have an account?{" "}
        <Link to="/login" className="text-[color:var(--color-pine)] underline underline-offset-4">
          Sign in
        </Link>
      </p>
    </AuthLayout>
  );
}
