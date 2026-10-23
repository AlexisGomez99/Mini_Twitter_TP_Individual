import { useState } from "react";
import ListTweets from "./ListTweets.jsx";
import NewTweet from "./NewTweet.jsx";
import PageHeader from "../layout/PageHeader.jsx";

export default function Home(){
    // Cada vez que se publica un tweet este número sube y ListTweets vuelve a pedir el feed.
    const [claveRefresco, setClaveRefresco] = useState(0);

    return (
        <main className="content">
            <PageHeader titulo="Inicio"/>
            <NewTweet alPublicar={() => setClaveRefresco((clave) => clave + 1)}/>
            <ListTweets alcance="feed" claveRefresco={claveRefresco}/>
        </main>
    )
}
