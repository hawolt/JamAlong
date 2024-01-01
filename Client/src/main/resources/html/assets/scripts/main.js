window.onload = function () {
    connect("ws://127.0.0.1:8887");
}

function call(url) {
    fetch(url)
        .catch((error) => {
            console.error('Error:', error);
        });
}

function dispose() {
    call('http://localhost:35199/v1/config/close');
}

function maximize() {
    call('http://localhost:35199/v1/config/maximize');
}

function minimize() {
    call('http://localhost:35199/v1/config/minimize');
}

function connect(host) {
    let socket = new WebSocket(host);
    socket.onopen = function (msg) {
        console.log("Connected to " + host);
    };
    socket.onmessage = function (msg) {
        const json = JSON.parse(msg.data);
        console.log(json);
    };
    socket.onclose = function (msg) {
        console.log("disconnected from " + host);
    };
}

function copyToClipboard(text) {
    const textArea = document.createElement("textarea");
    textArea.value = text;
    document.body.appendChild(textArea);
    textArea.select();
    document.execCommand("copy");
    document.body.removeChild(textArea);
}