import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.datatransfer.*;   // Clipboard, DataFlavor
import java.net.*;               // URL, URI, URLConnection
import java.io.*;                // File, PrintWriter, BufferedReader, etc.
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
    private JButton addLinkButton;            // from clipboard/prompt
    private JButton deleteButton;             // delete selected
    private JList<LinkItem> playlistList;
    private DefaultListModel<LinkItem> listModel;
    private JLabel nowPlaying;
    private JPanel top;
    private String currentMood = "";
    private JTextField urlField;              // manual paste box

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

        // BOTTOM: URL field + Add + Add Link (clipboard) + Delete + Open
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));

        bottom.add(new JLabel("URL:"));
        urlField = new JTextField(28);
        bottom.add(urlField);

        JButton addFromUrlButton = new JButton("Add");
        bottom.add(addFromUrlButton);

        addLinkButton = new JButton("Add Link"); // from clipboard
        bottom.add(addLinkButton);

        deleteButton = new JButton("Delete Selected");
        bottom.add(deleteButton);

        openButton = new JButton("Open Selected");
        bottom.add(openButton);

        // Layout
        frame.add(top, BorderLayout.NORTH);
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(bottom, BorderLayout.SOUTH);

        // Data
        seedData();      // create empty lists
        loadFromFile();  // load saved links from previous runs

        // Actions
        happyButton.addActionListener(evt -> showPlaylist("happy"));
        chillButton.addActionListener(evt -> showPlaylist("chill"));
        energeticButton.addActionListener(evt -> showPlaylist("energetic"));
        mixedButton.addActionListener(evt -> showPlaylist("mixed"));

        playlistList.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 2) openSelected();
            }
        });

        // Add from text field
        addFromUrlButton.addActionListener(evt -> {
            String url = urlField.getText().trim();
            if (url.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Paste a URL into the box first.");
                return;
            }
            addLinkFromUrl(url);
            urlField.setText("");
        });
        // Enter key in URL box
        urlField.addActionListener(evt -> addFromUrlButton.doClick());

        addLinkButton.addActionListener(evt -> addLinkFromClipboardOrPrompt());
        openButton.addActionListener(evt -> openSelected());

        // Delete button
        deleteButton.addActionListener(evt -> deleteSelected());
        // Delete key
        playlistList.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent evt) {
                if (evt.getKeyCode() == KeyEvent.VK_DELETE || evt.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                    deleteSelected();
                }
            }
        });

        // Save on window close as a safety
        frame.addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                saveToFile();
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

    // -------- Delete selected --------
    private void deleteSelected() {
        int index = playlistList.getSelectedIndex();
        if (index < 0) {
            JOptionPane.showMessageDialog(frame, "Select a song to delete.");
            return;
        }
        LinkItem item = listModel.getElementAt(index);

        int choice = JOptionPane.showConfirmDialog(
            frame,
            "Delete \"" + item.title + "\" from \"" + currentMood + "\"?",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION
        );
        if (choice != JOptionPane.YES_OPTION) return;

        if ("mixed".equals(currentMood)) {
            listModel.remove(index); // derived view only
        } else {
            List<LinkItem> list = moodData.get(currentMood);
            if (list != null) {
                boolean removed = list.remove(item);
                if (!removed && index < list.size()) list.remove(index);
            }
            listModel.remove(index);
        }
        nowPlaying.setText("Now Playing: —");
        saveToFile(); // persist change
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
            saveToFile(); // persist change
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
        saveToFile(); // persist change
    }

    // -------- Clipboard helper --------
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

    // -------- Try to fetch web page <title> --------
    private String tryFetchPageTitle(String urlStr) {
        BufferedReader reader = null;
        try {
            URL url = URI.create(urlStr).toURL(); // avoid deprecated ctor warning
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
        } catch (Exception ignore) {
        } finally {
            try { if (reader != null) reader.close(); } catch (IOException ignored) {}
        }
        return null;
    }

    // -------- Persistence helpers (save/load to project file) --------
    private File dataFile() {
        // Save inside the project so it can be committed and shared
        File dir = new File("data");
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, "mood_links.txt");
    }

    private void saveToFile() {
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(dataFile()), "UTF-8"))) {
            // format: mood|title|url  (escape '|' in title)
            for (Map.Entry<String, List<LinkItem>> e : moodData.entrySet()) {
                String mood = e.getKey();
                if ("mixed".equals(mood)) continue; // derived
                for (LinkItem li : e.getValue()) {
                    String cleanTitle = li.title.replace("|", "\\|");
                    pw.println(mood + "|" + cleanTitle + "|" + li.url);
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(frame, "Could not save: " + ex.getMessage());
        }
    }

    private void loadFromFile() {
        File f = dataFile();
        if (!f.exists()) return;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                new FileInputStream(f), "UTF-8"))) {

            // clear current editable lists
            List<LinkItem> h = moodData.get("happy");
            List<LinkItem> c = moodData.get("chill");
            List<LinkItem> en = moodData.get("energetic");
            if (h != null) h.clear();
            if (c != null) c.clear();
            if (en != null) en.clear();

            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\\|", 3);
                if (parts.length < 3) continue;
                String mood  = parts[0];
                String title = parts[1].replace("\\|", "|");
                String url   = parts[2];
                List<LinkItem> list = moodData.get(mood);
                if (list != null) list.add(new LinkItem(title, url));
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(frame, "Could not load: " + ex.getMessage());
        }
    }

    // -------- Seed empty lists --------
    private void seedData() {
        moodData.put("happy",     new ArrayList<>());
        moodData.put("chill",     new ArrayList<>());
        moodData.put("energetic", new ArrayList<>());
        moodData.put("mixed",     new ArrayList<>()); // built dynamically
    }
}


