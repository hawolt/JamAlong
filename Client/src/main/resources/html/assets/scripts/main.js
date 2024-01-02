const origin = window.location;

window.onload = function () {
    fetch(origin + 'v1/config/websocket')
        .then((response) => response.text())
        .then((data) => {
            connect("ws://127.0.0.1:" + data);
        })
        .catch((error) => {
            console.error('Error:', error);
        });


    const settings = document.getElementById("settings");
    settings.addEventListener("click", function () {
        if (settings.classList.contains("fa-gear")) {
            settings.dataset.previous = getActiveSAAS();
            hideAllSAAS("page-settings");
            settings.classList = "settings fa-solid fa-backward back";
        } else {
            hideAllSAAS(settings.dataset.previous);
            settings.classList = "settings fa-solid fa-gear gear";
        }
    });

    const mainpage = document.getElementById("mainpage");
    mainpage.addEventListener("click", function () {
        settings.dataset.previous = "page-landing";
        settings.classList = "settings fa-solid fa-gear gear";
        hideAllSAAS("page-landing");
    });


    var range = document.querySelector("input[type=range]");
    var listener = function () {
        window.requestAnimationFrame(function () {
            adjustAudioGain(range.value);
        });
    };
    range.addEventListener("mousedown", function () {
        listener();
        range.addEventListener("mousemove", listener);
    });
    range.addEventListener("mouseup", function () {
        range.removeEventListener("mousemove", listener);
    });
    range.addEventListener("keydown", listener);


    document.getElementById("select-join").addEventListener("click", function () {
        hideAllSAAS("page-join");
    });
    document.getElementById("join").addEventListener("click", function () {
        join(document.getElementById("partyid").value);
    });

    document.getElementById("set-name").addEventListener("click", function () {
        username(document.getElementById("username").value, document.getElementById("partyid").value);
    });

    document.getElementById("copy").addEventListener("click", function () {
        copyToClipboard(document.getElementById("party").innerHTML);
    });

    document.getElementById("select-host").addEventListener("click", function () {
        host();
        hideAllSAAS("page-host");
    });

    const input = document.getElementById("sclink");
    input.addEventListener("keyup", function (event) {
        event.preventDefault();
        if (event.keyCode === 13) {
            load(input.value);
            input.value = "";
        }
    });
}

function getActiveSAAS() {
    var elements = document.getElementsByClassName("saas");
    for (let i = 0; i < elements.length; i++) {
        var element = elements[i];
        if (!element.classList.contains("hidden")) {
            return element.id;
        }
    }
    return "page-landing";
}

function hideAllSAAS(exception) {
    var elements = document.getElementsByClassName("saas");
    for (let i = 0; i < elements.length; i++) {
        var element = elements[i];
        if (element.id === exception) {
            elements[i].classList.remove("hidden");
        } else {
            elements[i].classList.add("hidden");
        }
    }
}

function call(url) {
    fetch(url)
        .catch((error) => {
            console.error('Error:', error);
        });
}

function adjustAudioGain(value) {
    call(origin + 'v1/config/gain/' + value);
}

function host() {
    fetch(origin + 'v1/api/host')
        .then((response) => response.json())
        .then((data) => {
            document.getElementById("party").innerHTML = data.result;
        })
        .catch((error) => {
            console.error('Error:', error);
        });
}

function username(name, partyId) {
    call(origin + 'v1/api/namechange/' + name + '/' + partyId);
}

function join(partyId) {
    fetch(origin + 'v1/api/join/' + partyId)
        .then((response) => response.json())
        .then((data) => {
            let args = data['result'].split(' ');
            if (args[0] === partyId) {
                hideAllSAAS("page-attendee");
            }
        })
        .catch((error) => {
            console.error('Error:', error);
        });
}

function load(link) {
    call(origin + 'v1/api/load?url=' + btoa(link));
}

function connect(host) {
    let socket = new WebSocket(host);
    socket.onopen = function (msg) {
        console.log("Connected to " + host);
    };
    socket.onmessage = function (msg) {
        const json = JSON.parse(msg.data);
        switch (json['instruction']) {
            case 'list':
                var users = json['users'];
                var string = 'Host: ' + users[0] + ", Listeners: ";
                for (let i = 1; i < users.length; i++) {
                    if (i != 1) string += ", ";
                    string += users[i];
                }
                document.getElementById('users').innerHTML = string;
                break;
        }
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