import { useEffect, useState } from "react";
import { api } from "../../api.js";

export default function ProofPanel() {
  const [url, setUrl] = useState("https://github.com/spring-projects/spring-petclinic");
  const [projects, setProjects] = useState([]);
  const [evidence, setEvidence] = useState([]);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  async function load() {
    setProjects(await api("/api/ai/projects"));
  }
  useEffect(() => { load(); }, []);

  async function importRepo() {
    setError("");
    setBusy(true);
    try {
      await api("/api/ai/projects", { method: "POST", body: JSON.stringify({ repoUrl: url }) });
      await load();
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  }

  async function reanalyze(id) {
    setError("");
    setBusy(true);
    try {
      await api(`/api/ai/projects/${id}/reanalyze`, { method: "POST" });
      await load();
      setEvidence(await api(`/api/ai/projects/${id}/evidence`));
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  }

  async function showEvidence(id) {
    setEvidence(await api(`/api/ai/projects/${id}/evidence`));
  }

  return (
    <div>
      <h2>Skill Proof Engine</h2>
      <p className="muted">Public GitHub trees only. Proofora never clones privately or executes code.</p>
      <div className="row">
        <input value={url} onChange={(e) => setUrl(e.target.value)} />
        <button className="btn" onClick={importRepo} disabled={busy}>{busy ? "Reading repository…" : "Import & detect"}</button>
      </div>
      {error && <p className="error">{error}</p>}
      {projects.map((p) => (
        <article className="card" key={p.id} style={{ marginTop: 12 }}>
          <h3>{p.owner}/{p.name}</h3>
          <p className="muted">{p.description}</p>
          <p className="muted">Last analyzed {p.lastAnalyzedAt ? new Date(p.lastAnalyzedAt).toLocaleString() : "not yet"}</p>
          <div>{(p.detectedTechnologies || []).map((t) => <span className="chip" key={t}>{t}</span>)}</div>
          <div className="row">
            <button className="btn ghost small" onClick={() => showEvidence(p.id)}>Show code evidence</button>
            <button className="btn ghost small" onClick={() => reanalyze(p.id)} disabled={busy}>Re-analyze</button>
          </div>
        </article>
      ))}
      {evidence.length > 0 && (
        <table className="table">
          <thead><tr><th>Skill</th><th>File</th><th>Snippet</th></tr></thead>
          <tbody>
            {evidence.map((e, i) => (
              <tr key={i}><td>{e.skill}</td><td>{e.filePath}</td><td>{e.snippet}</td></tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
