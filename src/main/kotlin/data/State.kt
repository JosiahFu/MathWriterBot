package archives.tater.bot.mathwriter.data

import archives.tater.bot.mathwriter.lib.ObservableMap
import dev.kord.core.behavior.channel.GuildMessageChannelBehavior
import dev.kord.core.behavior.channel.TopGuildMessageChannelBehavior
import dev.kord.core.behavior.channel.createWebhook
import dev.kord.core.behavior.channel.threads.ThreadChannelBehavior
import dev.kord.core.entity.Webhook
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileNotFoundException

val STATE_FILE = File("state.json")

lateinit var STATE: MutableMap<SerializableTopChannel, SerializableWebhook>

fun loadGameState() {
    STATE = ObservableMap(try {
        Json.decodeFromString(STATE_FILE.readText())
    } catch (e: FileNotFoundException) {
        mutableMapOf()
    }) {
        STATE_FILE.writeText(Json.encodeToString(STATE))
    }
}

suspend fun getWebhookFor(channelBehavior: GuildMessageChannelBehavior): Webhook? {
    val channel = channelBehavior.asChannel()
    val topChannel: TopGuildMessageChannelBehavior = when (channel) {
        is TopGuildMessageChannelBehavior -> channel
        is ThreadChannelBehavior -> channel.parent as? TopGuildMessageChannelBehavior ?: return null
        else -> return null
    }
    return STATE[topChannel]?.let {
        it as? Webhook ?: channel.kord.getWebhookOrNull(it.id)?.also { new -> STATE[topChannel] = new }
    } ?: topChannel.createWebhook("Math Writer") {
        reason = "To display math appearing as each user"
    }.also {
        STATE[topChannel] = it
    }
}
