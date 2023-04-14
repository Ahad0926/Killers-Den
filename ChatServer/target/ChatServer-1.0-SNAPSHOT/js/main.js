let ws;
let rooms = [];
function newRoom(){
    // calling the ChatServlet to retrieve a new room ID
    let callURL= "http://localhost:8080/WSChatServer-1.0-SNAPSHOT/chat-servlet";
    fetch(callURL, {
        method: 'GET',
        headers: {
            'Accept': 'text/plain',
        },
    })
        .then(response => response.text())
        .then(response => enterRoom(response)); // enter the room with the code
}

function getRooms() {
    fetch("http://localhost:8080/WSChatServer-1.0-SNAPSHOT/chat-servlet/rooms")
        .then(response => response.json())
        .then(data => {
            rooms = data;
            refreshRooms();
        });
}
function refreshRooms() {
    console.log(rooms);
    const tableBody = document.querySelector("#tableOfRooms tbody");
    // clear previous content of the table body
    tableBody.innerHTML = "";

    // iterate over the list of rooms and add each as a new row in the table
    rooms.forEach(room => {
        const row = document.createElement("tr");
        const roomName = document.createElement("td");
        roomName.textContent = room;
        row.appendChild(roomName);
        tableBody.appendChild(row);
    });
}

// Fetch the list of rooms initially and then fetch every 5 seconds
getRooms();
setInterval(getRooms, 5000);
function enterRoom(code){

    // push the new room to the array
    rooms.push(code);

    // send the room code to the server to add to the rooms set
    let callURL= "http://localhost:8080/WSChatServer-1.0-SNAPSHOT/chat-servlet?code=" + code;
    fetch(callURL, {
        method: 'POST',
    });

    // refresh the list of rooms
    refreshRooms();
    refreshName(code);

    // create the web socket
    ws = new WebSocket("ws://localhost:8080/WSChatServer-1.0-SNAPSHOT/ws/"+code);


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
    header.innerText = "You Are In Room " + code;
}

document.getElementById("input").addEventListener("keyup", function (event) {
    if (event.keyCode === 13) {
        let request = {"type":"chat", "msg":event.target.value};
        ws.send(JSON.stringify(request));
        event.target.value = "";
    }
});

function timestamp() {
    var d = new Date(), minutes = d.getMinutes();
    if (minutes < 10) minutes = '0' + minutes;
    return d.getHours() + ':' + minutes;
}



