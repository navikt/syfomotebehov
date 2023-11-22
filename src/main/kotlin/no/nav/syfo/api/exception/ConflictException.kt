package no.nav.syfo.api.exception

import jakarta.ws.rs.WebApplicationException

class ConflictException : WebApplicationException(409)
