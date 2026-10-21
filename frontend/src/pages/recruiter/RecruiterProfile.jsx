import { useEffect, useState } from "react";
import { api } from "../../api.js";
import AccountSecurity from "../../components/AccountSecurity.jsx";
import usePrivateImage from "../../components/usePrivateImage.js";

export default function RecruiterProfile({ onBack }) {
  const [profile, setProfile] = useState(null);
  const [editing, setEditing] = useState(false);
  const [msg, setMsg] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState("");
  const photo = usePrivateImage("/api/profile/photo", !!profile?.hasPhoto);
  const logo = usePrivateImage("/api/profile/logo", !!profile?.hasLogo);

  async function refresh() {
    setProfile(await api("/api/profile"));
  }
  useEffect(() => { refresh().catch((err) => setError(err.message)); }, []);
  if (!profile) return <p>Loading…</p>;

  function set(field, value) {
    setProfile({ ...profile, [field]: value });
  }

  async function save(e) {
    e.preventDefault();
    setError("");
    setBusy("save");
    try {
      setProfile(await api("/api/profile", {
        method: "PUT",
        body: JSON.stringify({
          fullName: profile.user.fullName,
          headline: profile.headline || "",
          location: profile.location || "",
          phone: profile.phone || "",
          designation: profile.designation || "",
          department: profile.department || "",
          yearsExperience: profile.yearsExperience === "" || profile.yearsExperience == null ? null : Number(profile.yearsExperience),
          hiringSpecialization: profile.hiringSpecialization || "",
          about: profile.about || "",
          companyName: profile.companyName || "",
          companyWebsite: profile.companyWebsite || "",
          industry: profile.industry || "",
          companySize: profile.companySize || "",
          headquarters: profile.headquarters || "",
          companyDescription: profile.companyDescription || "",
          linkedinUrl: profile.linkedinUrl || "",
          jobCategories: profile.jobCategories || "",
          preferredTechnologies: profile.preferredTechnologies || "",
          experienceLevels: profile.experienceLevels || "",
          hiringLocations: profile.hiringLocations || ""
        })
      }));
      setEditing(false);
      setMsg("Profile saved.");
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy("");
    }
  }

  async function upload(path, file) {
    if (!file) return;
    setError("");
    try {
      const body = new FormData();
      body.append("file", file);
      await api(path, { method: "POST", body });
      await refresh();
      setMsg("Image saved.");
    } catch (err) {
      setError(err.message);
    }
  }

  const open = (profile.postings || []).filter((job) => job.status === "OPEN");
  const previous = (profile.postings || []).filter((job) => job.status !== "OPEN");

  return (
    <div>
      <button className="btn ghost small" type="button" onClick={onBack}>Back to hiring desk</button>
      <div className="card profile-head" style={{ marginTop: 12 }}>
        {photo ? <img className="avatar" src={photo} alt="" /> : <div className="avatar" />}
        {logo ? <img className="logo-mark" src={logo} alt="" /> : <div className="logo-mark" />}
        <div>
          <p className="kicker">Recruiter profile</p>
          <h2 style={{ margin: "4px 0" }}>{profile.user.fullName}</h2>
          <p className="muted">{profile.headline || profile.designation || "Add a headline"} · {profile.companyName || "Company not set"}</p>
          <div className="meter"><span style={{ width: `${profile.completion || 0}%` }} /></div>
          <p className="muted">{profile.completion || 0}% complete</p>
        </div>
      </div>
      {msg && <p className="banner">{msg}</p>}
      {error && <p className="error">{error}</p>}
      <div className="card" style={{ marginTop: 12 }}>
        <h3>Recruitment overview</h3>
        <p>{profile.stats?.jobsPosted || 0} jobs posted · {profile.stats?.openJobs || 0} open · {profile.stats?.applicationsReceived || 0} applications received</p>
      </div>
      <div className="row" style={{ margin: "12px 0" }}>
        <button className="btn" type="button" onClick={() => setEditing((openEditor) => !openEditor)}>{editing ? "Close editor" : "Edit profile"}</button>
      </div>
      {editing ? (
        <form className="card" onSubmit={save}>
          <h3>Personal information</h3>
          <label>Profile photo</label>
          <input type="file" accept=".jpg,.jpeg,.png,.webp" onChange={(e) => upload("/api/profile/photo", e.target.files[0])} />
          <label>Full name</label>
          <input value={profile.user.fullName || ""} onChange={(e) => setProfile({ ...profile, user: { ...profile.user, fullName: e.target.value } })} required />
          <label>Headline</label>
          <input value={profile.headline || ""} onChange={(e) => set("headline", e.target.value)} />
          <div className="two">
            <div><label>Location</label><input value={profile.location || ""} onChange={(e) => set("location", e.target.value)} /></div>
            <div><label>Phone</label><input value={profile.phone || ""} onChange={(e) => set("phone", e.target.value)} /></div>
          </div>
          <p className="muted">Email: {profile.user.email}</p>
          <h3>Professional details</h3>
          <div className="two">
            <div><label>Designation</label><input value={profile.designation || ""} onChange={(e) => set("designation", e.target.value)} /></div>
            <div><label>Department</label><input value={profile.department || ""} onChange={(e) => set("department", e.target.value)} /></div>
          </div>
          <div className="two">
            <div><label>Years of experience</label><input type="number" min="0" max="60" value={profile.yearsExperience ?? ""} onChange={(e) => set("yearsExperience", e.target.value)} /></div>
            <div><label>Hiring specialization</label><input value={profile.hiringSpecialization || ""} onChange={(e) => set("hiringSpecialization", e.target.value)} /></div>
          </div>
          <label>About me</label>
          <textarea value={profile.about || ""} onChange={(e) => set("about", e.target.value)} />
          <h3>Company</h3>
          <label>Logo</label>
          <input type="file" accept=".jpg,.jpeg,.png,.webp" onChange={(e) => upload("/api/profile/logo", e.target.files[0])} />
          <label>Company name</label>
          <input value={profile.companyName || ""} onChange={(e) => set("companyName", e.target.value)} />
          <div className="two">
            <div><label>Industry</label><input value={profile.industry || ""} onChange={(e) => set("industry", e.target.value)} /></div>
            <div><label>Company size</label><input value={profile.companySize || ""} onChange={(e) => set("companySize", e.target.value)} placeholder="1-50, 51-200" /></div>
          </div>
          <label>Headquarters</label>
          <input value={profile.headquarters || ""} onChange={(e) => set("headquarters", e.target.value)} />
          <label>Website</label>
          <input value={profile.companyWebsite || ""} onChange={(e) => set("companyWebsite", e.target.value)} placeholder="https://" />
          <label>Description</label>
          <textarea value={profile.companyDescription || ""} onChange={(e) => set("companyDescription", e.target.value)} />
          <label>LinkedIn</label>
          <input value={profile.linkedinUrl || ""} onChange={(e) => set("linkedinUrl", e.target.value)} placeholder="https://" />
          <h3>Hiring preferences</h3>
          <label>Job categories</label>
          <input value={profile.jobCategories || ""} onChange={(e) => set("jobCategories", e.target.value)} />
          <label>Preferred technologies</label>
          <input value={profile.preferredTechnologies || ""} onChange={(e) => set("preferredTechnologies", e.target.value)} />
          <label>Experience levels</label>
          <input value={profile.experienceLevels || ""} onChange={(e) => set("experienceLevels", e.target.value)} />
          <label>Hiring locations</label>
          <input value={profile.hiringLocations || ""} onChange={(e) => set("hiringLocations", e.target.value)} />
          <button className="btn" type="submit" disabled={busy === "save"}>{busy === "save" ? "Saving…" : "Save profile"}</button>
        </form>
      ) : (
        <div className="profile-grid">
          <section className="card">
            <h3>About</h3>
            <p>{profile.about || "No introduction yet."}</p>
            <p className="muted">{[profile.designation, profile.department, profile.location, profile.phone, profile.user.email].filter(Boolean).join(" · ")}</p>
            <p className="muted">{profile.hiringSpecialization}</p>
          </section>
          <section className="card">
            <h3>Company</h3>
            <p><strong>{profile.companyName || "Not set"}</strong></p>
            <p className="muted">{[profile.industry, profile.companySize, profile.headquarters].filter(Boolean).join(" · ")}</p>
            <p>{profile.companyDescription}</p>
            <p>{profile.companyWebsite || "No website"}</p>
            <p>{profile.linkedinUrl || "No LinkedIn link"}</p>
          </section>
          <section className="card">
            <h3>Hiring preferences</h3>
            <p>{profile.jobCategories || "No categories"}</p>
            <p>{profile.preferredTechnologies || "No technologies"}</p>
            <p className="muted">{[profile.experienceLevels, profile.hiringLocations].filter(Boolean).join(" · ")}</p>
          </section>
        </div>
      )}
      <h3>Active postings</h3>
      {open.length === 0 && <p className="muted">No open roles.</p>}
      {open.map((job) => (
        <article className="card" key={job.id}><h3>{job.title}</h3><p className="muted">{job.status} · {job.location} · {job.applications} applications</p></article>
      ))}
      {previous.length > 0 && <h3>Previous postings</h3>}
      {previous.map((job) => (
        <article className="card" key={job.id}><h3>{job.title}</h3><p className="muted">{job.status} · {job.location} · {job.applications} applications</p></article>
      ))}
      <AccountSecurity />
    </div>
  );
}
