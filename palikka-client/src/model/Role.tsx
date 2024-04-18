import Privilege from "./Privilege";

interface Role {
    id: number,
    name: string,
    description?: string,
    privileges: Privilege[]
}

export default Role;