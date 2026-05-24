package com.safecarealert.dashboard;

import com.safecarealert.alerts.GroupKey;
import com.safecarealert.alerts.registry.GroupRegistry;
//import io.quarkus.qute.Template;
//import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/dashboard")
public class DashboardResource {
//    @Inject
//    Template dashboard;

    @Inject
    GroupRegistry registry;

//    @GET
//    @Path("/{tenantId}/{workplaceId}/{groupId}")
//    @Produces(MediaType.TEXT_HTML)
//    public TemplateInstance get(@PathParam("tenantId") String tenantId,
//                                @PathParam("workplaceId") String workplaceId,
//                                @PathParam("groupId") String groupId) {
//        GroupKey groupKey = new GroupKey(tenantId, workplaceId, groupId);
//
//        return dashboard.data("tenantId", tenantId)
//                .data("workplaceId", workplaceId)
//                .data("groupId", groupId)
//                .data("initialCount", registry.getConnectedDeviceCount(groupKey));
//    }

    @GET
    public String dummy() {
        return "";
    }

}
