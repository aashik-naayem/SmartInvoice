import { createContext, useCallback, useContext, useRef, useState } from "react";
import Button from "../components/Button";
import Modal from "../components/Modal";

const ConfirmContext = createContext(null);

export function ConfirmProvider({ children }) {
  const [request, setRequest] = useState(null);
  const resolver = useRef(null);

  const confirm = useCallback((options) => {
    setRequest(typeof options === "string" ? { message: options } : options);
    return new Promise((resolve) => {
      resolver.current = resolve;
    });
  }, []);

  const settle = (result) => {
    setRequest(null);
    if (resolver.current) {
      resolver.current(result);
      resolver.current = null;
    }
  };

  return (
    <ConfirmContext.Provider value={confirm}>
      {children}
      <Modal open={Boolean(request)} title={request?.title || "Are you sure?"} onClose={() => settle(false)}>
        <p className="text-sm text-[color:var(--color-ink-muted)]">{request?.message}</p>
        <div className="mt-5 flex justify-end gap-2">
          <Button variant="secondary" onClick={() => settle(false)}>
            {request?.cancelLabel || "Cancel"}
          </Button>
          <Button
            variant={request?.danger === false ? "primary" : "dangerSolid"}
            onClick={() => settle(true)}
          >
            {request?.confirmLabel || "Confirm"}
          </Button>
        </div>
      </Modal>
    </ConfirmContext.Provider>
  );
}

/** Returns confirm(options) -> Promise<boolean>. options: string | { title, message, confirmLabel, cancelLabel, danger }. */
export function useConfirm() {
  const ctx = useContext(ConfirmContext);
  if (!ctx) throw new Error("useConfirm must be used within a ConfirmProvider");
  return ctx;
}
