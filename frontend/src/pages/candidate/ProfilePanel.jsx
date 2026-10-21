import { useEffect, useState } from "react";
import { api } from "../../api.js";
import AccountSecurity from "../../components/AccountSecurity.jsx";
import usePrivateImage, { downloadAuthed } from "../../components/usePrivateImage.js";

const blankEducation = { degree: "", institution: "", specialization: "", graduationYear: "", score: "" };
const blankExperience = { company: "", position: "", employmentType: "Full-time", duration: "", responsibilities: "" };
const blankSkill = { category: "", skills: "" };
const blankProject = { name: "", description: "", technologies: "", repositoryUrl: "", demoUrl: "" };
const blankCertificate = { name: "", issuer: "", date: "", credentialUrl: "" };
const blankAchievement = { title: "", detail: "" };

function lines(items, render) {
  if (!items?.length) return <p className="muted">Nothing saved yet.</p>;
  return items.map((item, index) => <div className="entry" key={index}>{render(item)}</div>);
}

export default function ProfilePanel() {
  const [profile, setProfile] = useState(null);
  const [editing, setEditing] = useState(false);
  const [recs, setRecs] = useState([]);
  const [proof, setProof] = useState([]);
  const [extraction, setExtraction] = useState(null);
  const [skillsText, setSkillsText] = useState("");
  const [msg, setMsg] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState("");
  const photo = usePrivateImage("/api/profile/photo", !!profile?.hasPhoto);

  async function refresh() {
    const me = await api("/api/profile");
    setProfile({
      ...me,
      education: me.education || [],
      experience: me.experience || [],
      skillGroups: (me.skillGroups || []).map((group) => ({ category: group.category, skills: (group.skills || []).join(", ") })),
      projects: me.projects || [],
      certifications: me.certifications || [],
      achievements: me.achievements || []
    });
    setExtraction(await api("/api/ai/resume"));
    setRecs(await api("/api/ai/recommendations"));
    setProof(await api("/api/ai/projects"));
  }

  useEffect(() => { refresh().catch((err) => setError(err.message)); }, []);
  useEffect(() => {
    if (!extraction) return;
    const skills = extraction.userCorrected ? extraction.correctedSkills : extraction.extractedSkills;
    setSkillsText((skills || []).join(", "));
  }, [extraction]);

  if (!profile) return <p>Loading…</p>;

  function set(field, value) {
    setProfile({ ...profile, [field]: value });
  }
  function updateList(field, index, patch) {
    const next = profile[field].map((row, i) => (i === index ? { ...row, ...patch } : row));
    set(field, next);
  }

  function payload() {
    return {
      fullName: profile.user.fullName,
      headline: profile.headline || "",
      summary: profile.summary || "",
      careerObjective: profile.careerObjective || "",
      location: profile.location || "",
      phone: profile.phone || "",
      yearsExperience: profile.yearsExperience === "" || profile.yearsExperience == null ? null : Number(profile.yearsExperience),
      githubUsername: profile.githubUsername || "",
      linkedinUrl: profile.linkedinUrl || "",
      portfolioUrl: profile.portfolioUrl || "",
      websiteUrl: profile.websiteUrl || "",
      preferredRoles: profile.preferredRoles || "",
      preferredLocations: profile.preferredLocations || "",
      preferredEmploymentType: profile.preferredEmploymentType || "",
      workMode: profile.workMode || "",
      availability: profile.availability || "",
      education: profile.education.map((row) => ({
        ...row,
        graduationYear: row.graduationYear === "" || row.graduationYear == null ? null : Number(row.graduationYear)
      })),
      experience: profile.experience,
      skillGroups: profile.skillGroups.map((row) => ({
        category: row.category,
        skills: String(row.skills || "").split(",").map((skill) => skill.trim()).filter(Boolean)
      })),
      projects: profile.projects,
      certifications: profile.certifications,
      achievements: profile.achievements,
      aiConsent: profile.user.aiConsent
    };
  }

  async function save(e) {
    e.preventDefault();
    setError("");
    setBusy("save");
    try {
      await api("/api/profile", { method: "PUT", body: JSON.stringify(payload()) });
      await refresh();
      setEditing(false);
      setMsg("Profile saved.");
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy("");
    }
  }

  async function upload(kind, file) {
    if (!file) return;
    setError("");
    setBusy(kind);
    try {
      const body = new FormData();
      body.append("file", file);
      await api(kind === "photo" ? "/api/profile/photo" : "/api/profile/resume", { method: "POST", body });
      setMsg(kind === "photo" ? "Photo saved." : "Resume stored privately.");
      await refresh();
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy("");
    }
  }

  async function analyze() {
    setError("");
    setBusy("extract");
    try {
      setExtraction(await api("/api/ai/resume/analyze", { method: "POST" }));
      setRecs(await api("/api/ai/recommendations"));
      setMsg("Skills extracted from the resume or profile text.");
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy("");
    }
  }

  async function correct() {
    setError("");
    try {
      const skills = skillsText.split(",").map((s) => s.trim()).filter(Boolean);
      setExtraction(await api("/api/ai/resume/skills", { method: "PUT", body: JSON.stringify({ skills }) }));
      setRecs(await api("/api/ai/recommendations"));
      setMsg("Corrections saved.");
    } catch (err) {
      setError(err.message);
    }
  }

  const verified = extraction ? (extraction.userCorrected ? extraction.correctedSkills : extraction.extractedSkills) : [];

  return (
    <div>
      <div className="card profile-head">
        {photo ? <img className="avatar" src={photo} alt="" /> : <div className="avatar" />}
        <div>
          <p className="kicker">Candidate profile</p>
          <h2 style={{ margin: "4px 0" }}>{profile.user.fullName}</h2>
          <p className="muted">{profile.headline || "Add a professional headline"} · {profile.location || "Location not set"}</p>
          <div className="meter" aria-label="Profile completion"><span style={{ width: `${profile.completion || 0}%` }} /></div>
          <p className="muted">{profile.completion || 0}% complete</p>
        </div>
      </div>
      {msg && <p className="banner">{msg}</p>}
      {error && <p className="error">{error}</p>}
      <div className="row" style={{ margin: "12px 0" }}>
        <button className="btn" type="button" onClick={() => setEditing((open) => !open)}>{editing ? "Close editor" : "Edit profile"}</button>
      </div>

      {editing ? (
        <form onSubmit={save}>
          <section className="card">
            <h3>Personal information</h3>
            <label>Profile photo</label>
            <input type="file" accept=".jpg,.jpeg,.png,.webp" onChange={(e) => upload("photo", e.target.files[0])} />
            <label>Full name</label>
            <input value={profile.user.fullName || ""} onChange={(e) => setProfile({ ...profile, user: { ...profile.user, fullName: e.target.value } })} required />
            <label>Headline</label>
            <input value={profile.headline || ""} onChange={(e) => set("headline", e.target.value)} />
            <div className="two">
              <div><label>Location</label><input value={profile.location || ""} onChange={(e) => set("location", e.target.value)} /></div>
              <div><label>Phone</label><input value={profile.phone || ""} onChange={(e) => set("phone", e.target.value)} /></div>
            </div>
            <p className="muted">Email: {profile.user.email}</p>
          </section>
          <section className="card" style={{ marginTop: 12 }}>
            <h3>About me</h3>
            <label>Professional summary</label>
            <textarea value={profile.summary || ""} onChange={(e) => set("summary", e.target.value)} />
            <label>Career objective</label>
            <textarea value={profile.careerObjective || ""} onChange={(e) => set("careerObjective", e.target.value)} />
          </section>
          <Repeatable title="Education" rows={profile.education} blank={blankEducation} onAdd={() => set("education", [...profile.education, blankEducation])} onRemove={(i) => set("education", profile.education.filter((_, n) => n !== i))}>
            {(row, i) => (
              <>
                <div className="two">
                  <input placeholder="Degree" value={row.degree} onChange={(e) => updateList("education", i, { degree: e.target.value })} />
                  <input placeholder="Institution" value={row.institution} onChange={(e) => updateList("education", i, { institution: e.target.value })} />
                </div>
                <div className="two">
                  <input placeholder="Specialization" value={row.specialization || ""} onChange={(e) => updateList("education", i, { specialization: e.target.value })} />
                  <input placeholder="Year" value={row.graduationYear || ""} onChange={(e) => updateList("education", i, { graduationYear: e.target.value })} />
                </div>
                <input placeholder="CGPA or percentage" value={row.score || ""} onChange={(e) => updateList("education", i, { score: e.target.value })} />
              </>
            )}
          </Repeatable>
          <Repeatable title="Work experience" rows={profile.experience} blank={blankExperience} onAdd={() => set("experience", [...profile.experience, blankExperience])} onRemove={(i) => set("experience", profile.experience.filter((_, n) => n !== i))}>
            {(row, i) => (
              <>
                <div className="two">
                  <input placeholder="Company" value={row.company} onChange={(e) => updateList("experience", i, { company: e.target.value })} />
                  <input placeholder="Position" value={row.position} onChange={(e) => updateList("experience", i, { position: e.target.value })} />
                </div>
                <div className="two">
                  <input placeholder="Employment type" value={row.employmentType || ""} onChange={(e) => updateList("experience", i, { employmentType: e.target.value })} />
                  <input placeholder="Duration" value={row.duration || ""} onChange={(e) => updateList("experience", i, { duration: e.target.value })} />
                </div>
                <textarea placeholder="Responsibilities" value={row.responsibilities || ""} onChange={(e) => updateList("experience", i, { responsibilities: e.target.value })} />
              </>
            )}
          </Repeatable>
          <Repeatable title="Technical skills" rows={profile.skillGroups} blank={blankSkill} onAdd={() => set("skillGroups", [...profile.skillGroups, blankSkill])} onRemove={(i) => set("skillGroups", profile.skillGroups.filter((_, n) => n !== i))}>
            {(row, i) => (
              <div className="two">
                <input placeholder="Category" value={row.category} onChange={(e) => updateList("skillGroups", i, { category: e.target.value })} />
                <input placeholder="Skills, comma separated" value={row.skills} onChange={(e) => updateList("skillGroups", i, { skills: e.target.value })} />
              </div>
            )}
          </Repeatable>
          <Repeatable title="Projects" rows={profile.projects} blank={blankProject} onAdd={() => set("projects", [...profile.projects, blankProject])} onRemove={(i) => set("projects", profile.projects.filter((_, n) => n !== i))}>
            {(row, i) => (
              <>
                <input placeholder="Project name" value={row.name} onChange={(e) => updateList("projects", i, { name: e.target.value })} />
                <textarea placeholder="Description" value={row.description || ""} onChange={(e) => updateList("projects", i, { description: e.target.value })} />
                <input placeholder="Technologies" value={row.technologies || ""} onChange={(e) => updateList("projects", i, { technologies: e.target.value })} />
                <input placeholder="https://github.com/..." value={row.repositoryUrl || ""} onChange={(e) => updateList("projects", i, { repositoryUrl: e.target.value })} />
                <input placeholder="https://demo.example.com" value={row.demoUrl || ""} onChange={(e) => updateList("projects", i, { demoUrl: e.target.value })} />
              </>
            )}
          </Repeatable>
          <Repeatable title="Certifications" rows={profile.certifications} blank={blankCertificate} onAdd={() => set("certifications", [...profile.certifications, blankCertificate])} onRemove={(i) => set("certifications", profile.certifications.filter((_, n) => n !== i))}>
            {(row, i) => (
              <>
                <div className="two">
                  <input placeholder="Certificate" value={row.name} onChange={(e) => updateList("certifications", i, { name: e.target.value })} />
                  <input placeholder="Issuing organization" value={row.issuer || ""} onChange={(e) => updateList("certifications", i, { issuer: e.target.value })} />
                </div>
                <div className="two">
                  <input placeholder="Date" value={row.date || ""} onChange={(e) => updateList("certifications", i, { date: e.target.value })} />
                  <input placeholder="https://credential" value={row.credentialUrl || ""} onChange={(e) => updateList("certifications", i, { credentialUrl: e.target.value })} />
                </div>
              </>
            )}
          </Repeatable>
          <Repeatable title="Achievements" rows={profile.achievements} blank={blankAchievement} onAdd={() => set("achievements", [...profile.achievements, blankAchievement])} onRemove={(i) => set("achievements", profile.achievements.filter((_, n) => n !== i))}>
            {(row, i) => (
              <>
                <input placeholder="Title" value={row.title} onChange={(e) => updateList("achievements", i, { title: e.target.value })} />
                <textarea placeholder="Detail" value={row.detail || ""} onChange={(e) => updateList("achievements", i, { detail: e.target.value })} />
              </>
            )}
          </Repeatable>
          <section className="card" style={{ marginTop: 12 }}>
            <h3>Professional links</h3>
            <label>GitHub username</label>
            <input value={profile.githubUsername || ""} onChange={(e) => set("githubUsername", e.target.value)} />
            <label>LinkedIn</label>
            <input value={profile.linkedinUrl || ""} onChange={(e) => set("linkedinUrl", e.target.value)} placeholder="https://" />
            <label>Portfolio</label>
            <input value={profile.portfolioUrl || ""} onChange={(e) => set("portfolioUrl", e.target.value)} placeholder="https://" />
            <label>Personal website</label>
            <input value={profile.websiteUrl || ""} onChange={(e) => set("websiteUrl", e.target.value)} placeholder="https://" />
          </section>
          <section className="card" style={{ marginTop: 12 }}>
            <h3>Job preferences</h3>
            <label>Preferred roles</label>
            <input value={profile.preferredRoles || ""} onChange={(e) => set("preferredRoles", e.target.value)} />
            <label>Preferred locations</label>
            <input value={profile.preferredLocations || ""} onChange={(e) => set("preferredLocations", e.target.value)} />
            <div className="two">
              <div>
                <label>Employment type</label>
                <select value={profile.preferredEmploymentType || ""} onChange={(e) => set("preferredEmploymentType", e.target.value)}>
                  <option value="">Select</option>
                  <option>Full-time</option>
                  <option>Part-time</option>
                  <option>Contract</option>
                  <option>Internship</option>
                </select>
              </div>
              <div>
                <label>Work mode</label>
                <select value={profile.workMode || ""} onChange={(e) => set("workMode", e.target.value)}>
                  <option value="">Select</option>
                  <option>Remote</option>
                  <option>Hybrid</option>
                  <option>On-site</option>
                </select>
              </div>
            </div>
            <label>Availability</label>
            <input value={profile.availability || ""} onChange={(e) => set("availability", e.target.value)} placeholder="Immediately, or a notice period" />
            <label>Years of experience</label>
            <input type="number" min="0" max="60" value={profile.yearsExperience ?? ""} onChange={(e) => set("yearsExperience", e.target.value)} />
            <label className="row">
              <input type="checkbox" checked={!!profile.user.aiConsent} onChange={(e) => setProfile({ ...profile, user: { ...profile.user, aiConsent: e.target.checked } })} />
              Allow external AI analysis
            </label>
            <button className="btn" type="submit" disabled={busy === "save"}>{busy === "save" ? "Saving…" : "Save profile"}</button>
          </section>
        </form>
      ) : (
        <div className="profile-grid">
          <section className="card">
            <h3>About me</h3>
            <p>{profile.summary || "No summary yet."}</p>
            {profile.careerObjective && <p>{profile.careerObjective}</p>}
            <p className="muted">{profile.phone || "No phone"} · {profile.user.email}</p>
          </section>
          <section className="card">
            <h3>Education</h3>
            {lines(profile.education, (row) => <p><strong>{row.degree}</strong> · {row.institution}<br />{row.specialization} {row.graduationYear} {row.score}</p>)}
          </section>
          <section className="card">
            <h3>Work experience</h3>
            {lines(profile.experience, (row) => <p><strong>{row.position}</strong> at {row.company}<br />{row.employmentType} · {row.duration}<br />{row.responsibilities}</p>)}
          </section>
          <section className="card">
            <h3>Technical skills</h3>
            {lines(profile.skillGroups, (row) => <p><strong>{row.category}</strong><br />{row.skills}</p>)}
          </section>
          <section className="card">
            <h3>Projects</h3>
            {lines(profile.projects, (row) => (
              <p><strong>{row.name}</strong><br />{row.description}<br />{row.technologies}
                {row.repositoryUrl && <><br /><a href={row.repositoryUrl}>Repository</a></>}
                {row.demoUrl && <> · <a href={row.demoUrl}>Live demo</a></>}
              </p>
            ))}
          </section>
          <section className="card">
            <h3>Certifications and achievements</h3>
            {lines(profile.certifications, (row) => <p>{row.name} · {row.issuer} · {row.date}</p>)}
            {lines(profile.achievements, (row) => <p><strong>{row.title}</strong> {row.detail}</p>)}
          </section>
          <section className="card">
            <h3>Links and preferences</h3>
            <p>GitHub: {profile.githubUsername || "not set"}</p>
            <p>{profile.linkedinUrl || "No LinkedIn link"}</p>
            <p>{profile.portfolioUrl || profile.websiteUrl || "No portfolio or website"}</p>
            <p className="muted">{[profile.preferredRoles, profile.preferredLocations, profile.preferredEmploymentType, profile.workMode, profile.availability].filter(Boolean).join(" · ") || "Preferences not set"}</p>
          </section>
        </div>
      )}

      <div className="card" style={{ marginTop: 16 }}>
        <h3>Resume</h3>
        <p className="muted">{profile.resumeOriginalName || "No file yet"}</p>
        <input type="file" accept=".pdf,.doc,.docx,.txt" onChange={(e) => upload("resume", e.target.files[0])} />
        {profile.resumeOriginalName && (
          <div className="row" style={{ marginTop: 8 }}>
            <button className="btn ghost small" type="button" onClick={() => downloadAuthed(`/api/profile/${profile.user.id}/resume`, profile.resumeOriginalName).catch((err) => setError(err.message))}>Download</button>
          </div>
        )}
        <div className="row" style={{ marginTop: 12 }}>
          <button className="btn" type="button" onClick={analyze} disabled={busy === "extract"}>{busy === "extract" ? "Extracting…" : "Extract skills"}</button>
        </div>
        {verified?.length > 0 && <p>Verified from the resume: {verified.join(", ")}</p>}
        {extraction && (
          <>
            <p className="muted">{extraction.summary}</p>
            <label>Correct extracted skills</label>
            <input value={skillsText} onChange={(e) => setSkillsText(e.target.value)} />
            <button className="btn ghost" type="button" onClick={correct}>Save corrections</button>
          </>
        )}
      </div>
      <h3>Skill proof on file</h3>
      <div className="grid">
        {proof.length === 0 && <p className="muted">No GitHub repository is imported yet. Use Skill proof to add one.</p>}
        {proof.map((project) => (
          <article className="card" key={project.id}>
            <h3>{project.owner}/{project.name}</h3>
            <p className="muted">{(project.detectedTechnologies || []).join(", ")}</p>
          </article>
        ))}
      </div>
      <h3>Recommended roles</h3>
      <div className="grid">
        {recs.map((r) => (
          <article className="card" key={r.job.id}>
            <p className="score">{r.matchScore}%</p>
            <h3>{r.job.title}</h3>
            <p className="muted">{r.reason}</p>
          </article>
        ))}
      </div>
      <AccountSecurity />
    </div>
  );
}

function Repeatable({ title, rows, onAdd, onRemove, children }) {
  return (
    <section className="card" style={{ marginTop: 12 }}>
      <h3>{title}</h3>
      {rows.map((row, index) => (
        <div className="entry" key={index}>
          {children(row, index)}
          <button className="btn ghost small" type="button" onClick={() => onRemove(index)}>Remove</button>
        </div>
      ))}
      <button className="btn ghost small" type="button" onClick={onAdd}>Add</button>
    </section>
  );
}
