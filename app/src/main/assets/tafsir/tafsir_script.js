function changeFontSize(fontSize) {
    document.querySelector("body").style.fontSize = fontSize + "%";
}

function onWindowClick(e) {
    var btnTop = document.querySelector(".go-to-top");

    if (typeof btnTop !== 'undefined' && (e.target == btnTop || btnTop.contains(e.target))) {
        window.TafsirJSInterface.goToTop();
    }
}

addEventListener("click", onWindowClick);
