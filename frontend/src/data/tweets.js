const minutesAgo = (minutes) => new Date(Date.now() - minutes * 60 * 1000).toISOString();
const hoursAgo = (hours) => minutesAgo(hours * 60);
const daysAgo = (days) => hoursAgo(days * 24);

// Simula la respuesta del backend (OriginalTweetInfo), del más nuevo al más viejo,
// igual que ordena el repositorio de tweets. Las fechas son relativas a "ahora"
// para que el demo siempre se vea reciente.
const tweets = [
    {
        id: 7,
        authorUsername: "jperez",
        content: "Alguien más piensa que los lunes deberían empezar a las 11?",
        createdAt: minutesAgo(4),
    },
    {
        id: 6,
        authorUsername: "agomez",
        content: "Terminé la capa web del backend. Ahora toca el frontend.",
        createdAt: minutesAgo(37),
    },
    {
        id: 5,
        authorUsername: "Alexis",
        content: "Probando el login con JWT: la cookie token ya viaja en cada request y los endpoints responden 400 si no estás autenticado.",
        createdAt: hoursAgo(2),
    },
    {
        id: 4,
        authorUsername: "mlopez_dev",
        content: "Tip del día: un retweet no debería romperse cuando se borra el tweet original. Soft delete > DELETE físico.",
        createdAt: hoursAgo(5),
    },
    {
        id: 3,
        authorUsername: "sofi_codea",
        content: "Hoy aprendí que un @OneToMany sin mappedBy crea una tabla intermedia que nadie pidió.",
        createdAt: hoursAgo(26),
    },
    {
        id: 2,
        authorUsername: "agomez",
        content: "Migré todo el proyecto a Java 24 y los 130 tests siguen en verde. Cobertura arriba del 90% y JaCoCo contento, que es lo importante.",
        createdAt: daysAgo(3),
    },
    {
        id: 1,
        authorUsername: "Alexis",
        content: "Este es mi primer tweet",
        createdAt: daysAgo(9),
    },
];

export function getAllTweets() {
    return tweets;
}
