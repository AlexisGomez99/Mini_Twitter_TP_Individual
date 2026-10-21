import { useState } from "react";
import apiService from "../../services/apiService.js";
import { useAuth } from "../../context/AuthContext.js";

const MAX_CHARACTERS = 280;

export default function NewTweet({ onPublished }){
    const { user, endSession } = useAuth();
    const [content, setContent] = useState("");
    const [error, setError] = useState(null);
    const [published, setPublished] = useState(false);
    const [submitting, setSubmitting] = useState(false);

    const remaining = MAX_CHARACTERS - content.length;
    const exceeded = remaining < 0;
    const empty = content.trim().length === 0;

    function handleChange(event) {
        setContent(event.target.value);
        setError(null);
        setPublished(false);
    }

    async function handleSubmit(event) {
        event.preventDefault();
        setError(null);
        setSubmitting(true);
        try {
            await apiService.createTweet(user.id, content);
            setContent("");
            setPublished(true);
            onPublished();
        } catch (e) {
            if (e.status === 401) endSession();
            else setError(e.message);
        } finally {
            setSubmitting(false);
        }
    }

    return (
        <form className="new-tweet" onSubmit={handleSubmit}>
            <textarea
                value={content}
                onChange={handleChange}
                placeholder="¿Qué está pasando?"
                aria-label="Nuevo tweet"
                rows={3}
            />
            {exceeded && (
                <p className="new-tweet-message new-tweet-error" role="alert">
                    El tweet excede el máximo de {MAX_CHARACTERS} caracteres.
                </p>
            )}
            {error && <p className="new-tweet-message new-tweet-error" role="alert">{error}</p>}
            {published && <p className="new-tweet-message new-tweet-success" role="status">Tweet publicado</p>}
            <div className="new-tweet-footer">
                <span className={exceeded ? "new-tweet-counter new-tweet-counter-exceeded" : "new-tweet-counter"}>
                    {remaining} caracteres disponibles
                </span>
                <button className="new-tweet-button" type="submit" disabled={exceeded || empty || submitting}>
                    {submitting ? "Enviando..." : "Enviar"}
                </button>
            </div>
        </form>
    )
}
