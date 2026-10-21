import { useEffect, useState } from "react";
import { api } from "../../api.js";

export default function ApplicationsPanel() {
  const [apps, setApps] = useState([]);
  const [timeline, setTimeline] = useState([]);
  const [selected, setSelected] = useState(null);

  useEffect(() => { api("/api/applications/mine").then(setApps); }, []);

  async function open(id) {
    setSelected(id);
    setTimeline(await api(`/api/applications/${id}/timeline`));
  }

  return (
    <div>
      <h2>Applications</h2>
      <table className="table">
        <thead><tr><th>Role</th><th>Status</th><th>Feedback</th></tr></thead>
        <tbody>
          {apps.map((a) => (
            <tr key={a.id} onClick={() => open(a.id)} style={{ cursor: "pointer" }}>
              <td>{a.jobTitle}</td>
              <td>{a.status}</td>
              <td>{a.recruiterFeedback || "—"}</td>
            </tr>
          ))}
        </tbody>
      </table>
      {selected && (
        <div className="card" style={{ marginTop: 16 }}>
          <h3>Progress timeline</h3>
          {timeline.map((e, i) => (
            <p key={i}><strong>{e.status}</strong> · {e.note}</p>
          ))}
        </div>
      )}
    </div>
  );
}
