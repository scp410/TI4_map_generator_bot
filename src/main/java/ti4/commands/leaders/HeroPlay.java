package ti4.commands.leaders;

import java.util.ArrayList;
import java.util.List;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import ti4.buttons.Buttons;
import ti4.commands.agenda.DrawAgenda;
import ti4.commands.cardsac.ACInfo;
import ti4.commands.explore.DrawRelic;
import ti4.commands.planet.PlanetRefresh;
import ti4.commands.special.KeleresHeroMentak;
import ti4.commands.special.RiseOfMessiah;
import ti4.commands.status.ListTurnOrder;
import ti4.commands.tokens.AddCC;
import ti4.commands.tokens.AddFrontierTokens;
import ti4.commands.tokens.RemoveCC;
import ti4.commands.units.AddUnits;
import ti4.helpers.ButtonHelper;
import ti4.helpers.ButtonHelperAgents;
import ti4.helpers.ButtonHelperFactionSpecific;
import ti4.helpers.ButtonHelperHeroes;
import ti4.helpers.CombatTempModHelper;
import ti4.helpers.Constants;
import ti4.helpers.Emojis;
import ti4.helpers.FoWHelper;
import ti4.helpers.Helper;
import ti4.helpers.Units.UnitType;
import ti4.map.Game;
import ti4.map.Leader;
import ti4.map.Player;
import ti4.map.Tile;
import ti4.map.UnitHolder;
import ti4.message.BotLogger;
import ti4.message.MessageHelper;
import ti4.model.LeaderModel;
import ti4.model.TemporaryCombatModifierModel;

public class HeroPlay extends LeaderAction {
    public HeroPlay() {
        super(Constants.ACTIVE_LEADER, "Play Hero");
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        Game game = getActiveGame();
        Player player = game.getPlayer(getUser().getId());
        player = Helper.getGamePlayer(game, player, event, null);

        if (player == null) {
            MessageHelper.sendMessageToEventChannel(event, "Player could not be found");
            return;
        }
        action(event, "hero", game, player);
    }

    @Override
    protected void options() {
        addOptions(new OptionData(OptionType.STRING, Constants.LEADER, "Leader for which to do action")
            .setAutoComplete(true));
        addOptions(
            new OptionData(OptionType.STRING, Constants.FACTION_COLOR, "Faction or Color for which you set stats")
                .setAutoComplete(true));
    }

    @Override
    void action(SlashCommandInteractionEvent event, String leaderID, Game game, Player player) {
        Leader playerLeader = player.unsafeGetLeader(leaderID);

        if (playerLeader == null) {
            MessageHelper.sendMessageToEventChannel(event, "Leader '" + leaderID + "'' could not be found. The leader might have been purged earlier.");
            return;
        }

        if (playerLeader.isLocked()) {
            MessageHelper.sendMessageToEventChannel(event, "Leader is locked, use command to unlock `/leaders unlock leader:" + leaderID + "`");
            MessageHelper.sendMessageToEventChannel(event, Helper.getLeaderLockedRepresentation(playerLeader));
            return;
        }

        if (!playerLeader.getType().equals(Constants.HERO)) {
            MessageHelper.sendMessageToEventChannel(event, "Leader is not a hero");
            return;
        }

        playHero(event, game, player, playerLeader);
    }

    public static void playHero(GenericInteractionCreateEvent event, Game game, Player player, Leader playerLeader) {
        LeaderModel leaderModel = playerLeader.getLeaderModel().orElse(null);
        boolean showFlavourText = Constants.VERBOSITY_VERBOSE.equals(game.getOutputVerbosity());
        StringBuilder sb = new StringBuilder();
        if (leaderModel != null) {
            MessageHelper.sendMessageToChannel(player.getCorrectChannel(),
                player.getRepresentation() + " played:");
            player.getCorrectChannel()
                .sendMessageEmbeds(leaderModel.getRepresentationEmbed(false, true, false, showFlavourText)).queue();
        } else {
            MessageHelper.sendMessageToChannel(player.getCorrectChannel(),
                Emojis.getFactionLeaderEmoji(playerLeader));
            sb.append(player.getRepresentation()).append(" played ")
                .append(Helper.getLeaderFullRepresentation(playerLeader));
            BotLogger.log(event, "Missing LeaderModel: " + playerLeader.getId());
        }

        if ("letnevhero".equals(playerLeader.getId()) || "nomadhero".equals(playerLeader.getId())
            || "zealotshero".equals(playerLeader.getId()) || "nokarhero".equals(playerLeader.getId())
            || "kolumehero".equals(playerLeader.getId())) {
            playerLeader.setLocked(false);
            playerLeader.setActive(true);
            sb.append("\nLeader will be PURGED after status cleanup");
            MessageHelper.sendMessageToChannel(player.getCorrectChannel(), sb.toString());
            if ("zealotshero".equals(playerLeader.getId())) {
                MessageHelper.sendMessageToChannelWithButton(event.getMessageChannel(),
                    player.getRepresentation() + " Use the button to get your first non-faction tech",
                    Buttons.GET_A_FREE_TECH);
                MessageHelper.sendMessageToChannelWithButton(event.getMessageChannel(),
                    player.getRepresentation() + " Use the button to get your second non-faction tech",
                    Buttons.GET_A_FREE_TECH);
                MessageHelper.sendMessageToChannelWithButton(event.getMessageChannel(),
                    player.getRepresentation() + " Use the button to get your third non-faction tech",
                    Buttons.GET_A_FREE_TECH);
            }
        } else {
            boolean purged = true;
            String msg = "Leader " + playerLeader.getId() + " has been purged";
            if (!"mykomentorihero".equals(playerLeader.getId())) {
                purged = player.removeLeader(playerLeader);
                ButtonHelperHeroes.checkForMykoHero(game, playerLeader.getId(), player);
            } else {
                msg = "Leader " + playerLeader.getId() + " was used to copy a hero";
            }

            if (purged) {
                MessageHelper.sendMessageToChannel(player.getCorrectChannel(),
                    msg);

            } else {
                MessageHelper.sendMessageToChannel(event.getMessageChannel(),
                    "Leader was not purged - something went wrong");
                return;
            }
        }

        switch (playerLeader.getId()) {
            case "kollecchero" -> DrawRelic.drawWithAdvantage(player, event, game, game.getRealPlayers().size());
            case "titanshero" -> {
                Tile t = player.getHomeSystemTile();
                if (game.getTileFromPlanet("elysium") != null && game.getTileFromPlanet("elysium") == t) {
                    t.addToken("attachment_titanshero.png", "elysium");
                    MessageHelper.sendMessageToChannel(event.getMessageChannel(),
                        "Attachment added to Elysium and it has been readied");
                    new PlanetRefresh().doAction(player, "elysium", game);
                } else {
                    MessageHelper.sendMessageToChannel(event.getMessageChannel(),
                        "`Use the following command to add the attachment: /add_token token:titanshero`");
                }
            }
            case "florzenhero" -> {
                for (Tile tile : game.getTileMap().values()) {
                    for (UnitHolder uH : tile.getPlanetUnitHolders()) {
                        if (player.getPlanets().contains(uH.getName())
                            && !FoWHelper.otherPlayersHaveShipsInSystem(player, tile, game)) {
                            new AddUnits().unitParsing(event, player.getColor(), tile, "2 ff", game);
                            break;
                        }
                    }
                }
                MessageHelper.sendMessageToChannel(event.getMessageChannel(), player.getRepresentation(true, true)
                    + "Added 2 fighters to every system with an owned planet and no opponent ships.");
                ButtonHelperHeroes.resolveFlorzenHeroStep1(player, game);
            }
            case "kyrohero" -> {
                int dieResult = player.getLowestSC();
                game.setStoredValue("kyroHeroSC", dieResult + "");
                game.setStoredValue("kyroHeroPlayer", player.getFaction());
                MessageHelper.sendMessageToChannel(event.getMessageChannel(), "Marked the Blex Hero Target as SC #"
                    + dieResult + " and the faction that played the hero as " + player.getFaction());
                ListTurnOrder.turnOrder(event, game);
            }
            case "ghotihero" -> {
                MessageHelper.sendMessageToChannel(event.getMessageChannel(),
                    "Choose the tiles you would like to resolve ghoti hero in",
                    ButtonHelperHeroes.getTilesToGhotiHeroIn(player, game, event));
            }
            case "gledgehero" -> {
                ButtonHelperHeroes.resolveGledgeHero(player, game);
            }
            case "khraskhero" -> {
                ButtonHelperHeroes.resolveKhraskHero(player, game);
                ButtonHelperHeroes.resolveKhraskHero(player, game);
                ButtonHelperHeroes.resolveKhraskHero(player, game);
                ButtonHelperHeroes.resolveKhraskHero(player, game);
            }
            case "mortheushero" -> {
                MessageHelper.sendMessageToChannel(event.getMessageChannel(),
                    "Choose the tiles you would like to resolve ghoti hero in",
                    ButtonHelperHeroes.getTilesToGlimmersHeroIn(player, game, event));
            }
            case "axishero" -> {
                ButtonHelperHeroes.resolveAxisHeroStep1(player, game);
            }
            case "lanefirhero" -> {
                ButtonHelperHeroes.resolveLanefirHeroStep1(player, game);
            }
            case "cymiaehero" -> {
                List<Button> buttons = new ArrayList<>();
                buttons.add(
                    Button.success("cymiaeHeroStep1_" + (game.getRealPlayers().size() + 1), "Resolve Hero"));
                buttons.add(Button.primary("cymiaeHeroAutonetic", "Resolve Autonetic Memory first"));

                MessageHelper.sendMessageToChannel(player.getCorrectChannel(),
                    player.getRepresentation() + " choose whether to resolve autonetic memory or not", buttons);

            }
            case "lizhohero" -> {
                MessageHelper.sendMessageToChannelWithButton(event.getMessageChannel(),
                    "You can use the buttons in your cards info to set traps, then when you're done with that, press the following button to start distributing 12 fighters",
                    Button.success("lizhoHeroFighterResolution", "Distrubute 12 fighters"));
            }
            case "solhero" -> {
                MessageHelper.sendMessageToChannel(event.getMessageChannel(),
                    player.getRepresentation(true, true) + " removed all of your CCs from the board");
                for (Tile t : game.getTileMap().values()) {
                    if (AddCC.hasCC(event, player.getColor(), t)) {
                        RemoveCC.removeCC(event, player.getColor(), t, game);
                    }
                }
            }
            case "cheiranhero" -> {
                ButtonHelperHeroes.cheiranHeroResolution(player, game, event);
            }
            case "olradinhero" -> {
                MessageHelper.sendMessageToChannel(event.getMessageChannel(),
                    player.getRepresentation(true, true) + " added 1 infantry to each planet");
                RiseOfMessiah.doRise(player, event, game);
                ButtonHelperHeroes.offerOlradinHeroFlips(game, player);
            }
            case "argenthero" -> {
                ButtonHelperHeroes.argentHeroStep1(game, player, event);
            }
            case "l1z1xhero" -> {
                String message = player.getRepresentation()
                    + " Resolving L1 Hero. L1 Hero is at the moment implemented as a sort of tactical action, relying on the player to follow the rules. The game will know not to take a tactical CC from you, and will allow you to move out of locked systems. Reminder that you can carry infantry/ff with your dreads/flagship, and that they cant move into supernovas(or asteroid fields if you dont have antimass.)";
                List<Button> ringButtons = ButtonHelper.getPossibleRings(player, game);
                game.setL1Hero(true);
                game.resetCurrentMovedUnitsFrom1TacticalAction();
                MessageHelper.sendMessageToChannelWithButtons(event.getMessageChannel(), message, ringButtons);
            }
            case "winnuhero" -> {
                List<Button> buttons = ButtonHelperHeroes.getWinnuHeroSCButtons(game);
                MessageHelper.sendMessageToChannelWithButtons(event.getMessageChannel(), player.getRepresentation(true,
                    showFlavourText)
                    + " use the button to pick which SC you'd like to do the primary of. Reminder you can allow others to do the secondary, but they should still pay a CC for resolving it.",
                    buttons);
            }
            case "gheminaherolady" -> {
                List<Button> buttons = ButtonHelperHeroes.getButtonsForGheminaLadyHero(player, game);
                MessageHelper.sendMessageToChannelWithButtons(event.getMessageChannel(),
                    player.getRepresentation(true, true)
                        + " use the button to pick which planet you want to resolve the hero on",
                    buttons);
            }
            case "gheminaherolord" -> {
                List<Button> buttons = ButtonHelperHeroes.getButtonsForGheminaLordHero(player, game);
                MessageHelper.sendMessageToChannelWithButtons(event.getMessageChannel(),
                    player.getRepresentation(true, true)
                        + " use the button to pick which planet you want to resolve the hero on",
                    buttons);
            }
            case "arborechero" -> {
                List<Button> buttons = ButtonHelperHeroes.getArboHeroButtons(game, player);
                MessageHelper.sendMessageToChannelWithButtons(event.getMessageChannel(),
                    player.getRepresentation(true, showFlavourText)
                        + " use the buttons to build in the desired system(s)",
                    buttons);
            }
            case "saarhero" -> {
                List<Button> buttons = ButtonHelperHeroes.getSaarHeroButtons(game, player);
                MessageHelper.sendMessageToChannelWithButtons(event.getMessageChannel(),
                    player.getRepresentation(true, showFlavourText)
                        + " use the buttons to select the system to remove all opposing ff and inf from",
                    buttons);
            }
            case "edynhero" -> {
                int size = ButtonHelper.getTilesOfPlayersSpecificUnits(game, player, UnitType.Mech).size();
                MessageHelper.sendMessageToChannel(player.getCorrectChannel(), player
                    .getFactionEmoji() + " can resolve " + size
                    + " agendas cause thats how many sigils they got. After putting the agendas on top in the order you want (dont bottom any), please press the button to reveal an agenda");
                DrawAgenda.drawAgenda(event, size, game, player);
                Button flipAgenda = Button.primary("flip_agenda", "Press this to flip agenda");
                List<Button> buttons = List.of(flipAgenda);
                MessageHelper.sendMessageToChannelWithButtons(event.getMessageChannel(), "Flip Agenda", buttons);
            }
            case "kjalengardhero" -> {
                int size = ButtonHelperAgents.getGloryTokenTiles(game).size();
                for (Tile tile : ButtonHelperAgents.getGloryTokenTiles(game)) {
                    List<Button> buttons = ButtonHelper.getButtonsToRemoveYourCC(player, game, event,
                        "kjalHero_" + tile.getPosition());
                    if (buttons.size() > 0) {
                        MessageHelper.sendMessageToChannel(player.getCorrectChannel(),
                            "Use buttons to remove token from "
                                + tile.getRepresentationForButtons(game, player) + " or an adjacent tile",
                            buttons);
                    }
                }
                MessageHelper.sendMessageToChannel(player.getCorrectChannel(),
                    player.getFactionEmoji() + " can gain " + size + " CCs");
                List<Button> buttons = ButtonHelper.getGainCCButtons(player);
                String trueIdentity = player.getRepresentation(true, true);
                String message2 = trueIdentity + "! Your current CCs are " + player.getCCRepresentation()
                    + ". Use buttons to gain CCs";
                MessageHelper.sendMessageToChannelWithButtons((MessageChannel) event.getChannel(), message2, buttons);
                game.setStoredValue("originalCCsFor" + player.getFaction(), player.getCCRepresentation());
            }
            case "vaylerianhero" -> {
                if (!game.getNaaluAgent()) {
                    player.setTacticalCC(player.getTacticalCC() - 1);
                    AddCC.addCC(event, player.getColor(), game.getTileByPosition(game.getActiveSystem()));
                    game.setStoredValue("vaylerianHeroActive", "true");
                }
                List<Button> removeCCs = ButtonHelper.getButtonsToRemoveYourCC(player, game, event, "vaylerianhero");
                if (removeCCs.size() > 0) {
                    for (int x = 0; x < ButtonHelperAgents.getGloryTokenTiles(game).size(); x++) {
                        MessageHelper.sendMessageToChannel(player.getCorrectChannel(), "Use buttons to remove a token from the board", removeCCs);
                    }
                }
                MessageHelper.sendMessageToChannel(player.getCorrectChannel(),
                    player.getFactionEmoji() + " can gain 1 CC");
                List<Button> buttons = ButtonHelper.getGainCCButtons(player);
                String trueIdentity = player.getRepresentation(true, true);
                String message2 = trueIdentity + "! Your current CCs are " + player.getCCRepresentation()
                    + ". Use buttons to gain CCs";
                MessageHelper.sendMessageToChannelWithButtons((MessageChannel) event.getChannel(), message2, buttons);
                game.setStoredValue("originalCCsFor" + player.getFaction(), player.getCCRepresentation());
            }
            case "freesystemshero" -> {
                ButtonHelperHeroes.offerFreeSystemsButtons(player, game, event);
            }
            case "vadenhero" -> {
                ButtonHelperHeroes.startVadenHero(game, player);
            }
            case "veldyrhero" -> {
                game.setComponentAction(true);
                for (Player p2 : ButtonHelperFactionSpecific.getPlayersWithBranchOffices(game, player)) {
                    for (int x = 0; x < ButtonHelperFactionSpecific.getNumberOfBranchOffices(game, p2); x++) {
                        if (ButtonHelperHeroes.getPossibleTechForVeldyrToGainFromPlayer(player, p2, game)
                            .size() > 0) {
                            String msg = player.getRepresentation(true, true)
                                + " you can retrieve a unit upgrade tech from players with branch offices, one for each branch office. Here is the possible techs from "
                                + ButtonHelper.getIdentOrColor(p2, game);
                            MessageHelper.sendMessageToChannel(player.getCorrectChannel(), msg,
                                ButtonHelperHeroes.getPossibleTechForVeldyrToGainFromPlayer(player, p2, game));
                        }
                    }
                }
            }
            case "nekrohero" -> {
                List<Button> buttons = ButtonHelperHeroes.getNekroHeroButtons(player, game);
                MessageHelper.sendMessageToChannelWithButtons(event.getMessageChannel(), player.getRepresentation(true,
                    showFlavourText)
                    + " use the button to pick which planet youd like to get a tech and tgs from (and kill any opponent units)",
                    buttons);
            }
            case "bentorhero" -> {
                ButtonHelperHeroes.resolveBentorHero(game, player);
                MessageHelper.sendMessageToChannel(event.getMessageChannel(),
                    player.getFactionEmoji() + " offered buttons to explore all planets");
            }
            case "nivynhero" -> {
                ButtonHelperHeroes.resolveNivynHeroSustainEverything(game, player);
                MessageHelper.sendMessageToChannel(event.getMessageChannel(),
                    player.getFactionEmoji() + " sustained all units except their mechs");
            }
            case "jolnarhero" -> {
                List<Button> buttons = ButtonHelperHeroes.getJolNarHeroSwapOutOptions(player);
                MessageHelper.sendMessageToChannelWithButtons(event.getMessageChannel(), player.getRepresentation(true,
                    showFlavourText)
                    + " use the buttons to pick what tech you would like to swap out. Reminder that since all swap are simultenous, you cannot swap out a tech and then swap it back in.",
                    buttons);
            }
            case "yinhero" -> {
                List<Button> buttons = new ArrayList<>();
                buttons.add(Button.primary(player.getFinsFactionCheckerPrefix() + "yinHeroStart",
                    "Invade a planet with Yin Hero"));
                buttons.add(Button.danger("deleteButtons", "Delete Buttons"));
                MessageHelper.sendMessageToChannelWithButtons(event.getMessageChannel(), player.getRepresentation(true,
                    showFlavourText)
                    + " use the button to do individual invasions, then delete the buttons when you have placed 3 total infantry.",
                    buttons);
            }
            case "naazhero" -> {
                DrawRelic.drawRelicAndNotify(player, event, game);
                List<Button> buttons = ButtonHelperHeroes.getNRAHeroButtons(game);
                MessageHelper.sendMessageToChannelWithButtons(event.getMessageChannel(), player.getRepresentation(true,
                    showFlavourText)
                    + " use the button to do TWO of the available secondaries. (note, all are presented for conveinence, but two is the limit)",
                    buttons);
            }
            case "mahacthero" -> {
                List<Button> buttons = ButtonHelperHeroes.getBenediction1stTileOptions(player, game);
                MessageHelper.sendMessageToChannelWithButtons(event.getMessageChannel(),
                    player.getRepresentation(true, showFlavourText)
                        + " use the button to decide which tile you wish to force ships to move from.",
                    buttons);
            }
            case "ghosthero" -> {
                List<Button> buttons = ButtonHelperHeroes.getGhostHeroTilesStep1(game, player);
                MessageHelper.sendMessageToChannelWithButtons(event.getMessageChannel(),
                    player.getRepresentation(true, showFlavourText)
                        + " use the button to select the first tile you would like to swap with your hero.",
                    buttons);
            }
            case "augershero" -> {
                List<Button> buttons = new ArrayList<>();
                buttons.add(Button.primary(player.getFinsFactionCheckerPrefix() + "augersHeroStart_" + 1,
                    "Resolve Augers Hero on Stage 1 Deck"));
                buttons.add(Button.primary(player.getFinsFactionCheckerPrefix() + "augersHeroStart_" + 2,
                    "Resolve Augers Hero on Stage 2 Deck"));
                buttons.add(Button.danger("deleteButtons", "Delete Buttons"));
                MessageHelper.sendMessageToChannelWithButtons(event.getMessageChannel(),
                    player.getRepresentation(true, showFlavourText)
                        + " use the button to choose which objective type you wanna hero on",
                    buttons);
            }
            case "empyreanhero" -> {
                AddFrontierTokens.parsingForTile(event, game);
                MessageHelper.sendMessageToChannel(event.getMessageChannel(), "Added frontier tokens");
                MessageHelper.sendMessageToChannelWithButtons(event.getMessageChannel(),
                    "Use Buttons to explore empties", ButtonHelperHeroes.getEmpyHeroButtons(player, game));
            }
            case "cabalhero" -> {
                List<Button> buttons = ButtonHelperHeroes.getCabalHeroButtons(player, game);
                MessageHelper.sendMessageToChannelWithButtons(event.getMessageChannel(),
                    "Use Buttons to capture people", buttons);
            }
            case "yssarilhero" -> {
                for (Player p2 : game.getRealPlayers()) {
                    if (p2 == player || p2.getAc() == 0) {
                        continue;
                    }
                    List<Button> buttons = new ArrayList<>(
                        ACInfo.getYssarilHeroActionCardButtons(game, player, p2));
                    MessageHelper.sendMessageToChannelWithButtons(p2.getCardsInfoThread(),
                        p2.getRepresentation(true, true)
                            + " Yssaril hero played.  Use buttons to select which AC you will offer to them.",
                        buttons);
                }
                MessageHelper.sendMessageToChannel(event.getMessageChannel(),
                    player.getRepresentation(true, showFlavourText)
                        + " sent everyone a ping in their private threads with buttons to send you an AC");
            }
            case "keleresheroharka" -> KeleresHeroMentak.resolveKeleresHeroMentak(game, player, event);
        }
        TemporaryCombatModifierModel posssibleCombatMod = CombatTempModHelper.GetPossibleTempModifier(Constants.LEADER,
            playerLeader.getId(), player.getNumberTurns());
        if (posssibleCombatMod != null) {
            player.addNewTempCombatMod(posssibleCombatMod);
            MessageHelper.sendMessageToChannel(event.getMessageChannel(),
                "Combat modifier will be applied next time you push the combat roll button.");
        }
    }
}
