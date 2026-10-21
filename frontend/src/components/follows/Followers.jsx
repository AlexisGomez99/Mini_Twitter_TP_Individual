import { useEffect, useState } from "react";
import PageHeader from "../layout/PageHeader.jsx";
import UserItem from "./UserItem.jsx";
import apiService from "../../services/apiService.js";
import { useAuth } from "../../context/AuthContext.js";

export default function Followers(){
    const { user, endSession } = useAuth();
    const [followers, setFollowers] = useState([]);
    // Ids de las personas a las que yo sigo: sirve para saber qué botón mostrar en cada seguidor.
    const [followingIds, setFollowingIds] = useState(new Set());
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [actionError, setActionError] = useState(null);
    const [busyId, setBusyId] = useState(null);

    useEffect(() => {
        let ignore = false;
        Promise.all([apiService.getFollowers(user.id), apiService.getFollowing(user.id)])
            .then(([followersList, followingList]) => {
                if (ignore) return;
                setFollowers(followersList);
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

    async function handleToggleFollow(follower) {
        const isFollowing = followingIds.has(follower.id);
        setActionError(null);
        setBusyId(follower.id);
        try {
            if (isFollowing) await apiService.unfollow(user.id, follower.id);
            else await apiService.follow(user.id, follower.id);
            setFollowingIds((ids) => {
                const next = new Set(ids);
                if (isFollowing) next.delete(follower.id);
                else next.add(follower.id);
                return next;
            });
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
    else if (followers.length === 0) body = <p className="feed-message">Todavía no te sigue nadie.</p>;
    else body = (
        <>
            {actionError && <p className="feed-message feed-error" role="alert">{actionError}</p>}
            <div className="user-list">
                {followers.map((follower) => (
                    <UserItem
                        key={follower.id}
                        username={follower.username}
                        following={followingIds.has(follower.id)}
                        busy={busyId === follower.id}
                        onToggleFollow={() => handleToggleFollow(follower)}
                    />
                ))}
            </div>
        </>
    );

    return (
        <main className="content">
            <PageHeader title="Seguidores"/>
            {body}
        </main>
    )
}
