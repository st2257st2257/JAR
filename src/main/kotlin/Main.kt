@file:Suppress("ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import java.io.FileInputStream
import java.time.Instant


private val selectorManager = ActorSelectorManager(Dispatchers.IO)
internal const val virtualDevicePort = 9002


internal fun CoroutineScope.launchVirtualDevice(): Job {
    var noise: Double = 0.1
    val harmonics = mutableMapOf(0.1 to 1.0)


    fun readValue(): String {
        Instant.now().toEpochMilli().toDouble() / 1000.0
        val fileName = "/media/pi/88C5-1B1A/Samples/Papers/New"
        return FileInputStream(fileName).readBytes()
            .toString(Charsets.UTF_8) //harmonics.entries.sumOf {fileData}//it.value * sin(it.key * time) } + rng.nextGaussian() * noise
    }

    val serverSocket = aSocket(selectorManager).tcp().bind(port = virtualDevicePort)
    println("Virtual device server listening at ${serverSocket.localAddress}")

    var shutdown = false

    return launch {
        while (isActive && !shutdown ) {
            val socket = serverSocket.accept()
            println("Accepted $socket")
            launch {
                val read = socket.openReadChannel()
                val write = socket.openWriteChannel(autoFlush = true)

                write.writeStringUtf8("HELLO!\n")

                try {
                    while (isActive && !shutdown) {
                        val line = read.readUTF8Line() ?: continue
                        val tokens = line.split(" ")

                        if (tokens.isEmpty()) continue

                        val response = try {
                            when (val command = tokens.first().uppercase()) {
                                "READ" -> "VALUE ${readValue()}"
                                "SHUTDOWN" -> {
                                    shutdown = true
                                    "OK SHUTDOWN"
                                }
                                "SET" -> if (tokens.size == 3 && tokens[1].uppercase() == "NOISE") {
                                    val noiseValue = tokens[2].toDouble()
                                    noise = noiseValue
                                    "OK NOISE $noiseValue"
                                } else if (tokens.size == 4 && tokens[1].uppercase() == "FREQUENCY") {
                                    val freq = tokens[2].toDouble()
                                    val amplitude = tokens[3].toDouble()
                                    harmonics[freq] = amplitude
                                    "OK FREQUENCY $freq $amplitude"
                                } else {
                                    "ERROR \"Malformed request $line\""
                                }
                                else -> "ERROR \"Unknown command $command\""
                            }
                        } catch (t: Throwable) {
                            "ERROR \"${t.message}\""
                        }

                        write.writeStringUtf8("$response\n")
                    }
                } catch (e: Throwable) {
                    withContext(Dispatchers.IO) {
                        socket.close()
                    }
                }
            }
        }
    }
}


suspend fun main(): Unit {
    GlobalScope.launchVirtualDevice().join()
}

object Application {
    @JvmStatic
    fun main(args: Array<String>) {
    ///    main()
      println("server")
        runBlocking {
            GlobalScope.launchVirtualDevice().join()
        }


    }
}

/*
fun main(args: Array<String>) {
    println("Hello World!")

    // Try adding program arguments via Run/Debug configuration.
    // Learn more about running applications: https://www.jetbrains.com/help/idea/running-applications.html.
    println("Program arguments: ${args.joinToString()}")
}

 */