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

    let senders = document.getElementsByClassName('sender');
    for (let i = 0; i < senders.length; i++) {
        let sender = senders[i];
        sender.addEventListener("keyup", function (event) {
            event.preventDefault();
            if (event.keyCode === 13) {
                console.log(sender.value);
                message(sender.value);
                sender.value = "";
            }
        });
    }

    document.getElementById("accept-update").addEventListener("click", function () {
        call(origin + 'v1/config/invoke');
        hideAllSAAS("page-update");
    });

    document.getElementById("discord").addEventListener("click", function () {
        openURL('https://discord.hawolt.com');
    });

    document.getElementById("reveal").addEventListener("click", function () {
        reveal();
    });

    fetch(origin + 'v1/config/websocket')
        .then((response) => response.text())
        .then((data) => {
            connect("ws://127.0.0.1:" + data);
        })
        .catch((error) => {
            console.error('Error:', error);
        });


    const partyVisibility = document.getElementById('partyVisibility');
    partyVisibility.addEventListener('change', e => {
        if (e.target.checked === true) {
            togglePartyVisibility(document.getElementById("partyid").value, true);
        }
        if (e.target.checked === false) {
            togglePartyVisibility(document.getElementById("partyid").value, false);
        }
    });

    const trackVisibility = document.getElementById('trackVisibility');
    trackVisibility.addEventListener('change', e => {
        if (e.target.checked === true) {
            toggleTrackVisibility(document.getElementById("partyid").value, false);
        }
        if (e.target.checked === false) {
            toggleTrackVisibility(document.getElementById("partyid").value, true);
        }
    });


    const visibility = document.getElementById('visibility');

    var options = document.getElementsByClassName("option");
    for (let i = 0; i < options.length; i++) {
        options[i].addEventListener("click", e => {
            if (!e.target.classList.contains('disabled')) {
                let active = getActiveSAAS();
                if (!active.includes("settings")) {
                    settings.dataset.previous = active;
                }
                document.getElementById("return").classList.remove("disabled");
                for (let i = 0; i < options.length; i++) {
                    options[i].classList.add("disabled");
                }
                if (e.target.id == 'settings') {
                    if (document.getElementById('jamalong').dataset.status === "host") {
                        if (visibility.classList.contains("hidden")) visibility.classList.remove("hidden");
                    } else {
                        if (!visibility.classList.contains("hidden")) visibility.classList.add("hidden");
                    }
                }
                hideAllSAAS("page-" + e.target.id);
            }
        });
    }

    document.getElementById("return").addEventListener("click", e => {
        if (!e.target.classList.contains('disabled')) {
            hideAllSAAS(settings.dataset.previous);
            document.getElementById("return").classList.add("disabled");
            for (let i = 0; i < options.length; i++) {
                options[i].classList.remove("disabled");
            }
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

    document.getElementById("invite").addEventListener("click", function () {
        copyToClipboard('https://jamalo.ng/' + document.getElementById("party").innerHTML);
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
    gatekeep(true);
    configure("idle");
    var options = document.getElementsByClassName("option");
    for (let i = 0; i < options.length; i++) {
        document.getElementById("return").classList.add("disabled");
        for (let i = 0; i < options.length; i++) {
            options[i].classList.remove("disabled");
        }
    }
    document.getElementById('nowplaying').innerHTML = "";
    let boxes = document.getElementsByClassName('messagebox');
    for (let i = 0; i < boxes.length; i++) {
        boxes[i].innerHTML = "";
    }
    hideAllSAAS("page-landing");
    fetch(origin + 'v1/config/version')
        .then((response) => response.text())
        .then((data) => {
            if (data === "false") return;
            hideAllSAAS("page-updater");
        })
        .catch((error) => {
            console.error('Error:', error);
        });
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
    let active = getActiveSAAS();
    if (active === "updater" || active === "update") return;
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
    let party = partyId.length === 0 ? 'nil' : partyId;
    call(origin + 'v1/api/namechange/' + btoa(name) + '/' + party);
}

function join(partyId) {
    fetch(origin + 'v1/api/join/' + partyId)
        .then((response) => response.json())
        .then((data) => {
            let args = data['result'].split(' ');
            if (args[0] === partyId) {
                hideAllSAAS("page-chat");
            }
        })
        .catch((error) => {
            console.error('Error:', error);
        });
}

function openURL(link) {
    call(origin + 'v1/config/open/' + btoa(link));
}

function reveal() {
    call(origin + 'v1/api/reveal');
}

function message(msg) {
    call(origin + 'v1/api/chat/' + btoa(encodeURIComponent(msg)));
}

function load(link) {
    call(origin + 'v1/api/load?url=' + btoa(link));
}

function togglePartyVisibility(partyId, status) {
    call(origin + 'v1/api/visibility/' + partyId + '/' + status);
}

function toggleTrackVisibility(partyId, status) {
    call(origin + 'v1/api/gatekeeper/' + partyId + '/' + status);
}

function gatekeep(status) {
    let reveal = document.getElementById("reveal");
    let secondary = document.getElementById("secondary");
    if (status) {
        reveal.classList.add('hidden');
        secondary.classList.remove('hidden');
    } else {
        reveal.classList.remove('hidden');
        secondary.classList.add('hidden');
    }
}

function connect(host) {
    let socket = new WebSocket(host);
    socket.onopen = function (msg) {
        console.log("Connected to " + host);
        hideAllSAAS("page-landing");
    };
    socket.onmessage = function (msg) {
        console.log(msg.data);
        const json = JSON.parse(msg.data);
        console.log(json);
        if (json.hasOwnProperty('instruction')) {
            switch (json['instruction']) {
                case 'chat':
                    let boxes = document.getElementsByClassName('messagebox');
                    for (let i = 0; i < boxes.length; i++) {
                        let box = boxes[i];
                        let div = document.createElement('div');
                        let span = document.createElement('span');
                        span.classList = "msg-user";
                        span.innerHTML = json['user'];
                        span.title = json['identifier'];
                        div.appendChild(span);
                        let msg = document.createElement('span');
                        if (!json.hasOwnProperty('type')) {
                            msg.innerHTML = ": " + json['message'];
                        } else {
                            msg.innerHTML = " " + json['message'];
                        }
                        const currentDate = new Date();
                        const formattedTimestamp = currentDate.toLocaleString();
                        msg.title = formattedTimestamp;
                        div.appendChild(msg);
                        box.appendChild(div);
                        box.scrollTo(0, box.scrollHeight);
                    }
                    break;
                case 'rediscover':
                    discover();
                    break;
                case 'reset-gatekeeper':
                    gatekeep(true);
                    break;
                case 'gatekeeper':
                    gatekeep(json['status']);
                    break;
                case 'reveal':
                    document.getElementById('nowplaying').innerHTML = json['name'];
                    break;
                case 'close':
                    openLandingPage();
                    break;
                case 'download':
                    updateDownload(json['progress'])
                    break;
                case 'kill':
                    openLandingPage();
                    break;
                case 'list':
                    var users = json['users'];
                    var hosts = document.getElementsByClassName('hoster');
                    for (let i = 0; i < hosts.length; i++) {
                        hosts[i].innerHTML = users[0];
                    }

                    var userlists = document.getElementsByClassName('userlist');
                    for (let i = 0; i < userlists.length; i++) {
                        let userlist = userlists[i];
                        userlist.innerHTML = "";
                        for (let i = 1; i < users.length; i++) {
                            let div = document.createElement('div');
                            div.innerHTML = users[i];
                            userlist.appendChild(div);
                        }
                    }

                    var totals = document.getElementsByClassName('total');
                    for (let i = 0; i < totals.length; i++) {
                        totals[i].innerHTML = users.length;
                    }
                    break;
            }
        } else if (json.hasOwnProperty('result')) {
            document.getElementById("partyid").value = json["result"].split(" ")[0];
            hideAllSAAS("page-chat");
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