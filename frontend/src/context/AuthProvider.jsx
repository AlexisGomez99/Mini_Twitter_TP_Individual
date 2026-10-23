import { useCallback, useEffect, useState } from "react";
import servicioApi from "../services/apiService.js";
import { AuthContext } from "./AuthContext.js";

export default function AuthProvider({ children }) {
    const [usuario, setUsuario] = useState(null);
    const [cargando, setCargando] = useState(true);

    useEffect(() => {
        servicioApi.obtenerYo()
            .then(setUsuario)
            .catch(() => setUsuario(null))
            .finally(() => setCargando(false));
    }, []);

    async function iniciarSesion(nombreUsuario, contrasenia) {
        await servicioApi.iniciarSesion(nombreUsuario, contrasenia);
        setUsuario(await servicioApi.obtenerYo());
    }

    async function registrar(nombreUsuario, contrasenia) {
        await servicioApi.registrar(nombreUsuario, contrasenia);
        await iniciarSesion(nombreUsuario, contrasenia);
    }

    async function cerrarSesion() {
        await servicioApi.cerrarSesion();
        setUsuario(null);
    }

    const terminarSesion = useCallback(() => setUsuario(null), []);

    return (
        <AuthContext value={{ usuario, cargando, iniciarSesion, registrar, cerrarSesion, terminarSesion }}>
            {children}
        </AuthContext>
    )
}
