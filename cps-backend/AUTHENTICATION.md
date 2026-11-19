# Backend authentication

The backend authenticates usernames and BCrypt passwords with Spring Security's
`AuthenticationManager`. It issues a signed JWT access token and an opaque refresh
token. Spring Security's OAuth2 Resource Server validates Bearer tokens. HTTP
sessions, form login, Basic authentication, and authentication cookies are disabled.
All request and response bodies below are JSON.

Only `ADMIN` accounts can create and edit users, query the paginated user list,
reset users' passwords, and enable or disable accounts. HTTP security and `@PreAuthorize`
enforce these permissions. `MANAGER` remains a procurement role. Users can change
their own password by supplying the current password.

## Required password change

Passwords assigned when an administrator creates an account, resets its password,
or provisions the first administrator are temporary. Login and refresh responses,
as well as user responses including `GET /api/auth/me`, contain the boolean
`mustChangePassword` field.

When this field is `true`, the account can only log in, refresh tokens, log out,
read its own profile (`GET /api/auth/me`), and change its password
(`PUT /api/auth/password`). All other protected requests return HTTP 403 with
Problem Detail property `code: "PASSWORD_CHANGE_REQUIRED"`, including requests
from an administrator. The server checks the current database flag for each
Bearer request and withholds role authorities while a password change is required.

Change the password using `currentPassword` and `newPassword`. The new password
must differ from the current password and satisfy the existing password policy.
On success, the endpoint returns 204, clears `mustChangePassword`, and revokes
all of the account's sessions. Log in again with the new password to get
unrestricted tokens. Invalid changes preserve both the flag and existing sessions.

The migration preserves existing accounts without forcing an unexpected password
change. New accounts and subsequent administrative password resets require one.
Restarting bootstrap provisioning never resets an existing administrator's flag
or password.

## Configuration

Set `JWT_SECRET` to a Base64-encoded cryptographically random secret containing at
least 32 bytes. There is no default signing secret. Keep this value stable across
restarts and shared across backend instances. Changing it invalidates existing
access tokens. The issuer is an identifier and does not require a running server
at its URL.

| Property                       | Default                               |
|--------------------------------|---------------------------------------|
| `cps.security.jwt.issuer`      | `https://corporate-procurement.local` |
| `cps.security.jwt.audience`    | `cps-api`                             |
| `cps.security.jwt.access-ttl`  | `PT15M`                               |
| `cps.security.jwt.refresh-ttl` | `P30D`                                |

Provision the first administrator with `BOOTSTRAP_ADMIN_USERNAME` and
`BOOTSTRAP_ADMIN_PASSWORD`.
Remove these provisioning variables after the first account is created, before
changing that administrator's login or role; provisioning follows the configured
username on each application startup.

## Endpoints

| Endpoint                             | Access and body                                                | Success            |
|--------------------------------------|----------------------------------------------------------------|--------------------|
| `POST /api/auth/login`               | Public; `username`, `password`                                 | 200 token response |
| `POST /api/auth/refresh`             | Refresh token; `refreshToken`                                  | 200 token response |
| `POST /api/auth/logout`              | Refresh token; `refreshToken`                                  | 204                |
| `GET /api/auth/me`                   | Bearer access token                                            | 200 user details   |
| `PUT /api/auth/password`             | Bearer token; `currentPassword`, `newPassword`                 | 204                |
| `GET /api/admin/users`               | Admin Bearer token; optional `page`, `size`                    | 200 user page      |
| `GET /api/admin/users/{id}`          | Admin Bearer token                                             | 200 user details   |
| `POST /api/admin/users`              | Admin Bearer token; `username`, `fullName`, `password`, `role` | 201                |
| `PUT /api/admin/users/{id}`          | Admin Bearer token; `username`, `fullName`, `role`, `enabled`  | 200 user details   |
| `PUT /api/admin/users/{id}/password` | Admin Bearer token; `password`                                 | 204                |
| `PATCH /api/admin/users/{id}/status` | Admin Bearer token; `enabled` (boolean)                        | 200 user details   |

## User administration

Administrative user endpoints are under `/api/admin/users`. The former
`/api/users` routes are no longer mapped. User responses, including
`GET /api/auth/me`, contain both the login `username` and display name `fullName`.
Both values are trimmed, required, and limited to 255 characters. Login names
remain unique and case-sensitive. Existing users receive their current username
as their initial full name during migration.

`GET /api/admin/users?page=0&size=20` uses zero-based page numbers. `page` must
be nonnegative; `size` must be between 1 and 100 and defaults to 20. Results have
a stable order by username and UUID. The response is an object with `content`,
`page`, `size`, `totalElements`, and `totalPages`. An out-of-range page returns an
empty `content` array with the actual totals. Users without password credentials
are included. Password hashes are never returned.

`PUT /api/admin/users/{id}` replaces all four editable fields. It does not change
the password or `mustChangePassword`. For example:

```json
{
  "username": "procurement-manager",
  "fullName": "Алексей Демченков",
  "role": "MANAGER",
  "enabled": true
}
```

Changing the login, role, or enabled status revokes all of the affected account's
sessions. Changing only the full name preserves them. A conflicting login returns
409, invalid fields or pagination parameters return 400, and an unknown UUID
returns 404. All changes in a failed update are rolled back.

A token response contains `accessToken`, `refreshToken`, `tokenType` (`Bearer`),
`expiresIn`, and `refreshExpiresIn` (lifetimes in seconds). Send the access JWT in
`Authorization: Bearer <accessToken>` for protected requests. Refresh and logout
authenticate with the refresh token in the JSON body and work even when an access
JWT supplied in a header has expired. Tokens are never accepted from cookies.
CSRF tokens are unnecessary for this explicit-header/body token transport.

Refresh rotates the refresh token and returns a new access JWT. Replace both stored
tokens after each successful refresh and serialize refresh calls. Reusing any
consumed refresh token revokes its entire session, including the most recently
issued tokens. Session lifetime is absolute and does not extend with rotation.

## Brute-force limits

| Property                                   | Default |
|--------------------------------------------|---------|
| `cps.security.rate-limit.account-attempts` | 5       |
| `cps.security.rate-limit.account-window`   | `PT15M` |
| `cps.security.rate-limit.ip-attempts`      | 30      |
| `cps.security.rate-limit.ip-window`        | `PT1M`  |
| `cps.security.rate-limit.refresh-attempts` | 60      |
| `cps.security.rate-limit.max-tracked-keys` | 10000   |

Limits are atomic within a backend process and stored in bounded memory. Active
lockouts are not evicted to make room for new usernames. Limits return 429 with
`Retry-After` in seconds. Authentication/session failures return 401,
authorization failures return 403, and validation failures return 400.
