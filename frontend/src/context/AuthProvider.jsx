import { useCallback, useEffect, useState } from "react";
import apiService from "../services/apiService.js";
import { AuthContext } from "./AuthContext.js";

export default function AuthProvider({ children }) {
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);

    // La cookie del token es HttpOnly: el front no puede leerla, así que al cargar
    // la página le pregunta al backend quién está logueado.
    useEffect(() => {
        apiService.getMe()
            .then(setUser)
            .catch(() => setUser(null))
            .finally(() => setLoading(false));
    }, []);

    async function login(username, password) {
        await apiService.login(username, password);
        setUser(await apiService.getMe());
    }

    // La cookie es HttpOnly: solo el backend puede borrarla. Si el pedido falla no se
    // limpia el usuario, así la sesión sigue siendo coherente con lo que hay en la cookie.
    // Al quedar sin usuario, ProtectedRoute redirige al login.
    async function logout() {
        await apiService.logout();
        setUser(null);
    }

    // Se usa cuando el backend responde 401 (la cookie venció): al quedar sin usuario,
    // ProtectedRoute redirige al login. useCallback mantiene la misma función entre
    // renders para poder usarla como dependencia de un useEffect.
    const endSession = useCallback(() => setUser(null), []);

    return (
        <AuthContext value={{ user, loading, login, logout, endSession }}>
            {children}
        </AuthContext>
    )
}
