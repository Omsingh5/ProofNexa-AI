import { useEffect, useState } from "react";
import { api } from "../../api.js";

export default function GrowthPanel() {
  const [jobs, setJobs] = useState([]);
  const [jobId, setJobId] = useState("");
  const [projects, setProjects] = useState([]);
  const [projectId, setProjectId] = useState("");
  const [roadmaps, setRoadmaps] = useState([]);
  const [challenges, setChallenges] = useState([]);
  const [skill, setSkill] = useState("JUnit");
  const [submission, setSubmission] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState("");

  async function load() {
    const page = await api("/api/jobs?size=20");
    setJobs(page.content);
    if (page.content[0]) setJobId(String(page.content[0].id));
    const p = await api("/api/ai/projects");
    setProjects(p);
    if (p[0]) setProjectId(String(p[0].id));
    setRoadmaps(await api("/api/ai/roadmaps"));
    setChallenges(await api("/api/ai/challenges"));
  }
  useEffect(() => { load(); }, []);

  async function roadmap() {
    setError("");
    setBusy("roadmap");
    try {
      await api("/api/ai/roadmaps", { method: "POST", body: JSON.stringify({ jobId: Number(jobId), projectId: projectId ? Number(projectId) : null }) });
      await load();
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy("");
    }
  }

  async function challenge() {
    setError("");
    setBusy("challenge");
    try {
      await api("/api/ai/challenges", { method: "POST", body: JSON.stringify({ skill }) });
      await load();
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy("");
    }
  }

  async function submit(id) {
    setError("");
    try {
      await api(`/api/ai/challenges/${id}/submit`, { method: "POST", body: JSON.stringify({ submission }) });
      setSubmission("");
      await load();
    } catch (err) {
      setError(err.message);
    }
  }

  return (
    <div>
      <h2>Career growth</h2>
      <div className="row">
        <select value={jobId} onChange={(e) => setJobId(e.target.value)}>
          {jobs.map((j) => <option key={j.id} value={j.id}>{j.title}</option>)}
        </select>
        <select value={projectId} onChange={(e) => setProjectId(e.target.value)}>
          <option value="">No project</option>
          {projects.map((p) => <option key={p.id} value={p.id}>{p.name}</option>)}
        </select>
        <button className="btn" onClick={roadmap} disabled={busy === "roadmap"}>{busy === "roadmap" ? "Generating…" : "Generate roadmap"}</button>
      </div>
      {error && <p className="error">{error}</p>}
      {roadmaps.map((r) => (
        <article className="card" key={r.id} style={{ marginTop: 12 }}>
          <h3>{r.title}</h3>
          <pre className="md">{r.contentMarkdown}</pre>
        </article>
      ))}
      <div className="row" style={{ marginTop: 24 }}>
        <input value={skill} onChange={(e) => setSkill(e.target.value)} style={{ maxWidth: 200 }} />
        <button className="btn ghost" onClick={challenge}>New challenge</button>
      </div>
      {challenges.map((c) => (
        <article className="card" key={c.id} style={{ marginTop: 12 }}>
          <p className="kicker">{c.status} · {c.skill}</p>
          <h3>{c.title}</h3>
          <pre className="md">{c.prompt}</pre>
          {c.status !== "COMPLETED" && (
            <>
              <textarea value={submission} onChange={(e) => setSubmission(e.target.value)} placeholder="Paste design/code. It will not be executed." />
              <button className="btn small" onClick={() => submit(c.id)}>Submit for AI review</button>
            </>
          )}
          {c.feedback && <div className="banner">{c.feedback}</div>}
        </article>
      ))}
    </div>
  );
}
