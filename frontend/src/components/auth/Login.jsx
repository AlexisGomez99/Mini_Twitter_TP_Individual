import { useState } from "react";
import { Link, Navigate } from "react-router";
import { useAutenticacion } from "../../context/AuthContext.js";

export default function Login(){
    const { usuario, cargando, iniciarSesion } = useAutenticacion();
    const [nombreUsuario, setNombreUsuario] = useState("");
    const [contrasenia, setContrasenia] = useState("");
    const [error, setError] = useState(null);
    const [enviando, setEnviando] = useState(false);

    if (cargando) return null;
    if (usuario) return <Navigate to="/" replace/>

    async function manejarEnvio(evento) {
        evento.preventDefault();
        setError(null);
        setEnviando(true);
        try {
            await iniciarSesion(nombreUsuario, contrasenia);
        } catch (e) {
            setError(e.message);
        } finally {
            setEnviando(false);
        }
    }

    return (
        <main className="login">
            <h1>Iniciar sesión</h1>
            <form onSubmit={manejarEnvio}>
                <label className="login-field">
                    Usuario
                    <input
                        type="text"
                        value={nombreUsuario}
                        onChange={(e) => setNombreUsuario(e.target.value)}
                        autoComplete="username"
                        required
                    />
                </label>
                <label className="login-field">
                    Contraseña
                    <input
                        type="password"
                        value={contrasenia}
                        onChange={(e) => setContrasenia(e.target.value)}
                        autoComplete="current-password"
                        required
                    />
                </label>
                {error && <p className="login-error" role="alert">{error}</p>}
                <button className="login-button" type="submit" disabled={enviando}>
                    {enviando ? "Ingresando..." : "Ingresar"}
                </button>
            </form>
            <Link to="/registro" className="login-secondary-button">Crear cuenta</Link>
        </main>
    )
}
