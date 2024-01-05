const origin = window.location;

window.onload = function () {

    configure("idle");
    setGain();

    fetch(origin + 'v1/config/version')
        .then((response) => response.text())
        .then((data) => {
            if (data === "false") return;
            hideAllSAAS("page-updater");
        })
        .catch((error) => {
            console.error('Error:', error);
        });

    document.getElementById("decline-update").addEventListener("click", function () {
        hideAllSAAS("page-landing");
    });

    document.getElementById("accept-update").addEventListener("click", function () {
        call(origin + 'v1/config/invoke');
        hideAllSAAS("page-update");
    });

    fetch(origin + 'v1/config/websocket')
        .then((response) => response.text())
        .then((data) => {
            connect("ws://127.0.0.1:" + data);
        })
        .catch((error) => {
            console.error('Error:', error);
        });


    const visibility = document.getElementById('visibility');
    visibility.addEventListener('change', e => {
        if (e.target.checked === true) {
            togglePartyVisibility(document.getElementById("partyid").value, true);
        }
        if (e.target.checked === false) {
            togglePartyVisibility(document.getElementById("partyid").value, false);
        }
    });

    const settings = document.getElementById("settings");
    settings.addEventListener("click", function () {
        if (settings.classList.contains("fa-gear")) {
            settings.dataset.previous = getActiveSAAS();
            hideAllSAAS("page-settings");
            if (document.getElementById('jamalong').dataset.status === "host") {
                if (visibility.classList.contains("hidden")) visibility.classList.remove("hidden");
            } else {
                if (!visibility.classList.contains("hidden")) visibility.classList.add("hidden");
            }
            settings.classList = "settings fa-solid fa-backward back";
        } else {
            hideAllSAAS(settings.dataset.previous);
            settings.classList = "settings fa-solid fa-gear gear";
        }
    });

    const mainpage = document.getElementById("mainpage");
    mainpage.addEventListener("click", function () {
        reset();
        openLandingPage();
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
        configure("attendee");
        discover();
        hideAllSAAS("page-join");
    });
    document.getElementById("join").addEventListener("click", function () {
        join(document.getElementById("partyid").value);
    });

    document.getElementById("skip").addEventListener("click", function () {
        skip();
    });

    document.getElementById("set-name").addEventListener("click", function () {
        username(document.getElementById("username").value, document.getElementById("partyid").value);
    });

    document.getElementById("copy").addEventListener("click", function () {
        copyToClipboard(document.getElementById("party").innerHTML);
    });

    document.getElementById("select-host").addEventListener("click", function () {
        configure("host");
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

function openLandingPage() {
    configure("idle");
    const settings = document.getElementById("settings");
    settings.dataset.previous = "page-landing";
    settings.classList = "settings fa-solid fa-gear gear";
    hideAllSAAS("page-landing");
}

function configure(current) {
    document.getElementById('jamalong').dataset.status = current;
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

function setGain() {
    fetch(origin + 'v1/config/gain')
        .then((response) => response.text())
        .then((data) => {
            document.getElementById("audio-gain").value = data;
        })
        .catch((error) => {
            console.error('Error:', error);
        });
}

function skip() {
    call(origin + 'v1/config/skip');
}

function reset() {
    call(origin + 'v1/config/reset');
}

function discover() {
    fetch(origin + 'v1/api/discover')
        .then((response) => response.json())
        .then((data) => {
            const roomlist = document.getElementById("roomlist");
            roomlist.innerHTML = "";
            const rooms = data.result.split(',');
            if (data.result.length == 0) return;
            for (let i = 0; i < rooms.length; i++) {
                var details = rooms[i].split(";");
                var name = details[0];
                var room = details[1];
                var users = details[2];
                roomlist.appendChild(build(name, room, users));
            }
        })
        .catch((error) => {
            console.error('Error:', error);
        });
}

function host() {
    fetch(origin + 'v1/api/host')
        .then((response) => response.json())
        .then((data) => {
            document.getElementById("partyid").value = data.result;
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

function togglePartyVisibility(partyId, status) {
    call(origin + 'v1/api/visibility/' + partyId + '/' + status);
}


function connect(host) {
    let socket = new WebSocket(host);
    socket.onopen = function (msg) {
        console.log("Connected to " + host);
    };
    socket.onmessage = function (msg) {
        const json = JSON.parse(msg.data);
        if (json.hasOwnProperty('instruction')) {
            switch (json['instruction']) {
                case 'download':
                    updateDownload(json['progress'])
                    break;
                case 'kill':
                    openLandingPage();
                    break;
                case 'list':
                    var users = json['users'];
                    var hosts = document.getElementsByClassName('host');
                    for (let i = 0; i < hosts.length; i++) {
                        hosts[i].innerHTML = users[0];
                    }
                    var string = "";
                    for (let i = 1; i < users.length; i++) {
                        if (i != 1) string += ", ";
                        string += users[i];
                    }
                    var userlists = document.getElementsByClassName('userlist');
                    for (let i = 0; i < userlists.length; i++) {
                        userlists[i].innerHTML = string;
                    }
                    var totals = document.getElementsByClassName('total');
                    for (let i = 0; i < totals.length; i++) {
                        totals[i].innerHTML = users.length;
                    }
                    break;
            }
        } else if (json.hasOwnProperty('result')) {
            document.getElementById("partyid").value = json["result"].split(" ")[0];
            hideAllSAAS("page-attendee");
        }
    };
    socket.onclose = function (msg) {
        console.log("disconnected from " + host);
    };
}

function build(owner, partyId, users) {
    const roomDiv = document.createElement('div');
    roomDiv.classList.add('room');

    const roomDetailDiv = document.createElement('div');
    roomDetailDiv.classList.add('room-detail', 'flex', 'bar', 'mini-margin');

    const flexContainer1 = document.createElement('div');
    flexContainer1.classList.add('flex', 'gap');

    const userIconDiv = document.createElement('div');
    userIconDiv.innerHTML = '<i class="fa-solid fa-user fa-xl"></i>';
    flexContainer1.appendChild(userIconDiv);

    const hostDiv = document.createElement('div');
    hostDiv.classList.add('host');
    hostDiv.textContent = users;
    flexContainer1.appendChild(hostDiv);

    roomDetailDiv.appendChild(flexContainer1);

    const flexContainer2 = document.createElement('div');
    flexContainer2.classList.add('flex', 'gap');

    const roomIdDiv = document.createElement('div');
    roomIdDiv.classList.add('room-id');
    roomIdDiv.textContent = owner;
    flexContainer2.appendChild(roomIdDiv);

    const clipboardIconDiv = document.createElement('div');
    clipboardIconDiv.innerHTML = '<i class="selectable fa-solid fa-door-open"></i>';
    clipboardIconDiv.addEventListener("click", function () {
        document.getElementById("partyid").value = partyId;
        join(partyId);
    });

    flexContainer2.appendChild(clipboardIconDiv);

    roomDetailDiv.appendChild(flexContainer2);

    roomDiv.appendChild(roomDetailDiv);

    return roomDiv;
}

function updateDownload(progress) {
    document.getElementById("update").value = progress;
    document.getElementById("visual").innerHTML = progress + "%";
}

function copyToClipboard(text) {
    const textArea = document.createElement("textarea");
    textArea.value = text;
    document.body.appendChild(textArea);
    textArea.select();
    document.execCommand("copy");
    document.body.removeChild(textArea);
}