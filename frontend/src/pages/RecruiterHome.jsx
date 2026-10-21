import { useEffect, useState } from "react";
import { api } from "../api.js";
import AccountSecurity from "../components/AccountSecurity.jsx";
import RecruiterProfile from "./recruiter/RecruiterProfile.jsx";

const emptyJob = {
  title: "",
  description: "",
  location: "Remote",
  employmentType: "FULL_TIME",
  experienceLevel: "Mid",
  requiredSkills: "Java, Spring Boot",
  status: "OPEN"
};

function toForm(job) {
  return {
    title: job.title || "",
    description: job.description || "",
    location: job.location || "Remote",
    employmentType: job.employmentType || "FULL_TIME",
    experienceLevel: job.experienceLevel || "Mid",
    requiredSkills: (job.requiredSkills || []).join(", "),
    status: job.status || "OPEN"
  };
}

export default function RecruiterHome() {
  const [jobs, setJobs] = useState([]);
  const [form, setForm] = useState(emptyJob);
  const [editingId, setEditingId] = useState(null);
  const [viewing, setViewing] = useState(null);
  const [apps, setApps] = useState([]);
  const [selected, setSelected] = useState(null);
  const [analytics, setAnalytics] = useState(null);
  const [jdNotes, setJdNotes] = useState("Need someone who can own JWT APIs and MySQL indexes.");
  const [jd, setJd] = useState("");
  const [interview, setInterview] = useState("");
  const [msg, setMsg] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const [profileOpen, setProfileOpen] = useState(false);

  async function load() {
    setJobs(await api("/api/jobs/mine"));
    setAnalytics(await api("/api/ai/analytics"));
  }
  useEffect(() => { load(); }, []);

  function payload(status) {
    return {
      ...form,
      status,
      requiredSkills: form.requiredSkills.split(",").map((s) => s.trim()).filter(Boolean)
    };
  }

  async function save(e, status) {
    e.preventDefault();
    setError("");
    setBusy(true);
    try {
      const body = JSON.stringify(payload(status));
      if (editingId) {
        await api(`/api/jobs/${editingId}`, { method: "PUT", body });
        setMsg(status === "OPEN" ? "Role republished." : "Draft saved. It is hidden from candidates until you republish.");
      } else {
        await api("/api/jobs", { method: "POST", body });
        setMsg(status === "OPEN" ? "Role published." : "Draft saved.");
      }
      setEditingId(null);
      setForm(emptyJob);
      setViewing(null);
      await load();
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  function edit(job) {
    setEditingId(job.id);
    setForm(toForm(job));
    setViewing(job);
    setMsg("");
    setError("");
  }

  function cancelEdit() {
    setEditingId(null);
    setForm(emptyJob);
    setError("");
  }

  async function openApps(job) {
    setSelected(job);
    setApps(await api(`/api/applications/jobs/${job.id}`));
  }

  async function status(id, next) {
    await api(`/api/applications/${id}/status`, {
      method: "PATCH",
      body: JSON.stringify({ status: next, feedback: `Moved to ${next}` })
    });
    if (selected) await openApps(selected);
  }

  async function writeJd() {
    const res = await api("/api/ai/jd", { method: "POST", body: JSON.stringify({ title: form.title || "Engineer", notes: jdNotes }) });
    setJd(res.markdown);
  }

  async function pack() {
    if (!selected) return;
    const res = await api(`/api/ai/jobs/${selected.id}/interview`, { method: "POST" });
    setInterview(res.questionsMarkdown);
  }

  const published = jobs.filter((j) => j.status === "OPEN");
  const drafts = jobs.filter((j) => j.status !== "OPEN");

  if (profileOpen) {
    return <RecruiterProfile onBack={() => setProfileOpen(false)} />;
  }

  return (
    <div className="desk">
      <section>
        <div className="row">
          <h2>Published roles</h2>
          <button className="btn ghost small" type="button" onClick={() => setProfileOpen(true)}>Company profile</button>
        </div>
        {analytics && <div className="banner">{analytics.narrative}</div>}
        {msg && <p className="banner">{msg}</p>}
        {published.length === 0 && <p className="muted">No published roles yet.</p>}
        {published.map((j) => (
          <article className="card" key={j.id}>
            <p className="kicker">{j.status} · {j.location} · {j.employmentType}</p>
            <h3>{j.title}</h3>
            <p className="muted">{(j.requiredSkills || []).join(", ")}</p>
            <div className="row">
              <button className="btn small" type="button" onClick={() => setViewing(viewing?.id === j.id ? null : j)}>
                {viewing?.id === j.id ? "Viewing" : "View"}
              </button>
              <button className="btn ghost small" type="button" onClick={() => edit(j)}>Edit</button>
              <button className="btn ghost small" type="button" onClick={() => openApps(j)}>Applications</button>
            </div>
            {viewing?.id === j.id && (
              <div className="expand">
                <p>{j.description}</p>
                <p className="muted">{j.location} · {j.employmentType} · {j.experienceLevel}</p>
                <div>{(j.requiredSkills || []).map((s) => <span className="chip" key={s}>{s}</span>)}</div>
                <button className="btn ghost small" type="button" onClick={() => setViewing(null)}>Minimize</button>
              </div>
            )}
          </article>
        ))}
        {drafts.length > 0 && <h3>Drafts</h3>}
        {drafts.map((j) => (
          <article className="card" key={j.id}>
            <p className="kicker">{j.status}</p>
            <h3>{j.title}</h3>
            <div className="row">
              <button className="btn small" type="button" onClick={() => edit(j)}>Edit and republish</button>
            </div>
          </article>
        ))}
      </section>
      <section>
        <form className="card" onSubmit={(e) => save(e, editingId ? "OPEN" : "OPEN")}>
          <h3>{editingId ? "Edit published role" : "Post a role"}</h3>
          <label>Title</label>
          <input value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} required />
          <label>Description</label>
          <textarea value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} required />
          <label>Location</label>
          <input value={form.location} onChange={(e) => setForm({ ...form, location: e.target.value })} />
          <label>Employment type</label>
          <select value={form.employmentType} onChange={(e) => setForm({ ...form, employmentType: e.target.value })}>
            <option value="FULL_TIME">Full time</option>
            <option value="PART_TIME">Part time</option>
            <option value="CONTRACT">Contract</option>
            <option value="INTERNSHIP">Internship</option>
          </select>
          <label>Experience level</label>
          <input value={form.experienceLevel} onChange={(e) => setForm({ ...form, experienceLevel: e.target.value })} />
          <label>Skills (comma)</label>
          <input value={form.requiredSkills} onChange={(e) => setForm({ ...form, requiredSkills: e.target.value })} />
          {error && <p className="error">{error}</p>}
          <div className="row" style={{ marginTop: 12 }}>
            {editingId ? (
              <>
                <button className="btn" type="submit" disabled={busy}>{busy ? "Saving…" : "Republish"}</button>
                <button className="btn ghost" type="button" disabled={busy} onClick={(e) => save(e, "DRAFT")}>Save as draft</button>
                <button className="btn ghost" type="button" onClick={cancelEdit}>Cancel</button>
              </>
            ) : (
              <>
                <button className="btn" type="submit" disabled={busy}>{busy ? "Saving…" : "Publish"}</button>
                <button className="btn ghost" type="button" disabled={busy} onClick={(e) => save(e, "DRAFT")}>Save draft</button>
              </>
            )}
          </div>
        </form>
        <div className="card" style={{ marginTop: 12 }}>
          <h3>JD writing assistant</h3>
          <textarea value={jdNotes} onChange={(e) => setJdNotes(e.target.value)} />
          <button className="btn ghost" type="button" onClick={writeJd}>Draft JD</button>
          {jd && <pre className="md">{jd}</pre>}
        </div>
        {selected && (
          <div className="card" style={{ marginTop: 12 }}>
            <h3>{selected.title} pipeline</h3>
            <button className="btn small" onClick={pack}>Interview pack</button>
            <div className="table-wrap">
              <table className="table">
                <thead><tr><th>Candidate</th><th>Status</th><th></th></tr></thead>
                <tbody>
                  {apps.map((a) => (
                    <tr key={a.id}>
                      <td>{a.candidateName}</td>
                      <td>{a.status}</td>
                      <td>
                        <button className="btn ghost small" onClick={() => status(a.id, "REVIEWING")}>Review</button>
                        <button className="btn ghost small" onClick={() => status(a.id, "INTERVIEW")}>Interview</button>
                        <button className="btn ghost small" onClick={() => status(a.id, "OFFER")}>Offer</button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            {interview && <pre className="md">{interview}</pre>}
          </div>
        )}
        <AccountSecurity />
      </section>
    </div>
  );
}
