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