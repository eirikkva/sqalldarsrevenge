import com.sqalldarsrevenge.discord.aws.InstanceManager
import dev.kord.core.Kord
import dev.kord.core.entity.ReactionEmoji
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import io.github.cdimascio.dotenv.dotenv
import kotlinx.coroutines.delay

suspend fun main() {
    val dotenv = dotenv()
    val botToken = dotenv["DISCORD_BOT_TOKEN"]
    val client = Kord(botToken)
    val pingPong = ReactionEmoji.Unicode("\uD83C\uDFD3")
    val grin = ReactionEmoji.Unicode("\uD83D\uDE01")

    val gameServer = object {
        val INSTANCE_ID = dotenv["INSTANCE_ID"]
        val NAME = dotenv["SERVER_NAME"]
        val PASSWORD = dotenv["SERVER_PASSWORD"]
    }

    client.on<MessageCreateEvent> {
        if (message.content.startsWith("?start")) {
            val confirmation = message.channel.createMessage("Starting ${gameServer.NAME}")
            val manager = InstanceManager(instanceId = gameServer.INSTANCE_ID)
            manager.startInstance()
            val description = manager.describeEc2Instance()
            val ip = description.ip ?: "unknown"

            confirmation.addReaction(grin)
            message.channel.createMessage("${gameServer.NAME} / ${gameServer.PASSWORD} is running on IP: $ip")        }

        if (message.content.startsWith("?stop")) {
            message.channel.createMessage("Stopping ${gameServer.NAME}")
            val manager = InstanceManager(instanceId = gameServer.INSTANCE_ID)
            manager.stopInstance()
            var instance =
                manager.describeInstance().reservations?.map { r -> r.instances?.find { i -> i.instanceId == gameServer.INSTANCE_ID } }
                    ?.first()
            var currentState = instance?.state?.name?.value ?: "unknown"

            while (currentState != "stopped" && currentState != "unknown") {
                println("Current state: $currentState")
                delay(3000)
                instance =
                    manager.describeInstance().reservations?.map { r -> r.instances?.find { i -> i.instanceId == gameServer.INSTANCE_ID } }
                        ?.first()
                currentState = instance?.state?.name?.value ?: "unknown"
            }

            message.channel.createMessage("Stopped ${gameServer.NAME}")
        }

        if (message.content.startsWith("?state")) {
            message.channel.createMessage("Checking state on ${gameServer.NAME}")
            val manager = InstanceManager(instanceId = gameServer.INSTANCE_ID)
            val instanceState = manager.checkInstance()
            val currentState = instanceState?.name?.value ?: "Unknown"
            val description = manager.describeEc2Instance()
            val gamerServerState =
                description.ip?.let { "${gameServer.NAME} / ${gameServer.PASSWORD} is : $currentState on: $it" }
                    ?: "${gameServer.NAME} is : $currentState"

            message.channel.createMessage(gamerServerState)
        }

        if (message.content == "!ping") {
            val response = message.channel.createMessage("Pong!")
            response.addReaction(pingPong)

            delay(5000)
            message.delete()
            response.delete()
        }
    }


    client.login()

}