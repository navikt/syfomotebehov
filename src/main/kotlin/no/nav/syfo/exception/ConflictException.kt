package no.nav.syfo.exception

import javax.ws.rs.WebApplicationException

class ConflictException : WebApplicationException(409)
