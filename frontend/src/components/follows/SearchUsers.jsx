import { useEffect, useState } from "react";
import PageHeader from "../layout/PageHeader.jsx";
import UserItem from "./UserItem.jsx";
import servicioApi from "../../services/apiService.js";
import { useAutenticacion } from "../../context/AuthContext.js";

export default function SearchUsers(){
    const { usuario, terminarSesion } = useAutenticacion();
    const [usuarios, setUsuarios] = useState([]);
    // Ids de las personas a las que yo sigo: define si cada resultado muestra "Seguir" o "Dejar de seguir".
    const [idsSeguidos, setIdsSeguidos] = useState(new Set());
    const [busqueda, setBusqueda] = useState("");
    const [cargando, setCargando] = useState(true);
    const [error, setError] = useState(null);
    const [errorDeAccion, setErrorDeAccion] = useState(null);
    const [idOcupado, setIdOcupado] = useState(null);

    // Se traen todos los usuarios una sola vez y se filtra en el front mientras se escribe:
    // el backend solo busca por nombre exacto (GET /users?username=).
    useEffect(() => {
        let ignorar = false;
        Promise.all([servicioApi.listarUsuarios(), servicioApi.obtenerSeguidos(usuario.id)])
            .then(([listaUsuarios, listaSeguidos]) => {
                if (ignorar) return;
                setUsuarios(listaUsuarios);
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

    async function manejarAlternarSeguir(encontrado) {
        const yaLoSigo = idsSeguidos.has(encontrado.id);
        setErrorDeAccion(null);
        setIdOcupado(encontrado.id);
        try {
            if (yaLoSigo) await servicioApi.dejarDeSeguir(usuario.id, encontrado.id);
            else await servicioApi.seguir(usuario.id, encontrado.id);
            setIdsSeguidos((ids) => {
                const siguiente = new Set(ids);
                if (yaLoSigo) siguiente.delete(encontrado.id);
                else siguiente.add(encontrado.id);
                return siguiente;
            });
        } catch (e) {
            if (e.estado === 401) terminarSesion();
            else setErrorDeAccion(e.message);
        } finally {
            setIdOcupado(null);
        }
    }

    const termino = busqueda.trim().toLowerCase();
    // No se ofrece seguirse a uno mismo.
    const resultados = termino === ""
        ? []
        : usuarios.filter((encontrado) => encontrado.id !== usuario.id && encontrado.username.toLowerCase().includes(termino));

    let cuerpo;
    if (cargando) cuerpo = <p className="feed-message">Cargando...</p>;
    else if (error) cuerpo = <p className="feed-message feed-error" role="alert">{error}</p>;
    else if (termino === "") cuerpo = <p className="feed-message">Escribí un nombre para buscar usuarios.</p>;
    else if (resultados.length === 0) cuerpo = <p className="feed-message">No se encontraron usuarios para "{busqueda.trim()}".</p>;
    else cuerpo = (
        <>
            {errorDeAccion && <p className="feed-message feed-error" role="alert">{errorDeAccion}</p>}
            <div className="user-list">
                {resultados.map((encontrado) => (
                    <UserItem
                        key={encontrado.id}
                        nombreUsuario={encontrado.username}
                        ruta={`/usuarios/${encontrado.id}`}
                        siguiendo={idsSeguidos.has(encontrado.id)}
                        ocupado={idOcupado === encontrado.id}
                        alAlternarSeguir={() => manejarAlternarSeguir(encontrado)}
                    />
                ))}
            </div>
        </>
    );

    return (
        <main className="content">
            <PageHeader titulo="Seguir a"/>
            <div className="search-box">
                <input
                    type="search"
                    className="search-input"
                    placeholder="Buscar usuarios por nombre"
                    aria-label="Buscar usuarios por nombre"
                    value={busqueda}
                    onChange={(e) => setBusqueda(e.target.value)}
                    autoFocus
                />
            </div>
            {cuerpo}
        </main>
    )
}
