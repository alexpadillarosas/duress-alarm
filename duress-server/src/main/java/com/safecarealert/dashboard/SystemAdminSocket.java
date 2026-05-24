package com.safecarealert.dashboard;


import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;

@WebSocket(path = "/ws/system")
@RolesAllowed("SYSTEM_ADMIN")
public class SystemAdminSocket {

    @Inject
    SecurityIdentity identity;

    @OnOpen
    public Uni<Void> onOpen(WebSocketConnection connection) {

        // SYSTEM_ADMIN has no tenant/workplace/group
        // No cross-check needed
//        systemRegistry.register(connection);

        return Uni.createFrom().voidItem();
    }
}
/*
What SYSTEM_ADMIN receives
You decide the scope, but typical features include:

🔹 System‑wide alert stream
Every alert from every tenant.

🔹 System‑wide device connection events
Every device connect/disconnect.

🔹 System‑wide monitor events
Every monitor connect/disconnect.

🔹 System health
Metrics, heartbeat, load, etc.

🔹 Administrative commands (optional)
force disconnect device

disable tenant

reload config

⭐ How SYSTEM_ADMIN fits into your identity model
Your JWT for SYSTEM_ADMIN contains:

json
{
  "sub": "SYS-1234",
  "groups": ["SYSTEM_ADMIN"]
}
No tenantUUID.
No workplaceUUID.
No groupUUID.

Your augmentor attaches:

subjectUUID = SYS‑1234

roles = SYSTEM_ADMIN

And nothing else.

Your WebSocket validator bypasses domain checks for SYSTEM_ADMIN.
 */