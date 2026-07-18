package br.com.achadosperdidos.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthEventServiceMaskTest {

    @Test
    void mascararEmailPreservaDominio() {
        assertThat(AuthEventService.mascararIdentificador("admin@teste.com"))
                .isEqualTo("ad***@teste.com");
    }

    @Test
    void mascararLoginCurto() {
        assertThat(AuthEventService.mascararIdentificador("ab")).isEqualTo("a*");
        assertThat(AuthEventService.mascararIdentificador("admin")).isEqualTo("ad***");
    }
}
