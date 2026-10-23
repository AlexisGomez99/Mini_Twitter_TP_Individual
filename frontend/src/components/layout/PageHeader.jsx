import { useAutenticacion } from "../../context/AuthContext.js";

export default function PageHeader({ titulo }){
    const { usuario } = useAutenticacion();

    return (
        <header className="page-header">
            <h1>{titulo}</h1>
            <span className="page-user">@{usuario.username}</span>
        </header>
    )
}
