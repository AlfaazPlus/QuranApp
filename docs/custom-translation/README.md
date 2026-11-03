# Custom translations

A custom translation is a UTF-8 JSON file of verse text. It uses the same shape as the files in [inventory/translations](/inventory/translations), so open one of those files and follow it. The import sheet is the source of the book title, author, display name, and language, and any book details inside the file are ignored.

![Import sheet for a custom translation](import.jpg)

Importing the same book and language again replaces that translation. The translation stays on the device and is not updated from the catalog.

The file must contain a `suras` array with at least one chapter. Each chapter has an `index` from 1 to 114 and an `ayas` array, which may be empty. Each verse has an `index` from 1 to 286 and a `translation` string, which may also be empty. Chapter numbers must be unique in the file, and verse numbers must be unique within their chapter. An `index` may be a number or a numeric string such as `"2"`. Any other keys, such as `id`, are ignored.

Footnotes are optional. When a verse includes them, `footnotes` is an array of objects. Each object has a positive `index`, unique within that verse, and a `text` string, which may be empty.

```json
{
  "suras": [
    {
      "index": 1,
      "ayas": [
        {
          "index": 2,
          "translation": "All praise is for Allah.<fn index=\"1\">1</fn>",
          "footnotes": [
            { "index": 1, "text": "A short note. See <reference chapter=\"2\" verses=\"255\">2:255</reference>." }
          ]
        }
      ]
    }
  ]
}
```

Verse text and footnote text accept the same markup, and the only allowed tags are `<fn index="1">` and `<reference chapter="2" verses="255">`. Every tag must be closed. The `index` on `<fn>` and the `chapter` on `<reference>` must be positive whole numbers. The `verses` attribute is optional. When it is present, it is a single number (`12`), a range (`12-15`), or a list (`12,15,18-20`). Any `<` or `&` in the text must belong to a tag or an escape such as `&amp;` or `&#39;`. Quotes and backslashes must be valid JSON.

Verses that are left out of the file stay blank in the reader. The file must be 20 MB or smaller.
