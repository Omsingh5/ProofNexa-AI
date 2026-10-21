import { Link } from "react-router-dom";

export default function Landing() {
  return (
    <>
      <section className="hero">
        <div>
          <p className="kicker">Skill proof, not skill claims</p>
          <h1>Hire from evidence. Grow from gaps.</h1>
          <p className="lede">
            ProofNexa AI is an AI-powered recruitment platform. Candidates connect skills to GitHub and resume evidence.
            Recruiters review applications and project quality. Candidate code is inspected, never executed.
          </p>
          <div className="row" style={{ marginTop: 24 }}>
            <Link to="/register"><button className="btn">Start as candidate</button></Link>
            <Link to="/jobs"><button className="btn ghost">Browse open roles</button></Link>
          </div>
        </div>
        <figure className="hero-visual">
          <img src="/hero-proof.jpg" alt="A resume linked to code, a skill graph, and a project quality score" />
        </figure>
      </section>
      <div className="grid">
        <article className="card">
          <p className="kicker">Phase 1</p>
          <h3>Recruiting core</h3>
          <p className="muted">Roles, jobs, applications, resumes, MySQL + Spring Data JPA.</p>
        </article>
        <article className="card">
          <p className="kicker">Flagship</p>
          <h3>Skill Proof Engine</h3>
          <p className="muted">GitHub trees, file-linked evidence, roadmaps, and challenges.</p>
        </article>
        <article className="card">
          <p className="kicker">Flagship</p>
          <h3>Project Quality Analyzer</h3>
          <p className="muted">Architecture, maintainability, security patterns, tests, data design.</p>
        </article>
      </div>
    </>
  );
}
