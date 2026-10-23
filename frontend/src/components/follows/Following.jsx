import { useEffect, useState } from "react";
import PageHeader from "../layout/PageHeader.jsx";
import UserItem from "./UserItem.jsx";
import servicioApi from "../../services/apiService.js";
import { useAutenticacion } from "../../context/AuthContext.js";

export default function Following(){
    const { usuario, terminarSesion } = useAutenticacion();
    const [usuariosSeguidos, setUsuariosSeguidos] = useState([]);
    const [cargando, setCargando] = useState(true);
    const [error, setError] = useState(null);
    const [errorDeAccion, setErrorDeAccion] = useState(null);
    const [idOcupado, setIdOcupado] = useState(null);

    useEffect(() => {
        let ignorar = false;
        servicioApi.obtenerSeguidos(usuario.id)
            .then((lista) => {
                if (ignorar) return;
                setUsuariosSeguidos(lista);
                setCargando(false);
            })
            .catch((e) => {
                if (ignorar) return;
                if (e.estado === 401) terminarSesion();
                else setError(e.message);
                setCargando(false);
            });
        return () => { ignorar = true; };
    }, [terminarSesion, usuario.id]);

    async function manejarDejarDeSeguir(seguido) {
        setErrorDeAccion(null);
        setIdOcupado(seguido.id);
        try {
            await servicioApi.dejarDeSeguir(usuario.id, seguido.id);
            // Ya no lo sigo: sale de la lista.
            setUsuariosSeguidos((lista) => lista.filter((item) => item.id !== seguido.id));
        } catch (e) {
            if (e.estado === 401) terminarSesion();
            else setErrorDeAccion(e.message);
        } finally {
            setIdOcupado(null);
        }
    }

    let cuerpo;
    if (cargando) cuerpo = <p className="feed-message">Cargando...</p>;
    else if (error) cuerpo = <p className="feed-message feed-error" role="alert">{error}</p>;
    else if (usuariosSeguidos.length === 0) cuerpo = <p className="feed-message">Todavía no seguís a nadie.</p>;
    else cuerpo = (
        <>
            {errorDeAccion && <p className="feed-message feed-error" role="alert">{errorDeAccion}</p>}
            <div className="user-list">
                {usuariosSeguidos.map((seguido) => (
                    <UserItem
                        key={seguido.id}
                        nombreUsuario={seguido.username}
                        ruta={`/usuarios/${seguido.id}`}
                        siguiendo
                        ocupado={idOcupado === seguido.id}
                        alAlternarSeguir={() => manejarDejarDeSeguir(seguido)}
                    />
                ))}
            </div>
        </>
    );

    return (
        <main className="content">
            <PageHeader titulo="Seguidos"/>
            {cuerpo}
        </main>
    )
}
