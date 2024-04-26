import {HalLink} from "./HalLink";
import Role from "./Role";

type UserLinks = {
    self: HalLink,
    user_roles: HalLink
}

type User = {
    id?: number,
    username: string,
    password?: string,
    roles?: string[],
    active: boolean,
    root?: boolean,
    created_at?: Date,
    last_updated_at?: Date,
    _links?: UserLinks,
    mappedRoles?: Role[]
}

export default User