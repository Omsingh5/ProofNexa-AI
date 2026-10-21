import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { api } from "../api.js";
import { useAuth } from "../AuthContext.jsx";

export default function JobDetail() {
  const { id } = useParams();
  const { user } = useAuth();
  const [job, setJob] = useState(null);
  const [cover, setCover] = useState("");
  const [gap, setGap] = useState(null);
  const [msg, setMsg] = useState("");

  useEffect(() => {
    api(`/api/jobs/${id}`).then(setJob);
  }, [id]);

  if (!job) return <p>Loading…</p>;

  async function apply() {
    setMsg("");
    try {
      await api(`/api/applications/jobs/${id}`, { method: "POST", body: JSON.stringify({ coverLetter: cover }) });
      setMsg("Application submitted.");
    } catch (e) {
      setMsg(e.message);
    }
  }

  async function runGap() {
    setGap(await api(`/api/ai/jobs/${id}/gap`, { method: "POST" }));
  }

  return (
    <article className="card">
      <p className="kicker">{job.location} · {job.employmentType}</p>
      <h2>{job.title}</h2>
      <p className="muted">Posted by {job.recruiterName}</p>
      <p>{job.description}</p>
      <div>{(job.requiredSkills || []).map((s) => <span key={s} className="chip">{s}</span>)}</div>
      {user?.role === "CANDIDATE" && (
        <>
          <label>Cover letter</label>
          <textarea value={cover} onChange={(e) => setCover(e.target.value)} />
          <div className="row" style={{ marginTop: 12 }}>
            <button className="btn" onClick={apply}>Apply</button>
            <button className="btn ghost" onClick={runGap}>Skill-gap analysis</button>
          </div>
        </>
      )}
      {msg && <p className="banner">{msg}</p>}
      {gap && (
        <div className="banner">
          <p className="score">{gap.matchScore}%</p>
          <p>{gap.explanation}</p>
          <p>Missing: {(gap.missingSkills || []).join(", ") || "none"}</p>
        </div>
      )}
    </article>
  );
}
