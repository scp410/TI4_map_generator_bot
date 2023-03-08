package ti4.commands.cardsso;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import ti4.commands.cards.CardsInfo;
import ti4.generator.Mapper;
import ti4.helpers.Constants;
import ti4.helpers.Helper;
import ti4.map.Map;
import ti4.map.Player;
import ti4.message.MessageHelper;

public class UnscoreSO extends SOCardsSubcommandData {
    public UnscoreSO() {
        super(Constants.UNSCORE_SO, "Unscore Secret Objective");
        addOptions(new OptionData(OptionType.INTEGER, Constants.SECRET_OBJECTIVE_ID, "Scored Secret objective ID that is sent between ()").setRequired(true));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        Map activeMap = getActiveMap();
        Player player = activeMap.getPlayer(getUser().getId());
        player = Helper.getGamePlayer(activeMap, player, event, null);
        if (player == null) {
            sendMessage("Player could not be found");
            return;
        }
        OptionMapping option = event.getOption(Constants.SECRET_OBJECTIVE_ID);
        if (option == null) {
            sendMessage("Please select what Secret Objective to unscore");
            return;
        }

        boolean scored = activeMap.unscoreSecretObjective(getUser().getId(), option.getAsInt());
        if (!scored) {
            sendMessage("No such Secret Objective ID found, please retry");
            return;
        }

        sendMessage("Unscored SO " + option.getAsInt());
        CardsInfo.sentUserCardInfo(event, activeMap, player);
    }
}
