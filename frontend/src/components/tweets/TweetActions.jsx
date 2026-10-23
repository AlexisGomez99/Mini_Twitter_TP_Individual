import { useState } from "react";

const PROPIEDADES_ICONO = {
    viewBox: "0 0 24 24",
    width: 18,
    height: 18,
    fill: "none",
    stroke: "currentColor",
    strokeWidth: 2,
    strokeLinecap: "round",
    strokeLinejoin: "round",
    "aria-hidden": "true",
};

// Barra de acciones de un tweet original: comentar, retwittear y dar like.
// - Comentar: todavía no hace nada.
// - Retwittear: lo maneja quien lo usa (retuiteado dice si ya retwitteé este tweet).
//   puedeRetuitear es false en los tweets propios: el backend no deja retwittear el propio tweet.
// - Like: solo es visual. El estado vive acá, no se guarda en ningún lado y se pierde al
//   recargar la página.
export default function TweetActions({ puedeRetuitear, retuiteado, retuiteoOcupado, alAlternarRetweet }){
    const [meGusta, setMeGusta] = useState(false);

    return (
        <div className="tweet-actions">
            <button type="button" className="tweet-action tweet-action-comment" aria-label="Comentar" title="Comentar">
                <svg {...PROPIEDADES_ICONO}>
                    <path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z"/>
                </svg>
            </button>
            <button
                type="button"
                className={retuiteado ? "tweet-action tweet-action-retweet active" : "tweet-action tweet-action-retweet"}
                onClick={alAlternarRetweet}
                disabled={!puedeRetuitear || retuiteoOcupado}
                aria-label={retuiteado ? "Deshacer retweet" : "Retwittear"}
                aria-pressed={retuiteado}
                title={puedeRetuitear ? (retuiteado ? "Deshacer retweet" : "Retwittear") : "No podés retwittear tu propio tweet"}
            >
                <svg {...PROPIEDADES_ICONO}>
                    <polyline points="17 1 21 5 17 9"/>
                    <path d="M3 11V9a4 4 0 0 1 4-4h14"/>
                    <polyline points="7 23 3 19 7 15"/>
                    <path d="M21 13v2a4 4 0 0 1-4 4H3"/>
                </svg>
            </button>
            <button
                type="button"
                className={meGusta ? "tweet-action tweet-action-like active" : "tweet-action tweet-action-like"}
                onClick={() => setMeGusta((actual) => !actual)}
                aria-label={meGusta ? "Quitar me gusta" : "Me gusta"}
                aria-pressed={meGusta}
                title={meGusta ? "Quitar me gusta" : "Me gusta"}
            >
                <svg {...PROPIEDADES_ICONO}>
                    <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/>
                </svg>
            </button>
        </div>
    )
}
