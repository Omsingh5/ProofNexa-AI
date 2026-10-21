import { useRef, useState } from "react";
import { api, getToken, rejectApi } from "../api.js";

const CANDIDATE = [
  "Analyze my technical skills",
  "Find matching jobs",
  "Explain my project quality report",
  "Prepare me for an interview",
  "What is my application status?",
  "Show my skill gaps",
  "Show my roadmap",
  "Help with my technical challenge"
];

const RECRUITER = [
  "Draft a job description for a Java role",
  "Summarize applicants",
  "Plan an interview",
  "Show application pipeline",
  "Explain recruitment analytics",
  "Summarize technical evidence we have"
];

const ACCEPT = ".pdf,.docx,.txt,.jpg,.jpeg,.png,.webp";

function formatSize(bytes) {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${Math.ceil(bytes / 1024)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

export default function NexaChat({ role }) {
  const [open, setOpen] = useState(false);
  const [messages, setMessages] = useState([]);
  const [text, setText] = useState("");
  const [files, setFiles] = useState([]);
  const [progress, setProgress] = useState(0);
  const [pending, setPending] = useState(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const fileRef = useRef(null);
  const suggestions = role === "CANDIDATE" ? CANDIDATE : RECRUITER;

  function addFiles(list) {
    const incoming = Array.from(list || []);
    if (!incoming.length) return;
    setError("");
    setFiles((prev) => {
      const next = [...prev];
      for (const file of incoming) {
        if (next.length >= 3) {
          setError("Attach up to 3 files.");
          break;
        }
        if (file.size > 4 * 1024 * 1024) {
          setError(`${file.name} is larger than 4 MB.`);
          continue;
        }
        const allowed = /\.(pdf|docx|txt|jpe?g|png|webp)$/i.test(file.name);
        if (!allowed) {
          setError("Attach a PDF, Word (.docx), text, or image file.");
          continue;
        }
        const preview = file.type.startsWith("image/") ? URL.createObjectURL(file) : "";
        next.push({ file, preview });
      }
      return next;
    });
    if (fileRef.current) fileRef.current.value = "";
  }

  function removeFile(index) {
    setFiles((prev) => {
      const removed = prev[index];
      if (removed?.preview) URL.revokeObjectURL(removed.preview);
      return prev.filter((_, i) => i !== index);
    });
  }

  async function send(message, confirmAction) {
    const outgoing = (message ?? text).trim();
    const attached = confirmAction ? [] : files;
    if (!outgoing && !confirmAction && attached.length === 0) return;
    setError("");
    setBusy(true);
    setPending(null);
    setProgress(0);
    if (!confirmAction) {
      setMessages((prev) => [...prev, {
        role: "user",
        content: outgoing || "Please review the attached file.",
        files: attached.map((item) => ({ name: item.file.name, size: item.file.size, preview: item.preview }))
      }]);
      setText("");
      setFiles([]);
    }
    try {
      const reply = attached.length
        ? await sendWithFiles(outgoing || "Please review the attached file.", attached, setProgress)
        : await api("/api/ai/nexa", {
          method: "POST",
          body: JSON.stringify({ message: outgoing || "Confirmed.", confirmAction: confirmAction || null })
        });
      setMessages((prev) => [...prev, { role: "assistant", content: reply.content, feature: reply.feature }]);
      setPending(reply.pendingAction || null);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
      setProgress(0);
    }
  }

  return (
    <div className="nexa">
      {open && (
        <section className="nexa-panel" aria-label="Nexa AI">
          <header className="nexa-head">
            <div className="nexa-id">
              <img src="/logo.png?v=2" alt="" />
              <div>
                <strong>Nexa AI</strong>
                <p>Your intelligent career assistant</p>
              </div>
            </div>
            <span className="nexa-badge">AI Assistant</span>
            <button className="nexa-x" type="button" onClick={() => setOpen(false)} aria-label="Minimize chat">×</button>
          </header>
          <div className="nexa-body">
            {messages.length === 0 && (
              <div className="nexa-welcome">
                <strong>Welcome to Nexa AI</strong>
                <p>Ask in your own words, or attach a resume, document, or image. I use your ProofNexa account when the question is about it.</p>
              </div>
            )}
            {messages.map((m, i) => (
              <div className={m.role === "user" ? "nexa-msg user" : "nexa-msg"} key={i}>
                {m.files?.length > 0 && (
                  <div className="nexa-files">
                    {m.files.map((file) => (
                      <span className="nexa-file" key={file.name}>
                        {file.preview && <img src={file.preview} alt="" />}
                        <span>{file.name}<small>{formatSize(file.size)}</small></span>
                      </span>
                    ))}
                  </div>
                )}
                <p>{m.content}</p>
              </div>
            ))}
            {busy && <p className="muted">{progress > 0 && progress < 100 ? `Uploading ${progress}%…` : "Nexa is reading your question…"}</p>}
            {error && <p className="error">{error}</p>}
            {pending && (
              <button className="btn small" type="button" disabled={busy} onClick={() => send("Yes, go ahead.", pending)}>
                Confirm and run
              </button>
            )}
            {messages.length === 0 && (
              <>
                <p className="nexa-label">Suggested questions</p>
                {suggestions.map((q) => (
                  <button className="nexa-suggest" type="button" key={q} disabled={busy} onClick={() => send(q)}>
                    {q} <span>↗</span>
                  </button>
                ))}
              </>
            )}
          </div>
          {files.length > 0 && (
            <div className="nexa-files composer">
              {files.map((item, index) => (
                <span className="nexa-file" key={`${item.file.name}-${index}`}>
                  {item.preview && <img src={item.preview} alt="" />}
                  <span>{item.file.name}<small>{formatSize(item.file.size)}</small></span>
                  <button type="button" onClick={() => removeFile(index)} aria-label={`Remove ${item.file.name}`}>×</button>
                </span>
              ))}
            </div>
          )}
          {progress > 0 && <div className="nexa-progress" style={{ width: `${progress}%` }} />}
          <form className="nexa-form" onSubmit={(e) => { e.preventDefault(); send(); }}>
            <input ref={fileRef} type="file" accept={ACCEPT} multiple hidden onChange={(e) => addFiles(e.target.files)} />
            <button className="nexa-clip" type="button" disabled={busy} aria-label="Attach a file" onClick={() => fileRef.current?.click()}>
              <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M8.5 12.5 14 7a3 3 0 0 1 4.2 4.2l-7.1 7.1a4.5 4.5 0 0 1-6.4-6.4l6.4-6.3" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" /></svg>
            </button>
            <input value={text} onChange={(e) => setText(e.target.value)} placeholder="Ask Nexa AI anything..." />
            <button className="btn small" type="submit" disabled={busy} aria-label="Send">↑</button>
          </form>
        </section>
      )}
      {!open && (
        <button className="nexa-launcher" type="button" onClick={() => setOpen(true)}>Nexa AI</button>
      )}
    </div>
  );
}

function sendWithFiles(message, attached, onProgress) {
  const body = new FormData();
  body.append("message", message);
  attached.forEach((item) => body.append("files", item.file));
  const token = getToken();
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    xhr.open("POST", "/api/ai/nexa");
    if (token) xhr.setRequestHeader("Authorization", `Bearer ${token}`);
    xhr.upload.onprogress = (event) => {
      if (event.lengthComputable) onProgress(Math.round((event.loaded / event.total) * 100));
    };
    xhr.onload = () => {
      let data = {};
      try { data = xhr.responseText ? JSON.parse(xhr.responseText) : {}; } catch { data = { message: xhr.responseText }; }
      if (xhr.status < 200 || xhr.status >= 300) reject(rejectApi("/api/ai/nexa", xhr.status, data));
      else resolve(data);
    };
    xhr.onerror = () => reject(new Error("Cannot reach the API. The backend must be running on port 8080."));
    xhr.send(body);
  });
}
