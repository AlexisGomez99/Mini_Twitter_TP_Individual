import { useCallback, useEffect, useRef, useState } from "react";
import Tweet from "./Tweet.jsx";
import servicioApi from "../../services/apiService.js";
import { useAutenticacion } from "../../context/AuthContext.js";

// "feed": tweets propios y de la gente que sigo. "mios": solo mis tweets.
// "usuario": solo los tweets de otra persona (se indica con la prop idUsuario).
const MENSAJES_VACIO = {
    feed: "Todavía no hay tweets en tu inicio. Publicá el primero o seguí a alguien.",
    mios: "Todavía no publicaste ningún tweet.",
    usuario: "Este usuario todavía no publicó ningún tweet.",
};

export default function ListTweets({ alcance = "feed", idUsuario, claveRefresco }){
    const { usuario, terminarSesion } = useAutenticacion();
    // Los tweets ya cargados, de todas las páginas, uno detrás de otro.
    const [tweets, setTweets] = useState([]);
    // Última página cargada (empieza en 0) y si el backend dice que hay otra después.
    const [pagina, setPagina] = useState(0);
    const [haySiguiente, setHaySiguiente] = useState(false);
    const [cargando, setCargando] = useState(true);
    const [cargandoMas, setCargandoMas] = useState(false);
    const [error, setError] = useState(null);
    const [errorAlCargarMas, setErrorAlCargarMas] = useState(null);
    const [errorDeAccion, setErrorDeAccion] = useState(null);
    // Elemento invisible al final de la lista: cuando aparece en pantalla se pide la página siguiente.
    const centinelaRef = useRef(null);
    // Sube cada vez que la lista se reinicia; sirve para descartar la respuesta de una
    // "página siguiente" pedida antes del reinicio (si no, se pegaría a la lista nueva).
    const generacionRef = useRef(0);
    // Mis retweets: id del tweet original -> id de mi retweet. Sirve para saber qué botones
    // de retwittear están activos y para poder deshacer el retweet.
    const [misRetweets, setMisRetweets] = useState(new Map());
    // Tweet cuyo botón de retwittear está esperando respuesta.
    const [idRetuiteando, setIdRetuiteando] = useState(null);

    // Pide mis retweets al backend y los devuelve como Map (originalTweetId -> id del retweet).
    const pedirMisRetweets = useCallback(async () => {
        const lista = await servicioApi.obtenerRetweetsDelUsuario(usuario.id);
        return new Map(lista.map((retweet) => [retweet.originalTweetId, retweet.id]));
    }, [usuario.id]);

    useEffect(() => {
        let ignorar = false;
        pedirMisRetweets()
            .then((retweets) => { if (!ignorar) setMisRetweets(retweets); })
            .catch((e) => { if (!ignorar && e.estado === 401) terminarSesion(); });
        return () => { ignorar = true; };
    }, [terminarSesion, pedirMisRetweets]);

    // Pide una página del listado que corresponde al alcance.
    const pedirPagina = useCallback((numeroPagina) => (
        alcance === "feed"
            ? servicioApi.obtenerFeed(numeroPagina)
            : servicioApi.obtenerTweetsDelUsuario(alcance === "usuario" ? idUsuario : usuario.id, numeroPagina)
    ), [alcance, idUsuario, usuario.id]);

    // Carga inicial: siempre la página 0 (los tweets más nuevos). Se repite cuando cambia
    // el listado (alcance/idUsuario) o cuando se publica un tweet (claveRefresco), y en ese caso
    // la lista vuelve a empezar desde arriba, descartando las páginas ya cargadas.
    useEffect(() => {
        // Si el efecto se vuelve a ejecutar antes de que llegue la respuesta anterior
        // (por ejemplo, dos tweets seguidos), se descarta la vieja para que no pise a la nueva.
        let ignorar = false;
        generacionRef.current += 1;
        pedirPagina(0)
            .then((resultado) => {
                if (ignorar) return;
                setTweets(resultado.content);
                setPagina(0);
                setHaySiguiente(resultado.hasNext);
                setCargandoMas(false);
                setError(null);
                setErrorAlCargarMas(null);
                setErrorDeAccion(null);
                setCargando(false);
            })
            .catch((e) => {
                if (ignorar) return;
                if (e.estado === 401) terminarSesion();
                else setError(e.message);
                setCargando(false);
            });
        return () => { ignorar = true; };
    }, [terminarSesion, pedirPagina, claveRefresco]);

    // Pide la página siguiente y la agrega al final de la lista.
    const cargarMas = useCallback(async () => {
        const generacion = generacionRef.current;
        setCargandoMas(true);
        setErrorAlCargarMas(null);
        try {
            const resultado = await pedirPagina(pagina + 1);
            if (generacion !== generacionRef.current) return;
            setTweets((actuales) => [...actuales, ...resultado.content]);
            setPagina(pagina + 1);
            setHaySiguiente(resultado.hasNext);
        } catch (e) {
            if (generacion !== generacionRef.current) return;
            if (e.estado === 401) terminarSesion();
            else setErrorAlCargarMas(e.message);
        } finally {
            if (generacion === generacionRef.current) setCargandoMas(false);
        }
    }, [terminarSesion, pedirPagina, pagina]);

    // Scroll infinito: IntersectionObserver avisa cuando el elemento del final se acerca
    // a la pantalla (rootMargin: empieza a pedir 300px antes de llegar). Se vuelve a crear
    // después de cada página: si la lista todavía no llena la pantalla, el centinela sigue
    // visible y se pide otra enseguida. Si falló una página no se pide sola (hay un botón).
    useEffect(() => {
        const centinela = centinelaRef.current;
        if (!centinela || !haySiguiente || cargandoMas || errorAlCargarMas) return;
        const observador = new IntersectionObserver(
            ([entrada]) => { if (entrada.isIntersecting) cargarMas(); },
            { rootMargin: "300px" },
        );
        observador.observe(centinela);
        return () => observador.disconnect();
    }, [haySiguiente, cargandoMas, errorAlCargarMas, cargarMas, tweets]);

    // Cuando la lista cambia en el backend (se borró un tweet, o apareció/desapareció un
    // retweet mío), todo lo que viene después se corre de lugar. Para no saltearse ni repetir
    // ningún tweet al seguir bajando, se vuelven a pedir las páginas que ya estaban cargadas
    // (de la 0 a la actual) y se reemplaza la lista, sin perder el lugar en la pantalla.
    async function recargarPaginasCargadas() {
        const resultados = await Promise.all(
            Array.from({ length: pagina + 1 }, (_, numero) => pedirPagina(numero)),
        );
        setTweets(resultados.flatMap((resultado) => resultado.content));
        setHaySiguiente(resultados[resultados.length - 1].hasNext);
    }

    async function manejarEliminar(idTweet) {
        if (!window.confirm("¿Querés eliminar este tweet?")) return;
        setErrorDeAccion(null);
        try {
            await servicioApi.eliminarTweet(idTweet);
            await recargarPaginasCargadas();
        } catch (e) {
            if (e.estado === 401) terminarSesion();
            else setErrorDeAccion(e.message);
        }
    }

    // Retwittear / deshacer el retweet. El backend deja retwittear el mismo tweet más de una
    // vez, así que el botón funciona como un interruptor: si ya hay un retweet mío de este
    // tweet lo borra, y si no lo crea.
    async function manejarAlternarRetweet(idTweet) {
        setErrorDeAccion(null);
        setIdRetuiteando(idTweet);
        try {
            const idRetweet = misRetweets.get(idTweet);
            if (idRetweet === undefined) await servicioApi.crearRetweet(usuario.id, idTweet);
            else await servicioApi.eliminarRetweet(idRetweet);
            // POST /retweets no devuelve el retweet creado, por eso se vuelven a pedir los míos.
            setMisRetweets(await pedirMisRetweets());
            // En el inicio, mi retweet aparece (o desaparece) arriba de todo.
            if (alcance === "feed") await recargarPaginasCargadas();
        } catch (e) {
            if (e.estado === 401) terminarSesion();
            else setErrorDeAccion(e.message);
        } finally {
            setIdRetuiteando(null);
        }
    }

    if (cargando) return <p className="feed-message">Cargando...</p>
    if (error) return <p className="feed-message feed-error" role="alert">{error}</p>
    if (tweets.length === 0) return <p className="feed-message">{MENSAJES_VACIO[alcance]}</p>

    return (
        <>
            {errorDeAccion && <p className="feed-message feed-error" role="alert">{errorDeAccion}</p>}
            <div className="tweet-list">
                {tweets.map((tweet) =>
                    // Un retweet trae originalTweetId; un tweet común no.
                    tweet.originalTweetId === undefined ? (
                        <Tweet
                            key={tweet.id}
                            autor={tweet.authorUsername}
                            contenido={tweet.content}
                            fecha={tweet.createdAt}
                            alEliminar={tweet.authorUsername === usuario.username ? () => manejarEliminar(tweet.id) : undefined}
                            mostrarAcciones
                            puedeRetuitear={tweet.authorUsername !== usuario.username}
                            retuiteado={misRetweets.has(tweet.id)}
                            retuiteoOcupado={idRetuiteando === tweet.id}
                            alAlternarRetweet={() => manejarAlternarRetweet(tweet.id)}
                        />
                    ) : (
                        <Tweet
                            key={tweet.id}
                            retuiteadoPor={tweet.authorUsername}
                            autor={tweet.originalAuthorUsername}
                            contenido={tweet.originalContent}
                            noDisponible={tweet.originalDeleted}
                            fecha={tweet.createdAt}
                        />
                    )
                )}
            </div>
            {cargandoMas && <p className="feed-message">Cargando más...</p>}
            {errorAlCargarMas && (
                <div className="feed-more-error">
                    <p className="feed-message feed-error" role="alert">{errorAlCargarMas}</p>
                    <button type="button" className="feed-retry" onClick={cargarMas}>Reintentar</button>
                </div>
            )}
            {!haySiguiente && pagina > 0 && <p className="feed-message">No hay más tweets.</p>}
            <div ref={centinelaRef} className="feed-sentinel"/>
        </>
    )
}
