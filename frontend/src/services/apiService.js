const URL_BASE = "http://localhost:8080";

export class ApiError extends Error {
    constructor(estado, mensaje) {
        super(mensaje);
        this.estado = estado;
    }
}

async function pedir(ruta, { metodo = "GET", cuerpo } = {}) {
    let respuesta;
    try {
        respuesta = await fetch(`${URL_BASE}${ruta}`, {
            method: metodo,
            credentials: "include",
            headers: cuerpo ? { "Content-Type": "application/json" } : undefined,
            body: cuerpo ? JSON.stringify(cuerpo) : undefined,
        });
    } catch {
        throw new ApiError(0, "No se pudo conectar con el servidor");
    }

    const texto = await respuesta.text();
    const datos = texto ? JSON.parse(texto) : null;

    if (!respuesta.ok) {
        throw new ApiError(respuesta.status, datos?.message ?? "Error inesperado");
    }
    return datos;
}

const servicioApi = {
    // --- Auth ---
    iniciarSesion: (nombreUsuario, contrasenia) =>
        pedir("/login", { metodo: "POST", cuerpo: { username: nombreUsuario, password: contrasenia } }),
    registrar: (nombreUsuario, contrasenia) =>
        pedir("/registrar", { metodo: "POST", cuerpo: { username: nombreUsuario, password: contrasenia } }),
    cerrarSesion: () => pedir("/logout", { metodo: "POST" }),
    obtenerYo: () => pedir("/me"),

    // --- Usuarios ---
    listarUsuarios: () => pedir("/users"),
    obtenerUsuarioPorId: (id) => pedir(`/users/${id}`),
    obtenerUsuarioPorNombre: (nombreUsuario) => pedir(`/users?username=${encodeURIComponent(nombreUsuario)}`),
    eliminarUsuario: (id) => pedir(`/users/${id}`, { metodo: "DELETE" }),

    // --- Tweets ---
    crearTweet: (contenido) =>
        pedir("/tweets", { metodo: "POST", cuerpo: { content: contenido } }),
    // Los tweets y el feed vienen paginados: la página empieza en 0 y la respuesta es
    // { content, page, totalPages, totalElements, hasNext }.
    obtenerTweetsDelUsuario: (idUsuario, pagina = 0) => pedir(`/tweets?userId=${idUsuario}&page=${pagina}`),
    obtenerFeed: (pagina = 0) => pedir(`/tweets/feed?page=${pagina}`),
    eliminarTweet: (idTweet) => pedir(`/tweets/${idTweet}`, { metodo: "DELETE" }),

    // --- Retweets ---
    crearRetweet: (idTweetOrigen) =>
        pedir("/retweets", { metodo: "POST", cuerpo: { originTweetId: idTweetOrigen } }),
    obtenerRetweetsDelUsuario: (idUsuario) => pedir(`/retweets?userId=${idUsuario}`),
    eliminarRetweet: (idRetweet) => pedir(`/retweets/${idRetweet}`, { metodo: "DELETE" }),

    // --- Follows ---
    seguir: (idSeguido) =>
        pedir("/follows", { metodo: "POST", cuerpo: { followedId: idSeguido } }),
    dejarDeSeguir: (idSeguido) =>
        pedir(`/follows?followedId=${idSeguido}`, { metodo: "DELETE" }),
    estaSiguiendo: (idSeguidor, idSeguido) =>
        pedir(`/follows/is-following?followerId=${idSeguidor}&followedId=${idSeguido}`),
    obtenerSeguidores: (idUsuario) => pedir(`/follows/followers?userId=${idUsuario}`),
    obtenerSeguidos: (idUsuario) => pedir(`/follows/following?userId=${idUsuario}`),
};

export default servicioApi;
