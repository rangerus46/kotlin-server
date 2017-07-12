package me.tomaszwojcik.kotlinserver

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URI
import java.nio.file.Paths

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
        log.info("${req.method} ${req.uri} ${req.httpVersion} -- ${res.status.code}")
        return true
    }
}

class HostHeaderHandler : Handler {
    override fun invoke(req: HttpReq, res: HttpRes): Boolean {
        if (req.httpVersion == HttpVersions.HTTP_1_0) {
            return true
        }

        val host = req.headers[HttpHeaders.HOST]
        if (host == null) {
            res.status = HttpStatus.BAD_REQUEST
            return false
        }

        // TODO: handle non-null host

        return true
    }
}

class StaticContentHandler(
        val root: String,
        val index: Array<String> = arrayOf("index.html")
) : Handler {
    override fun invoke(req: HttpReq, res: HttpRes): Boolean {
        val uri = URI.create(req.uri)
        val path = Paths.get(root, uri.path).normalize()

        var file: File? = null
        if (path.startsWith(root)) {
            file = path.toFile()
            if (file.isDirectory && uri.path.endsWith('/')) {
                file = findIndex(file)
            }
        }

        if (file != null && file.isFile) {
            res.status = HttpStatus.OK
            res.body = file
        } else {
            res.status = HttpStatus.NOT_FOUND
            res.body = null
        }

        return true
    }

    // --

    fun findIndex(dir: File): File? {
            return index
                    .map { s -> File(dir, s) }
                    .firstOrNull { it.isFile }
    }
}
