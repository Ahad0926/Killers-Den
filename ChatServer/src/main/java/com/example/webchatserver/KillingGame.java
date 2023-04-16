package com.example.webchatserver;
import java.util.ArrayList;
import java.util.Map;
import java.util.Random;


public class KillingGame {
    public boolean running = false;
    public boolean daytime = true;
    public boolean votingTime = false;
    public int state = 0; //state 1 = daytime regular, state 2 = daytime voting, state 3 = nighttime.
    public ArrayList<Player> players = new ArrayList<Player>();

    public KillingGame(){
        this.running = false; this.daytime = true; this.votingTime = false;
    }

    public KillingGame(boolean running, boolean daytime, boolean votingTime){
        this.running = running; this.daytime = daytime; this.votingTime = votingTime;
    }

    public boolean getRunning(){
        return running;
    }
    public void setRunning(boolean running){
        this.running = running;
    }

    public int getState(){
        return this.state;
    }
    public void setState(int state){
        this.state = state;
    }

    public void setupPlayers(Map<String, String> users){//random number is chosen,
        // that number gets to be the killer in the array of players
        ArrayList<Player> players = new ArrayList<>();
        Random rand = new Random();
        int n = rand.nextInt(users.size());//random number
        for (Map.Entry<String, String> entry : users.entrySet()) {
            Player player = new Player(entry.getKey(), entry.getValue());//put hashmap into Player
            players.add(player);
        }
        for (int i = 0; i < users.size(); i++){
            players.get(i).setUniquePlayerNumber(i+1);
            if (i == n){
                players.get(i).setRole("killer");
            } else {
                players.get(i).setRole("innocent");
            }
        }
        this.players = players;
    }

    public void addSpectator(String userID){
        Player spectator = new Player(userID, "");
        spectator.setAlive(false); spectator.setRole("spectator");
        this.players.add(spectator);
    }
    public Boolean playerIsAlive(String userID){ //checks if specific player is alive
        for (int i = 0; i < players.size(); i++){
            if ((players.get(i).getUserID().equals(userID)) && (players.get(i).getAlive())){
                return true;
            }
        }
        return false;
    }

    public String playerRole(String userID){ //checks if specific player is alive
        for (int i = 0; i < players.size(); i++){
            if (players.get(i).getUserID().equals(userID)){//if we find a match
                return (players.get(i).getRole());
            }
        }
        return "Hold up he ain't in the player list";
    }

    public Player getPlayerFromChosenNumber(int chosenNumber){ //returns the specific player from the given UniqueNumber
        for (int i = 0; i < players.size(); i++){
            if (players.get(i).getUniquePlayerNumber() == chosenNumber){//if we find a match
                return (players.get(i));
            }
        }
        return null;
    }

    public Player getPlayerFromSessionID(String sessionID){ //returns the specific player from the given sessionID
        for (int i = 0; i < players.size(); i++){
            if (players.get(i).getUserID().equals(sessionID)){//if we find a match
                return (players.get(i));
            }
        }
        return null;
    }


    public String getKillerName(){
        for (int i = 0; i < players.size(); i++){
            if (players.get(i).getRole().equals("killer")){
                return players.get(i).getUsername();
            }
        }
        return "this should be impossible";
    }


    public String getPlayerNumbers(){
        String s = "";
        for (int i = 0; i < players.size(); i++){
            s = s + players.get(i).getUsername() + ": " +(players.get(i).getUniquePlayerNumber()) +"\\n";
        }
        return s;
    }

    public String checkGameWin(){
        boolean killerWon = false;
        boolean killerAlive = false;
        int livingPlayers = 0;
        for (int i = 0; i < players.size(); i++){
            if (players.get(i).getRole().equals("killer") && players.get(i).getAlive()) { //if we find killer & he alive, set to win
                killerWon = true;
                killerAlive = true;
            }
            if(players.get(i).getAlive()){ //count living players
                livingPlayers++;
            }
        }
        if (livingPlayers > 1){
            killerWon = false;
        }
        if (killerWon){
            return "K"; //killer win scenario.
        } else if (!killerAlive){
            return "I";
        } else {
            return "N";
        }

    }

    public boolean enoughVotes(int chosenNumber){ //figures out how many votes is needed to vote them out.
        int livingPlayers = 0;
        for (int i = 0; i < players.size(); i++){
            if (players.get(i).getAlive()){
                livingPlayers++;
            }
        }
        if (this.getPlayerFromChosenNumber(chosenNumber).getVotes() > (livingPlayers/2)){ //if more than half of the living players votes this player.
            return true;
        }
        return false;
    }

    public void clearVotes(){
        for (int i = 0; i < players.size(); i++){
            players.get(i).setVotes(0);
        }
    }





}

