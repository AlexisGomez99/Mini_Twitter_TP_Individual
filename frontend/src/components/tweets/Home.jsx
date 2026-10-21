import { useState } from "react";
import ListTweets from "./ListTweets.jsx";
import NewTweet from "./NewTweet.jsx";
import PageHeader from "../layout/PageHeader.jsx";

export default function Home(){
    // Cada vez que se publica un tweet este número sube y ListTweets vuelve a pedir el feed.
    const [refreshKey, setRefreshKey] = useState(0);

    return (
        <main className="content">
            <PageHeader title="Inicio"/>
            <NewTweet onPublished={() => setRefreshKey((key) => key + 1)}/>
            <ListTweets scope="feed" refreshKey={refreshKey}/>
        </main>
    )
}
