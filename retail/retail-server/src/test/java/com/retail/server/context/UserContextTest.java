package com.retail.server.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserContextTest {

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void storesUserAndRoleInCurrentThread() {
        UserContext.setCurrentUserId(7L);
        UserContext.setCurrentRole("ADMIN");

        assertThat(UserContext.getCurrentUserId()).isEqualTo(7L);
        assertThat(UserContext.getCurrentRole()).isEqualTo("ADMIN");
    }

    @Test
    void clearRemovesCurrentThreadValues() {
        UserContext.setCurrentUserId(8L);
        UserContext.setCurrentRole("USER");

        UserContext.clear();

        assertThat(UserContext.getCurrentUserId()).isNull();
        assertThat(UserContext.getCurrentRole()).isNull();
    }
}
