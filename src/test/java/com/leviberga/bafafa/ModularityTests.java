package com.leviberga.bafafa;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTests {

    ApplicationModules modules = ApplicationModules.of(BafafaApplication.class);

    @Test
    void verifiesModularStructure() {
        modules.verify();
        modules.forEach(System.out::println);
    }
}