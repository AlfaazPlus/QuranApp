# Contributing to the QuranApp

Thank you for considering contributing to the QuranApp! May Allah reward you for any useful contribution. We welcome and appreciate any contributions, it can be:

- Quran Translations
- App Translations
- Recitations
- Bug reports
- Feature requests
- Code contributions
- Documentation improvements
- Design suggestions

We welcome your contribution to even this page.

## Before you start
Please note that all new code contributions must be written in Kotlin. Only critical patches can be done in Java. Please follow the [Kotlin Coding Conventions](https://kotlinlang.org/docs/reference/coding-conventions.html) and the [Android Kotlin Style Guide](https://developer.android.com/kotlin/style-guide). You may also setup our [kotlin_code_style.xml](https://github.com/AlfaazPlus/QuranApp/blob/master/kotlin_code_style.xml) in your IDE.

## Getting Started
1. Fork the repository and clone it to your local machine. 
2. Make sure you have the necessary tools and dependencies to build the project, including Android Studio and the Android SDK. 
3. Open the project in Android Studio and make your changes.
4. Create a new branch for your changes. 
5. Commit your changes with a clear and descriptive commit message. 
6. Push your changes to your fork and submit a pull request with a proper explanation.

## Firebase Configuration
Our project uses Firebase for various functionality, including authentication and storage. In order to build and run the project on your local machine, you will need to at least set up a Firebase project and configure the app with your own Firebase credentials.
1. Go to the [Firebase Console](https://console.firebase.google.com/) and create a new project. 
2. Add an Android app to the project and download the `google-services.json` file. 
3. Place the `google-services.json` file in the `app` directory of the project. 
4. Following services are being used:
   - Cloud Storage
5. You don't really need to enable all of them if you are not working on features requiring them.

## Quran Translations
The quran translations used in the app are in a special format. If you would like to contribute a translation, you can find the format in one of the [pre-built translations](https://github.com/AlfaazPlus/QuranApp/tree/master/app/src/main/assets/prebuilt_translations). You must consider the following points:
1. The translation must be in JSON format and illegal characters like quotes, backslashes, etc. must be escaped.
2. Every footnote reference must be enclosed in html syntax or it will not be parsed correctly. The syntax is `<fn id="[id]" index="[index]">`. Explanation:
    - `[id]` is the id of the footnote. It is unique to the whole translation file.
    - `[index]` is the one-based index of the footnote in the specific verse. It is unique to the verse.
    - Example: `<fn id="1" index="1">`. This means that the footnote with id 1 is the first footnote in the verse.
3. Every verse reference must be enclosed in html syntax or it will not be parsed correctly. The verse references have several syntax which are:
    - `<reference chapter="[chapter_no]" verses="[verse_no]">[display_text]</reference>`.
    
       Example: `<reference chapter="1" verses="1">1:1</reference>`. This means that the reference is to chapter 1, verse 1.
   
    - `<reference chapter="[chapter_no]" verses="[from_verse_no]-[to_verse_no]">[display_text]</reference>`.
    
       Example: `<reference chapter="1" verses="1-5">1:1-5</reference>`. This means that the reference is to chapter 1, verses from 1 to 5.
    - `<reference chapter="[chapter_no]" verses="[verse_1],[verse_2],[verse_n]">[display_text]</reference>`.
    
       Example: `<reference chapter="2" verses="1,5,29">2:1,5,29</reference>`. This means that the reference is to chapter 1, verses 1, 5, and 29.

## Recitations
You can provide free api links to recitations.
