import ListTweets from "./ListTweets.jsx";
import PageHeader from "../layout/PageHeader.jsx";

export default function MyTweets(){

    return (
        <main className="content">
            <PageHeader title="Mis tweets"/>
            <ListTweets scope="mine"/>
        </main>
    )
}
