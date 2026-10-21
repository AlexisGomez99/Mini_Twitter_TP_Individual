import { useEffect, useState } from "react";
import { useParams } from "react-router";
import ListTweets from "./ListTweets.jsx";
import PageHeader from "../layout/PageHeader.jsx";
import apiService from "../../services/apiService.js";
import { useAuth } from "../../context/AuthContext.js";

export default function UserTweets(){
    const { id } = useParams();
    const { endSession } = useAuth();
    const [username, setUsername] = useState(null);
    const [error, setError] = useState(null);

    // Se pide el usuario para poner su nombre en el título y avisar si no existe.
    useEffect(() => {
        let ignore = false;
        apiService.getUserById(id)
            .then((info) => {
                if (!ignore) setUsername(info.username);
            })
            .catch((e) => {
                if (ignore) return;
                if (e.status === 401) endSession();
                else setError(e.message);
            });
        return () => { ignore = true; };
    }, [id, endSession]);

    return (
        <main className="content">
            <PageHeader title={username ?? "Usuario"}/>
            {error
                ? <p className="feed-message feed-error" role="alert">{error}</p>
                : <ListTweets scope="user" userId={id}/>}
        </main>
    )
}
