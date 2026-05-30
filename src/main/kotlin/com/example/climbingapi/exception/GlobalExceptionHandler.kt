package com.example.climbingapi.exception

import jakarta.servlet.http.HttpServletRequest
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.FieldError
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import java.time.OffsetDateTime

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException::class)
    fun handleNotFound(exception: NotFoundException, request: HttpServletRequest): ResponseEntity<ErrorResponse> {
        return buildResponse(HttpStatus.NOT_FOUND, "NOT_FOUND", exception.message ?: "Not found", request)
    }

    @ExceptionHandler(
        MethodArgumentNotValidException::class,
        MethodArgumentTypeMismatchException::class,
        HttpMessageNotReadableException::class,
        IllegalArgumentException::class
    )
    fun handleBadRequest(exception: Exception, request: HttpServletRequest): ResponseEntity<ErrorResponse> {
        val message = extractBadRequestMessage(exception)
        return buildResponse(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, request)
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleMethodNotAllowed(exception: HttpRequestMethodNotSupportedException, request: HttpServletRequest): ResponseEntity<ErrorResponse> {
        return buildResponse(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", exception.message ?: "Method not allowed", request)
    }

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleConflict(exception: DataIntegrityViolationException, request: HttpServletRequest): ResponseEntity<ErrorResponse> {
        return buildResponse(HttpStatus.CONFLICT, "CONFLICT", "Database constraint violation.", request)
    }

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(exception: Exception, request: HttpServletRequest): ResponseEntity<ErrorResponse> {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred.", request)
    }

    private fun buildResponse(
        status: HttpStatus,
        errorCode: String,
        message: String,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val response = ErrorResponse(
            timestamp = OffsetDateTime.now(),
            status = status.value(),
            error = status.reasonPhrase,
            errorCode = errorCode,
            message = message,
            path = request.requestURI
        )
        return ResponseEntity.status(status).body(response)
    }

    private fun extractBadRequestMessage(exception: Exception): String {
        return when (exception) {
            is MethodArgumentNotValidException -> {
                exception.bindingResult.allErrors.joinToString("; ") { error ->
                    if (error is FieldError) "${error.field}: ${error.defaultMessage}"
                    else error.defaultMessage ?: "Invalid value"
                }
            }
            is MethodArgumentTypeMismatchException -> "Invalid value for parameter: ${exception.name}"
            is HttpMessageNotReadableException -> "Request body is missing or invalid."
            else -> exception.message ?: "Bad request"
        }
    }
}
