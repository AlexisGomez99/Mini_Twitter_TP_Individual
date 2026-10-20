const hueFor = (username) => [...username].reduce((sum, char) => sum + char.charCodeAt(0), 0) % 360;

function formatDate(isoString) {
    const date = new Date(isoString);
    const minutes = Math.floor((Date.now() - date.getTime()) / 60000);

    if (minutes < 1) return "ahora";
    if (minutes < 60) return `${minutes} min`;

    const hours = Math.floor(minutes / 60);
    if (hours < 24) return `${hours} h`;

    return date.toLocaleDateString("es-AR", { day: "numeric", month: "short" });
}

export default function Tweet({ authorUsername, content, createdAt }) {

    return (
        <article className="tweet">
            <div
                className="tweet-avatar"
                style={{ background: `hsl(${hueFor(authorUsername)} 55% 42%)` }}
                aria-hidden="true"
            >
                {authorUsername[0].toUpperCase()}
            </div>
            <div className="tweet-body">
                <div className="tweet-header">
                    <span className="tweet-author">{authorUsername}</span>
                    <span className="tweet-dot">·</span>
                    <time dateTime={createdAt} title={new Date(createdAt).toLocaleString("es-AR")}>
                        {formatDate(createdAt)}
                    </time>
                </div>
                <p className="tweet-content">{content}</p>
            </div>
        </article>
    )
}
