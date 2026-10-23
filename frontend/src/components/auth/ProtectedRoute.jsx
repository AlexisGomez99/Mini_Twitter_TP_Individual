import { Navigate } from "react-router";
import { useAutenticacion } from "../../context/AuthContext.js";

export default function ProtectedRoute({ children }){
    const { usuario, cargando } = useAutenticacion();

    if (cargando) return null;

    return usuario ? children : <Navigate to="/login" replace/>
}
