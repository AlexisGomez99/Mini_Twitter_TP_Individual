import { useEffect, useState } from "react";
import PageHeader from "../layout/PageHeader.jsx";
import UserItem from "./UserItem.jsx";
import apiService from "../../services/apiService.js";
import { useAuth } from "../../context/AuthContext.js";

export default function SearchUsers(){
    const { user, endSession } = useAuth();
    const [users, setUsers] = useState([]);
    // Ids de las personas a las que yo sigo: define si cada resultado muestra "Seguir" o "Dejar de seguir".
    const [followingIds, setFollowingIds] = useState(new Set());
    const [query, setQuery] = useState("");
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [actionError, setActionError] = useState(null);
    const [busyId, setBusyId] = useState(null);

    // Se traen todos los usuarios una sola vez y se filtra en el front mientras se escribe:
    // el backend solo busca por nombre exacto (GET /users?username=).
    useEffect(() => {
        let ignore = false;
        Promise.all([apiService.listUsers(), apiService.getFollowing(user.id)])
            .then(([usersList, followingList]) => {
                if (ignore) return;
                setUsers(usersList);
                setFollowingIds(new Set(followingList.map((followed) => followed.id)));
                setLoading(false);
            })
            .catch((e) => {
                if (ignore) return;
                if (e.status === 401) endSession();
                else setError(e.message);
                setLoading(false);
            });
        return () => { ignore = true; };
    }, [endSession, user.id]);

    async function handleToggleFollow(found) {
        const isFollowing = followingIds.has(found.id);
        setActionError(null);
        setBusyId(found.id);
        try {
            if (isFollowing) await apiService.unfollow(user.id, found.id);
            else await apiService.follow(user.id, found.id);
            setFollowingIds((ids) => {
                const next = new Set(ids);
                if (isFollowing) next.delete(found.id);
                else next.add(found.id);
                return next;
            });
        } catch (e) {
            if (e.status === 401) endSession();
            else setActionError(e.message);
        } finally {
            setBusyId(null);
        }
    }

    const term = query.trim().toLowerCase();
    // No se ofrece seguirse a uno mismo.
    const results = term === ""
        ? []
        : users.filter((found) => found.id !== user.id && found.username.toLowerCase().includes(term));

    let body;
    if (loading) body = <p className="feed-message">Cargando...</p>;
    else if (error) body = <p className="feed-message feed-error" role="alert">{error}</p>;
    else if (term === "") body = <p className="feed-message">Escribí un nombre para buscar usuarios.</p>;
    else if (results.length === 0) body = <p className="feed-message">No se encontraron usuarios para "{query.trim()}".</p>;
    else body = (
        <>
            {actionError && <p className="feed-message feed-error" role="alert">{actionError}</p>}
            <div className="user-list">
                {results.map((found) => (
                    <UserItem
                        key={found.id}
                        username={found.username}
                        to={`/usuarios/${found.id}`}
                        following={followingIds.has(found.id)}
                        busy={busyId === found.id}
                        onToggleFollow={() => handleToggleFollow(found)}
                    />
                ))}
            </div>
        </>
    );

    return (
        <main className="content">
            <PageHeader title="Seguir a"/>
            <div className="search-box">
                <input
                    type="search"
                    className="search-input"
                    placeholder="Buscar usuarios por nombre"
                    aria-label="Buscar usuarios por nombre"
                    value={query}
                    onChange={(e) => setQuery(e.target.value)}
                    autoFocus
                />
            </div>
            {body}
        </main>
    )
}
