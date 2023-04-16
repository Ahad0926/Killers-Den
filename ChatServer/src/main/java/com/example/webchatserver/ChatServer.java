package com.example.webchatserver;


import com.example.webchatserver.util.ResourceAPI;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import org.json.JSONObject;

import java.io.IOException;

import java.util.HashMap;
import java.util.Map;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.*;

import static com.example.webchatserver.util.ResourceAPI.loadChatRoomHistory;
import static com.example.webchatserver.util.ResourceAPI.saveChatRoomHistory;


/**
 * This class represents a web socket server, a new connection is created and it receives a roomID as a parameter
 * **/
@ServerEndpoint(value="/ws/{roomID}")
public class ChatServer {

    // contains a static List of ChatRoom used to control the existing rooms and their users
    //private static ArrayList<ChatRoom> activeChatRooms = new ArrayList<ChatRoom>();
    // you may add other attributes as you see fit

    private String currentRoomId = "";
    private Boolean isHost = false;
    //private Map<String, String> usernames = new HashMap<String, String>();//session id, username,
    private static Map<String, String> roomList = new HashMap<String, String>();//UserID (key)[NOT USERNAME], RoomID (value)
    private static Map<String, String> roomHistoryList = new HashMap<String, String>();//purely for history
    @OnOpen
    public void open(@PathParam("roomID") String roomID, Session session) throws IOException, EncodeException {
        this.currentRoomId = roomID;
        ChatRoom currentRoom = new ChatRoom(roomID,session.getId()); //Room code, and the userID.
        if (ChatServlet.activeRooms.containsKey(roomID) == false) {  //if activeRooms list DOESN'T have the current room,
            ChatServlet.activeRooms.put(roomID, currentRoom);       //then it adds the current room to the list.
        }
        else {                                                      //if the current room IS IN the active rooms list,
            ChatServlet.activeRooms.get(roomID).putNoNameUser(session.getId()); //then put his sessionID into the chatroom namesList
        }

        roomList.put(session.getId(), roomID); // adding userID to a room
        // loading the history chat
        String history = loadChatRoomHistory(roomID);
        System.out.println("Room joined ");


        if (history!=null && !(history.isBlank())){
            System.out.println(history);
            history = history.replaceAll(System.lineSeparator(), "\\\n");
            System.out.println(history);
            session.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\""+history+" \\n Chat room history loaded\"}");
            roomHistoryList.put(roomID, history+" \\n "+roomID + " room resumed.");
        }
        if(!roomHistoryList.containsKey(roomID)) { // only if this room has no history yet
            roomHistoryList.put(roomID, roomID + " room Created."); //initiating the room history
        }

        if(ChatServlet.activeRooms.get(roomID).getUsers().size() < 2){//if the user is the first one, then he is the host
            isHost = true;
        }

        session.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(Server ): Welcome to the chat room. Here are some existing rooms: "+(ChatServlet.activeRooms.keySet()) +
                "\\nHere are are the users in the room: "+(ChatServlet.activeRooms.get(roomID).toString()) +
                "\\nHost: [" + isHost +"]" +
                "\\nGame Running: [" + ChatServlet.activeRooms.get(roomID).getGame().getRunning() +"]" +
                "\\nTo start a game, wait for 3 players to enter, and then type '!game start'. Once you start, you may '!vote x' during voting time, '!kill x' at night if you are the killer, and '!status x' to check if a player is alive!" +
                "\\nPlease state your username to begin.\"}");

        if (ChatServlet.activeRooms.get(roomID).getGame().getRunning() == true){
            ChatServlet.activeRooms.get(roomID).getGame().addSpectator(session.getId());
        }
    }

    @OnClose
    public void close(Session session) throws IOException, EncodeException {
        String userId = session.getId();

        if (ChatServlet.activeRooms.get(currentRoomId).inRoom(userId)){
            //if (usernames.containsKey(userId)) {
            String username = ChatServlet.activeRooms.get(currentRoomId).getUsers().get(userId);
            //String username = usernames.get(userId);
            String roomID = roomList.get(userId);
            ChatServlet.activeRooms.get(currentRoomId).removeUser(userId); //remove user from chatroom
            //usernames.remove(userId);
            ChatServlet.activeRooms.get(roomID).removeUser(userId);
            // remove this user from the roomList
            roomList.remove(roomID);

            // adding event to the history of the room
            String logHistory = roomHistoryList.get(roomID);
            roomHistoryList.put(roomID, logHistory+" \\n " + username + " left the chat room.");

            // broadcasting it to peers in the same room
            int countPeers = 0;
            for (Session peer : session.getOpenSessions()){ //broadcast this person left the server
                if(roomList.get(peer.getId()).equals(roomID)) { // broadcast only to those in the same room
                    peer.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(Server): " + username + " left the chat room.\"}");
                    countPeers++; // count how many peers are left in the room
                }
            }

            // if everyone in the room left, save history
            if(!(countPeers >0)){
                saveChatRoomHistory(roomID, roomHistoryList.get(roomID));
                ChatServlet.activeRooms.remove(roomID);   //removes the room from the list
            }
        }
    }

    @OnMessage
    public void handleMessage(String comm, Session session) throws IOException, EncodeException, ExecutionException, InterruptedException {
        String userID = session.getId();  String roomID = roomList.get(userID); // myID & my roomID
        JSONObject jsonmsg = new JSONObject(comm); String type = (String) jsonmsg.get("type"); String message = (String) jsonmsg.get("msg");

        if(!(ChatServlet.activeRooms.get(roomID).getUsers().get(userID).equals("")||ChatServlet.activeRooms.get(roomID).getUsers().get(userID) == null)){ // not their first message
            //if(usernames.containsKey(userID)){ // not their first message AKA they have ALREADY typed their name
            String username = ChatServlet.activeRooms.get(roomID).getUsers().get(userID);
            //String username = usernames.get(userID);
            System.out.println(username);

            if (((message.equals("!start game") || message.equals("!game start"))&& isHost) &&
                    (!(ChatServlet.activeRooms.get(roomID).getGame().running))) { // if !start game or !game start command & user is Host & game isn't running
                int countPeers = 0;
                for (Session peer : session.getOpenSessions()) {
                    // only people in the same room count
                    if(roomList.get(peer.getId()).equals(roomID)){
                        countPeers++;
                    }
                } //counts the number of people.
                if (countPeers > 2) {  //sufficient number of players to start game command
                    session.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(Server): You Have Chosen to start!" + "\"}");
                    for (Session peer : session.getOpenSessions()) {
                        // only send my messages to those in the same room
                        // only announce to those in the same room as me, excluding myself
                        if ((!peer.getId().equals(userID)) && (roomList.get(peer.getId()).equals(roomID))) {
                            peer.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(Server): " + "The Host has chosen to start!" + "\"}");
                        }
                    }

                    ChatServlet.activeRooms.get(roomID).startGame(); //Game then displays the role to the Host,
                    session.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(Server): The Game Has Started! "+
                            "To kill or vote for a player, refer to their number (!vote 3 or !kill 1):\\n\\n"+ ChatServlet.activeRooms.get(roomID).getGame().getPlayerNumbers()+
                            "\"}");
                    session.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(Server): Your role is: "+
                            ChatServlet.activeRooms.get(roomID).getGame().playerRole(userID)+"!" + "\"}");

                    for (Session peer : session.getOpenSessions()) {     //Game then displays role to everyone else, individually
                        if ((!peer.getId().equals(userID)) && (roomList.get(peer.getId()).equals(roomID))) {
                            peer.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(Server): The Game Has Started! "+
                                    "To kill or vote for a player, refer to their number (!vote 3 or !kill 1).\\n\\n"+ ChatServlet.activeRooms.get(roomID).getGame().getPlayerNumbers()+"\"}");
                            peer.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(Server): Your role is: "+
                                    ChatServlet.activeRooms.get(roomID).getGame().playerRole(peer.getId())+"!" + "\"}");
                        }
                    }

                    ChatServlet.activeRooms.get(roomID).getGame().setState(3);//start at night, so the game can announce daytime message.
                    //Three state changes, daytime regular-->daytime voting--> nighttime---> daytime regular
                    class MyTask extends TimerTask {
                        public void run() {
                            if (ChatServlet.activeRooms.get(roomID).getGame().getRunning()) {
                                if (ChatServlet.activeRooms.get(roomID).getGame().getState() == 1) {
                                    //if its daytime, but NOT voting, time, then it becomes voting time.
                                    ChatServlet.activeRooms.get(roomID).getGame().votingTime = true;

                                    for (Session peer : session.getOpenSessions()) {
                                        // only announce to those in the same room as me
                                        if (roomList.get(peer.getId()).equals(roomID) && ChatServlet.activeRooms.get(roomID).getGame().getRunning()) {
                                            ChatServlet.activeRooms.get(roomID).getGame().getPlayerFromSessionID(peer.getId()).setHasVoted(false);
                                            try {
                                                peer.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(Server): " + "The voting has Started!" + "\"}");
                                            } catch (IOException e) {
                                                throw new RuntimeException(e);
                                            }
                                        }
                                    }

                                    ChatServlet.activeRooms.get(roomID).getGame().setState(2);

                                } else if (ChatServlet.activeRooms.get(roomID).getGame().getState() == 2) {
                                    ChatServlet.activeRooms.get(roomID).getGame().votingTime = false;
                                    ChatServlet.activeRooms.get(roomID).getGame().daytime = false;

                                    for (Session peer : session.getOpenSessions()) {
                                        // only announce to those in the same room as me
                                        if (roomList.get(peer.getId()).equals(roomID) && ChatServlet.activeRooms.get(roomID).getGame().getRunning()) {
                                            if (ChatServlet.activeRooms.get(roomID).getGame().getPlayerFromSessionID(peer.getId()).getRole().equals("killer")) {
                                                ChatServlet.activeRooms.get(roomID).getGame().getPlayerFromSessionID(peer.getId()).setHasKilled(false);
                                                ChatServlet.activeRooms.get(roomID).getGame().clearVotes();
                                                try {
                                                    peer.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(Server): " + "You may kill once again!" + "\"}");
                                                } catch (IOException e) {
                                                    throw new RuntimeException(e);
                                                }
                                            }
                                            try {
                                                peer.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(Server): " + "The night begins!" + "\"}");
                                            } catch (IOException e) {
                                                throw new RuntimeException(e);
                                            }
                                        }

                                    }
                                    ChatServlet.activeRooms.get(roomID).getGame().setState(3); //changes the state to 3, night.
                                } else if (ChatServlet.activeRooms.get(roomID).getGame().getState() == 3) {
                                    ChatServlet.activeRooms.get(roomID).getGame().daytime = true;

                                    for (Session peer : session.getOpenSessions()) {
                                        // only announce to those in the same room as me
                                        if (roomList.get(peer.getId()).equals(roomID) && ChatServlet.activeRooms.get(roomID).getGame().getRunning()) {
                                            try {
                                                peer.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(Server): " + "The day begins!" + "\"}");
                                            } catch (IOException e) {
                                                throw new RuntimeException(e);
                                            }
                                        }

                                        ChatServlet.activeRooms.get(roomID).getGame().setState(1); //changes the state to 1, regular daytime.
                                    }


                                    System.out.println("Task is running");
                                }
                            }
                        }
                    }
                    //TIMER THAT IS CONCURRENT WITH THE CHAT. USEFUL FOR
                    Timer timer = new Timer(); // creating timer
                    TimerTask task = new MyTask(); // creating timer task
                    timer.scheduleAtFixedRate(task,300,(1000*20));
                    // scheduling the task after the delay at fixed-rate
                    //https://www.tutorialspoint.com/how-to-schedule-tasks-in-java-to-run-for-repeated-fixed-rate-execution-beginning-after-the-specified-delay



                } else { //game fails to start
                    session.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(" + "Server" + "): Game has Failed to Start! "+
                            "Not enough people or you are not the host!" + "\"}");
                }
            }

            //should've used regex honestly. "!status {number ranging from above 0 to below players list length}"
            else if ((message.startsWith("!status ")) && Character.isDigit(message.charAt(message.length() - 1))//checks if starts with "status ", &
                    // last character is a number between 0 and players.size() (exclusive)
                    && (Character.getNumericValue(message.charAt(message.length() - 1)) <= ChatServlet.activeRooms.get(roomID).getGame().players.size()) &&
                    (Character.getNumericValue(message.charAt(message.length() - 1)) > 0)){
                int chosenPlayerNumber = Character.getNumericValue(message.charAt(message.length() - 1));
                session.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(" + username + "): This player's Living Status: "+
                        ChatServlet.activeRooms.get(roomID).getGame().playerIsAlive(ChatServlet.activeRooms.get(roomID).getGame().getPlayerFromChosenNumber(chosenPlayerNumber).getUserID()) +"\"}");
            }


            else if ((message.startsWith("!vote ")) && Character.isDigit(message.charAt(message.length() - 1))
                    && (Character.getNumericValue(message.charAt(message.length() - 1)) <= ChatServlet.activeRooms.get(roomID).getGame().players.size()) &&
                    (Character.getNumericValue(message.charAt(message.length() - 1)) > 0)) {
                int chosenPlayerNumber = Character.getNumericValue(message.charAt(message.length() - 1));
                Player thePlayer = ChatServlet.activeRooms.get(roomID).getGame().getPlayerFromSessionID(userID);
                if ((!thePlayer.getHasVoted()) && thePlayer.getAlive() &&
                        ((ChatServlet.activeRooms.get(roomID).getGame().getState()== 2))
                        && (ChatServlet.activeRooms.get(roomID).getGame().getPlayerFromChosenNumber(chosenPlayerNumber).getAlive())) {
                    //only a living player who hasn't voted yet can do the vote command during voting time, on a living player.

                    session.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(Server): You have voted for: (" + chosenPlayerNumber + "): " +
                            ChatServlet.activeRooms.get(roomID).getGame().getPlayerFromChosenNumber(chosenPlayerNumber).getUsername() + "\"}"); //Sends the vote message to the voter
                    ChatServlet.activeRooms.get(roomID).getGame().getPlayerFromChosenNumber(chosenPlayerNumber).addVotes(); //this adds a vote.
                    ChatServlet.activeRooms.get(roomID).getGame().getPlayerFromSessionID(userID).setHasVoted(true);

                    for (Session peer : session.getOpenSessions()) {   //We find specific voted player and send message saying they've been voted for.
                        if (ChatServlet.activeRooms.get(roomID).getGame().getPlayerFromChosenNumber(chosenPlayerNumber).getUserID().equals(peer.getId())) {
                            peer.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(Server): You've been voted for!\"}");
                        } else if ((!peer.getId().equals(userID)) && (!ChatServlet.activeRooms.get(roomID).getGame().getPlayerFromChosenNumber(chosenPlayerNumber).getUserID().equals(peer.getId()))) {
                            //not the voter and not the guy who got voted
                            peer.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(Server): (" + chosenPlayerNumber + ") " +
                                    ChatServlet.activeRooms.get(roomID).getGame().getPlayerFromChosenNumber(chosenPlayerNumber).getUsername() + " has received a vote!\"}");

                        }

                    } //Check if voter won from voting up that player.

                    if (ChatServlet.activeRooms.get(roomID).getGame().enoughVotes(chosenPlayerNumber)){
                        ChatServlet.activeRooms.get(roomID).getGame().getPlayerFromChosenNumber(chosenPlayerNumber).setAlive(false); //this guy is now voted off and dead
                        for (Session peer : session.getOpenSessions()) {
                            if ((roomList.get(peer.getId()).equals(roomID))) {//display to same room people
                                peer.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(Server):This player is to be put to death! " +
                                        ChatServlet.activeRooms.get(roomID).getGame().getPlayerFromChosenNumber(chosenPlayerNumber).getUsername() + ", is now dead!\"}");
                                peer.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(Server): " +
                                        ChatServlet.activeRooms.get(roomID).getGame().getPlayerFromChosenNumber(chosenPlayerNumber).getUsername() + "'s role was: "+
                                        ChatServlet.activeRooms.get(roomID).getGame().getPlayerFromChosenNumber(chosenPlayerNumber).getRole()+"!\"}");
                            }
                        }
                        if (ChatServlet.activeRooms.get(roomID).getGame().checkGameWin().equals("I")){
                            for (Session peer : session.getOpenSessions()) {
                                if ((roomList.get(peer.getId()).equals(roomID))) {//display to same room people
                                    peer.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(Server):The killer has been hung! The killer, " +
                                            ChatServlet.activeRooms.get(roomID).getGame().getKillerName()+", loses! Everyone else wins!\"}");
                                }
                            }
                            ChatServlet.activeRooms.get(roomID).getGame().setRunning(false);
                        }

                    }


                    //variety of fail messages below.
                } else if (!thePlayer.getAlive()) { //player sending !vote is dead.
                    session.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(Server): You are dead! you can't vote!\"}");
                } else if (!(ChatServlet.activeRooms.get(roomID).getGame().getState()== 2)) {  //it is not voting time.
                    session.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(Server): It's not voting time! Hold out until then!\"}");
                }else if (thePlayer.getHasVoted()){ //player sending !vote has already voted once today.
                    session.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(Server): You have already voted once today!\"}");
                }   else if (!ChatServlet.activeRooms.get(roomID).getGame().getPlayerFromChosenNumber(chosenPlayerNumber).getAlive()){
                    //player sending !vote tried to vote a dead player.
                    session.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(Server): That player is already dead! Don't waste your vote!\"}");
                }

            }

            //"!kill {uniqueNumber}" // I have decided that you can kill yourself in this game, it's funnier, even if I am capable of programming otherwise.
            else if ((message.startsWith("!kill ")) && Character.isDigit(message.charAt(message.length() - 1))//checks if starts with "status ", &
                    // last character is a number between 0 and players.size() (exclusive)
                    && (Character.getNumericValue(message.charAt(message.length() - 1)) <= ChatServlet.activeRooms.get(roomID).getGame().players.size()) &&
                    (Character.getNumericValue(message.charAt(message.length() - 1)) > 0)) {
                int chosenPlayerNumber = Character.getNumericValue(message.charAt(message.length() - 1));

                Player thePlayer = ChatServlet.activeRooms.get(roomID).getGame().getPlayerFromSessionID(userID);
                if ((!thePlayer.getHasKilled()) && thePlayer.getRole().equals("killer") && thePlayer.getAlive() &&
                        ((ChatServlet.activeRooms.get(roomID).getGame().getState()== 3))
                        && (ChatServlet.activeRooms.get(roomID).getGame().getPlayerFromChosenNumber(chosenPlayerNumber).getAlive())) {
                    //only a living killer who hasn't killed yet can do the kill command.

                    session.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(Server): You have chosen to kill: (" + chosenPlayerNumber + "): " +
                            ChatServlet.activeRooms.get(roomID).getGame().getPlayerFromChosenNumber(chosenPlayerNumber).getUsername() + "\"}"); //Sends the kill message to the killer
                    ChatServlet.activeRooms.get(roomID).getGame().getPlayerFromChosenNumber(chosenPlayerNumber).setAlive(false);//this kills the player chosen.
                    ChatServlet.activeRooms.get(roomID).getGame().getPlayerFromSessionID(userID).setHasKilled(true);

                    for (Session peer : session.getOpenSessions()) {   //We find specific killed player and send message saying they've been killed.
                        if (ChatServlet.activeRooms.get(roomID).getGame().getPlayerFromChosenNumber(chosenPlayerNumber).getUserID().equals(peer.getId())) {
                            peer.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(Server): You've been killed!\"}");
                        } else if ((!peer.getId().equals(userID)) && (!ChatServlet.activeRooms.get(roomID).getGame().getPlayerFromChosenNumber(chosenPlayerNumber).getUserID().equals(peer.getId()))) {
                            //not the killer and not the guy who got killed
                            peer.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(Server): (" + chosenPlayerNumber + ") " +
                                    ChatServlet.activeRooms.get(roomID).getGame().getPlayerFromChosenNumber(chosenPlayerNumber).getUsername() + " has been killed!\"}");

                        }

                    } //Check if killer won from killing that player.
                    if (ChatServlet.activeRooms.get(roomID).getGame().checkGameWin().equals("K")){
                        for (Session peer : session.getOpenSessions()) {
                            if ((roomList.get(peer.getId()).equals(roomID))) {//display to same room people
                                peer.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(Server):The last innocent player has been killed! The killer, " +
                                        ChatServlet.activeRooms.get(roomID).getGame().getKillerName()+", wins!\"}");
                            }
                        }
                        ChatServlet.activeRooms.get(roomID).getGame().setRunning(false);
                    }
                    else if (ChatServlet.activeRooms.get(roomID).getGame().checkGameWin().equals("I")){
                        for (Session peer : session.getOpenSessions()) {
                            if ((roomList.get(peer.getId()).equals(roomID))) {//display to same room people
                                peer.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(Server):The killer killed themselves like an idiot! The killer, " +
                                        ChatServlet.activeRooms.get(roomID).getGame().getKillerName()+", loses!\"}");
                            }
                        }
                        ChatServlet.activeRooms.get(roomID).getGame().setRunning(false);
                    }


                    //variety of fail messages below.
                } else if (!thePlayer.getAlive()) { //player sending !kill is dead.
                    session.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(Server): You are dead! you can't kill now even if you are a killer!\"}");
                } else if (!thePlayer.getRole().equals("killer")) {  //player sending !kill is not a killer
                    session.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(Server): You are not a killer! Be better than that!\"}");
                } else if (!(ChatServlet.activeRooms.get(roomID).getGame().getState()== 3)) {  //it is not night.
                    session.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(Server): You cannot kill in broad daylight! Be smarter than that!\"}");
                }else if (thePlayer.getHasKilled()){ //player sending !kill has already killed once today.
                    session.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(Server): You have already killed once tonight!\"}");
                }   else if (!ChatServlet.activeRooms.get(roomID).getGame().getPlayerFromChosenNumber(chosenPlayerNumber).getAlive()){
                    //player sending !kill tried to kill a dead player.
                    session.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(Server): That player is already dead! Don't waste your kills!\"}");
                }


            }


            else if (message.charAt(0) == '!'){                      // if the message is any other command
                session.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(Server): " + message + ": This command does nothing!"+
                        "\\nList of commands: '!kill {playerNumber}', '!vote {playerNumber}', '!status {playerNumber}', (host only) '!game start', (host only) '!start game'" +"\"}");
                for(Session peer: session.getOpenSessions()){
                    // only announce to those in the same room as me, excluding myself
                    if((!peer.getId().equals(userID)) && (roomList.get(peer.getId()).equals(roomID))){
                        peer.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(" + username + "): " + message + ": Someone tried and failed to do a command!"+"\"}");

                    }
                }
            }
            else if (!(ChatServlet.activeRooms.get(roomID).getGame().running)){ //GAME IS NOT RUNNING, everyone can talk and everyone can see it.

                // adding event to the history of the room
                String logHistory = roomHistoryList.get(roomID);
                roomHistoryList.put(roomID, logHistory+" \\n " +"(" + username + "): " + message);

                // broadcasting it to peers in the same room
                for(Session peer: session.getOpenSessions()){
                    // only send my messages to those in the same room
                    if(roomList.get(peer.getId()).equals(roomID)) {
                        peer.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(" + username + "): " + message +"\"}");

                    }
                }
            }
            //if player is dead
            else if (!(ChatServlet.activeRooms.get(roomID).getGame().getPlayerFromSessionID(userID).getAlive())) {
                for (Session peer : session.getOpenSessions()) {
                    // only send my messages to those in the same room
                    if (roomList.get(peer.getId()).equals(roomID) && (!(ChatServlet.activeRooms.get(roomID).getGame().getPlayerFromSessionID(peer.getId()).getAlive()))) {
                        peer.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"[Spectator Chat](" + username + "): " + message +"\"}");
                    }
                }
            }

            else if ((ChatServlet.activeRooms.get(roomID).getGame().daytime)){ //IT IS DAYTIME (free speaking period for alive players)
                // DEAD CHAT IS ALSO HERE. they can only watch and talk to each other.
                // adding event to the history of the room
                String logHistory = roomHistoryList.get(roomID);
                roomHistoryList.put(roomID, logHistory+" \\n " +"(" + username + "): " + message);

                // broadcasting it to peers in the same room
                for(Session peer: session.getOpenSessions()){
                    // only send my messages to those in the same room
                    if(roomList.get(peer.getId()).equals(roomID) && ChatServlet.activeRooms.get(roomID).getGame().playerIsAlive(userID)) {
                        //Player sending the message is alive && peer is in the same room. EVERYONE CAN SEE THIS.
                        peer.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(" + username + "): " + message +"\"}");
                    } else if (roomList.get(peer.getId()).equals(roomID) && (!ChatServlet.activeRooms.get(roomID).getGame().playerIsAlive(userID)) &&
                            (!(ChatServlet.activeRooms.get(roomID).getGame().getPlayerFromSessionID(peer.getId()).getAlive()))){ //Player is not alive. && peer is also not alive
                        peer.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"[Spectator Chat](" + username + "): " + message +"\"}");
                    }
                }
            }





        }else{ //first message is their username
            //usernames.put(userID, message);
            ChatServlet.activeRooms.get(roomID).setUserName(userID, message);
            session.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(Server ): Welcome, " + message + "! " +
                    "You are now in the room with everyone else: " +(ChatServlet.activeRooms.get(roomID).toString())+"\"}");

            // adding event to the history of the room
            String logHistory = roomHistoryList.get(roomID);
            roomHistoryList.put(roomID, logHistory+" \\n " + message + " joined the chat room.");

            // broadcasting it to peers in the same room
            for(Session peer: session.getOpenSessions()){
                // only announce to those in the same room as me, excluding myself
                if((!peer.getId().equals(userID)) && (roomList.get(peer.getId()).equals(roomID))){
                    peer.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(Server): " + message + " joined the chat room.\"}");
                }
            }
            if (ChatServlet.activeRooms.get(roomID).getGame().getRunning()){
                ChatServlet.activeRooms.get(roomID).getGame().getPlayerFromSessionID(session.getId()).setUsername(message);
            }
        }
    }


}