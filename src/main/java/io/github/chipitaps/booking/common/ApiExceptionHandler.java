package io.github.chipitaps.booking.common;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler(ApiException.class)
	ProblemDetail handleApiException(ApiException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(exception.status(), exception.getMessage());
		problem.setTitle(exception.status().getReasonPhrase());
		return problem;
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		Map<String, String> errors = new LinkedHashMap<>();
		exception.getBindingResult()
			.getFieldErrors()
			.forEach((error) -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
		ProblemDetail problem = exception.getBody();
		problem.setDetail("Some fields are missing or invalid.");
		problem.setProperty("errors", errors);
		return handleExceptionInternal(exception, problem, headers, status, request);
	}

}
