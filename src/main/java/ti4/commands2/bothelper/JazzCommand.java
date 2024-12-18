package ti4.commands2.bothelper;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import ti4.buttons.Buttons;
import ti4.commands2.GameStateSubcommand;
import ti4.helpers.ButtonHelper;
import ti4.helpers.Constants;
import ti4.helpers.Emojis;
import ti4.helpers.settingsFramework.menus.MiltySettings;
import ti4.map.Game;
import ti4.message.BotLogger;
import ti4.message.MessageHelper;

class JazzCommand extends GameStateSubcommand {

    public JazzCommand() {
        super("jazz_command", "jazzxhands", false, false);
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!jazzCheck(event)) return;
        //sendJazzButton(event);

        Game game = getGame();
        ButtonHelper.resolveSetupColorChecker(game);
    }

    public static void sendJazzButton(GenericInteractionCreateEvent event) {
        Emoji spinner = Emoji.fromFormatted(Emojis.scoutSpinner);
        Button jazz = Buttons.green("jazzButton", spinner.getFormatted());
        MessageHelper.sendMessageToChannelWithButton(event.getMessageChannel(), Constants.jazzPing() + " button", jazz);
    }

    public static boolean jazzCheck(GenericInteractionCreateEvent event) {
        if (Constants.jazzId.equals(event.getUser().getId())) return true;
        if (Constants.honoraryJazz.contains(event.getUser().getId())) {
            MessageHelper.sendMessageToChannel(event.getMessageChannel(), "You are an honorary jazz so you may proceed");
            return true;
        }
        MessageHelper.sendMessageToChannel(event.getMessageChannel(), "You are not " + Constants.jazzPing());
        return false;
    }

    public String json(MiltySettings object) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(object);
        } catch (Exception e) {
            BotLogger.log("Error mapping to json: ", e);
        }
        return null;
    }
}
