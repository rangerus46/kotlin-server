package me.tomaszwojcik.kotlinserver

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.net.Socket

class Worker(val socket: Socket, val handlers: List<Handler>) : Runnable {
    companion object {
        val log: Logger = LoggerFactory.getLogger(Worker::class.java)
    }

    private val reader = socket.getInputStream().bufferedReader()
    private val writer = socket.getOutputStream().writer()

    override fun run() {
        socket.use {
            val res = try {
                val req = readReq()
                try {
                    handleReq(req)
                } catch (e: Exception) {
                    handleError(e, req)
                }
            } catch (e: Exception) {
                log.error("Failed to read a request!", e)
                handleError(e)
            }
            writeRes(res)
        }
    }

    // --

    private fun readReq(): HttpReq {
        var line: String
        do {
            line = reader.readLine()
        } while (line.isEmpty())

        val tokens = line.split(' ')
        if (tokens.size != 3) {
            throw ServerException("Unreadable request line: '$line'")
        }

        val method = try {
            HttpMethod.valueOf(tokens[0])
        } catch (e: IllegalArgumentException) {
            throw ServerException("Unsupported HTTP method: ${tokens[0]}")
        }

        if (!HttpVersions.validate(tokens[2])) {
            throw ServerException("Unsupported HTTP version: ${tokens[2]}")
        }

        val req = HttpReq(
                method = method,
                uri = tokens[1],
                httpVersion = tokens[2]
        )

        while (true) {
            line = reader.readLine()
            if (line.isEmpty()) {
                break
            }

            val separatorIndex = line.indexOf(':')
            if (separatorIndex < 1) {
                throw ServerException("Unreadable header line: '$line'")
            }

            req.headers.put(
                    key = line.substring(0, separatorIndex),
                    value = line.substring(separatorIndex + 2)
            )
        }

        return req
    }

    private fun handleReq(req: HttpReq): HttpRes {
        val res = HttpRes(httpVersion = req.httpVersion)
        handlers.takeWhile { it.invoke(req, res) }
        return res
    }

    private fun handleError(e: Exception, req: HttpReq? = null): HttpRes {
        return HttpRes(
                status = HttpStatus.INTERNAL_SERVER_ERROR,
                httpVersion = req?.httpVersion ?: HttpVersions.HTTP_1_1,
                body = e.toString()
        )
    }

    private fun writeRes(res: HttpRes) {
        writer.write(res.httpVersion + " " + res.status + "\r\n")

        res.headers
                .map { it.key + ": " + it.value + "\r\n" }
                .forEach(writer::write)

        writer.write("\r\n")

        val body = res.body
        when (body) {
            null -> {}
            is File -> TODO()
            else -> writer.write(body.toString())
        }

        writer.flush()
    }
}

data class HttpReq(
        var method: HttpMethod,
        var uri: String,
        val httpVersion: String,
        val headers: MutableMap<String, String> = HashMap()
)

data class HttpRes(
        var status: HttpStatus = HttpStatus.OK,
        val httpVersion: String,
        val headers: MutableMap<String, String> = HashMap(),
        var body: Any? = null
)

object HttpVersions {
    val HTTP_1_0 = "HTTP/1.0"
    val HTTP_1_1 = "HTTP/1.1"

    fun validate(s: String): Boolean =
            HTTP_1_0 == s || HTTP_1_1 == s
}

enum class HttpMethod {
    GET, POST
}

enum class HttpStatus(val code: Int, val message: String) {
    OK(200, "OK"),
    NOT_FOUND(404, "Not Found"),
    INTERNAL_SERVER_ERROR(500, "Internal Server Error");

    override fun toString(): String = "$code $message"
}
