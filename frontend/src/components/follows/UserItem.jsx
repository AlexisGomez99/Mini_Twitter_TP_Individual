import { Link } from "react-router";
import Avatar from "../common/Avatar.jsx";

// siguiendo indica si yo sigo a este usuario: define si el botón dice "Seguir" o "Dejar de seguir".
// ruta (opcional): si se pasa, el nombre es un enlace a esa ruta.
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
