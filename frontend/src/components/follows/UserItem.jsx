import { Link } from "react-router";
import Avatar from "../common/Avatar.jsx";

// following indica si yo sigo a este usuario: define si el botón dice "Seguir" o "Dejar de seguir".
// to (opcional): si se pasa, el nombre es un enlace a esa ruta.
export default function UserItem({ username, to, following, busy, onToggleFollow }){

    return (
        <article className="user-item">
            <Avatar username={username}/>
            {to
                ? <Link to={to} className="user-item-name user-item-link">{username}</Link>
                : <span className="user-item-name">{username}</span>}
            <button
                type="button"
                className={following ? "follow-button follow-button-secondary" : "follow-button"}
                onClick={onToggleFollow}
                disabled={busy}
            >
                {following ? "Dejar de seguir" : "Seguir"}
            </button>
        </article>
    )
}
