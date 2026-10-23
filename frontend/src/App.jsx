import { Navigate, Route, Routes } from "react-router";
import Login from "./components/auth/Login.jsx";
import ProtectedRoute from "./components/auth/ProtectedRoute.jsx";
import Register from "./components/auth/Register.jsx";
import Followers from "./components/follows/Followers.jsx";
import Following from "./components/follows/Following.jsx";
import SearchUsers from "./components/follows/SearchUsers.jsx";
import Layout from "./components/layout/Layout.jsx";
import Home from "./components/tweets/Home.jsx";
import MyTweets from "./components/tweets/MyTweets.jsx";
import UserTweets from "./components/tweets/UserTweets.jsx";

export default function App(){

    return (
        <Routes>
            <Route path="/login" element={<Login/>}/>
            <Route path="/registro" element={<Register/>}/>
            <Route element={<ProtectedRoute><Layout/></ProtectedRoute>}>
                <Route path="/" element={<Home/>}/>
                <Route path="/mis-tweets" element={<MyTweets/>}/>
                <Route path="/seguidores" element={<Followers/>}/>
                <Route path="/seguidos" element={<Following/>}/>
                <Route path="/buscar" element={<SearchUsers/>}/>
                <Route path="/usuarios/:id" element={<UserTweets/>}/>
            </Route>
            <Route path="*" element={<Navigate to="/" replace/>}/>
        </Routes>
    )
}
