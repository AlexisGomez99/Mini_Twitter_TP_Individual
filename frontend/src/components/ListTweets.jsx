import Tweet from "./Tweet.jsx";
import { getAllTweets } from "../data/tweets.js";

export default function ListTweets(){
    const tweets = getAllTweets();
    const tweetElements = [];

    for (const tweet of tweets) {
        tweetElements.push(<Tweet key={tweet.id} {...tweet}/>);
    }

    return (
        <section className="tweet-list">
            {tweetElements}
        </section>
    )
}
