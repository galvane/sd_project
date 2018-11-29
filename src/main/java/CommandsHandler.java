import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import twitter4j.TwitterException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

/*////////////////////////////////////////////////////////////////////////////////
Bot is a proxy for games and players, it forwards output and input to respective
entities

*///////////////////////////////////////////////////////////////////////////////*/
public class CommandsHandler extends TelegramLongPollingBot {


    @Override
    public void onUpdateReceived (Update update){

        if (update.hasMessage() && update.getMessage().hasText()){

            SendMessage message = new SendMessage();

            message.setChatId(update.getMessage().getChatId());

            ChatInput in = new ChatInput(CommandUtils.getInput(update.getMessage().getText()));

            switch(in.getCommand())
            {
                case "/start": {
                    message.setText(Responses.onStart());
                    break;
                }
                case "/listAllGames": {
                    message.setText(Responses.onListAllGames());
                    break;
                }
                case "/skipReinforce": {
                    // THESE ARE FOR TESTING, REMOVE IF NEED BE
                    message.setText(Responses.onSkipReinforce(CommandUtils.getGame(update.getMessage().getFrom().getId())));
                    break;

                }

                case "/skipClaim": {
                    // THESE ARE FOR TESTING, REMOVE IF NEED BE
                    message.setText(Responses.onSkipClaim(CommandUtils.getGame(update.getMessage().getFrom().getId())));
                    break;
                }

                case "/listGamesS3": {
                    AWS aws = null;
                    try {
                        aws = new AWS();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    ArrayList<String> games = aws.listObjects();
                    message.setText("Available games to load: \n");
                    for (String g: games) {
                        message.setText(g + "\n");
                    }
                    break;
                }

                case "/saveGame": {
                    AWS aws = null;
                    try {
                        aws = new AWS();
                        Game game = CommandUtils.getGame(update.getMessage().getFrom().getId());
                        aws.upload(game.gameID);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    message.setText("your game has been saved");
                    break;
                }

                case "/undo": {
                    try {
                        AWS aws = new AWS();
                        aws.download(in.getArgs().get(0));
                        // create new loader & game using the input gameID
                        Loader loader = new Loader(in.getArgs().get(0));
                        _GameMaster.gamesListing.put(in.getArgs().get(0), loader.LoadGame());
                        message.setText("Turn undid");
                        break;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                }

                // format -> /loadGame (gameID)
                case "/loadGame": {
                    try {
                        AWS aws = new AWS();
                        aws.download(in.getArgs().get(0));
                    } catch (IOException e) {
                        e.printStackTrace();
                        message.setText("Game Could not be loaded");
                        break;
                    }
                    // create new loader & game using the input gameID
                    Loader loader = new Loader(in.getArgs().get(0));

                    try {
                        _GameMaster.gamesListing.put(in.getArgs().get(0), loader.LoadGame());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    int turn = _GameMaster.gamesListing.get(in.getArgs().get(0)).turn;
                    message.setText("Game loaded, it is now the " + turn + " turn");
                    break;
                }
                case "/join": {
                    message.setText(Responses.onJoin(in, update.getMessage().getFrom().getId(), update.getMessage().getFrom().getUserName(), update.getMessage().getChatId()));
                    break;
                }

                case "/listMyGames": {
                    message.setText(Responses.onListMyGames(update.getMessage().getFrom().getId()));
                    break;
                }

                case "/create": {
                    message.setText(Responses.onCreate(
                            update.getMessage().getFrom().getId(),
                            "risk-game-" + UUID.randomUUID().toString(),
                            update.getMessage().getFrom().getUserName(),
                            update.getMessage().getChatId())
                    );
                    break;
                }

                case "/help": {
                    message.setText(Responses.onHelp());
                    break;
                }

                // function to pick territory, there may be a way to message a player directly and in turn order given a player's user_id and turn number
                case "/pick": {
                    message.setText(Responses.onPick(
                            in,
                            update.getMessage().getFrom().getId(),
                            CommandUtils.getGame(update.getMessage().getFrom().getId())));
                    break;
                }

                case "/reinforce": {
                    message.setText(Responses.onReinforce(
                            in,
                            update.getMessage().getFrom().getId(),
                            CommandUtils.getGame(update.getMessage().getFrom().getId())));

                    break;
                }

                // message should be formatted /attack (attack territory) (defend territory) (Number of armies to attack with) (number of armies to defend with)
                case "/attack": {
                    Game game = CommandUtils.getGame(update.getMessage().getFrom().getId());
                    message.setText(Responses.onAttack(game, in));
                    break;
                }

                case "/atttackWith": {
                    Game game = CommandUtils.getGame(update.getMessage().getFrom().getId());
                    message.setText(Responses.onAttackWith(game, in));
                    break;
                }

                case "/defendWith": {
                    Game game = CommandUtils.getGame(update.getMessage().getFrom().getId());
                    message.setText(Responses.onDefendWith(game, in));
                    break;
                }

                // message should be formatted /fortify (move from) (move to) (Num armies)
                case "/fortify": {
                    Game game = CommandUtils.getGame(update.getMessage().getFrom().getId());
                    String from = null;
                    String to = null;
                    int i = 0; // number to keep track of the size of the country inputs

                    if (game.BM.boardMap.containsKey(in.getArgs().get(0))) {
                        from = in.getArgs().get(0);
                        i = 1;
                    }
                    else if (game.BM.boardMap.containsKey(in.getArgs().get(0) + " " + in.getArgs().get(1))) {
                        from = in.getArgs().get(0) + " " + in.getArgs().get(1);
                        i = 2;
                    }
                    else if (game.BM.boardMap.containsKey(in.getArgs().get(0) + " " + in.getArgs().get(1) + " " + in.getArgs().get(2))) {
                        from = in.getArgs().get(0) + " " + in.getArgs().get(1) + " " + in.getArgs().get(2);
                        i = 3;
                    }

                    if (game.BM.boardMap.containsKey(in.getArgs().get(i))) {
                        to = in.getArgs().get(i);
                        i += 1;
                    }
                    else if (game.BM.boardMap.containsKey(in.getArgs().get(i) + " " + in.getArgs().get(i+1))) {
                        to = in.getArgs().get(i) + " " + in.getArgs().get(i+1);
                        i += 2;
                    }
                    else if (game.BM.boardMap.containsKey(in.getArgs().get(i) + " " + in.getArgs().get(i+1) + " " + in.getArgs().get(i+2))) {
                        to = in.getArgs().get(i) + " " + in.getArgs().get(i+1) + " " + in.getArgs().get(i+2);
                        i += 3;
                    }

                    int turnNo = game.turn % game.playerDirectory.size();
                    Player player = game.playerDirectory.get(turnNo);
                    int transfer = Integer.parseInt(in.getArgs().get(i));
                    game.BM.fortifyTerritory(from,to,transfer);
                }

                // assumes it is your turn, checks your hand for three matching cards, pops them from your hand and gives you the armies
                case "/tradecards": {
                    Game game = CommandUtils.getGame(update.getMessage().getFrom().getId());
                    Player player = CommandUtils.getPlayer(game);
                    Turn turn = game.currentTurn;

                }

                // message format -> /buycredit (credit amount)
                case "/buycredit": {
                    Game game = CommandUtils.getGame(update.getMessage().getFrom().getId());
                    int turnNo = game.turn % game.playerDirectory.size();
                    Player player = game.playerDirectory.get(turnNo);
                    player.addMoney(Double.parseDouble(in.getArgs().get(0)));
                }

                // format -> /buystuff (# undos to buy) (# cards to buy)
                case "/buystuff": {
                    Game game = CommandUtils.getGame(update.getMessage().getFrom().getId());
                    int turnNo = game.turn % game.playerDirectory.size();
                    Player player = game.playerDirectory.get(turnNo);

                    Double cash = player.getWallet();

                    int undos = Integer.parseInt(in.getArgs().get(0));
                    if (undos * 1000 < cash) {
                        player.addUndos(undos);
                        player.addMoney( undos * 1000 * -1);
                    }

                    Double cards = Double.valueOf(in.getArgs().get(1));
                    if (cards * 100 < cash) {
                        for (int i = 0; i < cards; i++) {
                            Card c = game.BM.getGameDeck().draw();
                            if(c != null) player.getHand().get(c.getUnit()).push(c);
                        }
                        player.addMoney( cards * 100 * -1);
                    }
                }

                case "/endturn": {
                    Game game = CommandUtils.getGame(update.getMessage().getFrom().getId());
                    Turn turn = game.currentTurn;
                    try {
                        turn.earnCards();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    // Write game to save game file
                    try {
                        JSONhandler JSONhandler = new JSONhandler(game);
                        JSONhandler.JSONwriter();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    // broadcast to Twitter
                    Twitter tw = new Twitter();
                    try {
                        tw.broadcastToTwitter(game.currentTurn, game.currentTurn.player);
                    } catch (TwitterException e) {
                        e.printStackTrace();
                    }

                    // move to next turn
                    game.turn += 1;
                    int turnNo = game.turn % game.playerDirectory.size();
                    Player player = game.playerDirectory.get(turnNo);
                    game.nextTurnUserID = player.id;
                    message.setText("Player " +player.getUsername()+ " it is now your turn, type /beginTurn to begin your turn");
                    break;
                }

                case "/beginTurn": {
                    Game game = CommandUtils.getGame(update.getMessage().getFrom().getId());
                    //int turnNo = game.turn % game.playerDirectory.size();
                    message.setText(Responses.onBeginTurn(game));
                    break;
                }
                default:
                    message.setText("Command " + in.getCommand() + " not found.\n\n" + Responses.onHelp());
                    break;
            }

            try{
                execute(message);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }


            // follow up messages
            SendMessage announcement = new SendMessage();
            if (in.getCommand().equals("/join") && _GameMaster.gamesListing.containsKey(in.args.get(0)) && _GameMaster.gamesListing.get(in.args.get(0)).playerDirectory.size() == 2 && in.args.size() > 0) {
                announcement.setText(Responses.onFollowUpJoin(in.args.get(0)));
            }
            else if((in.getCommand().equals("/pick") || (in.getCommand().equals("/skipClaim"))) && _GameMaster.gamesListing.get(CommandUtils.getGame(update.getMessage().getFrom().getId()).gameID).BM.getFreeTerritories().size() == 0)
            {
                announcement.setText(Responses.onFollowUpInitPick(CommandUtils.getGame(update.getMessage().getFrom().getId())));

            }
            else if (in.getCommand().equals("/reinforce") || in.getCommand().equals("/skipReinforce"))
            {
                announcement.setText(Responses.onFollowUpReinforce(CommandUtils.getGame(update.getMessage().getFrom().getId())));
            }
            else {
                announcement.setText("Follow-up Message: none");
            }
            announcement.setChatId(update.getMessage().getChatId());

            try {
                execute(announcement);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public String getBotUsername(){
        try {
            Props k = new Props();
            return k.getBot_name();
        } catch(IOException e)
        {
            e.printStackTrace();
            return "";
        }
    }

    @Override
    public String getBotToken(){
        try {
            Props k = new Props();
            return k.getBot_apiToken();
        } catch(IOException e)
        {
            e.printStackTrace();
            return "";
        }
    }

}