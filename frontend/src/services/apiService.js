const BASE_URL = import.meta.env.VITE_API_URL ?? "http://localhost:8080";

export class ApiError extends Error {
    constructor(status, message) {
        super(message);
        this.status = status;
    }
}

async function request(path, { method = "GET", body } = {}) {
    let response;
    try {
        response = await fetch(`${BASE_URL}${path}`, {
            method,
            credentials: "include",
            headers: body ? { "Content-Type": "application/json" } : undefined,
            body: body ? JSON.stringify(body) : undefined,
        });
    } catch {
        throw new ApiError(0, "No se pudo conectar con el servidor");
    }

    const text = await response.text();
    const data = text ? JSON.parse(text) : null;

    if (!response.ok) {
        throw new ApiError(response.status, data?.message ?? "Error inesperado");
    }
    return data;
}

const apiService = {
    // --- Auth ---
    login: (username, password) => request("/login", { method: "POST", body: { username, password } }),
    register: (username, password) => request("/registrar", { method: "POST", body: { username, password } }),
    logout: () => request("/logout", { method: "POST" }),
    getMe: () => request("/me"),

    // --- Usuarios ---
    listUsers: () => request("/users"),
    getUserById: (id) => request(`/users/${id}`),
    getUserByUsername: (username) => request(`/users?username=${encodeURIComponent(username)}`),
    deleteUser: (id) => request(`/users/${id}`, { method: "DELETE" }),

    // --- Tweets ---
    createTweet: (userId, content) => request("/tweets", { method: "POST", body: { userId, content } }),
    getTweetsByUser: (userId) => request(`/tweets?userId=${userId}`),
    getTimeline: (userId) => request(`/tweets/timeline?userId=${userId}`),
    getFeed: () => request("/tweets/feed"),
    deleteTweet: (tweetId) => request(`/tweets/${tweetId}`, { method: "DELETE" }),

    // --- Retweets ---
    createRetweet: (userId, originTweetId) => request("/retweets", { method: "POST", body: { userId, originTweetId } }),
    getRetweetsByUser: (userId) => request(`/retweets?userId=${userId}`),
    deleteRetweet: (retweetId) => request(`/retweets/${retweetId}`, { method: "DELETE" }),

    // --- Follows ---
    follow: (followerId, followedId) => request("/follows", { method: "POST", body: { followerId, followedId } }),
    unfollow: (followerId, followedId) => request(`/follows?followerId=${followerId}&followedId=${followedId}`, { method: "DELETE" }),
    isFollowing: (followerId, followedId) => request(`/follows/is-following?followerId=${followerId}&followedId=${followedId}`),
    getFollowers: (userId) => request(`/follows/followers?userId=${userId}`),
    getFollowing: (userId) => request(`/follows/following?userId=${userId}`),
};

export default apiService;
