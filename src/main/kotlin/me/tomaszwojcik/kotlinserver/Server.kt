package me.tomaszwojcik.kotlinserver

import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.util.concurrent.Executors

fun main(args: Array<String>) {
    val server = Server(port = 8080)
    val serverThread = Thread(server)
    serverThread.start()
    serverThread.join()
}

class Server(val port: Int = 8080, nThreads: Int = 1) : Runnable {
    companion object {
        private val LOG = LoggerFactory.getLogger(Server::class.java)
    }

    private var stopped = false
    private val serverSocket = ServerSocket()
    private val executorService = Executors.newFixedThreadPool(nThreads)

    private val handlers = listOf(
            LogHandler()
    )

    override fun run() {
        val address = InetSocketAddress(port)
        serverSocket.bind(address)

        LOG.info("Listening on port: {}", port)

        serverSocket.use {
            while (!stopped) {
                val socket = serverSocket.accept()
                val worker = Worker(socket, handlers)
                executorService.execute(worker)
            }
        }
    }
}

class ServerException(message: String) : Exception(message)
