@file:JvmName("Main")

package archives.tater.bot.mathwriter

import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.on
import dev.kord.gateway.Intent
import dev.kord.gateway.Intents
import dev.kord.gateway.PrivilegedIntent
import dev.kord.rest.builder.interaction.string
import io.github.cdimascio.dotenv.Dotenv
import io.ktor.client.request.forms.*
import io.ktor.utils.io.*

@OptIn(PrivilegedIntent::class)
suspend fun main() {
    WORK_DIR.mkdir()
    val dotenv = Dotenv.load()
    with (Kord(dotenv["BOT_TOKEN"])) {
        createGlobalChatInputCommand("latex", "Display LaTeX as image") {
            dmPermission = false
            string("latex", "The LaTeX to display") {
                required = true
            }
        }

        on<GuildChatInputCommandInteractionCreateEvent> {
            if (interaction.invokedCommandName == "latex") {
                val message = interaction.deferPublicResponse()
                val image = renderLatex(interaction.command.strings["latex"]!!)
                if (image == null) {
                    message.respond {
                        content = "Error rendering latex"
                    }
                    return@on
                }
                message.respond {
                    addFile("math.png", ChannelProvider(image.size.toLong()) { ByteReadChannel(image) })
                }
            }
        }

        on<ReadyEvent> {
            println("Logged in!")
        }

        login {
            intents = Intents(Intent.GuildMessages, Intent.MessageContent, Intent.GuildWebhooks)
        }
    }
}
