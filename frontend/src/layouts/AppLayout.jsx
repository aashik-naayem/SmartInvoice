import { NavLink, Outlet } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

const NAV_ITEMS = [
  { to: "/", label: "Dashboard", end: true },
  { to: "/invoices", label: "Invoices" },
  { to: "/clients", label: "Clients" },
  { to: "/recurring", label: "Recurring" },
];

function navLinkClasses({ isActive }) {
  return `block rounded-sm px-3 py-2 text-sm transition-colors ${
    isActive
      ? "bg-[color:var(--color-pine)] text-white"
      : "text-[color:var(--color-ink-muted)] hover:bg-[color:var(--color-surface)] hover:text-[color:var(--color-ink)]"
  }`;
}

export default function AppLayout() {
  const { user, logout } = useAuth();

  return (
    <div className="min-h-screen bg-[color:var(--color-paper)] text-[color:var(--color-ink)] lg:flex">
      <aside className="border-b border-[color:var(--color-border)] px-5 py-6 lg:h-screen lg:w-56 lg:flex-shrink-0 lg:border-b-0 lg:border-r lg:sticky lg:top-0">
        <div className="flex items-center justify-between lg:block">
          <div>
            <p className="font-serif text-lg leading-none">SmartInvoice</p>
            <p className="mt-1 text-xs text-[color:var(--color-ink-muted)]">Billing ledger</p>
          </div>
        </div>

        <nav className="mt-6 flex gap-1 overflow-x-auto lg:mt-8 lg:flex-col lg:overflow-visible">
          {NAV_ITEMS.map((item) => (
            <NavLink key={item.to} to={item.to} end={item.end} className={navLinkClasses}>
              {item.label}
            </NavLink>
          ))}
        </nav>

        <div className="mt-8 hidden border-t border-[color:var(--color-border)] pt-4 lg:block">
          <p className="truncate text-sm font-medium">{user?.fullName}</p>
          <p className="truncate text-xs text-[color:var(--color-ink-muted)]">{user?.email}</p>
          <button
            onClick={logout}
            className="mt-3 text-sm text-[color:var(--color-ink-muted)] underline decoration-dotted underline-offset-4 hover:text-[color:var(--color-overdue)]"
          >
            Sign out
          </button>
        </div>
      </aside>

      <main className="flex-1 px-5 py-8 lg:px-10">
        <Outlet />
      </main>
    </div>
  );
}
