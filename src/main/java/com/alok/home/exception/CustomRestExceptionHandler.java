package com.alok.home.exception;

import com.alok.home.commons.dto.exception.GlobalRestExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class CustomRestExceptionHandler extends GlobalRestExceptionHandler {
}
