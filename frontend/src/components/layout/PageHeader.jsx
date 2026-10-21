import { useAuth } from "../../context/AuthContext.js";

export default function PageHeader({ title }){
    const { user } = useAuth();

    return (
        <header className="page-header">
            <h1>{title}</h1>
            <span className="page-user">@{user.username}</span>
        </header>
    )
}
