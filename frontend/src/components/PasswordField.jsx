import { useState } from "react";

export default function PasswordField({ value, onChange, autoComplete = "current-password" }) {
  const [visible, setVisible] = useState(false);
  return (
    <div className="pw">
      <input
        type={visible ? "text" : "password"}
        value={value}
        autoComplete={autoComplete}
        onChange={(e) => onChange(e.target.value)}
        required
      />
      <button
        type="button"
        className="btn ghost small pw-toggle"
        aria-pressed={visible}
        onClick={() => setVisible((show) => !show)}
      >
        {visible ? "Hide" : "Show"}
      </button>
    </div>
  );
}
