import { useState } from "react";
import servicioApi from "../../services/apiService.js";
import { useAutenticacion } from "../../context/AuthContext.js";

const MAXIMO_CARACTERES = 280;

export default function NewTweet({ alPublicar }){
    const { usuario, terminarSesion } = useAutenticacion();
    const [contenido, setContenido] = useState("");
    const [error, setError] = useState(null);
    const [publicado, setPublicado] = useState(false);
    const [enviando, setEnviando] = useState(false);

    const restantes = MAXIMO_CARACTERES - contenido.length;
    const excedido = restantes < 0;
    const vacio = contenido.trim().length === 0;

    function manejarCambio(evento) {
        setContenido(evento.target.value);
        setError(null);
        setPublicado(false);
    }

    async function manejarEnvio(evento) {
        evento.preventDefault();
        setError(null);
        setEnviando(true);
        try {
            await servicioApi.crearTweet(usuario.id, contenido);
            setContenido("");
            setPublicado(true);
            alPublicar();
        } catch (e) {
            if (e.estado === 401) terminarSesion();
            else setError(e.message);
        } finally {
            setEnviando(false);
        }
    }

    return (
        <form className="new-tweet" onSubmit={manejarEnvio}>
            <textarea
                value={contenido}
                onChange={manejarCambio}
                placeholder="¿Qué está pasando?"
                aria-label="Nuevo tweet"
                rows={3}
            />
            {excedido && (
                <p className="new-tweet-message new-tweet-error" role="alert">
                    El tweet excede el máximo de {MAXIMO_CARACTERES} caracteres.
                </p>
            )}
            {error && <p className="new-tweet-message new-tweet-error" role="alert">{error}</p>}
            {publicado && <p className="new-tweet-message new-tweet-success" role="status">Tweet publicado</p>}
            <div className="new-tweet-footer">
                <span className={excedido ? "new-tweet-counter new-tweet-counter-exceeded" : "new-tweet-counter"}>
                    {restantes} caracteres disponibles
                </span>
                <button className="new-tweet-button" type="submit" disabled={excedido || vacio || enviando}>
                    {enviando ? "Enviando..." : "Enviar"}
                </button>
            </div>
        </form>
    )
}
