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