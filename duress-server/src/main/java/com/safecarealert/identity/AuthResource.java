package com.safecarealert.identity;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

@Path("/auth")
public class AuthResource {

//    @Inject
//    TokenService tokenService;

    @GET
    public String dummy() {
        return "";
    }

    public Uni<Response> login(LoginRequest request){
        return null;

        /* UNCOMMENT THIS WHEN THE TIME COMES
        return findLicenceInDb(request.licenceKey())
                .onItem().transform(licence -> {
                            if (licence == null || !licence.isActive()) {
                                throw new SecurityException("Invalid or expired licence");
                            }


                            // 2. Map the Serial Number to a consistent Device ID
                            String deviceId = request.serialNumber();
                            String tenantUuid = licence.getTenantUuid();

                            return tokenService.generateDeviceToken(deviceId, tenantUuid);
                        });

         */
        /*
        return identityRespository.verify(request.serialNumber(), request.secret())
                .onItem().tranform(user -> {
                    if (user == null) return Response.status(Response.Status.UNAUTHORIZED).build();

                    String jwt = tokenService.generateDeviceToken(user.deviceId(), user.tenantUUID());
                    return Response.status(Response.Status.OK).entity(Map.of("access_token", jwt)).build();
                });
        */
    }

    private Uni<Object> findLicenceInDb(String s) {
        return null;
    }

}
