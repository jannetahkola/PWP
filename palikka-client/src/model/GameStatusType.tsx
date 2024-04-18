type Players = {
    max: number
    online: number
}

type GameStatusType = {
    online: boolean
    version?: string
    description?: string
    motd?: string
    players?: Players
    favicon?: string
    host?: string
    port?: number
}

export default GameStatusType;