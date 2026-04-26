import os
import json

files = [
    "astronomy.html", "biology.html", "botany.html", "embryology.html",
    "general.html", "geography.html", "geology.html", "medicine.html",
    "oceanography.html", "physics.html", "physiology.html", "zoology.html"
]

langs = ["ru", "it", "pt", "pl", "uk"]

translations = {
    "ru": {
        "astronomy.html": "astronomy_ru_content", # Placeholder
        "biology.html": "biology_ru_content",
        # ...
    }
}

# Da ich den gesamten Text generieren muss, werde ich eine Funktion schreiben, 
# die die Übersetzung für eine Datei in eine Sprache simuliert oder bereitstellt.
# Da ich aber 60 Dateien (12 * 5) bearbeiten muss, werde ich dies schrittweise tun.

def create_translated_files():
    # Example for IT Biology
    it_biology = \"\"\"<h1>Biologia</h1><h3>OGNI COSA VIVENTE È FATTA DI ACQUA</h3><img src=\\\"https://assets-file/science/img/human_cells_water.webp\\\"/><p>Nel campo della biologia, il Sacro Corano dice nella Sura Al Anbiya, Capitolo n. 21, Versetto n. 30:<br><br><i>“Abbiamo creato ogni cosa vivente dall'acqua - Non crederete dunque?“</i></p><div class=\\\"reference\\\">{{REF_AR=21:30}}<p class=\\\"translation\\\">{{REF_TR=21:30}}</p><div class=\\\"footer\\\"><span>{{REF_NAME=21:30}}</span><span class=\\\"sep\\\"></span><a href=\\\"https://quranapp.verse.reader/21/30\\\">Apri nel lettore</a></div></div><p>Immaginate nei deserti dell'Arabia, dove c'era scarsità d'acqua, il Corano dice... Tutto è stato creato dall'acqua... chi ci avrebbe creduto? Qualsiasi altra cosa il Corano avesse detto, la gente avrebbe potuto crederci. <b>“Acqua”</b> - dove c'era scarsità - il Corano dice 1400 anni fa... “Ogni cosa vivente è creata dall'acqua”. Oggi siamo venuti a sapere che il citoplasma, che è la sostanza fondamentale della cellula vivente, contiene l'80% di acqua. Ogni creatura vivente contiene dal 50 al 70% di acqua, e senza acqua, la creatura vivente non può sopravvivere - è un must.<br><br>Il Corano vi chiede... <i>“Abbiamo creato dall'acqua ogni cosa vivente - Non crederete dunque?”</i><br><br>Allah sta dicendo... <b>“che tutto è creato dall'acqua”</b> - cosa che avete scoperto oggi. Il Corano lo menziona 1400 anni fa... “Non crederete dunque?” Ti sta ponendo una domanda, vuole una risposta.<br><br>Il Corano dice nella Sura Nur, Capitolo n. 24, Versetto n. 45, che:<br><br><i>“Abbiamo creato ogni animale vivente dall'acqua.”</i></p><div class=\\\"reference\\\">{{REF_AR=24:45}}<p class=\\\"translation\\\">{{REF_TR=24:45}}</p><div class=\\\"footer\\\"><span>{{REF_NAME=24:45}}</span><span class=\\\"sep\\\"></span><a href=\\\"https://quranapp.verse.reader/24/45\\\">Apri nel lettore</a></div></div><p>Il Corano dice nella Sura Furqan, Capitolo n. 25, Versetto n. 54:<br><br><i>“Abbiamo creato l'uomo dall'acqua”</i></p><div class=\\\"reference\\\">{{REF_AR=25:54}}<p class=\\\"translation\\\">{{REF_TR=25:54}}</p><div class=\\\"footer\\\"><span>{{REF_NAME=25:54}}</span><span class=\\\"sep\\\"></span><a href=\\\"https://quranapp.verse.reader/25/54\\\">Apri nel lettore</a></div></div><p>Immaginate che 1400 anni fa, il Corano menzioni questo.</p>\"\"\"
    with open(\"/home/mario/Github/QuranApp/app/src/main/assets/science/topics/it/biology.html\", \"w\") as f:
        f.write(it_biology)

create_translated_files()
