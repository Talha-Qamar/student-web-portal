package com.studentportal.exception;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void mapsKnownExceptionsToExpectedStatuses() throws Exception {
        assertAll(
                () -> assertEquals(HttpStatus.NOT_FOUND, handler.handleNotFound(new ResourceNotFoundException("missing")).getStatusCode()),
                () -> assertEquals(HttpStatus.BAD_REQUEST, handler.handleBadRequest(new BadRequestException("bad")).getStatusCode()),
                () -> assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, handler.handleGeneric(new RuntimeException("boom")).getStatusCode())
        );
    }

    @Test
    void formatsValidationErrorsUsingFirstFieldError() throws Exception {
        Method method = Dummy.class.getDeclaredMethod("sample", String.class);
        MethodParameter parameter = new MethodParameter(method, 0);
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Dummy(), "dummy");
        bindingResult.addError(new FieldError("dummy", "email", "must be valid"));
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(parameter, bindingResult);

        var response = handler.handleValidation(exception);
        var body = response.getBody();

        assertAll(
                () -> assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode()),
                () -> assertEquals("email: must be valid", body.get("message")),
                () -> assertTrue(body.containsKey("timestamp"))
        );
    }

    private static final class Dummy {
        @SuppressWarnings("unused")
        void sample(String value) {
        }
    }
}