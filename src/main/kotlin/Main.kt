@file:JvmName("Main")

package archives.tater.bot.mathwriter

import archives.tater.bot.mathwriter.data.KordSerializer
import archives.tater.bot.mathwriter.data.getWebhookFor
import archives.tater.bot.mathwriter.data.loadGameState
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.threads.ThreadChannelBehavior
import dev.kord.core.behavior.execute
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.on
import dev.kord.gateway.Intent
import dev.kord.gateway.Intents
import dev.kord.gateway.PrivilegedIntent
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.message.MessageBuilder
import io.github.cdimascio.dotenv.Dotenv
import io.ktor.client.request.forms.*
import io.ktor.utils.io.*

@OptIn(PrivilegedIntent::class)
suspend fun main() {
    val dotenv = Dotenv.load()
    with (Kord(dotenv["BOT_TOKEN"])) {
        KordSerializer.setup(this)
        loadGameState()

        createGlobalChatInputCommand("latex", "Display LaTeX as image") {
            dmPermission = false
            string("latex", "The LaTeX to display") {
                required = true
            }
        }

        on<GuildChatInputCommandInteractionCreateEvent> {
            if (interaction.invokedCommandName != "latex") interaction.respondEphemeral {
                content = "Unknown command"
            }

            val message = interaction.deferPublicResponse()
            when (val result = renderLatex(interaction.command.strings["latex"]!!)) {
                is ImageResult.Error -> {
                    message.respond {
                        content = "Error rendering latex:\n```${result.message.let {
                            if (it.length > 1500) "...${it.substring(it.length-1500, it.length)}" else this
                        }}```"
                    }
                    //                        launch {
                    //                            delay(5000)
                    //                            message.delete()
                    //                        }
                }

                is ImageResult.Success -> {
                    fun MessageBuilder.addMathImage() {
                        addFile("math.png", ChannelProvider(result.bytes.size.toLong()) {
                            ByteReadChannel(result.bytes)
                        })
                    }

                    getWebhookFor(interaction.channel)?.run {
                        execute(token!!, (interaction.channel as? ThreadChannelBehavior)?.id) {
                            username = interaction.user.effectiveName
                            avatarUrl = interaction.user.run { memberAvatar ?: avatar ?: defaultAvatar}.cdnUrl.toUrl()
                            addMathImage()
                        }
                        message.delete()
                    } ?: message.respond {
                        addMathImage()
                    }
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
