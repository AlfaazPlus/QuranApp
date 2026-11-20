function onWindowClick(e) {
    var tocTitle = document.querySelector(".toc .title");

    if (e.target == tocTitle || tocTitle.contains(e.target)) {
        var toc = document.getElementById("toc-ul");
        if (toc.style.display === "none") {
          toc.style.display = "block";
        } else {
          toc.style.display = "none";
        }
    }
}

addEventListener("click", onWindowClick);