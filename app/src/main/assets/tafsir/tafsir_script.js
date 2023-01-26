function installContents(contentJson) {
    contentJson = JSON.parse(contentJson);

    // contentJsonFormat = {
    //     'tafsir-title': '',
    //     'verse-info-title': '',
    //     'verse-preview': '',
    //     'previous-tafsir-title': '',
    //     'next-tafsir-title': ''
    // };

    setTafsirTitle(contentJson["tafsir-title"]);
    setVerseInfoTitle(contentJson["verse-info-title"]);
    setVersePreview(contentJson["verse-preview"]);
    setPrevTafsirBtnTitle(contentJson["previous-tafsir-title"]);
    setNextTafsirBtnTitle(contentJson["next-tafsir-title"]);

}

function setTafsirTitle(tafsirTitle) {
    document.querySelector(".tafsir-title").innerHTML = tafsirTitle;
}
function setVerseInfoTitle(verseInfoTitle) {
    document.querySelector(".verse-info-title").innerHTML = verseInfoTitle;
}
function setVersePreview(versePreview) {
    var versePreviewElem = document.querySelector(".verse-preview");
    versePreviewElem.style.fontSize = versePreview.length > 200 ? "24px" : "30px";
    versePreviewElem.innerHTML = versePreview;
}

function setPrevTafsirBtnTitle(title) {
    title = title.trim();

    var prevBtns = document.querySelectorAll(".previous-verse");
    for (var i = 0; i < prevBtns.length; i++) {
        var btn = prevBtns[i];
        btn.querySelector(".title").innerHTML = title;
        if (!title || title.length == 0) {
            btn.classList.add("disabled");
        } else {
            btn.classList.remove("disabled");
        }
    }
}
function setNextTafsirBtnTitle(title) {
    title = title.trim();

    var nextBtnBtns = document.querySelectorAll(".next-verse");
    for (var i = 0; i < nextBtnBtns.length; i++) {
        var btn = nextBtnBtns[i];
        btn.querySelector(".title").innerHTML = title;
        if (!title || title.length == 0) {
            btn.classList.add("disabled");
        } else {
            btn.classList.remove("disabled");
        }
    }
}


function onWindowClick(e) {
    var btnTop = document.querySelector(".btn-action.go-to-top");
    var btnVerse = document.querySelector(".go-to-verse");

    if (e.target == btnTop || btnTop.contains(e.target)) {
        scrollTo(0, 0);
    } if (e.target == btnVerse || btnVerse.contains(e.target)) {
        window.TafsirJSInterface.goToVerse();
    } else {
        var btnsPrevVerse = document.querySelectorAll(".btn-action.previous-verse");
        var btnsNextVerse = document.querySelectorAll(".btn-action.next-verse");

        var found = false;
        for (var i = 0; i < btnsPrevVerse.length; i++) {
            var btn = btnsPrevVerse[i];

            if ((e.target == btn || btn.contains(e.target)) && !btn.classList.contains("disabled")) {
                console.log("PREVIOUS TAFSIR");
                window.TafsirJSInterface.previousTafsir();
                found = true;
                break;
            }
        }

        if (!found) {
            for (var i = 0; i < btnsNextVerse.length; i++) {
                var btn = btnsNextVerse[i];
                if ((e.target == btn || btn.contains(e.target)) && !btn.classList.contains("disabled")) {
                    console.log("NEXT TAFSIR");
                    window.TafsirJSInterface.nextTafsir();
                    break;
                }
            }

        }
    }
}

addEventListener("click", onWindowClick);
