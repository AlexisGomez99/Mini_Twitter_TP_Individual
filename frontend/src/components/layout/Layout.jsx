import { Outlet } from "react-router";
import Sidebar from "./Sidebar.jsx";

export default function Layout(){

    return (
        <div className="app-shell">
            <Sidebar/>
            <Outlet/>
        </div>
    )
}
