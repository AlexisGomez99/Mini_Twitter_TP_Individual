import { Navigate } from "react-router";
import { useAuth } from "../../context/AuthContext.js";

export default function ProtectedRoute({ children }){
    const { user, loading } = useAuth();

    if (loading) return null;

    return user ? children : <Navigate to="/login" replace/>
}
