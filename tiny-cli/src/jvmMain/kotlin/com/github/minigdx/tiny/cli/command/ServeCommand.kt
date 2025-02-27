package com.github.minigdx.tiny.cli.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.get
import io.ktor.server.routing.head
import io.ktor.server.routing.routing
import java.io.FileInputStream
import java.util.zip.ZipInputStream

class ServeCommand : CliktCommand(name = "serve") {

    private val port by option(help = "Port of the local webserver.")
        .int()
        .default(8080)

    private val game by argument(
        help =
        "The game to serve. It has to be a tiny game exported " +
            "by the export command (ie: zip file).",
    )
        .file(mustExist = true, canBeDir = true, canBeFile = true)

    private val resources = mutableMapOf<String, ByteArray>()

    override fun run() {
        // Get the zip
        val zipFile = if (game.isDirectory) {
            GameExporter().export(game, "tiny-export.zip")
            game.resolve("tiny-export.zip")
        } else {
            game
        }

        // Uncompressed in memory
        val zip = ZipInputStream(FileInputStream(zipFile))

        var entry = zip.nextEntry
        while (entry != null) {
            resources[entry.name] = zip.readAllBytes()
            zip.closeEntry()
            entry = zip.nextEntry
        }

        val method = fun Application.() {
            routing {
                head("/{key}") {
                    call.respond(HttpStatusCode.OK)
                }
                get("/{key}") {
                    val key = call.parameters["key"]?.let { k ->
                        // Small hack as the engine add a /.
                        // Need to fix it...
                        if (k.startsWith("/")) {
                            k.drop(1)
                        } else {
                            k
                        }
                    }
                    if (key != null && resources.containsKey(key)) {
                        val value = resources[key]
                        if (value != null) {
                            val contentType = if (key.endsWith(".js")) {
                                ContentType.Application.JavaScript
                            } else if (key.endsWith(".png")) {
                                ContentType.Image.PNG
                            } else if (key.endsWith(".json")) {
                                ContentType.Application.Json
                            } else if (key.endsWith(".mid")) {
                                ContentType.Application.OctetStream
                            } else {
                                ContentType.Application.OctetStream
                            }
                            call.respondBytes(value, contentType)
                        } else {
                            call.respond(HttpStatusCode.NotFound)
                        }
                    } else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                }
                get("/") {
                    val key = "index.html"
                    val value = resources[key]
                    if (value != null) {
                        call.respondBytes(value, ContentType.Text.Html)
                    } else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                }
            }
        }

        // Start a webserver using ktor
        // Creates a Netty server
        val server = embeddedServer(Netty, port = port, module = method)

        echo("\uD83D\uDE80 Try your game on http://localhost:$port with your browser.")
        // Starts the server and waits for the engine to stop and exits.
        server.start(wait = true)
        // start a browser to the address
        // route to files from the zip.
    }
}
