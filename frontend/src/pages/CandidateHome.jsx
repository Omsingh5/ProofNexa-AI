import { NavLink, Route, Routes } from "react-router-dom";
import ProfilePanel from "./candidate/ProfilePanel.jsx";
import ApplicationsPanel from "./candidate/ApplicationsPanel.jsx";
import ProofPanel from "./candidate/ProofPanel.jsx";
import QualityPanel from "./candidate/QualityPanel.jsx";
import GrowthPanel from "./candidate/GrowthPanel.jsx";
import AssistantPanel from "./candidate/AssistantPanel.jsx";

export default function CandidateHome() {
  return (
    <div className="app-frame">
      <aside className="side">
        <p className="kicker">Candidate</p>
        <NavLink to="/app" end>Profile</NavLink>
        <NavLink to="/app/applications">Applications</NavLink>
        <NavLink to="/app/proof">Skill proof</NavLink>
        <NavLink to="/app/quality">Project quality</NavLink>
        <NavLink to="/app/growth">Growth</NavLink>
        <NavLink to="/app/assistant">Career assistant</NavLink>
      </aside>
      <Routes>
        <Route index element={<ProfilePanel />} />
        <Route path="applications" element={<ApplicationsPanel />} />
        <Route path="proof" element={<ProofPanel />} />
        <Route path="quality" element={<QualityPanel />} />
        <Route path="growth" element={<GrowthPanel />} />
        <Route path="assistant" element={<AssistantPanel />} />
      </Routes>
    </div>
  );
}
