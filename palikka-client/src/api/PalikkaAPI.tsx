import Config from "../config";
import GameProcessControlRequestType from "../model/GameProcessControlRequestType";
import User from "../model/User";
import {NewUser} from "../component/users/NewUserDialog";

const urls = Config.urls;

const PalikkaAPI = {
    users: {
        getUsers: async (token: string): Promise<Response> => {
            return await fetch(`${urls.usersApi}/users`, {
                headers: {
                    'Authorization': 'Bearer ' + token
                }
            });
        },
        getCurrentUser: async (token: string): Promise<Response> => {
            return await fetch(`${urls.usersApi}/current-user`, {
                headers: {
                    'Authorization': 'Bearer ' + token
                }
            });
        },
        createUser: async (token: string, user: NewUser): Promise<Response> => {
            return await fetch(`${urls.usersApi}/users`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': 'Bearer ' + token
                },
                body: JSON.stringify(user)
            });
        },
        updateUser: async (token: string, user: User) => {
            return await fetch(`${urls.usersApi}/users/${user.id}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': 'Bearer ' + token
                },
                body: JSON.stringify(user)
            });
        },
        login: async (username: string, password: string): Promise<Response> => {
            const body = {
                username: username,
                password: password
            }
            return await fetch(`${urls.usersApi}/auth/login`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(body)
            });
        },
        roles: {
            createUserRoleAssociation: async (token: string, userId: number, roleId: number): Promise<Response> => {
                return await fetch(`${urls.usersApi}/users/${userId}/roles`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': 'Bearer ' + token
                    },
                    body: JSON.stringify({
                        role_id: roleId
                    })
                });
            },
            deleteUserRoleAssociation: async (token: string, userId: number, roleId: number): Promise<Response> => {
                return await fetch(`${urls.usersApi}/users/${userId}/roles/${roleId}`, {
                    method: 'DELETE',
                    headers: {
                        'Authorization': 'Bearer ' + token
                    }
                });
            }
        }
    },
    roles: {
        getRoles: async (token: string): Promise<Response> => {
            return await fetch(`${urls.usersApi}/roles`, {
                headers: {
                    'Authorization': 'Bearer ' + token
                }
            });
        },
        createRolePrivilegeAssociation: async (token: string, roleId: number, privilegeId: number): Promise<Response> => {
            return await fetch(`${urls.usersApi}/roles/${roleId}/privileges`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': 'Bearer ' + token
                },
                body: JSON.stringify({
                    privilege_id: privilegeId
                })
            })
        },
        deleteRolePrivilegeAssociation: async (token: string, roleId: number, privilegeId: number): Promise<Response> => {
            return await fetch(`${urls.usersApi}/roles/${roleId}/privileges/${privilegeId}`, {
                method: 'DELETE',
                headers: {
                    'Authorization': 'Bearer ' + token
                }
            })
        }
    },
    privileges: {
        getPrivileges: async (token: string): Promise<Response> => {
            return await fetch(`${urls.usersApi}/privileges`, {
                headers: {
                    'Authorization': 'Bearer ' + token
                }
            })
        }
    },
    game: {
        status: async (token: string): Promise<Response> => {
            return await fetch(`${urls.gameApi}/game/status`, {
                headers: {
                    'Authorization': 'Bearer ' + token
                }
            });
        },
        process: {
            control: async (token: string, request: GameProcessControlRequestType): Promise<Response> => {
                return await fetch(`${urls.gameApi}/game/process`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': 'Bearer ' + token
                    },
                    body: JSON.stringify(request)
                });
            },
            status: async (token: string): Promise<Response> => {
                return await fetch(`${urls.gameApi}/game/process`, {
                    headers: {
                        'Authorization': 'Bearer ' + token
                    }
                });
            }
        },
        file: {
            download: async (token: string, request: any): Promise<Response> => {
                return await fetch(`${urls.gameApi}/game/files/executable/download`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': 'Bearer ' + token
                    },
                    body: JSON.stringify(request)
                });
            },
            downloadStatus: async (token: string): Promise<Response> => {
                return await fetch(`${urls.gameApi}/game/files/executable/download`, {
                    headers: {
                        'Authorization': 'Bearer ' + token
                    }
                });
            },
            executable: {
                meta: async (token: string): Promise<Response> => {
                    return await fetch(`${urls.gameApi}/game/files/executable/meta`, {
                        headers: {
                            'Authorization': 'Bearer ' + token
                        }
                    });
                }
            },
            config: async (token: string): Promise<Response> => {
                return await fetch(`${urls.gameApi}/game/files/config`, {
                    headers: {
                        'Authorization': 'Bearer ' + token
                    }
                });
            }
        }
    }
}

export default PalikkaAPI;