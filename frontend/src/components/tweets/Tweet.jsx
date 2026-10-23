import Avatar from "../common/Avatar.jsx";
import TweetActions from "./TweetActions.jsx";

function formatearFecha(fechaIso) {
    const fecha = new Date(fechaIso);
    const minutos = Math.floor((Date.now() - fecha.getTime()) / 60000);

    if (minutos < 1) return "ahora";
    if (minutos < 60) return `${minutos} min`;

    const horas = Math.floor(minutos / 60);
    if (horas < 24) return `${horas} h`;

    return fecha.toLocaleDateString("es-AR", { day: "numeric", month: "short" });
}

// mostrarAcciones: muestra la barra de comentar/retwittear/like (solo en tweets originales).
// puedeRetuitear, retuiteado, retuiteoOcupado y alAlternarRetweet son para el botón de retwittear.
export default function Tweet({ autor, contenido, fecha, retuiteadoPor, noDisponible, alEliminar,
                                mostrarAcciones, puedeRetuitear, retuiteado, retuiteoOcupado, alAlternarRetweet }){
    const hora = (
        <time dateTime={fecha} title={new Date(fecha).toLocaleString("es-AR")}>
            {formatearFecha(fecha)}
        </time>
    );

    return (
        <article className="tweet">
            {retuiteadoPor && (
                <div className="tweet-context">
                    {retuiteadoPor} retwiteó <span className="tweet-dot">·</span> {hora}
                </div>
            )}
            <div className="tweet-main">
                <Avatar nombreUsuario={autor}/>
                <div className="tweet-body">
                    <div className="tweet-header">
                        <span className="tweet-author">{autor}</span>
                        {!retuiteadoPor && (
                            <>
                                <span className="tweet-dot">·</span>
                                {hora}
                            </>
                        )}
                        {alEliminar && (
                            <button type="button" className="tweet-delete" onClick={alEliminar} aria-label="Eliminar este tweet">
                                Eliminar
                            </button>
                        )}
                    </div>
                    {/* Si el tweet original se borró se sigue mostrando quién lo escribió,
                        pero en lugar del texto va el aviso. */}
                    {noDisponible
                        ? <p className="tweet-unavailable">Publicación no disponible</p>
                        : <p className="tweet-content">{contenido}</p>}
                    {mostrarAcciones && (
                        <TweetActions
                            puedeRetuitear={puedeRetuitear}
                            retuiteado={retuiteado}
                            retuiteoOcupado={retuiteoOcupado}
                            alAlternarRetweet={alAlternarRetweet}
                        />
                    )}
                </div>
            </div>
        </article>
    )
}
