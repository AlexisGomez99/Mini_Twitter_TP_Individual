import { useEffect, useState } from "react";
import { useParams } from "react-router";
import ListTweets from "./ListTweets.jsx";
import PageHeader from "../layout/PageHeader.jsx";
import servicioApi from "../../services/apiService.js";
import { useAutenticacion } from "../../context/AuthContext.js";

export default function UserTweets(){
    const { id } = useParams();
    const { terminarSesion } = useAutenticacion();
    const [nombreUsuario, setNombreUsuario] = useState(null);
    const [error, setError] = useState(null);

    // Se pide el usuario para poner su nombre en el título y avisar si no existe.
    useEffect(() => {
        let ignorar = false;
        servicioApi.obtenerUsuarioPorId(id)
            .then((info) => {
                if (!ignorar) setNombreUsuario(info.username);
            })
            .catch((e) => {
                if (ignorar) return;
                if (e.estado === 401) terminarSesion();
                else setError(e.message);
            });
        return () => { ignorar = true; };
    }, [id, terminarSesion]);

    return (
        <main className="content">
            <PageHeader titulo={nombreUsuario ?? "Usuario"}/>
            {error
                ? <p className="feed-message feed-error" role="alert">{error}</p>
                : <ListTweets alcance="usuario" idUsuario={id}/>}
        </main>
    )
}
