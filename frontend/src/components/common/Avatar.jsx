const tonoPara = (nombreUsuario) => [...nombreUsuario].reduce((suma, caracter) => suma + caracter.charCodeAt(0), 0) % 360;

export default function Avatar({ nombreUsuario }){

    return (
        <div
            className="avatar"
            style={{ background: `hsl(${tonoPara(nombreUsuario)} 55% 42%)` }}
            aria-hidden="true"
        >
            {nombreUsuario[0].toUpperCase()}
        </div>
    )
}
