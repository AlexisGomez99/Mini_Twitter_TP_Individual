import ListTweets from "./ListTweets.jsx";
import PageHeader from "../layout/PageHeader.jsx";

export default function MyTweets(){

    return (
        <main className="content">
            <PageHeader titulo="Mis tweets"/>
            <ListTweets alcance="mios"/>
        </main>
    )
}
