package me.tomaszwojcik.kotlinserver

import org.slf4j.Logger
import org.slf4j.LoggerFactory

interface Handler: (HttpReq, HttpRes) -> Boolean {
    /**
     * @param req inbound http request
     * @param res outbound http response to be modified by the handler
     *
     * @return true if the next handler should be invoked
     */
    override fun invoke(req: HttpReq, res: HttpRes): Boolean
}

class LogHandler(name: String? = null) : Handler {
    private val log: Logger = LoggerFactory.getLogger(name ?: javaClass.name)

    override fun invoke(req: HttpReq, res: HttpRes): Boolean {
        log.info("${req.method} ${req.uri} ${req.httpVersion} -- ${res.status}")
        return true
    }
}
