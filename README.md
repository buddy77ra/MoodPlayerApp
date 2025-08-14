MoodPlayerApp
📌 Description
MoodPlayerApp is a beginner-friendly Java desktop application that lets you choose a mood and instantly see a matching playlist of songs you’ve added yourself.
It uses a simple GUI (Graphical User Interface) built with Java Swing, and opens music links directly in your web browser.

✨ Features
Choose from moods like Happy, Chill, Energetic, and Mixed.

Click songs to open them instantly in your browser.

Add or delete songs in each mood category.

Your songs are saved so they’re still there the next time you open the app.

Mood-based color themes for the interface.

Lightweight — runs on any computer with Java installed.

▶ How to Run
Install Java
Make sure you have Java 8 or higher installed.
You can check by running:

bash
Copy
Edit
java -version
Download or Clone This Repository

To clone with Git:

bash
Copy
Edit
git clone https://github.com/buddy77ra/MoodPlayerApp.git
cd MoodPlayerApp
Or download as a ZIP and extract.

Compile the App

bash
Copy
Edit
javac MoodPlayerApp.java
Run the App

bash
Copy
Edit
java MoodPlayerApp
💾 Saving & Loading Songs
Songs you add are stored automatically in a save file inside the app’s folder.

When you close and reopen the app, your playlists will still be there.

Deleting a song removes it from the save file.

📂 File Structure
bash
Copy
Edit
MoodPlayerApp/
│── MoodPlayerApp.java   # Main application source code
│── README.md            # Project description (this file)
│── manifest.mf          # Manifest for packaging as a JAR
│── savedPlaylists.dat   # (Created automatically) Stores your songs
📝 License
This project is for educational purposes only and contains links provided by the user.
All rights to linked music remain with their respective owners.
