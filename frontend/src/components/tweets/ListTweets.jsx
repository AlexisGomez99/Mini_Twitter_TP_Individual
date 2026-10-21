import { useEffect, useState } from "react";
import Tweet from "./Tweet.jsx";
import apiService from "../../services/apiService.js";
import { useAuth } from "../../context/AuthContext.js";

// "feed": tweets propios y de la gente que sigo. "mine": solo mis tweets.
// "user": solo los tweets de otra persona (se indica con la prop userId).
const EMPTY_MESSAGES = {
    feed: "Todavía no hay tweets en tu inicio. Publicá el primero o seguí a alguien.",
    mine: "Todavía no publicaste ningún tweet.",
    user: "Este usuario todavía no publicó ningún tweet.",
};

export default function ListTweets({ scope = "feed", userId, refreshKey }){
    const { user, endSession } = useAuth();
    const [tweets, setTweets] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [deleteError, setDeleteError] = useState(null);
    // Sube después de eliminar un tweet para volver a pedir la lista.
    const [reloadKey, setReloadKey] = useState(0);

    useEffect(() => {
        // Si el efecto se vuelve a ejecutar antes de que llegue la respuesta anterior
        // (por ejemplo, dos tweets seguidos), se descarta la vieja para que no pise a la nueva.
        let ignore = false;
        const request = scope === "feed"
            ? apiService.getFeed()
            : apiService.getTweetsByUser(scope === "user" ? userId : user.id);
        request
            .then((list) => {
                if (ignore) return;
                setTweets(list);
                setError(null);
                setDeleteError(null);
                setLoading(false);
            })
            .catch((e) => {
                if (ignore) return;
                if (e.status === 401) endSession();
                else setError(e.message);
                setLoading(false);
            });
        return () => { ignore = true; };
    }, [endSession, scope, userId, user.id, refreshKey, reloadKey]);

    async function handleDelete(tweetId) {
        if (!window.confirm("¿Querés eliminar este tweet?")) return;
        setDeleteError(null);
        try {
            await apiService.deleteTweet(tweetId);
            setReloadKey((key) => key + 1);
        } catch (e) {
            if (e.status === 401) endSession();
            else setDeleteError(e.message);
        }
    }

    if (loading) return <p className="feed-message">Cargando...</p>
    if (error) return <p className="feed-message feed-error" role="alert">{error}</p>
    if (tweets.length === 0) return <p className="feed-message">{EMPTY_MESSAGES[scope]}</p>

    return (
        <>
            {deleteError && <p className="feed-message feed-error" role="alert">{deleteError}</p>}
            <div className="tweet-list">
                {tweets.map((tweet) =>
                    // Un retweet trae originalTweetId; un tweet común no.
                    tweet.originalTweetId === undefined ? (
                        <Tweet
                            key={tweet.id}
                            authorUsername={tweet.authorUsername}
                            content={tweet.content}
                            createdAt={tweet.createdAt}
                            onDelete={tweet.authorUsername === user.username ? () => handleDelete(tweet.id) : undefined}
                        />
                    ) : (
                        <Tweet
                            key={tweet.id}
                            retweetedBy={tweet.authorUsername}
                            authorUsername={tweet.originalAuthorUsername}
                            content={tweet.originalContent}
                            unavailable={tweet.originalDeleted}
                            createdAt={tweet.createdAt}
                        />
                    )
                )}
            </div>
        </>
    )
}
