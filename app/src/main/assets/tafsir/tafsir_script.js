function onWindowClick(e) {
    var btnTop = document.querySelector(".go-to-top");

    if (e.target == btnTop || btnTop.contains(e.target)) {
        window.TafsirJSInterface.goToTop();
    }
}

addEventListener("click", onWindowClick);
