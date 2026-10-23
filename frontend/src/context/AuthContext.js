import { createContext, useContext } from "react";

export const AuthContext = createContext(null);

export function useAutenticacion() {
    return useContext(AuthContext);
}
