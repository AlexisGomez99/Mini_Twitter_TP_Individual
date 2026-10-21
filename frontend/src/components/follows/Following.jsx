import { useEffect, useState } from "react";
import PageHeader from "../layout/PageHeader.jsx";
import UserItem from "./UserItem.jsx";
import apiService from "../../services/apiService.js";
import { useAuth } from "../../context/AuthContext.js";

export default function Following(){
    const { user, endSession } = useAuth();
    const [followedUsers, setFollowedUsers] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [actionError, setActionError] = useState(null);
    const [busyId, setBusyId] = useState(null);

    useEffect(() => {
        let ignore = false;
        apiService.getFollowing(user.id)
            .then((list) => {
                if (ignore) return;
                setFollowedUsers(list);
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

    async function handleUnfollow(followed) {
        setActionError(null);
        setBusyId(followed.id);
        try {
            await apiService.unfollow(user.id, followed.id);
            // Ya no lo sigo: sale de la lista.
            setFollowedUsers((list) => list.filter((item) => item.id !== followed.id));
        } catch (e) {
            if (e.status === 401) endSession();
            else setActionError(e.message);
        } finally {
            setBusyId(null);
        }
    }

    let body;
    if (loading) body = <p className="feed-message">Cargando...</p>;
    else if (error) body = <p className="feed-message feed-error" role="alert">{error}</p>;
    else if (followedUsers.length === 0) body = <p className="feed-message">Todavía no seguís a nadie.</p>;
    else body = (
        <>
            {actionError && <p className="feed-message feed-error" role="alert">{actionError}</p>}
            <div className="user-list">
                {followedUsers.map((followed) => (
                    <UserItem
                        key={followed.id}
                        username={followed.username}
                        to={`/usuarios/${followed.id}`}
                        following
                        busy={busyId === followed.id}
                        onToggleFollow={() => handleUnfollow(followed)}
                    />
                ))}
            </div>
        </>
    );

    return (
        <main className="content">
            <PageHeader title="Seguidos"/>
            {body}
        </main>
    )
}
