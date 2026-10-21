import { useEffect, useState } from "react";
import { api } from "../../api.js";

export default function QualityPanel() {
  const [projects, setProjects] = useState([]);
  const [projectId, setProjectId] = useState("");
  const [mode, setMode] = useState("VALIDATION");
  const [report, setReport] = useState(null);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    api("/api/ai/projects").then((p) => {
      setProjects(p);
      if (p[0]) setProjectId(String(p[0].id));
    });
  }, []);

  useEffect(() => {
    if (!projectId) return;
    api(`/api/ai/projects/${projectId}/quality`)
      .then((rows) => setReport(rows[0] || null))
      .catch(() => setReport(null));
  }, [projectId]);

  async function run() {
    setError("");
    setBusy(true);
    try {
      setReport(await api(`/api/ai/projects/${projectId}/quality`, {
        method: "POST",
        body: JSON.stringify({ mode })
      }));
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <div>
      <h2>Project Quality Analyzer</h2>
      <div className="row">
        <select value={projectId} onChange={(e) => setProjectId(e.target.value)}>
          {projects.map((p) => <option key={p.id} value={p.id}>{p.owner}/{p.name}</option>)}
        </select>
        <select value={mode} onChange={(e) => setMode(e.target.value)}>
          <option value="SKILL_DEVELOPMENT">Skill Development</option>
          <option value="VALIDATION">Validation</option>
          <option value="CAREER_ADVANCEMENT">Career Advancement</option>
        </select>
        <button className="btn" onClick={run} disabled={!projectId || busy}>{busy ? "Analyzing…" : "Analyze"}</button>
      </div>
      {error && <p className="error">{error}</p>}
      {report && (
        <div className="card" style={{ marginTop: 16 }}>
          <p className="muted">{report.mode} · {report.createdAt ? new Date(report.createdAt).toLocaleString() : "latest saved report"}</p>
          <p>{report.evidenceSummary}</p>
          <div className="grid">
            <div><p className="muted">Architecture</p><p className="score">{report.architectureScore}</p><p>{report.architectureNotes}</p></div>
            <div><p className="muted">Maintainability</p><p className="score">{report.maintainabilityScore}</p><p>{report.maintainabilityNotes}</p></div>
            <div><p className="muted">Security</p><p className="score">{report.securityScore}</p>{(report.securityFindings || []).map((f) => <p key={f}>{f}</p>)}</div>
            <div><p className="muted">Testing</p><p className="score">{report.testingScore}</p><p>{report.testingNotes}</p></div>
            <div><p className="muted">Data design</p><p className="score">{report.dataDesignScore}</p><p>{report.dataDesignNotes}</p></div>
          </div>
          <pre className="md">{report.recommendations}</pre>
        </div>
      )}
    </div>
  );
}
