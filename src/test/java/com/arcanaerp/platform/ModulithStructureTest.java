package com.arcanaerp.platform;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModulithStructureTest {

    @Test
    void verifiesModuleBoundaries() {
        ApplicationModules.of(PlatformApplication.class).verify();
    }
}
