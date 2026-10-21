import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../api.js";

export default function Jobs() {
  const [q, setQ] = useState("");
  const [location, setLocation] = useState("");
  const [page, setPage] = useState(0);
  const [data, setData] = useState({ content: [], totalPages: 0 });

  async function load(p = 0) {
    const params = new URLSearchParams({ page: p, size: 6 });
    if (q) params.set("q", q);
    if (location) params.set("location", location);
    setData(await api(`/api/jobs?${params}`));
    setPage(p);
  }

  useEffect(() => { load(0); }, []);

  return (
    <div>
      <h2 className="serif">Open roles</h2>
      <div className="row" style={{ margin: "16px 0" }}>
        <input placeholder="Search title or stack" value={q} onChange={(e) => setQ(e.target.value)} style={{ maxWidth: 280 }} />
        <input placeholder="Location" value={location} onChange={(e) => setLocation(e.target.value)} style={{ maxWidth: 180 }} />
        <button className="btn" onClick={() => load(0)}>Filter</button>
      </div>
      <div className="grid">
        {data.content.map((job) => (
          <Link key={job.id} to={`/jobs/${job.id}`} style={{ textDecoration: "none" }}>
            <article className="card">
              <p className="kicker">{job.employmentType} · {job.location}</p>
              <h3>{job.title}</h3>
              <p className="muted">{job.recruiterName}</p>
              <div>{(job.requiredSkills || []).slice(0, 5).map((s) => <span key={s} className="chip">{s}</span>)}</div>
            </article>
          </Link>
        ))}
      </div>
      <div className="row" style={{ marginTop: 20 }}>
        <button className="btn ghost" disabled={page <= 0} onClick={() => load(page - 1)}>Previous</button>
        <button className="btn ghost" disabled={page + 1 >= data.totalPages} onClick={() => load(page + 1)}>Next</button>
      </div>
    </div>
  );
}
