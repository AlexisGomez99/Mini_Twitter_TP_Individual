import { useState } from "react";
import { Link, NavLink } from "react-router";
import { useAuth } from "../../context/AuthContext.js";

// Para agregar una opción al menú, sumá un objeto a esta lista.
const OPTIONS = [
    { to: "/mis-tweets", label: "Mis tweets" },
    { to: "/seguidores", label: "Seguidores" },
    { to: "/seguidos", label: "Seguidos" },
    { to: "/buscar", label: "Seguir a" },
];

export default function Sidebar(){
    const { logout } = useAuth();
    const [busy, setBusy] = useState(false);
    const [error, setError] = useState(null);

    // Si sale bien, AuthProvider deja user en null y ProtectedRoute manda al login
    // (este componente se desmonta). Si falla, se queda logueado y se avisa.
    async function handleLogout() {
        setBusy(true);
        setError(null);
        try {
            await logout();
        } catch (e) {
            setError(e.message);
            setBusy(false);
        }
    }

    return (
        <nav className="sidebar" aria-label="Navegación principal">
            <Link to="/" className="sidebar-brand">Mini Twitter</Link>
            <ul className="sidebar-menu">
                {OPTIONS.map((option) => (
                    <li key={option.to}>
                        <NavLink to={option.to} className="sidebar-link">{option.label}</NavLink>
                    </li>
                ))}
            </ul>
            <div className="sidebar-footer">
                {error && <p className="sidebar-error" role="alert">{error}</p>}
                <button type="button" className="sidebar-logout" onClick={handleLogout} disabled={busy}>
                    Cerrar sesión
                </button>
            </div>
        </nav>
    )
}
