let ws;
let joiningRoom = false;
let creatingRoom = false;
let roomId = "";
let nickname = '';
let players = [];
document.getElementById("input").addEventListener("keyup", function(event) {
    if (event.keyCode === 13) {
        let request = {"type":"chat", "msg":event.target.value};
        ws.send(JSON.stringify(request));
        event.target.value = "";
    }
});
function setRoomId(code){
    localStorage.setItem("roomId", code);
    window.location.href = "joinedIndex.html";
}
function newRoom(){
    console.log(`Nickname: ${nickname}`);
    console.log(`Players: ${players}`);
    // Switch to index.html
    // calling the ChatServlet to retrieve a new room ID
    let callURL= "http://localhost:8080/ChatServer-1.0-SNAPSHOT/chat-servlet";
    fetch(callURL, {
        method: 'GET',
        headers: {
            'Accept': 'text/plain',
        },
    })
        .then(response => response.text())
        .then(response => enterRoom(response)); // enter the room with the code
}
function enterMadeRoom(){
    roomId = localStorage.getItem("roomId");
    // send the room code to the server to add to the rooms set
    let callURL= "http://localhost:8080/ChatServer-1.0-SNAPSHOT/chat-servlet?code=" + roomId;
    fetch(callURL, {
        method: 'POST',
    });

    // refresh the list of rooms
    //refreshRooms();
    refreshName(roomId);

    // create the web socket
    ws = new WebSocket("ws://localhost:8080/ChatServer-1.0-SNAPSHOT/ws/"+roomId);


    // parse messages received from the server and update the UI accordingly
    ws.onmessage = function (event) {
        console.log(event.data);
        // parsing the server's message as json
        let message = JSON.parse(event.data);
        document.getElementById("log").value += "[" + timestamp() + "] " + message.message + "\n";
    }
}
function enterRoom(code){
    // send the room code to the server to add to the rooms set
    let callURL= "http://localhost:8080/ChatServer-1.0-SNAPSHOT/chat-servlet?code=" + code;
    fetch(callURL, {
        method: 'POST',
    });

    // refresh the list of rooms
    //refreshRooms();
    refreshName(code);

    // create the web socket
    ws = new WebSocket("ws://localhost:8080/ChatServer-1.0-SNAPSHOT/ws/"+code);


    // parse messages received from the server and update the UI accordingly
    ws.onmessage = function (event) {
        console.log(event.data);
        // parsing the server's message as json
        let message = JSON.parse(event.data);
        document.getElementById("log").value += "[" + timestamp() + "] " + message.message + "\n";
    }
}

function refreshName(code) {
    const header = document.getElementById("roomNameHeader");
    header.innerText = "Room Code:  " + code;
}

function timestamp() {
    var d = new Date(), minutes = d.getMinutes();
    if (minutes < 10) minutes = '0' + minutes;
    return d.getHours() + ':' + minutes;
}