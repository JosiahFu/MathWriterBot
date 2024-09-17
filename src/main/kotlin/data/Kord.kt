package archives.tater.bot.mathwriter.data

import dev.kord.common.annotation.KordExperimental
import dev.kord.common.annotation.KordUnsafe
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.Unsafe
import dev.kord.core.behavior.MemberBehavior
import dev.kord.core.behavior.MessageBehavior
import dev.kord.core.behavior.WebhookBehavior
import dev.kord.core.behavior.channel.TopGuildMessageChannelBehavior
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@JvmInline
value class Box<T>(val value: T)

operator fun <K, V> Map<Box<K>, Box<V>>.get(key: K): V? = get(Box(key))?.value
operator fun <K, V> MutableMap<Box<K>, Box<V>>.set(key: K, value: V) {
    set(Box(key), Box(value))
}

@OptIn(KordExperimental::class, KordUnsafe::class)
internal object KordSerializer {
    lateinit var unsafe: Unsafe

    fun setup(kord: Kord) {
        unsafe = Unsafe(kord)
    }

    private fun Decoder.decodeSnowflake() = Snowflake(decodeLong().toULong())

    abstract class Single<T>(
        name: String,
        private val init: (Snowflake) -> T,
        private val getId: T.() -> Snowflake,
    ) : KSerializer<T> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(name, PrimitiveKind.LONG)

        override fun deserialize(decoder: Decoder): T = init(decoder.decodeSnowflake())

        override fun serialize(encoder: Encoder, value: T) {
            encoder.encodeLong(value.getId().value.toLong())
        }
    }

    abstract class Paired<T>(
        name: String,
        private val init: (Snowflake, Snowflake) -> T,
        private val getFirst: T.() -> Snowflake,
        private val getSecond: T.() -> Snowflake
    ) : KSerializer<T> {
        override val descriptor = PrimitiveSerialDescriptor(name, PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): T {
            val (first, second) = decoder.decodeString().split(":").map { Snowflake(it.toLong()) }
            return init(first, second)
        }

        override fun serialize(encoder: Encoder, value: T) {
            encoder.encodeString("${value.getFirst().value.toLong()}:${value.getSecond().value.toLong()}")
        }
    }

    abstract class BoxedPaired<T>(
        name: String,
        init: (Snowflake, Snowflake) -> T,
        getFirst: T.() -> Snowflake,
        getSecond: T.() -> Snowflake
    ) : Paired<Box<T>>(
        name,
        { first, second -> Box(init(first, second)) },
        { value.getFirst() },
        { value.getSecond() }
    )

    abstract class BoxedSingle<T>(
        name: String,
        init: (Snowflake) -> T,
        getId: T.() -> Snowflake,
    ) : Single<Box<T>>(
        name,
        { Box(init(it)) },
        { value.getId() }
    )

    class TopGuildMessageChannel : BoxedPaired<TopGuildMessageChannelBehavior>(
        "TopGuildMessageChannelBehavior",
        unsafe::topGuildMessageChannel,
        TopGuildMessageChannelBehavior::guildId,
        TopGuildMessageChannelBehavior::id,
    )

    class Member : Paired<MemberBehavior>(
        "MemberBehavior",
        unsafe::member,
        MemberBehavior::guildId,
        MemberBehavior::id,
    )

    class Message : Paired<MessageBehavior>(
        "MessageBehavior",
        unsafe::message,
        MessageBehavior::channelId,
        MessageBehavior::id,
    )

    class Webhook : BoxedSingle<WebhookBehavior>(
        "WebhookBehavior",
        unsafe::webhook,
        WebhookBehavior::id,
    )
}

typealias SerializableTopChannel = @Serializable(with = KordSerializer.TopGuildMessageChannel::class) Box<TopGuildMessageChannelBehavior>
typealias SerializableMessage = @Serializable(with = KordSerializer.Message::class) MessageBehavior
typealias SerializableMember = @Serializable(with = KordSerializer.Member::class) MemberBehavior
typealias SerializableWebhook = @Serializable(with = KordSerializer.Webhook::class) Box<WebhookBehavior>
