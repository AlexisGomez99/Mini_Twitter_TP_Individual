import Tweet from "./Tweet.jsx";

const tweets = [
    {
        id: 1,
        nombre: "Alexis Gómez",
        contenido: "Este es mi primer tweet",
        fechaCreacion: "2026-09-18",
    },
    {
        id: 2,
        nombre: "María López",
        contenido: "Probando el mini Twitter que estamos armando para el TP",
        fechaCreacion: "2026-09-19",
    },
    {
        id: 3,
        nombre: "Juan Pérez",
        contenido: "React + backend en camino, todo pinta bien",
        fechaCreacion: "2026-09-20",
    },
];

export default function ListTweets(){

    return (
        <div className="list-tweets">
            {tweets.map((tweet) => (
                <Tweet
                    key={tweet.id}
                    nombre={tweet.nombre}
                    contenido={tweet.contenido}
                    fechaCreacion={tweet.fechaCreacion}
                />
            ))}
        </div>
    )
}
