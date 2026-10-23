import { useState } from "react";
import { Link, NavLink } from "react-router";
import { useAutenticacion } from "../../context/AuthContext.js";

// Para agregar una opción al menú, sumá un objeto a esta lista.
const OPCIONES = [
    { ruta: "/mis-tweets", etiqueta: "Mis tweets" },
    { ruta: "/seguidores", etiqueta: "Seguidores" },
    { ruta: "/seguidos", etiqueta: "Seguidos" },
    { ruta: "/buscar", etiqueta: "Seguir a" },
];

export default function Sidebar(){
    const { cerrarSesion } = useAutenticacion();
    const [ocupado, setOcupado] = useState(false);
    const [error, setError] = useState(null);

    // Si sale bien, AuthProvider deja usuario en null y ProtectedRoute manda al login
    // (este componente se desmonta). Si falla, se queda logueado y se avisa.
    async function manejarCerrarSesion() {
        setOcupado(true);
        setError(null);
        try {
            await cerrarSesion();
        } catch (e) {
            setError(e.message);
            setOcupado(false);
        }
    }

    return (
        <nav className="sidebar" aria-label="Navegación principal">
            <Link to="/" className="sidebar-brand">Mini Twitter</Link>
            <ul className="sidebar-menu">
                {OPCIONES.map((opcion) => (
                    <li key={opcion.ruta}>
                        <NavLink to={opcion.ruta} className="sidebar-link">{opcion.etiqueta}</NavLink>
                    </li>
                ))}
            </ul>
            <div className="sidebar-footer">
                {error && <p className="sidebar-error" role="alert">{error}</p>}
                <button type="button" className="sidebar-logout" onClick={manejarCerrarSesion} disabled={ocupado}>
                    Cerrar sesión
                </button>
            </div>
        </nav>
    )
}
