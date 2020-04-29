package no.nav.syfo.exception

import no.nav.security.spring.oidc.validation.interceptor.OIDCUnauthorizedException
import no.nav.syfo.brukertilgang.RequestUnauthorizedException
import no.nav.syfo.metric.Metric
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest
import org.springframework.web.util.WebUtils
import javax.inject.Inject
import javax.validation.ConstraintViolationException
import javax.ws.rs.ForbiddenException

@ControllerAdvice
class ControllerExceptionHandler @Inject constructor(
        private val metric: Metric
) {
    private val BAD_REQUEST_MSG = "Vi kunne ikke tolke inndataene"
    private val CONFLICT_MSG = "Dette oppsto en konflikt i tilstand"
    private val FORBIDDEN_MSG = "Handling er forbudt"
    private val UNAUTHORIZED_MSG = "Autorisasjonsfeil"
    private val INTERNAL_MSG = "Det skjedde en uventet feil"

    @ExceptionHandler(Exception::class, IllegalArgumentException::class, ConstraintViolationException::class, ForbiddenException::class, OIDCUnauthorizedException::class)
    fun handleException(ex: Exception, request: WebRequest): ResponseEntity<ApiError> {
        val headers = HttpHeaders()
        if (ex is OIDCUnauthorizedException) {
            return handleOIDCUnauthorizedException(ex, headers, request)
        } else if (ex is RequestUnauthorizedException) {
            return handleRequestUnauthorizedException(ex, headers, request)
        }
        return when (ex) {
            is ForbiddenException -> {
                handleForbiddenException(ex, headers, request)
            }
            is IllegalArgumentException -> {
                handleIllegalArgumentException(ex, headers, request)
            }
            is ConstraintViolationException -> {
                handleConstraintViolationException(ex, headers, request)
            }
            is ConflictException -> {
                handleConflictException(ex, headers, request)
            }
            else -> {
                val status = HttpStatus.INTERNAL_SERVER_ERROR
                handleExceptionInternal(ex, ApiError(status.value(), INTERNAL_MSG), headers, status, request)
            }
        }
    }

    private fun handleOIDCUnauthorizedException(ex: OIDCUnauthorizedException, headers: HttpHeaders, request: WebRequest): ResponseEntity<ApiError> {
        return handleExceptionInternal(ex, ApiError(HttpStatus.UNAUTHORIZED.value(), UNAUTHORIZED_MSG), headers, HttpStatus.UNAUTHORIZED, request)
    }

    private fun handleRequestUnauthorizedException(ex: RequestUnauthorizedException, headers: HttpHeaders, request: WebRequest): ResponseEntity<ApiError> {
        return handleExceptionInternal(ex, ApiError(HttpStatus.UNAUTHORIZED.value(), UNAUTHORIZED_MSG), headers, HttpStatus.UNAUTHORIZED, request)
    }

    private fun handleForbiddenException(ex: ForbiddenException, headers: HttpHeaders, request: WebRequest): ResponseEntity<ApiError> {
        return handleExceptionInternal(ex, ApiError(HttpStatus.FORBIDDEN.value(), FORBIDDEN_MSG), headers, HttpStatus.FORBIDDEN, request)
    }

    private fun handleIllegalArgumentException(ex: IllegalArgumentException, headers: HttpHeaders, request: WebRequest): ResponseEntity<ApiError> {
        return handleExceptionInternal(ex, ApiError(HttpStatus.BAD_REQUEST.value(), BAD_REQUEST_MSG), headers, HttpStatus.BAD_REQUEST, request)
    }

    private fun handleConstraintViolationException(ex: ConstraintViolationException, headers: HttpHeaders, request: WebRequest): ResponseEntity<ApiError> {
        return handleExceptionInternal(ex, ApiError(HttpStatus.BAD_REQUEST.value(), BAD_REQUEST_MSG), headers, HttpStatus.BAD_REQUEST, request)
    }

    private fun handleConflictException(ex: ConflictException, headers: HttpHeaders, request: WebRequest): ResponseEntity<ApiError> {
        val status = HttpStatus.CONFLICT
        return handleExceptionInternal(ex, ApiError(status.value(), CONFLICT_MSG), headers, status, request)
    }

    private fun handleExceptionInternal(ex: Exception, body: ApiError, headers: HttpHeaders, status: HttpStatus, request: WebRequest): ResponseEntity<ApiError> {
        metric.tellHttpKall(status.value())
        if (!status.is2xxSuccessful) {
            if (HttpStatus.INTERNAL_SERVER_ERROR == status) {
                log.error("Uventet feil: {} : {}", ex.javaClass.toString(), ex.message, ex)
                request.setAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE, ex, WebRequest.SCOPE_REQUEST)
            } else {
                log.warn("Fikk response med kode : {} : {} : {}", status.value(), ex.javaClass.toString(), ex.message, ex)
            }
        }
        return ResponseEntity(body, headers, status)
    }

    companion object {
        private val log = LoggerFactory.getLogger(ControllerExceptionHandler::class.java)
    }
}
