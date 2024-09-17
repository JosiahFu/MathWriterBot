package archives.tater.bot.mathwriter.data

import dev.kord.core.behavior.channel.GuildMessageChannelBehavior
import dev.kord.core.behavior.channel.TopGuildMessageChannelBehavior
import dev.kord.core.behavior.channel.createWebhook
import dev.kord.core.behavior.channel.threads.ThreadChannelBehavior
import dev.kord.core.entity.Webhook
import kotlinx.coroutines.flow.firstOrNull

val webhooks = mutableMapOf<TopGuildMessageChannelBehavior, Webhook>()

suspend fun topChannelOf(channelBehavior: GuildMessageChannelBehavior): TopGuildMessageChannelBehavior? = when (val channel = channelBehavior.asChannel()) {
    is TopGuildMessageChannelBehavior -> channel
    is ThreadChannelBehavior -> channel.parent as? TopGuildMessageChannelBehavior
    else -> null
}

suspend fun getWebhookFor(channelBehavior: GuildMessageChannelBehavior): Webhook? {
    val topChannel = topChannelOf(channelBehavior) ?: return null
    return webhooks[topChannel] ?: topChannel.webhooks.firstOrNull {
        it.data.applicationId == channelBehavior.kord.selfId
    }?.also {
        webhooks[topChannel] = it
    }
}

suspend fun getOrCreateWebhookFor(channelBehavior: GuildMessageChannelBehavior): Webhook? {
    val topChannel = topChannelOf(channelBehavior) ?: return null
    return getWebhookFor(channelBehavior) ?: topChannel.createWebhook("Math Writer") {
        reason = "To display math appearing as each user"
    }.also {
        webhooks[topChannel] = it
    }
}
