[![Review Assignment Due Date](https://classroom.github.com/assets/deadline-readme-button-8d59dc4de5201274e310e4c54b9627a8934c3b88527886e3b421487c677d23eb.svg)](https://classroom.github.com/a/xVkiPLj0)
# Sys Dev Final
Sys Dev Final is a chatroom game akin to Town of Salem where a player is given the Killer role and must kill someone every night while not getting caught. During the day, players disscuss and try to vote the killer out!

## Prerequisites
Before running this application, you will need to have the following installed:

- Java Development Kit (JDK) 1.8 or later
- Maven
- Glassfish server
## Configuration
- First you have to set up the **ChatResourceApi** on a **local** Glassfish server using the following configurations:
    - Server
        - URL: http://localhost:8080/ChatResourceAPI-1.0-SNAPSHOT/api/history
    - Deployment
        - Artifact: ChatResourceAPI:war exploded

- Next you have to set up the **ChatServer** on a **remote** Glassfish server using the following configurations:
    - Server
        - URL: http://localhost:8080/ChatServer-1.0-SNAPSHOT/homepage.html
    - Deployment
        - Artifact: ChatServer:war

## Deployment

To deploy this project one must simply deploy the local Glassfish server (ChatResourceAPI) then the remote Glassfish Server (ChatServer)

## Usage

Click Create Game to create a new room. Send the code at the top of the screen to share with friends.
Next, type in your name once you enter the chatroom.

For Friends, they should click join game, and then input the code you've given them.
Note: rooms maintain history.
Once enough people enter a lobby, the first person in the lobby, the host, types in '!start game' or '!game start'.
You are then assigned a unique number, and a role. this number determines how you use commands on other people, including yourself.
'!status 3' is used to check if the player assigned 3 is alive, for example.

Your role can either be innocent, or killer. the innocents must vote out the killer, while the killer gets to pick them off one by one every night phase.

## Screenshots

![Screenshot of a game running](Capture1.png)

![Screenshot of a game running](Capture2.png)

![Screenshot of a game running](Capture3.png)

## Collaborators:

Ahad Abdul: 100787992
Filip Takov: 100828604
Mukku Chemjong 100828440

## References:
the following was used to set up the timer for the phases.

https://www.tutorialspoint.com/how-to-schedule-tasks-in-java-to-run-for-repeated-fixed-rate-execution-beginning-after-the-specified-delay