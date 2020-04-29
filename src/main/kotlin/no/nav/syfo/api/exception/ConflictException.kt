package no.nav.syfo.api.exception

import javax.ws.rs.WebApplicationException

class ConflictException : WebApplicationException(409)
