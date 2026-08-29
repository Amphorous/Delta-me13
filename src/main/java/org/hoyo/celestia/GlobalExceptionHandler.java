package org.hoyo.celestia;

import org.hoyo.celestia.fightprops.service.FightPropCalculationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // More specific than the RuntimeException handler below, so Spring picks
    // this one first - same response contract (500, same body shape), just a
    // log line that names the uid/avatarId instead of a bare stack trace.
    @ExceptionHandler(FightPropCalculationException.class)
    public ResponseEntity<String> handleFightPropCalculation(FightPropCalculationException ex) {
        logger.error("Fight prop calculation failed for uid {} avatarId {}", ex.getUid(), ex.getAvatarId(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Something went wrong: " + ex.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleRuntime(RuntimeException ex) {
        logger.error("Unhandled RuntimeException caught", ex); // logs full stack trace
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Something went wrong: " + ex.getMessage());
    }
}
