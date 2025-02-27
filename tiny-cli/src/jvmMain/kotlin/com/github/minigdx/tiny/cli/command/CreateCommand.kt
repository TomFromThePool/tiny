package com.github.minigdx.tiny.cli.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import com.github.minigdx.tiny.cli.GamePalette
import com.github.minigdx.tiny.cli.config.GameParameters
import com.github.minigdx.tiny.cli.config.GameParameters.Companion.JSON
import com.github.minigdx.tiny.cli.config.GameParametersV1
import com.github.minigdx.tiny.cli.config.Size
import kotlinx.serialization.json.encodeToStream
import org.intellij.lang.annotations.Language
import java.io.File
import java.io.FileOutputStream

@Language("Lua")
private const val DEFAULT_GAME_SCRIPT = """
function _init()
    
end
    
    
function _update()
    
end
    
    
function _draw()

end
"""
class CreateCommand : CliktCommand(name = "create") {

    val gameDirectory by argument(help = "The directory containing all game information")
        .file(mustExist = false, canBeDir = true, canBeFile = false)
        .default(File("."))

    private val gameName by option(help = "🏷 The name of the game")
        .prompt(default = generateRandomGameName())

    private val gameResolution by option(help = "🖥 The game resolution (e.g., 800x600)")
        .prompt(default = "256x256")
        .validate { require(it.matches(Regex("\\d+x\\d+"))) { "Invalid resolution format: $it" } }

    private val gameScript by option(help = "\uD83D\uDCDD Name of the default game script")
        .prompt(default = "game.lua")
        .validate { require(it.endsWith(".lua")) { "Invalid game script extension: $it" } }

    private val spriteSize by option(help = "📐 The sprite size (e.g., 16x16)")
        .prompt(default = "16x16")
        .validate { require(it.matches(Regex("\\d+x\\d+"))) { "Invalid resolution format: $it" } }

    private val zoom by option(help = "🔍 Game zoom")
        .int()
        .prompt(default = "2")

    private val spritesheets by option(help = "\uD83D\uDCC4 The filenames of the sprite sheets, separated by a comma (e.g., file1.png, file2.png)")
        .prompt(default = "")
        .validate {
            require(
                it.isEmpty() || it.split(",")
                    .all { f -> f.trim().endsWith(".png") },
            ) { "Invalid image file $it. Only *.png are supported" }
        }

    private val palette by option(help = "🎨 The Color palette to use")
        .int()
        .prompt(
            """Please choose a game color palette:
${
            GamePalette.ALL.mapIndexed { index, gamePalette ->
                "[${index + 1}] ${gamePalette.name}"
            }.joinToString("\n")
            }
""",
        )

    override fun run() {
        echo("➡\uFE0F Game Name: $gameName")
        echo("➡\uFE0F Game Resolution: $gameResolution")
        echo("➡\uFE0F Game Resolution: $spriteSize")
        echo("➡\uFE0F Sprite Sheet Filenames: ${spritesheets.ifBlank { "No spritesheet added!" }}")
        echo("➡\uFE0F Color palette: ${GamePalette.ALL[palette - 1].name}")

        val configuration = GameParametersV1(
            name = gameName,
            resolution = gameResolution.toSize(),
            sprites = spriteSize.toSize(),
            zoom = zoom,
            colors = GamePalette.ALL[palette - 1].colors,
            scripts = listOf(gameScript),
        ) as GameParameters

        if (!gameDirectory.exists()) gameDirectory.mkdirs()

        val configurationFile = gameDirectory.resolve("_tiny.json")
        FileOutputStream(configurationFile).use {
            JSON.encodeToStream(configuration, it)
        }

        gameDirectory.resolve(gameScript).writeText(DEFAULT_GAME_SCRIPT)

        echo("\uD83C\uDFD7\uFE0F  Game created into: ${gameDirectory.absolutePath}")
        echo("\uD83C\uDFC3\u200D♂\uFE0F To run the game: tiny-cli run ${gameDirectory.absolutePath}")
    }

    private fun String.toSize(): Size {
        val (w, h) = this.split("x")
        return Size(w.toInt(), h.toInt())
    }

    private fun generateRandomGameName(): String {
        val adjectives = listOf("Funny", "Awesome", "Crazy", "Epic", "Mystical", "Magical")
        val nouns = listOf("Unicorns", "Pandas", "Robots", "Dragons", "Ninjas", "Pirates")
        return "${adjectives.random()} ${nouns.random()} Game"
    }
}
