package ti4;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import ti4.commands.*;
import ti4.generator.PositionMapper;
import ti4.generator.Mapper;
import ti4.helpers.AliasHandler;
import ti4.helpers.LoggerHandler;
import ti4.helpers.Storage;
import ti4.map.MapSaveLoadManager;

import javax.security.auth.login.LoginException;
import java.util.List;

public class MapGenerator {

    public static JDA jda;
    public static String userID;

    public static void main(String[] args)
            throws LoginException {

        jda = JDABuilder.createDefault(args[0]).build();
//        jda = JDABuilder.createLight(args[0], Collections.emptyList())
//                .addEventListeners(new Bot())
//                .setActivity(Activity.playing("Type /ping"))
//                .build();


        jda.addEventListener(new MessageListener());

        try {
            jda.awaitReady();
        } catch (InterruptedException e) {
            LoggerHandler.log("Error waiting for bot to get ready");
        }


//        User user = event.getJDA().getUserById();
//            user.getName()





        userID = args[1];

        PositionMapper.init();
        Mapper.init();
        AliasHandler.init();
        Storage.init();

        CommandManager commandManager = CommandManager.getInstance();
        commandManager.addCommand(new AddTile());
        commandManager.addCommand(new RemoveTile());
        commandManager.addCommand(new AddUnits());
        commandManager.addCommand(new RemoveUnits());
        commandManager.addCommand(new RemoveAllUnits());
        commandManager.addCommand(new CreateMap());
        commandManager.addCommand(new Shutdown());
        commandManager.addCommand(new ListTiles());
        commandManager.addCommand(new SaveMaps());
        commandManager.addCommand(new ListMaps());
        commandManager.addCommand(new SetMap());
        commandManager.addCommand(new ShowMap());
        commandManager.addCommand(new AddTileList());
        commandManager.addCommand(new DeleteMap());
        commandManager.addCommand(new ListUnits());

        Guild guild = jda.getGuildById(args[2]);

        CommandListUpdateAction commands = guild.updateCommands();

//        RestAction<List<net.dv8tion.jda.api.interactions.commands.Command>> listRestAction = guild.retrieveCommands();
//        for (Command command : commandManager.getCommandList()) {
//            guild.deleteCommandById(command.getActionID());
//
//        }
//        CommandListUpdateAction commands = jda.updateCommands();

//        commandManager.getCommandList().forEach(command -> command.registerCommands(commands));
//        commands.queue();
        //        guild.upsertCommand("show_map", "Shows selected map").queue();

        CommandListUpdateAction commands_ = jda.updateCommands();

        commandManager.getCommandList().forEach(command -> command.registerCommands(commands_));
        commands_.queue();

        MapSaveLoadManager.loadMaps();
    }
}
