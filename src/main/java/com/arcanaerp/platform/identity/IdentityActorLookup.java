package com.arcanaerp.platform.identity;

public interface IdentityActorLookup {

    boolean actorExists(String tenantCode, String actorEmail);
}
