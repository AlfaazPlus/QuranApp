function installContents(contentJson) {
    contentJson = JSON.parse(contentJson);

    // contentJsonFormat = {
    //     'text-direction': '',
    //     'chapter-icon-unicode': '',
    //     'chapter-title': '',
    //     'chapter-no': '',
    //     'juz-no': '',
    //     'verse-count': '',
    //     'ruku-count': '',
    //     'pages': '',
    //     'revelation-order': '',
    //     'revelation-type': '',
    // }; 

    setContentTextDirection(contentJson["text-direction"]);
    setChapterIconUnicode(contentJson["chapter-icon-unicode"]);
    setChapterTitle(contentJson["chapter-title"]);
    setChapterNo(contentJson["chapter-no"]);
    setJuzNo(contentJson["juz-no"]);
    setVerseCount(contentJson["verse-count"]);
    setRukuCount(contentJson["ruku-count"]);
    setPages(contentJson["pages"]);
    setRevelationOrder(contentJson["revelation-order"]);
    setRevelationType(contentJson["revelation-type"]);

}

function setContentTextDirection(direction) {
    var contentElem = document.querySelector(".content");
    contentElem.style.direction = direction;
    contentElem.style.display = "";

    var elems = contentElem.querySelectorAll("*");
    var len = elems.length;
    for (var i = 0; i < len; i++) {
        var elem = elems[i];
        if (elem.innerHTML) {
            elem.innerHTML = elem.innerHTML.trim().replace(/&nbsp;/g, "");
            if (elem.innerHTML.length == 0) {
                elem.remove();
            }
        }
    }
}


function setChapterIconUnicode(unicode) {
    document.querySelector(".chapter-title-icon").innerHTML = unicode;
}

function setChapterTitle(title) {
    document.querySelector(".chapter-title-trans").innerHTML = title;
}

function setChapterNo(chapterNo) {
    var split = chapterNo.split(":");
    document.querySelector(".chapter-no").innerHTML = split[0] + ": " + split[1];
}

function setJuzNo(juzNo) {
    var split = juzNo.split(":");
    document.querySelector(".info-card.juz-no .title").innerHTML = split[0];
    document.querySelector(".info-card.juz-no .value").innerHTML = split[1];
}
function setVerseCount(verseCount) {
    var split = verseCount.split(":");
    document.querySelector(".info-card.verse-count .title").innerHTML = split[0];
    document.querySelector(".info-card.verse-count .value").innerHTML = split[1];
}
function setRukuCount(rukuCount) {
    var split = rukuCount.split(":");
    document.querySelector(".info-card.ruku-count .title").innerHTML = split[0];
    document.querySelector(".info-card.ruku-count .value").innerHTML = split[1];
}
function setPages(pages) {
    var split = pages.split(":");
    document.querySelector(".info-card.pages .title").innerHTML = split[0];
    document.querySelector(".info-card.pages .value").innerHTML = split[1];
}
function setRevelationOrder(order) {
    var split = order.split(":");
    document.querySelector(".info-card.revelation-order .title").innerHTML = split[0];
    document.querySelector(".info-card.revelation-order .value").innerHTML = split[1];
}
function setRevelationType(revlType) {
    var split = revlType.split(":");
    document.querySelector(".info-card.revelation-type .title").innerHTML = split[0];
    document.querySelector(".info-card.revelation-type .value").innerHTML = split[1];
}

function setDarkMode(darkMode) {
    if (darkMode == true || darkMode == "true" || darkMode == "True") {
        document.body.classList = "dark";
    } else {
        document.body.classList = "light";
    }
}

addEventListener("click", function (e) {
    e.preventDefault();
    var links = this.document.querySelectorAll("a[href]");

    for (var i = 0; i < links.length; i++) {
        var link = links[i];

        if (link == e.target || link == e.target.parentElement) {
            var splits = link.getAttribute("href").split("/");

            if (!splits) {
                console.log(splits)
                window.ChapterInfoJSInterface.openReference(-1, -1, -1);
                return;
            }

            splits = splits.filter(function (item) {
                return item;
            });


            if (splits.length < 2) {
                console.log(splits)
                window.ChapterInfoJSInterface.openReference(-1, -1, -1);
                return;
            }


            var chapterNo = parseInt(splits[0]);
            var verses = splits[1].split("-");
            var fromVerse = parseInt(verses[0]);
            var toVerse = verses.length > 1 ? parseInt(verses[1]) : fromVerse;

            window.ChapterInfoJSInterface.openReference(chapterNo, fromVerse, toVerse);

            break;
        }
    }
});