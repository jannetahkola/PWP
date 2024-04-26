import {
    Accordion, AccordionDetails, AccordionSummary,
    Alert,
    Button, Chip, Container, Divider,
    FormControlLabel,
    Grid,
    IconButton,
    Paper,
    Snackbar,
    Switch,
    Tooltip,
    Typography
} from "@mui/material";
import PalikkaAPI from "../../api/PalikkaAPI";
import User from "../../model/User";
import React, {useEffect, useState} from "react";
import {useAuthContext} from "../../context/AuthContext";
import {Add, AdminPanelSettings, ArrowDropDown, Edit} from "@mui/icons-material";
import Role from "../../model/Role";
import {HalLink} from "../../model/HalLink";
import Privilege from "../../model/Privilege";
import {AlertDialogProps} from "./AlertDialog";
import {ListDialog, ListDialogProps} from "./ListDialog";
import {NewUser, NewUserDialog, NewUserDialogProps} from "./NewUserDialog";
import {EditUserDialog, EditUserDialogProps} from "./EditUserDialog";

const mapPrivilegesFromRole = (role: Role): [string, Privilege[]][] => {
    let privilegesByDomain = new Map<string, Privilege[]>();
    role.privileges?.filter((privilege, index, self) => self.indexOf(privilege) === index)
        .forEach((privilege, index, self) => {
            if (privilegesByDomain.has(privilege.domain)) {
                privilegesByDomain.get(privilege.domain)?.push(privilege);
            } else {
                privilegesByDomain.set(privilege.domain, [privilege]);
            }
        });
    return Array.from(privilegesByDomain);
}

const mapPrivilegesFromRoles = (roles?: Role[]): [string, Privilege[]][] => {
    let privilegesByDomain = new Map<string, Privilege[]>();

    roles?.forEach((userRole, userRoleIndex, self) => {
        userRole.privileges?.filter((privilege, index, self) => self.indexOf(privilege) === index)
            .forEach((privilege, index, self) => {
                if (privilegesByDomain.has(privilege.domain)) {
                    privilegesByDomain.get(privilege.domain)?.push(privilege);
                } else {
                    privilegesByDomain.set(privilege.domain, [privilege]);
                }
            });
    });

    return Array.from(privilegesByDomain);
}

type UsersLinks = {
    self: HalLink,
    user: HalLink
}

type Users = {
    users: User[]
}

type UsersCollection = {
    _embedded: Users,
    _links: UsersLinks
}

type RolesLinks = {
    self: HalLink,
    role: HalLink
}

type Roles = {
    roles: Role[]
}

type Privileges = {
    privileges: Privilege[]
}

type PrivilegesLinks = {
    self: HalLink
}

type RolesCollection = {
    _embedded?: Roles,
    _links: RolesLinks
}

type PrivilegesCollection = {
    _embedded: Privileges,
    _links: PrivilegesLinks
}

async function getUsers(token: string): Promise<UsersCollection> {
    return await PalikkaAPI.users.getUsers(token)
        .then(async (res) => {
            if (res.ok) {
                let json = await res.json();
                return json as UsersCollection
            }
            return Promise.reject(Error('Failed to get users, status ' + res.status));
        })
        .catch(e => Promise.reject(e.message));
}

async function getRoles(token: string): Promise<RolesCollection> {
    return await PalikkaAPI.roles.getRoles(token)
        .then(async (res) => {
            if (res.ok) {
                return await res.json() as RolesCollection;
            }
            return Promise.reject();
        })
        .catch(_ => Promise.reject());
}

async function getPrivileges(token: string): Promise<PrivilegesCollection> {
    return await PalikkaAPI.privileges.getPrivileges(token)
        .then(async (res) => {
            if (res.ok) {
                return await res.json() as PrivilegesCollection;
            }
            return Promise.reject(Error('Failed to get privileges, status ' + res.status));
        })
        .catch(e => Promise.reject(Error(e.message)));
}

async function createUserRequest(token: string, user: NewUser): Promise<void> {
    return await PalikkaAPI.users.createUser(token, user)
        .then(async (res) => {
            if (res.ok) {
                return Promise.resolve();
            }
            let error;
            if (res.status === 409) {
                error = Error('Failed to create user - username already exists');
            } else {
                error = Error('Failed to create user, status ' + res.status);
            }
            return Promise.reject(error);
        })
        .catch(e => Promise.reject(Error(e.message)));
}

async function updateUserRequest(token: string, user: User): Promise<void> {
    return await PalikkaAPI.users.updateUser(token, user)
        .then(res => {
            if (res.ok) return Promise.resolve();
            return Promise.reject(Error('Update failed'));
        })
        .catch(_ => Promise.reject(Error('Update failed')));
}

async function createUserRoleAssociationRequest(token: string, userId: number, roleId: number): Promise<Role[]> {
    return PalikkaAPI.users.roles.createUserRoleAssociation(token, userId, roleId)
        .then(async (res) => {
            if (res.ok) {
                return await res.json() as Role[];
            }
            return Promise.reject(Error("Update failed, status " + res.status));
        })
        .catch((e) => Promise.reject(Error("Update failed - " + e.message)));
}

async function deleteUserRoleAssociationRequest(token: string, userId: number, roleId: number): Promise<void> {
    return PalikkaAPI.users.roles.deleteUserRoleAssociation(token, userId, roleId)
        .then(async (res) => {
            if (res.ok) {
                return Promise.resolve();
            }
            return Promise.reject(Error("Delete failed, status " + res.status));
        })
        .catch((e) => Promise.reject(Error("Delete failed - " + e.message)));
}

async function createRolePrivilegeAssociationRequest(token: string, roleId: number, privilegeId: number): Promise<PrivilegesCollection> {
    return PalikkaAPI.roles.createRolePrivilegeAssociation(token, roleId, privilegeId)
        .then(async (res) => {
            if (res.ok) {
                return await res.json() as PrivilegesCollection;
            }
            return Promise.reject(Error("Update failed, status " + res.status));
        })
        .catch((e) => Promise.reject(Error("Update failed - " + e.message)));
}

async function deleteRolePrivilegeAssociationRequest(token: string, roleId: number, privilegeId: number): Promise<void> {
    return PalikkaAPI.roles.deleteRolePrivilegeAssociation(token, roleId, privilegeId)
        .then(async (res) => {
            if (res.ok) {
                return Promise.resolve();
            }
            return Promise.reject(Error("Delete failed, status " + res.status));
        })
        .catch((e) => Promise.reject(Error("Delete failed - " + e.message)));
}

function UserManagementPage() {
    const { token, user, setUser } = useAuthContext();

    const [ currentUserRoles, setCurrentUserRoles ] = useState<string[]>([]);
    const [errorMessage, setErrorMessage] = useState<string>('');
    const [successMessage, setSuccessMessage] = useState<string>('');

    const [users, setUsers] = useState<null | User[]>(null);
    const [roles, setRoles] = useState<null | Role[]>(null);
    const [privileges, setPrivileges] = useState<null | Privilege[]>(null);

    const [loading, setLoading] = useState<boolean>(false);

    const [dialogProps, setDialogProps] = useState<AlertDialogProps>({ open: false, titleText: '', contentText: '' });
    const [listDialogProps, setListDialogProps] = useState<ListDialogProps>({ open: false, titleText: '', listItems: [] });
    const [newUserDialogProps, setNewUserDialogProps] = useState<NewUserDialogProps>({ open: false });
    const [editUserDialogProps, setEditUserDialogProps] = useState<EditUserDialogProps>({ open: false, user: null });

    const refreshUsers = async (): Promise<void> => {
        let usersCollection = await getUsers(token!)
            .catch(e => {
                // ignore
            });
        if (!usersCollection) {
            // mock users collection to only include the current user
            usersCollection = {
                _embedded: {
                    users: [
                        user!
                    ]
                },
                _links: {
                    self: {
                        href: ''
                    },
                    user: {
                        href: ''
                    }
                }
            }
        }
        let usersWithMappedRoles = await Promise.all(
            usersCollection._embedded.users.map(async (user, idx, self) => {
                return fetch(user._links!.user_roles.href, {
                    headers: { 'Authorization': 'Bearer ' + token }
                }).then(async (res) => {
                    if (res.ok) {
                        let roles = await res.json() as RolesCollection;
                        user.mappedRoles = roles._embedded?.roles ?? [];
                    }
                    return user;
                });
            }));
        setUsers(usersWithMappedRoles);
    }

    const refreshRoles = async (): Promise<void> => {
        return await getRoles(token!)
            .then(rolesCollection => setRoles(rolesCollection._embedded?.roles ?? []))
            .catch(_ => {
                // ignore
            });
    }

    const refreshPrivileges = async (): Promise<void> => {
        return await getPrivileges(token!)
            .then(privilegesCollection => setPrivileges(privilegesCollection._embedded.privileges))
            .catch(_ => {
                // ignore
            });
    }

    const createUser = (user: NewUser) => {
        createUserRequest(token!, user)
            .then(async (_) => {
                await refreshUsers();
                setSuccessMessage('User created successfully');
            })
            .catch((e) => setErrorMessage(e.message));
    }

    const updateUser = (updatedUser: User) => {
        updateUserRequest(token!, updatedUser)
            .then(async (_) => {
                await PalikkaAPI.users.getCurrentUser(token!)
                    .then(async (res) => {
                        if (res.ok) {
                            setUser((await res.json()) as User);
                            // Refresh after current user in case the fallback logic in "refreshUsers" is applied
                            // todo not updated, fix
                            await refreshUsers();
                        }
                    }).catch(_ => console.error("Failed to fetch current user after update"));
                setSuccessMessage("User updated successfully");
            })
            .catch(e => setErrorMessage(e.message));
    }

    const createUserRoleAssociation = (userId: number, roleId: number) => {
        createUserRoleAssociationRequest(token!, userId, roleId)
            .then(async (_) => {
                await refreshUsers();
                setSuccessMessage(`Role added to user successfully`);
            })
            .catch((e) => setErrorMessage(e.message));
    }

    const deleteUserRoleAssociation = (userId: number, roleId: number) => {
        deleteUserRoleAssociationRequest(token!, userId, roleId)
            .then(async (_) => {
                await refreshUsers();
                setSuccessMessage(`Role deleted from user successfully`);
            })
            .catch((e) => setErrorMessage(e.message));
    }

    const toggleUserActiveStatus = (user: User): Promise<void> => {
        user.active = !user.active;
        return updateUserRequest(token!, user)
            .then(async (_) => {
                await refreshRoles();
                await refreshUsers();
                setSuccessMessage(
                    `User ${ user.active ? "activated" : "deactivated" } successfully`);
            })
            .catch(e => refreshUsers().then(_ => setErrorMessage(e.message)));
    }

    const createRolePrivilegeAssociation = (roleId: number, privilegeId: number) => {
        createRolePrivilegeAssociationRequest(token!, roleId, privilegeId)
            .then(async (res) => {
                // todo update the roles here since we have the updated privileges already?
                await refreshRoles();
                await refreshUsers();
                setSuccessMessage(`Privilege added to role successfully`);
            })
            .catch(e => setErrorMessage(e.message));
    }

    const deleteRolePrivilegeAssociation = (roleId: number, privilegeId: number) => {
        deleteRolePrivilegeAssociationRequest(token!, roleId, privilegeId)
            .then(async (res) => {
                await refreshRoles();
                await refreshUsers();
                setSuccessMessage(`Privilege deleted from role successfully`);
            })
            .catch(e => setErrorMessage(e.message));
    }

    const isAdmin = (): boolean => currentUserRoles.includes("ROLE_ADMIN");

    useEffect(() => {
        fetch(user!._links!.user_roles.href, {
            headers: { 'Authorization': 'Bearer ' + token }
        }).then(async (res) => {
            if (res.ok) {
                let roles = await res.json() as RolesCollection;
                let roleNames = roles._embedded?.roles.map(role => role.name);
                setCurrentUserRoles(roleNames ?? []);
            }
        });
    }, [token, user]);

    useEffect(() => {
        refreshUsers();
    }, []);

    useEffect(() => {
        refreshRoles();
    }, []);

    useEffect(() => {
        refreshPrivileges();
    }, []);

    return (
        <Container>
            <Grid container padding={2}>
                <Grid
                    container
                    direction={"row"}
                    justifyContent={"space-between"}
                    alignItems={"center"}
                    marginBottom={2}>
                    <Typography variant={"h6"}>
                        Users ({ users?.length ?? 0 })
                    </Typography>
                    {
                        isAdmin() ?? <Button
                            endIcon={<Add/>}
                            onClick={_ => {
                                setNewUserDialogProps({
                                    open: true,
                                    onClose: (user?: NewUser) => {
                                        setNewUserDialogProps({
                                            open: false
                                        });
                                        if (user) {
                                            createUser(user);
                                        }
                                    }
                                });
                            }}>
                            {"New user"}
                        </Button>
                    }
                </Grid>
                <Grid>
                    {
                        users?.map((user, index) => (
                            <Paper
                                key={index}
                                style={{ width: "100%", marginBottom: "8px" }}>
                                <div>
                                    <Grid
                                        container
                                        padding={2}
                                        direction={"row"}
                                        justifyContent={"space-between"}
                                        alignItems={"center"}
                                        xs={12}>
                                        <Grid item xs={8}>
                                            <Grid container alignItems={"center"}>
                                                <Typography variant={"h6"} marginRight={1}>{ user.username }</Typography>
                                                {
                                                    user.root ?
                                                        <Tooltip title={"Root user"} placement={"top"}>
                                                            <IconButton size={"small"}>
                                                                <AdminPanelSettings/>
                                                            </IconButton>
                                                        </Tooltip>
                                                        :
                                                        <div></div>
                                                }
                                            </Grid>
                                        </Grid>
                                        <Grid
                                            item
                                            container
                                            direction={"row"}
                                            justifyContent={"end"}
                                            xs={4}>
                                            {
                                                isAdmin() ?? <Grid item>
                                                    <FormControlLabel
                                                        control={
                                                            <Tooltip
                                                                title={
                                                                    user.active
                                                                        ? "Deactivate '" + user.username + "'"
                                                                        : "Activate '" + user.username + "'"
                                                                }>
                                                                <Switch
                                                                    size={"medium"}
                                                                    checked={user.active}
                                                                    onChange={e => toggleUserActiveStatus(user)}
                                                                    inputProps={{ 'aria-label': 'controlled' }}
                                                                />
                                                            </Tooltip>
                                                        }
                                                        disabled={user.root || loading}
                                                        labelPlacement={"start"}
                                                        label={ user.active ? "Active" : "Inactive" }/>
                                                </Grid>
                                            }
                                            <Grid item paddingLeft={4}>
                                                <Tooltip title={"Edit '" + user.username + "'"}>
                                                    <IconButton
                                                        disabled={user.root || loading}
                                                        onClick={_ => setEditUserDialogProps({
                                                            open: true,
                                                            user: user,
                                                            onClose: (editedUser?: User) => {
                                                                setEditUserDialogProps((currentProps) => {
                                                                   const newProps: EditUserDialogProps = {
                                                                       ...currentProps,
                                                                       open: false
                                                                   };
                                                                   return newProps;
                                                                });
                                                                if (editedUser) {
                                                                    updateUser(editedUser);
                                                                }
                                                            }
                                                        })}>
                                                        <Edit/>
                                                    </IconButton>
                                                </Tooltip>
                                            </Grid>
                                        </Grid>
                                    </Grid>
                                    <Divider/>
                                    <Grid xs={12}>
                                        <Accordion style={{ width: "100%" }}>
                                            <AccordionSummary expandIcon={<ArrowDropDown/>}>
                                                <Grid
                                                    container
                                                    direction={"row"}
                                                    justifyContent={"space-between"}
                                                    alignItems={"center"}>
                                                    <Grid item direction={"row"}>
                                                        {
                                                            // todo fix item alignment with multiple roles
                                                            user.mappedRoles && user.mappedRoles.length > 0
                                                                ?
                                                                <Grid item direction={"row"}>
                                                                    {
                                                                        user.mappedRoles?.map((role, roleIndex) => (
                                                                            <Grid item key={roleIndex}>
                                                                                <Tooltip title={role.description}>
                                                                                    <Chip
                                                                                        label={ role.name.replace("ROLE_", "") }
                                                                                        disabled={user.root}
                                                                                        // todo confirmation dialog for removing roles
                                                                                        onDelete={
                                                                                            isAdmin()
                                                                                                ? _ => deleteUserRoleAssociation(user.id!, role.id)
                                                                                                : undefined
                                                                                        }
                                                                                    />
                                                                                </Tooltip>
                                                                            </Grid>
                                                                        ))
                                                                    }
                                                                </Grid>
                                                                : <div className={"font-subtext"}>No roles</div>
                                                        }
                                                    </Grid>
                                                    <Grid item direction={"row"}>
                                                        <Typography variant={"subtitle2"}>Privileges</Typography>
                                                    </Grid>
                                                </Grid>
                                            </AccordionSummary>
                                            <AccordionDetails>
                                                <Grid container direction={"row"}>
                                                    {
                                                        mapPrivilegesFromRoles(user.mappedRoles).map((entry, idx, self) => (
                                                            <Grid key={idx} container>
                                                                <Typography marginBottom={1}>{ entry[0] }</Typography>
                                                                <Grid container direction={"row"}>
                                                                    {
                                                                        entry[1].map((privilege, index, arrSelf) => (
                                                                            <Grid key={index} item marginRight={1} marginBottom={1}>
                                                                                <Tooltip title={privilege.domain_description}>
                                                                                    <Chip label={privilege.name}></Chip>
                                                                                </Tooltip>
                                                                            </Grid>
                                                                        ))
                                                                    }
                                                                </Grid>
                                                            </Grid>
                                                        ))
                                                    }
                                                    {
                                                        isAdmin() ?? <Button startIcon={<Add/>} onClick={() => {
                                                            setListDialogProps({
                                                                open: true,
                                                                titleText: 'Add role',
                                                                // todo should filter to show only roles that are not yet associated with the user
                                                                listItems: roles?.map((role, idx, self) => role.name) ?? [],
                                                                onClose: (value: string | null) => {
                                                                    setListDialogProps((currentProps) => {
                                                                        const newProps: ListDialogProps = {
                                                                            ...currentProps,
                                                                            open: false
                                                                        };
                                                                        return newProps;
                                                                    });
                                                                    if (value) {
                                                                        console.debug('Adding role to user');
                                                                        const targetRole = roles?.find((role) => role.name === value);
                                                                        if (targetRole) {
                                                                            createUserRoleAssociation(user.id!, targetRole.id);
                                                                        } else {
                                                                            console.error(`No role found for "${value}"`);
                                                                        }
                                                                    }
                                                                }
                                                            });
                                                        }}>
                                                            Add role
                                                        </Button>
                                                    }
                                                </Grid>
                                            </AccordionDetails>
                                        </Accordion>
                                    </Grid>
                                </div>
                            </Paper>
                        ))
                    }
                </Grid>
                <Grid
                    container
                    direction={"row"}
                    justifyContent={"space-between"}
                    alignItems={"center"}
                    marginBottom={2}
                    marginTop={2}>
                    <Typography variant={"h6"}>
                        Roles ({ roles?.length ?? 0 })
                    </Typography>
                </Grid>
                <Grid xs={12}>
                    {
                        roles?.map((role, index) => (
                            <Paper key={index}
                                   style={{ width: "100%", marginBottom: "8px" }}>
                                <div>
                                    <Grid
                                        container
                                        direction={"row"}
                                        alignItems={"center"}
                                        xs={12}>
                                        <Accordion style={{ width: "100%" }}>
                                            <AccordionSummary expandIcon={<ArrowDropDown/>}>
                                                <Grid
                                                    container
                                                    item
                                                    direction={"row"}
                                                    justifyContent={"space-between"}
                                                    alignItems={"center"}
                                                    xs={12}>
                                                    <Grid item direction={"row"}>
                                                        <Typography variant={"subtitle1"} marginRight={1}>
                                                            { role.name.replace("ROLE_", "") }
                                                        </Typography>
                                                        <Typography variant={"subtitle2"} marginRight={1}>
                                                            { role.description ?? "No description" }
                                                        </Typography>
                                                    </Grid>
                                                    <Grid item direction={"row"}>
                                                        <Typography variant={"subtitle2"}>Privileges</Typography>
                                                    </Grid>
                                                </Grid>
                                            </AccordionSummary>
                                            <AccordionDetails>
                                                <Grid container direction={"row"}>
                                                    {
                                                        mapPrivilegesFromRole(role).map((entry, idx, self) => (
                                                            <Grid key={idx} container>
                                                                <Typography marginBottom={1}>{ entry[0] }</Typography>
                                                                <Grid container direction={"row"}>
                                                                    {
                                                                        entry[1].map((privilege, index, arrSelf) => (
                                                                            <Grid key={index} item marginRight={1} marginBottom={1}>
                                                                                <Tooltip title={privilege.domain_description}>
                                                                                    <Chip label={privilege.name} onDelete={() => {
                                                                                        setDialogProps({
                                                                                            open: true,
                                                                                            titleText: 'Confirm deletion',
                                                                                            contentText: `Do you really want to delete privilege:\n${privilege.domain}_${privilege.name}\nfrom role:\n${role.name}?`,
                                                                                            onClose: (value: boolean) => {
                                                                                                setDialogProps((currentProps) => {
                                                                                                    const newProps: AlertDialogProps = {
                                                                                                        ...currentProps,
                                                                                                        open: false
                                                                                                    }
                                                                                                    return newProps;
                                                                                                });
                                                                                                if (value) {
                                                                                                    console.debug('Deleting privilege from role');
                                                                                                    deleteRolePrivilegeAssociation(role.id, privilege.id);
                                                                                                }
                                                                                            }
                                                                                        });
                                                                                    }}></Chip>
                                                                                </Tooltip>
                                                                            </Grid>
                                                                        ))
                                                                    }
                                                                </Grid>
                                                            </Grid>
                                                        ))
                                                    }
                                                    {
                                                        isAdmin() ?? <Button startIcon={<Add/>} onClick={() => {
                                                            setListDialogProps({
                                                                open: true,
                                                                titleText: 'Add privilege',
                                                                // todo should filter to show only privileges that are not yet associated with the role
                                                                listItems: privileges?.map((privilege, idx, self) => privilege.domain + "_" + privilege.name) ?? [],
                                                                onClose: (value: string | null) => {
                                                                    setListDialogProps((currentProps) => {
                                                                        const newProps: ListDialogProps = {
                                                                            ...currentProps,
                                                                            open: false
                                                                        };
                                                                        return newProps;
                                                                    });
                                                                    if (value) {
                                                                        console.debug('Adding privilege to role');
                                                                        const parts = value.split("_");
                                                                        const targetPrivilege = privileges?.find((privilege) => privilege.domain === parts[0] && privilege.name === parts[1]);
                                                                        if (targetPrivilege) {
                                                                            createRolePrivilegeAssociation(role.id, targetPrivilege.id);
                                                                        } else {
                                                                            console.error(`No privilege found for "${value}"`);
                                                                        }
                                                                    }
                                                                }
                                                            });
                                                        }}>
                                                            Add privilege
                                                        </Button>
                                                    }
                                                </Grid>
                                            </AccordionDetails>
                                        </Accordion>
                                    </Grid>
                                </div>
                            </Paper>
                        ))
                    }
                </Grid>
                <Grid>
                    <Snackbar
                        open={errorMessage !== ''}
                        onClose={_ => setErrorMessage('')}
                        autoHideDuration={5000}
                        anchorOrigin={{
                            vertical: 'top',
                            horizontal: 'center'
                        }}>
                        <Alert
                            severity="error"
                            onClose={_ => setErrorMessage('')}>
                            {errorMessage}
                        </Alert>
                    </Snackbar>
                    <Snackbar
                        open={successMessage !== ''}
                        onClose={_ => setSuccessMessage('')}
                        autoHideDuration={3000}
                        anchorOrigin={{
                            vertical: 'top',
                            horizontal: 'center'
                        }}>
                        <Alert
                            severity="success"
                            onClose={_ => setSuccessMessage('')}>
                            {successMessage}
                        </Alert>
                    </Snackbar>
                </Grid>
                <Grid>
                    <ListDialog {...listDialogProps}/>
                </Grid>
                <Grid>
                    <NewUserDialog {...newUserDialogProps}/>
                </Grid>
                <Grid>
                    <EditUserDialog {...editUserDialogProps}/>
                </Grid>
            </Grid>
        </Container>
    );
}

export default UserManagementPage;