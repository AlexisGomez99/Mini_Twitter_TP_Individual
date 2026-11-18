import { Link } from "react-router";
import Avatar from "../common/Avatar.jsx";

export default function UserItem({ nombreUsuario, ruta, siguiendo, ocupado, alAlternarSeguir }){

    return (
        <article className="user-item">
            <Avatar nombreUsuario={nombreUsuario}/>
            {ruta
                ? <Link to={ruta} className="user-item-name user-item-link">{nombreUsuario}</Link>
                : <span className="user-item-name">{nombreUsuario}</span>}
            <button
                type="button"
                className={siguiendo ? "follow-button follow-button-secondary" : "follow-button"}
                onClick={alAlternarSeguir}
                disabled={ocupado}
            >
                {siguiendo ? "Dejar de seguir" : "Seguir"}
            </button>
        </article>
    )
}
