import { useEffect, useState } from "react";
import { api } from "../../api.js";

export default function AssistantPanel() {
  const [history, setHistory] = useState([]);
  const [message, setMessage] = useState("How does skill proof work?");

  async function load() {
    setHistory(await api("/api/ai/assistant"));
  }
  useEffect(() => { load(); }, []);

  async function send() {
    await api("/api/ai/assistant", { method: "POST", body: JSON.stringify({ message }) });
    setMessage("");
    await load();
  }

  return (
    <div>
      <h2>Authorized career assistant</h2>
      <p className="muted">Answers only from Proofora knowledge chunks and listed jobs (RAG-style retrieval).</p>
      {history.map((m, i) => (
        <div className="card" key={i} style={{ marginBottom: 8 }}>
          <p className="kicker">{m.role}{m.sources ? ` · ${m.sources}` : ""}</p>
          <p>{m.content}</p>
        </div>
      ))}
      <textarea value={message} onChange={(e) => setMessage(e.target.value)} />
      <button className="btn" onClick={send}>Ask</button>
    </div>
  );
}
