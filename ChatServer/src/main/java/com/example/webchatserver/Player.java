package com.example.webchatserver;

public class Player {
    private String username;
    private String userID;
    private String role = "innocent"; //either should only be "killer", "innocent", or "spectator". last one Cannot talk
    private boolean hasVoted = false; //this exists to prevent players from voting more than once.
    private boolean hasKilled = false; //this exists to prevent players from killing more than once.
    private boolean alive = true;
    private int uniquePlayerNumber = 0;//this is so that choosing a player to vote/kill is nice and easy: i.e "!vote 2" (to vote for the player numbered 2)
    private int votes = 0;


    public Player(String userID, String username){ //constructor
        this.userID = userID; this.username = username;
    }
    //GETTERS AND SETTERS
    public String getRole(){    //getters and setters for roles.
        return this.role;
    }
    public void setRole(String role){
        this.role = role;
    }
    public void setHasVoted(Boolean hasVoted){
        this.hasVoted = hasVoted;
    }
    public Boolean getHasVoted(){
        return this.hasVoted;
    }
    public void setHasKilled(Boolean hasKilled){
        this.hasKilled = hasKilled;
    }
    public Boolean getHasKilled(){
        return this.hasKilled;
    }
    public String getUsername(){
        return this.username;
    }
    public void setUsername(String username){
        this.username = username;
    }
    public String getUserID(){
        return this.userID;
    }
    public void setUserID(String userID){
        this.userID = userID;
    }
    public Boolean getAlive(){
        return this.alive;
    }
    public void setAlive(Boolean alive){
        this.alive = alive;
    }

    public int getVotes(){
        return this.votes;
    }
    public void setVotes(int votes){
        this.votes = votes;
    }
    public int getUniquePlayerNumber(){
        return this.uniquePlayerNumber;
    }
    public void setUniquePlayerNumber(int uniquePlayerNumber){
        this.uniquePlayerNumber = uniquePlayerNumber;
    }
    //END OF GETTERS AND SETTERS

    public void addVotes(){
        this.votes++;
    }




}
