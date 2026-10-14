package altn72.railpulse;

import altn72.railpulse.controleur.GlobalExceptionHandler;
import altn72.railpulse.controleur.GlobalUserControllerAdvice;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler(mock(GlobalUserControllerAdvice.class));

    @Test
    void unexpectedErrorsDoNotExposeTheirInternalMessage() {
        ExtendedModelMap model = new ExtendedModelMap();
        var view = handler.inattendue(new IllegalStateException("INTERNAL_SECRET"), model);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, view.getStatus());
        assertEquals("error", view.getViewName());
        assertEquals("Une erreur est survenue. Réessayez plus tard.", model.get("messageUtilisateur"));
        assertFalse(model.toString().contains("INTERNAL_SECRET"));
    }

    @Test
    void databaseConflictsHaveASafeActionableMessage() {
        ExtendedModelMap model = new ExtendedModelMap();
        assertEquals("error", handler.conflit(model));
        assertEquals(409, model.get("status"));
        assertTrue(model.get("messageUtilisateur").toString().contains("rechargez la page"));
    }

    @Test
    void frameworkErrorStatusIsPreservedWithoutExposingItsReason() {
        ExtendedModelMap model = new ExtendedModelMap();
        var view = handler.inattendue(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "INTERNAL_SECRET"), model);
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, view.getStatus());
        assertEquals(503, model.get("status"));
        assertFalse(model.toString().contains("INTERNAL_SECRET"));
    }
}
