const hueFor = (username) => [...username].reduce((sum, char) => sum + char.charCodeAt(0), 0) % 360;

export default function Avatar({ username }){

    return (
        <div
            className="avatar"
            style={{ background: `hsl(${hueFor(username)} 55% 42%)` }}
            aria-hidden="true"
        >
            {username[0].toUpperCase()}
        </div>
    )
}
