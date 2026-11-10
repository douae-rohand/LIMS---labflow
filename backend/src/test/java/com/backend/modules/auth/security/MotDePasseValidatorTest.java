package com.backend.modules.auth.security;

import com.backend.common.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MotDePasseValidatorTest {

    private final BCryptPasswordEncoder encodeur = new BCryptPasswordEncoder(4);

    @Test
    void politiqueRenforceeExigeLongueurEtCaractereSpecial() {
        BusinessRuleException tropCourt = assertThrows(BusinessRuleException.class,
                () -> MotDePasseValidator.valider("Abcdefgh12", null, encodeur, 12, false));
        assertEquals("MOT_DE_PASSE_TROP_COURT", tropCourt.getCode());

        BusinessRuleException sansSpecial = assertThrows(BusinessRuleException.class,
                () -> MotDePasseValidator.valider("Abcdefgh1234", null, encodeur, 12, true));
        assertEquals("MOT_DE_PASSE_SANS_SPECIAL", sansSpecial.getCode());

        assertDoesNotThrow(() -> MotDePasseValidator.valider("Abcdefgh123!", null, encodeur, 12, true));
    }

    @Test
    void laLongueurNeDescendJamaisSousDix() {
        assertThrows(BusinessRuleException.class,
                () -> MotDePasseValidator.valider("Abcdef12", null, encodeur, 6, false));
    }
}
