package com.safecarealert.identity;

import io.smallrye.jwt.build.Jwt;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TokenGeneratorResource {

    @Inject
    @ConfigProperty(name = "jwt.issuer")
    String issuer;

    @Inject
    IdentityService identityService;

    @Inject
    SubjectFactory subjectFactory;

    @GET
    @Path("/token")
    @PermitAll
    public Response generate(@HeaderParam("X-Tenant-Id") String tenantUUID,
                             @HeaderParam("X-Subject-Id") String subjectUUID) {

        if (subjectUUID == null || subjectUUID.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Missing X-Subject-Id header")
                    .build();
        }

        IdentityRecord record = identityService.lookup(subjectUUID, tenantUUID);
        if (record == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Identity not found")
                    .build();
        }

        Subject subject = subjectFactory.fromIdentityRecord(record);

        // ROLE : groups (explicit claim, not builder shortcut)
        Set<String> groups = subject.getRoles()
                .stream()
                .map(Role::name)
                .collect(Collectors.toSet());

        var jwtBuilder = Jwt.issuer(issuer)
                .subject(subject.getSubjectId())
                .upn(subject.getSubjectId())
                .claim("groups", groups)
                .issuedAt(Instant.now())
                .expiresIn(Duration.ofHours(24));

        // Claims aligned with your domain model

        if (subject.getTenantUUID() != null) {
            jwtBuilder.claim(Claim.TENANT_UUID.name(), subject.getTenantUUID());
        }

        if (subject.getWorkplaceUUID() != null) {
            jwtBuilder.claim(Claim.WORKPLACE_UUID.name(), subject.getWorkplaceUUID());
        }

        if (subject.getGroupUUIDs() != null && !subject.getGroupUUIDs().isEmpty()) {
            jwtBuilder.claim(Claim.GROUP_UUID.name(), subject.getGroupUUIDs());
        }

        if (subject.getAllowedWorkplaces() != null && !subject.getAllowedWorkplaces().isEmpty()) {
            jwtBuilder.claim(Claim.ALLOWED_WORKPLACES.name(), subject.getAllowedWorkplaces());
        }

        String token = jwtBuilder.sign();

        TokenResponse response = new TokenResponse(
                token,
                subject.getSubjectId(),
                groups
        );

        return Response.ok(response).build();
    }

    public record TokenResponse(String token, String subjectId, Set<String> roles) {}
}