import { createContext, useCallback, useContext, useRef, useState } from "react";

const ToastContext = createContext(null);

const STYLES = {
  success: "border-[color:var(--color-paid)]/30 bg-[#e4f0e9] text-[color:var(--color-paid)]",
  error: "border-[color:var(--color-overdue)]/30 bg-[#f6e8e3] text-[color:var(--color-overdue)]",
  info: "border-[color:var(--color-border)] bg-[color:var(--color-surface)] text-[color:var(--color-ink)]",
};

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([]);
  const nextId = useRef(0);

  const dismiss = useCallback((id) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  const showToast = useCallback(
    (message, type = "info") => {
      const id = nextId.current++;
      setToasts((prev) => [...prev, { id, message, type }]);
      setTimeout(() => dismiss(id), 4500);
    },
    [dismiss],
  );

  const value = {
    showToast,
    showSuccess: (message) => showToast(message, "success"),
    showError: (message) => showToast(message, "error"),
  };

  return (
    <ToastContext.Provider value={value}>
      {children}
      <div className="fixed bottom-4 right-4 z-[100] flex w-full max-w-sm flex-col gap-2">
        {toasts.map((t) => (
          <div
            key={t.id}
            role="status"
            className={`flex items-start justify-between gap-3 rounded-sm border px-4 py-3 text-sm shadow-md ${STYLES[t.type]}`}
          >
            <span>{t.message}</span>
            <button
              onClick={() => dismiss(t.id)}
              aria-label="Dismiss"
              className="text-current opacity-60 hover:opacity-100"
            >
              ✕
            </button>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
}

export function useToast() {
  const ctx = useContext(ToastContext);
  if (!ctx) throw new Error("useToast must be used within a ToastProvider");
  return ctx;
}
