import { useState } from "react";
import { Link, Navigate } from "react-router";
import { useAutenticacion } from "../../context/AuthContext.js";

// Mismos límites que valida el backend en User (5 a 25 caracteres).
const USUARIO_MINIMO = 5;
const USUARIO_MAXIMO = 25;

export default function Register(){
    const { usuario, cargando, registrar } = useAutenticacion();
    const [nombreUsuario, setNombreUsuario] = useState("");
    const [contrasenia, setContrasenia] = useState("");
    const [confirmarContrasenia, setConfirmarContrasenia] = useState("");
    const [error, setError] = useState(null);
    const [enviando, setEnviando] = useState(false);

    if (cargando) return null;
    if (usuario) return <Navigate to="/" replace/>

    async function manejarEnvio(evento) {
        evento.preventDefault();
        setError(null);
        // El navegador ya impide enviar campos vacíos (required); esto cubre lo que él no
        // puede saber: el usuario en blanco y que las dos contraseñas coincidan.
        if (nombreUsuario.trim().length < USUARIO_MINIMO) {
            setError(`El usuario debe tener entre ${USUARIO_MINIMO} y ${USUARIO_MAXIMO} caracteres.`);
            return;
        }
        if (contrasenia !== confirmarContrasenia) {
            setError("Las contraseñas no coinciden.");
            return;
        }
        setEnviando(true);
        try {
            // Si sale bien, AuthProvider deja al usuario logueado y esta pantalla redirige al inicio.
            await registrar(nombreUsuario.trim(), contrasenia);
        } catch (e) {
            setError(e.message);
        } finally {
            setEnviando(false);
        }
    }

    return (
        <main className="login">
            <h1>Crear cuenta</h1>
            <form onSubmit={manejarEnvio}>
                <label className="login-field">
                    Usuario
                    <input
                        type="text"
                        value={nombreUsuario}
                        onChange={(e) => setNombreUsuario(e.target.value)}
                        autoComplete="username"
                        minLength={USUARIO_MINIMO}
                        maxLength={USUARIO_MAXIMO}
                        required
                    />
                    <span className="login-hint">Entre {USUARIO_MINIMO} y {USUARIO_MAXIMO} caracteres.</span>
                </label>
                <label className="login-field">
                    Contraseña
                    <input
                        type="password"
                        value={contrasenia}
                        onChange={(e) => setContrasenia(e.target.value)}
                        autoComplete="new-password"
                        required
                    />
                </label>
                <label className="login-field">
                    Repetir contraseña
                    <input
                        type="password"
                        value={confirmarContrasenia}
                        onChange={(e) => setConfirmarContrasenia(e.target.value)}
                        autoComplete="new-password"
                        required
                    />
                </label>
                {error && <p className="login-error" role="alert">{error}</p>}
                <button className="login-button" type="submit" disabled={enviando}>
                    {enviando ? "Creando cuenta..." : "Crear cuenta"}
                </button>
            </form>
            <p className="login-switch">
                ¿Ya tenés cuenta? <Link to="/login" className="login-switch-link">Iniciar sesión</Link>
            </p>
        </main>
    )
}
