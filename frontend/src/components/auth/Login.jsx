import { useState } from "react";
import { Navigate } from "react-router";
import { useAuth } from "../../context/AuthContext.js";

export default function Login(){
    const { user, loading, login } = useAuth();
    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");
    const [error, setError] = useState(null);
    const [submitting, setSubmitting] = useState(false);

    if (loading) return null;
    if (user) return <Navigate to="/" replace/>

    async function handleSubmit(event) {
        event.preventDefault();
        setError(null);
        setSubmitting(true);
        try {
            await login(username, password);
        } catch (e) {
            setError(e.message);
        } finally {
            setSubmitting(false);
        }
    }

    return (
        <main className="login">
            <h1>Iniciar sesión</h1>
            <form onSubmit={handleSubmit}>
                <label className="login-field">
                    Usuario
                    <input
                        type="text"
                        value={username}
                        onChange={(e) => setUsername(e.target.value)}
                        autoComplete="username"
                        required
                    />
                </label>
                <label className="login-field">
                    Contraseña
                    <input
                        type="password"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        autoComplete="current-password"
                        required
                    />
                </label>
                {error && <p className="login-error" role="alert">{error}</p>}
                <button className="login-button" type="submit" disabled={submitting}>
                    {submitting ? "Ingresando..." : "Ingresar"}
                </button>
            </form>
        </main>
    )
}
