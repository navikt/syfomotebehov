package no.nav.syfo.api.exception

import jakarta.validation.ConstraintViolationException
import jakarta.ws.rs.ForbiddenException
import no.nav.security.token.support.spring.validation.interceptor.JwtTokenUnauthorizedException
import no.nav.syfo.consumer.brukertilgang.DineSykmeldteRequestException
import no.nav.syfo.metric.Metric
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.util.WebUtils
import javax.inject.Inject

@ControllerAdvice
class ControllerExceptionHandler
    @Inject
    constructor(
        private val metric: Metric,
    ) {
        private val badRequestMsg = "Vi kunne ikke tolke inndataene"
        private val conflictMsg = "Dette oppsto en konflikt i tilstand"
        private val forbiddenMsg = "Handling er forbudt"
        private val unauthorizedMsg = "Autorisasjonsfeil"
        private val badGatewayMsg = "Teknisk feil ved oppslag"
        private val internalMsg = "Det skjedde en uventet feil"

        @ExceptionHandler(
            Exception::class,
            IllegalArgumentException::class,
            ConstraintViolationException::class,
            ForbiddenException::class,
            JwtTokenUnauthorizedException::class,
        )
        fun handleException(
            ex: Exception,
            request: WebRequest,
        ): ResponseEntity<ApiError> {
            val headers = HttpHeaders()
            if (ex is JwtTokenUnauthorizedException) {
                return handleJwtTokenUnauthorizedException(ex, headers, request)
            } else if (ex is DineSykmeldteRequestException) {
                return handleDineSykmeldteRequestException(ex, headers, request)
            } else if (ex is HttpMessageNotReadableException) {
                return handleHttpMessageNotReadableException(headers)
            } else if (ex is MethodArgumentTypeMismatchException) {
                return handleHttpMessageNotReadableException(headers)
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
                    handleExceptionInternal(ex, ApiError(status.value(), internalMsg), headers, status, request)
                }
            }
        }

        private fun handleJwtTokenUnauthorizedException(
            ex: JwtTokenUnauthorizedException,
            headers: HttpHeaders,
            request: WebRequest,
        ): ResponseEntity<ApiError> =
            handleExceptionInternal(
                ex,
                ApiError(HttpStatus.UNAUTHORIZED.value(), unauthorizedMsg),
                headers,
                HttpStatus.UNAUTHORIZED,
                request,
            )

        private fun handleDineSykmeldteRequestException(
            ex: DineSykmeldteRequestException,
            headers: HttpHeaders,
            request: WebRequest,
        ): ResponseEntity<ApiError> =
            handleExceptionInternal(
                ex,
                ApiError(HttpStatus.BAD_GATEWAY.value(), badGatewayMsg),
                headers,
                HttpStatus.BAD_GATEWAY,
                request,
            )

        private fun handleHttpMessageNotReadableException(headers: HttpHeaders): ResponseEntity<ApiError> =
            ResponseEntity(
                ApiError(HttpStatus.BAD_REQUEST.value(), badRequestMsg),
                headers,
                HttpStatus.BAD_REQUEST,
            ).also {
                metric.tellHttpKall(HttpStatus.BAD_REQUEST.value())
            }

        private fun handleForbiddenException(
            ex: ForbiddenException,
            headers: HttpHeaders,
            request: WebRequest,
        ): ResponseEntity<ApiError> =
            handleExceptionInternal(ex, ApiError(HttpStatus.FORBIDDEN.value(), forbiddenMsg), headers, HttpStatus.FORBIDDEN, request)

        private fun handleIllegalArgumentException(
            ex: IllegalArgumentException,
            headers: HttpHeaders,
            request: WebRequest,
        ): ResponseEntity<ApiError> =
            handleExceptionInternal(ex, ApiError(HttpStatus.BAD_REQUEST.value(), badRequestMsg), headers, HttpStatus.BAD_REQUEST, request)

        private fun handleConstraintViolationException(
            ex: ConstraintViolationException,
            headers: HttpHeaders,
            request: WebRequest,
        ): ResponseEntity<ApiError> =
            handleExceptionInternal(ex, ApiError(HttpStatus.BAD_REQUEST.value(), badRequestMsg), headers, HttpStatus.BAD_REQUEST, request)

        private fun handleConflictException(
            ex: ConflictException,
            headers: HttpHeaders,
            request: WebRequest,
        ): ResponseEntity<ApiError> {
            val status = HttpStatus.CONFLICT
            return handleExceptionInternal(ex, ApiError(status.value(), conflictMsg), headers, status, request)
        }

        private fun handleExceptionInternal(
            ex: Exception,
            body: ApiError,
            headers: HttpHeaders,
            status: HttpStatus,
            request: WebRequest,
        ): ResponseEntity<ApiError> {
            metric.tellHttpKall(status.value())
            if (!status.is2xxSuccessful) {
                if (HttpStatus.INTERNAL_SERVER_ERROR == status) {
                    log.error("Uventet feil: {} : {}", ex.javaClass.toString(), ex.message, ex)
                    request.setAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE, ex, WebRequest.SCOPE_REQUEST)
                } else {
                    log.warn("Fikk response med kode : {} : {} : {}", status.value(), ex.javaClass.toString(), ex.message)
                }
            }
            return ResponseEntity(body, headers, status)
        }

        companion object {
            private val log = LoggerFactory.getLogger(ControllerExceptionHandler::class.java)
        }
    }
