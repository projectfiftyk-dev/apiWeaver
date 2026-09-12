import { NavLink, Route, Routes } from "react-router-dom";
import TemplatesPage from "./pages/TemplatesPage";
import TemplateEditorPage from "./pages/TemplateEditorPage";
import ChainsPage from "./pages/ChainsPage";
import ChainEditorPage from "./pages/ChainEditorPage";

export default function App() {
  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand">apiWeaver</div>
        <nav className="nav">
          <NavLink to="/templates" className={navClass}>
            Templates
          </NavLink>
          <NavLink to="/chains" className={navClass}>
            Chains
          </NavLink>
        </nav>
      </aside>
      <main className="content">
        <Routes>
          <Route path="/" element={<TemplatesPage />} />
          <Route path="/templates" element={<TemplatesPage />} />
          <Route path="/templates/new" element={<TemplateEditorPage />} />
          <Route path="/templates/:id" element={<TemplateEditorPage />} />
          <Route path="/chains" element={<ChainsPage />} />
          <Route path="/chains/new" element={<ChainEditorPage />} />
          <Route path="/chains/:id" element={<ChainEditorPage />} />
        </Routes>
      </main>
    </div>
  );
}

function navClass({ isActive }: { isActive: boolean }) {
  return isActive ? "nav-link nav-link-active" : "nav-link";
}
