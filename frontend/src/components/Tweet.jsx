export default function Tweet({ nombre, contenido, fechaCreacion }){

    return (
        <div className="tweet">
            <div className="tweet-header">
                <span className="tweet-nombre">{nombre}</span>
                <span className="tweet-fecha">{fechaCreacion}</span>
            </div>
            <p className="tweet-contenido">{contenido}</p>
            <div className="tweet-opciones">
                <button type="button" className="tweet-opcion">Me gusta</button>
                <button type="button" className="tweet-opcion">Retweet</button>
                <button type="button" className="tweet-opcion">Comentar</button>
            </div>
        </div>
    )
}
