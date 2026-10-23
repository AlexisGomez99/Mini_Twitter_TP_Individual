import { useEffect, useState } from "react";
import PageHeader from "../layout/PageHeader.jsx";
import UserItem from "./UserItem.jsx";
import servicioApi from "../../services/apiService.js";
import { useAutenticacion } from "../../context/AuthContext.js";

export default function Followers(){
    const { usuario, terminarSesion } = useAutenticacion();
    const [seguidores, setSeguidores] = useState([]);
    // Ids de las personas a las que yo sigo: sirve para saber qué botón mostrar en cada seguidor.
    const [idsSeguidos, setIdsSeguidos] = useState(new Set());
    const [cargando, setCargando] = useState(true);
    const [error, setError] = useState(null);
    const [errorDeAccion, setErrorDeAccion] = useState(null);
    const [idOcupado, setIdOcupado] = useState(null);

    useEffect(() => {
        let ignorar = false;
        Promise.all([servicioApi.obtenerSeguidores(usuario.id), servicioApi.obtenerSeguidos(usuario.id)])
            .then(([listaSeguidores, listaSeguidos]) => {
                if (ignorar) return;
                setSeguidores(listaSeguidores);
                setIdsSeguidos(new Set(listaSeguidos.map((seguido) => seguido.id)));
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

    async function manejarAlternarSeguir(seguidor) {
        const yaLoSigo = idsSeguidos.has(seguidor.id);
        setErrorDeAccion(null);
        setIdOcupado(seguidor.id);
        try {
            if (yaLoSigo) await servicioApi.dejarDeSeguir(usuario.id, seguidor.id);
            else await servicioApi.seguir(usuario.id, seguidor.id);
            setIdsSeguidos((ids) => {
                const siguiente = new Set(ids);
                if (yaLoSigo) siguiente.delete(seguidor.id);
                else siguiente.add(seguidor.id);
                return siguiente;
            });
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
    else if (seguidores.length === 0) cuerpo = <p className="feed-message">Todavía no te sigue nadie.</p>;
    else cuerpo = (
        <>
            {errorDeAccion && <p className="feed-message feed-error" role="alert">{errorDeAccion}</p>}
            <div className="user-list">
                {seguidores.map((seguidor) => (
                    <UserItem
                        key={seguidor.id}
                        nombreUsuario={seguidor.username}
                        siguiendo={idsSeguidos.has(seguidor.id)}
                        ocupado={idOcupado === seguidor.id}
                        alAlternarSeguir={() => manejarAlternarSeguir(seguidor)}
                    />
                ))}
            </div>
        </>
    );

    return (
        <main className="content">
            <PageHeader titulo="Seguidores"/>
            {cuerpo}
        </main>
    )
}
