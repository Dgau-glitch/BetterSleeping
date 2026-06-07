package be.betterplugins.bettersleeping.services.messaging;

import be.betterplugins.core.messaging.messenger.MsgEntry;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.UUID;

/**
 * Sends plugin messages in the correct Folia ownership context.
 */
public interface MessageDeliveryService
{
    void send(CommandSender receiver, String messageKey, MsgEntry... entries);

    void send(UUID receiverId, String messageKey, MsgEntry... entries);

    void send(Collection<Player> receivers, String messageKey, MsgEntry... entries);
}
