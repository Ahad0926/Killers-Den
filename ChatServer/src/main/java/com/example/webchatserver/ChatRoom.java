package com.example.webchatserver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class represents the data you may need to store about a Chat room
 * You may add more method or attributes as needed
 * **/
public class ChatRoom {
    private String  code;
    private KillingGame game = new KillingGame(false, true, false);

    //each user has an unique ID associate to their ws session and their username
    private Map<String, String> users = new HashMap<String, String>(); //SessionID and username

    // when created the chat room has at least one user
    public ChatRoom(String code, String user){  //roomCode and SessionID
        this.code = code;
        // when created the user has not entered their username yet
        this.users.put(user, "");
    }

    public void putNoNameUser(String userID){   //if the user is NOT in the room, then add them to the userList
        if (inRoom(userID) == false){
            this.users.put(userID, "");
        }
    }
    public void startGame(){

        this.game = new KillingGame();
        this.game.setupPlayers(users);
        setGame(true, true, false);//running, daytime, votingtime. start of any game.
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public void setGame(Boolean running, Boolean daytime, Boolean votingTime) {
        this.game.running = running; this.game.daytime = daytime; this.game.votingTime = votingTime;
    }

    public KillingGame getGame() {
        return this.game;
    }

    public String toString(){   //displays only the usernames
        String s = "";
        for (String i: users.values()){
            s = s + i + ", ";
        }
        return users.values().toString();// alternatively users.values().toString();
    }


    public Map<String, String> getUsers() {
        return users;
    }

    /**
     * This method will add the new userID to the room if not exists, or it will add a new userID,name pair
     * **/
    public void setUserName(String userID, String name) {
        // update the name
        if(users.containsKey(userID)){
            users.remove(userID);
            users.put(userID, name);
        }else{ // add new user
            users.put(userID, name);
        }
    }

    /**
     * This method will remove a user from this room
     * **/
    public void removeUser(String userID){
        if(users.containsKey(userID)){
            users.remove(userID);
        }

    }

    public boolean inRoom(String userID){
        return users.containsKey(userID);
    }
}
