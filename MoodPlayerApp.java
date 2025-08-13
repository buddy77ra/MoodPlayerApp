import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.datatransfer.*;   // Clipboard, DataFlavor
import java.net.*;               // URL, URI, URLConnection
import java.io.*;                // InputStream, BufferedReader, etc.
import java.util.*;
import java.util.List;

public class MoodPlayerApp {

    // -------- Simple item to hold a title + URL --------
    static class LinkItem {
        final String title;
        final String url;
        LinkItem(String title, String url) { this.title = title; this.url = url; }
        @Override public String toString() { return title; }
    }

    // -------- Fields --------
    private JFrame frame;
    private JButton happyButton, chillButton, energeticButton, mixedButton, openButton;
    private JButton addLinkButton;// from clipboard/prompt
    private JButton deleteButton;
    private JList<LinkItem> playlistList;
    private DefaultListModel<LinkItem> listModel;
    private JLabel nowPlaying;
    private JPanel top;
    private String currentMood = "";
    private JTextField urlField;              // manual paste box

private void deleteSelected() {
    int index = playlistList.getSelectedIndex();
    if (index < 0) {
        JOptionPane.showMessageDialog(frame, "Select a song to delete.");
        return;
    }

    LinkItem item = listModel.getElementAt(index);

    // Confirm (optional)
    int choice = JOptionPane.showConfirmDialog(
        frame,
        "Delete \"" + item.title + "\" from \"" + currentMood + "\"?",
        "Confirm Delete",
        JOptionPane.YES_NO_OPTION
    );
    if (choice != JOptionPane.YES_OPTION) return;

    // Update data model
    if ("mixed".equals(currentMood)) {
        // Mixed is rebuilt each time from other lists, so just remove from the visible list
        listModel.remove(index);
        // No change to happy/chill/energetic here
    } else {
        List<LinkItem> list = moodData.get(currentMood);
        if (list != null) {
            // Remove by identity if possible; fallback to index if lengths match
            boolean removed = list.remove(item);
            if (!removed && index < list.size()) list.remove(index);
        }
        listModel.remove(index);
    }

    // Clear Now Playing label if we deleted the selected row
    nowPlaying.setText("Now Playing: —");
}

    // mood -> list
    private final Map<String, List<LinkItem>> moodData = new LinkedHashMap<>();

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MoodPlayerApp app = new MoodPlayerApp();
            app.createAndShowGUI();
        });
    }

    // -------- GUI --------
    private void createAndShowGUI() {
        frame = new JFrame("Mood-based Music Player");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(680, 420);
        frame.setLayout(new BorderLayout(10, 10));

        // TOP: mood buttons + Now Playing
        top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        happyButton = new JButton("Happy");
        chillButton = new JButton("Chill");
        energeticButton = new JButton("Energetic");
        mixedButton = new JButton("Mixed");
        nowPlaying = new JLabel("Now Playing: —");

        top.add(new JLabel("Select mood:"));
        top.add(happyButton);
        top.add(chillButton);
        top.add(energeticButton);
        top.add(mixedButton);
        top.add(Box.createHorizontalStrut(10));
        top.add(nowPlaying);

        // CENTER: list
        listModel = new DefaultListModel<>();
        playlistList = new JList<>(listModel);
        playlistList.setVisibleRowCount(12);
        playlistList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(playlistList);

        // BOTTOM: URL field + Add + Add Link (clipboard) + Open
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));

        bottom.add(new JLabel("URL:"));
        urlField = new JTextField(28);
        bottom.add(urlField);

        JButton addFromUrlButton = new JButton("Add");
        bottom.add(addFromUrlButton);
        
        addLinkButton = new JButton("Add Link"); // from clipboard
        bottom.add(addLinkButton);

        deleteButton = new JButton("Delete Selected");   // <-- add this
        bottom.add(deleteButton);

        openButton = new JButton("Open Selected");
        bottom.add(openButton);

        // Layout
        frame.add(top, BorderLayout.NORTH);
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(bottom, BorderLayout.SOUTH);

        // Data
        seedData();

        // Actions
        happyButton.addActionListener(e -> showPlaylist("happy"));
        chillButton.addActionListener(e -> showPlaylist("chill"));
        energeticButton.addActionListener(e -> showPlaylist("energetic"));
        mixedButton.addActionListener(e -> showPlaylist("mixed"));

        playlistList.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) openSelected();
            }
        });

        // Buttons
        addFromUrlButton.addActionListener(e -> {
            String url = urlField.getText().trim();
            if (url.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Paste a URL into the box first.");
                return;
            }
            addLinkFromUrl(url);
            urlField.setText("");
        });
        // Enter key in URL box
        urlField.addActionListener(e -> addFromUrlButton.doClick());

        addLinkButton.addActionListener(e -> addLinkFromClipboardOrPrompt());
        openButton.addActionListener(e -> openSelected());

         // Delete button
        deleteButton.addActionListener(e -> deleteSelected());

           // Delete key on the list (Delete or Backspace)
        playlistList.addKeyListener(new KeyAdapter() {
     @Override public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_DELETE || e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
            deleteSelected();
        }
    }
});

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // default mood
        showPlaylist("happy");
    }

    // -------- Show playlist for a mood (rebuild "mixed" on the fly) --------
    private void showPlaylist(String moodKey) {
        currentMood = moodKey;

        if ("mixed".equals(moodKey)) {
            List<LinkItem> mixed = new ArrayList<>();
            mixed.addAll(moodData.getOrDefault("happy", Collections.emptyList()));
            mixed.addAll(moodData.getOrDefault("chill", Collections.emptyList()));
            mixed.addAll(moodData.getOrDefault("energetic", Collections.emptyList()));
            moodData.put("mixed", mixed);
        }

        listModel.clear();
        for (LinkItem item : moodData.getOrDefault(moodKey, Collections.emptyList())) {
            listModel.addElement(item);
        }

        Color c = switch (moodKey) {
            case "happy" -> new Color(255, 245, 180);
            case "chill" -> new Color(210, 230, 255);
            case "energetic" -> new Color(255, 215, 215);
            case "mixed" -> new Color(225, 225, 240);
            default -> top.getBackground();
        };
        top.setBackground(c);
        top.repaint();
    }

    // -------- Open selected link --------
    private void openSelected() {
        LinkItem item = playlistList.getSelectedValue();
        if (item == null) {
            JOptionPane.showMessageDialog(frame, "Pick a song/playlist first.");
            return;
        }
        nowPlaying.setText("Now Playing: " + item.title);
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(item.url));
            } else {
                JOptionPane.showMessageDialog(frame, "Desktop browse not supported on this system.");
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(frame, "Could not open link:\n" + ex.getMessage());
        }
    }

    // -------- Add link from clipboard or manual prompt --------
    private void addLinkFromClipboardOrPrompt() {
        try {
            String url = readUrlFromClipboard();
            if (url == null || !url.startsWith("http")) {
                url = JOptionPane.showInputDialog(frame, "Paste a YouTube/Spotify/Apple Music URL:");
                if (url == null || !url.startsWith("http")) return;
            }

            String title = tryFetchPageTitle(url);
            if (title == null || title.isBlank()) {
                title = JOptionPane.showInputDialog(frame, "Enter a title for this link:");
                if (title == null || title.isBlank()) return;
            }

            List<LinkItem> list = moodData.get(currentMood);
            if (list == null) {
                list = new ArrayList<>();
                moodData.put(currentMood, list);
            }
            LinkItem item = new LinkItem(title, url);
            list.add(item);

            if ("mixed".equals(currentMood)) {
                showPlaylist("mixed");
            } else {
                listModel.addElement(item);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(frame, "Could not add link:\n" + ex.getMessage());
        }
    }

    // -------- Add link from URL in the text box --------
    private void addLinkFromUrl(String url) {
        if (url == null || !url.startsWith("http")) {
            JOptionPane.showMessageDialog(frame, "That doesn't look like a valid URL.");
            return;
        }

        String title = tryFetchPageTitle(url);
        if (title == null || title.trim().isEmpty()) {
            title = JOptionPane.showInputDialog(frame, "Enter a title for this link:");
            if (title == null || title.trim().isEmpty()) return;
        }

        List<LinkItem> list = moodData.get(currentMood);
        if (list == null) {
            list = new ArrayList<>();
            moodData.put(currentMood, list);
        }

        LinkItem item = new LinkItem(title, url);
        list.add(item);

        if ("mixed".equals(currentMood)) {
            showPlaylist("mixed");
        } else {
            listModel.addElement(item);
        }
    }

    // -------- Helpers --------
    private String readUrlFromClipboard() {
        try {
            Toolkit tk = Toolkit.getDefaultToolkit();
            Clipboard cb = tk.getSystemClipboard();
            String data = (String) cb.getData(DataFlavor.stringFlavor);
            if (data != null) {
                data = data.trim();
                if (data.startsWith("http://") || data.startsWith("https://")) return data;
            }
        } catch (Exception ignore) { }
        return null;
    }

    private String tryFetchPageTitle(String urlStr) {
        BufferedReader reader = null;
        try {
            URL url = new URL(urlStr);
            URLConnection conn = url.openConnection();
            conn.setConnectTimeout(4000);
            conn.setReadTimeout(4000);
            InputStream is = conn.getInputStream();
            reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            int max = 20000; // read up to ~20KB
            while ((line = reader.readLine()) != null && sb.length() < max) {
                sb.append(line).append('\n');
                if (line.toLowerCase().contains("</title>")) break;
            }
            String html = sb.toString();
            int start = html.toLowerCase().indexOf("<title>");
            int end = html.toLowerCase().indexOf("</title>");
            if (start >= 0 && end > start) {
                String title = html.substring(start + 7, end).trim();
                if (title.length() > 120) title = title.substring(0, 117) + "...";
                return title;
            }
        } catch (Exception ignore) {    // network blocked is OK; we'll prompt instead
        } finally {
            try { if (reader != null) reader.close(); } catch (IOException ignored) {}
        }
        return null;
    }

    // -------- Seed starter data (editable lists) --------
    private void seedData() {
        moodData.put("happy", new ArrayList<>(Arrays.asList(
            new LinkItem("Happy Single 1", "https://www.youtube.com/watch?v=dQw4w9WgXcQ"),
            new LinkItem("Happy Single 2", "https://music.youtube.com/watch?v=xxxxxxxxxxx"),
            new LinkItem("Happy Album Playlist", "https://open.spotify.com/playlist/xxxxxxxxxxx")
        )));

        moodData.put("chill", new ArrayList<>(Arrays.asList(
            new LinkItem("Chill Single 1", "https://www.youtube.com/watch?v=yyyyyyyyyyy"),
            new LinkItem("Lo-fi Mix", "https://www.youtube.com/watch?v=5qap5aO4i9A"),
            new LinkItem("Chill Album", "https://open.spotify.com/album/xxxxxxxxxxx")
        )));

        moodData.put("energetic", new ArrayList<>(Arrays.asList(
            new LinkItem("Energetic Single 1", "https://www.youtube.com/watch?v=zzzzzzzzzzz"),
            new LinkItem("Workout Mix", "https://open.spotify.com/playlist/xxxxxxxxxxx"),
            new LinkItem("High-Energy Album", "https://music.apple.com/ca/album/ID_HERE")
        )));

        moodData.put("mixed", new ArrayList<>()); // built dynamically
    }
}

