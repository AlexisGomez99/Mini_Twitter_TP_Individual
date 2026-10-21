import Avatar from "../common/Avatar.jsx";

function formatDate(isoString) {
    const date = new Date(isoString);
    const minutes = Math.floor((Date.now() - date.getTime()) / 60000);

    if (minutes < 1) return "ahora";
    if (minutes < 60) return `${minutes} min`;

    const hours = Math.floor(minutes / 60);
    if (hours < 24) return `${hours} h`;

    return date.toLocaleDateString("es-AR", { day: "numeric", month: "short" });
}

export default function Tweet({ authorUsername, content, createdAt, retweetedBy, unavailable, onDelete }){
    const time = (
        <time dateTime={createdAt} title={new Date(createdAt).toLocaleString("es-AR")}>
            {formatDate(createdAt)}
        </time>
    );

    return (
        <article className="tweet">
            {retweetedBy && (
                <div className="tweet-context">
                    {retweetedBy} retwiteó <span className="tweet-dot">·</span> {time}
                </div>
            )}
            {unavailable ? (
                <p className="tweet-unavailable">Publicación no disponible</p>
            ) : (
                <div className="tweet-main">
                    <Avatar username={authorUsername}/>
                    <div className="tweet-body">
                        <div className="tweet-header">
                            <span className="tweet-author">{authorUsername}</span>
                            {!retweetedBy && (
                                <>
                                    <span className="tweet-dot">·</span>
                                    {time}
                                </>
                            )}
                            {onDelete && (
                                <button type="button" className="tweet-delete" onClick={onDelete} aria-label="Eliminar este tweet">
                                    Eliminar
                                </button>
                            )}
                        </div>
                        <p className="tweet-content">{content}</p>
                    </div>
                </div>
            )}
        </article>
    )
}
