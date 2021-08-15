package dev.vrba.discordminigames.games.akinator;

import com.markozajc.akiwrapper.core.exceptions.ServerNotFoundException;
import dev.vrba.discordminigames.games.akinator.entities.AkinatorGame;
import org.javacord.api.entity.message.Message;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.listener.interaction.SlashCommandCreateListener;
import org.springframework.stereotype.Component;

@Component
public class AkinatorCommandHandler implements SlashCommandCreateListener {

    private final AkinatorGamesManager manager;

    public AkinatorCommandHandler(AkinatorGamesManager manager) {
        this.manager = manager;
    }

    @Override
    public void onSlashCommandCreate(SlashCommandCreateEvent event) {
        if (!shouldHandle(event)) {
            return;
        }

        event.getInteraction().respondLater().join();

        Message message = event.getInteraction()
                .createFollowupMessageBuilder()
                .addEmbed(AkinatorEmbedBuilders.creatingNewGameEmbed())
                .send()
                .join();

        try {
            var game = this.manager.createGame(
                    event.getInteraction().getUser().getId(),
                    message.getId()
            );


            this.updateNextQuestion(message, game);
        }
        catch (ServerNotFoundException exception) {
            message.edit(AkinatorEmbedBuilders.serverNotFoundException());
        }
    }

    private void updateNextQuestion(Message message, AkinatorGame game) {
        this.manager.getApiWrapperById(game.getId())
                .ifPresentOrElse(
                        api -> {
                            var question = api.getCurrentQuestion();

                            if (question != null) {
                                message.edit(AkinatorEmbedBuilders.createQuestionMessage(question));
                                AkinatorEmbedBuilders.reactionEmojis.forEach(emoji ->  message.addReaction(emoji.getFirst()));
                            }
                        },
                        () -> message.edit(AkinatorEmbedBuilders.missingApiInstance())
                );
    }

    private boolean shouldHandle(SlashCommandCreateEvent event) {
        return !event.getInteraction().getUser().isBot() &&
                // TODO: Should this bot support DMs?
               event.getInteraction().getServer().isPresent() &&
               event.getSlashCommandInteraction().getCommandName().equalsIgnoreCase("akinator");
    }

}
