# Keycloak (IDEM) Authentication Implementation Guide
## React + Spring Boot + RHSSO/Keycloak

---

# Architecture Overview

```text
+-------------+
| React UI    |
+-------------+
       |
       | Login
       v
+-------------+
| IDEM/RHSSO  |
| Keycloak    |
+-------------+
       |
       | JWT Token
       v
+------------------+
| Spring Boot API  |
+------------------+
       |
       | Authorization
       v
+------------------+
| Protected APIs   |
+------------------+
```

---

# IDEM Configuration

Current IDEM Configuration:

```json
{
  "realm": "internal",
  "auth-server-url": "https://secure-sso-rhsso-np.apps.ocplife-np.bajajlife.com/auth/",
  "ssl-required": "external",
  "resource": "TMT",
  "public-client": true,
  "confidential-port": 0
}
```

## Mapping

| Field | Value |
|---------|---------|
| Realm | internal |
| Client ID | TMT |
| Auth Server | https://secure-sso-rhsso-np.apps.ocplife-np.bajajlife.com/auth |
| Client Type | Public Client |

---

# Step 1: Install Keycloak JS

```bash
npm install keycloak-js
```

---

# Step 2: Create keycloak.ts

Create:

```text
src/lib/keycloak.ts
```

```typescript
import Keycloak from 'keycloak-js';

const keycloak = new Keycloak({
    url: 'https://secure-sso-rhsso-np.apps.ocplife-np.bajajlife.com/auth',
    realm: 'internal',
    clientId: 'TMT'
});

export default keycloak;
```

---

# Step 3: Create Authentication Context

Create:

```text
src/context/AuthContext.tsx
```

Use the AuthProvider code provided earlier.

Responsibilities:

- Initialize Keycloak
- Check existing session
- Login
- Logout
- Store user information
- Handle authentication errors
- Support role-based authorization

---

# Step 4: Wrap Application

Open:

```text
src/main.tsx
```

or

```text
src/index.tsx
```

Update:

```tsx
import { BrowserRouter } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';

root.render(
    <BrowserRouter>
        <AuthProvider>
            <App />
        </AuthProvider>
    </BrowserRouter>
);
```

---

# Step 5: Create Login Page

```tsx
import { useAuth } from '../context/AuthContext';

export default function LoginPage() {
    const { login } = useAuth();

    return (
        <button onClick={login}>
            Login With IDEM
        </button>
    );
}
```

---

# Step 6: Create Protected Route

```tsx
import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function ProtectedRoute({
    children,
}: {
    children: JSX.Element;
}) {
    const { isAuthenticated, isLoading } = useAuth();

    if (isLoading) {
        return <div>Loading...</div>;
    }

    if (!isAuthenticated) {
        return <Navigate to="/login" />;
    }

    return children;
}
```

Usage:

```tsx
<Route
    path="/dashboard"
    element={
        <ProtectedRoute>
            <Dashboard />
        </ProtectedRoute>
    }
/>
```

---

# Step 7: Configure Axios

Create:

```text
src/lib/apiClient.ts
```

```typescript
import axios from "axios";
import keycloak from "./keycloak";

export const apiClient = axios.create({
    baseURL: "/api"
});

apiClient.interceptors.request.use((config) => {

    if (keycloak.token) {
        config.headers.Authorization =
            `Bearer ${keycloak.token}`;
    }

    return config;
});
```

This automatically sends:

```http
Authorization: Bearer eyJhbGciOi...
```

for every API call.

---

# Step 8: Add Spring Security Dependency

pom.xml

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>
        spring-boot-starter-oauth2-resource-server
    </artifactId>
</dependency>
```

---

# Step 9: Configure Spring Boot

application.properties

```properties
spring.security.oauth2.resourceserver.jwt.issuer-uri=https://secure-sso-rhsso-np.apps.ocplife-np.bajajlife.com/auth/realms/internal
```

---

# Step 10: Create SecurityConfig

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http)
            throws Exception {

        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        "/api/public/**"
                ).permitAll()
 **             .anyRequest()
      **        .authenticated()
        **  )
            .oauth2ResourceSe**er(
                oauth -> oaut**jwt()
            );

        ret**n http.build();
    }
}
```

---
** Step 11: Create User Profile API**```java
@RestController
@RequestM**ping("/api")
public class UserCon**oller {

    @GetMapping("/profil**)
    public Map<String, Object> **ofile(
            Jwt jwt) {

  **    return Map.of(
              **"username",
                jwt.g**Claim("preferred_username"),
    **          "email",
              **jwt.getClaim("email"),
          **    "name",
                jwt.g**Claim("name")
        );
    }
}
**`

---

# Step 12: Fetch User Aft** Login

Create endpoint:

```java**PostMapping("/auth/keycloak-login**
public ResponseEntity<?> login(
**      Jwt jwt) {

    Map<String,**bject> response =
            Map**f(
                    "data",
  **                Map.of(
         **                 "user",
        **                  Map.of(
       **                           "id",
**                                 **wt.getSubject(),

               **                   "email",
     **                             jwt.**tClaim("email"),

               **                   "fullName",
  **                                j**.getClaim("name"),

             **                     "role",
    **                              "US**"
                            )
 **                 )
            );**    return ResponseEntity.ok(resp**se);
}
```

This endpoint is call** from:

```typescript
POST /auth/**ycloak-login
```

inside AuthProv**er.

---

# Step 13: Extract Role**From Keycloak

Keycloak token exa**le:

```json
{
    "realm_access"**{
        "roles": [
            "ADMIN",
            "USER"
        ]
    }
}
```

Create converter:

**`java
@Bean
JwtAuthenticationConv**ter jwtAuthenticationConverter() **
    JwtAuthenticationConverter c**verter =
            new JwtAuthe**icationConverter();

    converte**setJwtGrantedAuthoritiesConverter**wt -> {

        Map<String, Obje**> realmAccess =
                j**.getClaim("realm_access");

     ** if (realmAccess == null) {
     **     return List.of();
        }
**       List<String> roles =
     **         (List<String>)
         **             realmAccess.get("rol**");

        return roles.stream(**                .map(role ->
    **                  new SimpleGrant**Authority(
                      **        "ROLE_" + role))
        **      .toList();
    });

    ret**n converter;
}
```

---

# Step 1** Protect APIs By Role

```java
@P**Authorize("hasRole('ADMIN')")
@Ge**apping("/admin")
public String ad**n() {
    return "Admin Access";
**```

```java
@PreAuthorize("hasAn**ole('ADMIN','MANAGER')")
@GetMapp**g("/reports")
public String repor**() {
    return "Reports";
}
```
**--

# Step 15: Logout

Frontend:
**``tsx
await keycloak.logout({
   **edirectUri:
        window.locati**.origin + "/login"
});
```

Backe**:

```java
@PostMapping("/auth/lo**ut")
public ResponseEntity<?> log**t() {

    SecurityContextHolder.**earContext();

    return Respons**ntity.ok().build();
}
```

---

#**tep 16: Verify Login

Open:

```t**t
http://localhost:3000
```

Clic**

```text
Login With IDEM
```

Ex**cted:

```text
React
   ↓
IDEM Lo**n Page
   ↓
Successful Login
   ↓**ashboard
```

---

# Step 17: Ver**y JWT Token

Browser Console:

``**avascript
console.log(keycloak.to**n);
```

Expected:

```text
eyJhb**iOiJSUzI1Ni...
```

---

# Step 1** Verify API Access

Request:

```**tp
GET /api/profile
Authorization**Bearer eyJhbGciOi...
```

Expecte**

```json
{
  "username":"rahul.b**gat",
  "email":"rahul.bhagat@tes**enii.com",
  "name":"Rahul Bhagat**}
```

---

# Keycloak Client Con**guration Required

Ask the IDM Te** to configure the TMT client.

##**alid Redirect URIs

Development

**`text
http://localhost:3000/*
```**UAT

```text
http://10.3.41.102/***``

Production

```text
https://t**tgenii.com/*
```

---

## Web Ori**ns

Development

```text
http://l**alhost:3000
```

UAT

```text
htt**//10.3.41.102
```

Production

``**ext
https://testgenii.com
```

--**
# Testing Checklist

## Login

-** ] IDEM Login Page Opens
- [ ] Lo**n Success
- [ ] Redirect To Dashb**rd

## Token

- [ ] JWT Generated** [ ] Token Sent In Header

## Bac**nd

- [ ] Spring Boot Validates J**
- [ ] User Profile API Works
- [ ] Unauthorized Requests Return 401**## Authorization

- [ ] ADMIN Rol**Works
- [ ] USER Restrictions Work
- [ ] Role-Based APIs Protected

---

# Final Flow

```text
User
  |
  v
React Login Page
  |
  v
keycloak.login()
  |
  v
IDEM / RHSSO
  |
  v
Access Token
  |
  v
React Stores Token
  |
  v
API Calls
Authorization: Bearer <token>
  |
  v
Spring Security
  |
  v
JWT Validation
  |
  v
Protected APIs
  |
  v
Dashboard
```