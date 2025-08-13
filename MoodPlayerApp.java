import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.net.URI;
import java.util.*;
import java.util.List;

public class MoodPlayerApp {

    // Simple item to hold a title + URL
    static class LinkItem {
        final String title;
        final String url;
        LinkItem(String title, String url) { this.title = title; this.url = url; }
        @Override public String toString() { return title; } // shows nicely in the list
    }

    private JFrame frame;
    private JButton happyButton, chillButton, energeticButton, mixedButton, openButton;
    private JList<LinkItem> playlistList;
    private DefaultListModel<LinkItem> listModel;
    private JLabel nowPlaying;  
    private JPanel top;

    // Mood -> list of links
    private final Map<String, List<LinkItem>> moodData = new LinkedHashMap<>();

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MoodPlayerApp app = new MoodPlayerApp();
            app.createAndShowGUI();
        });
    }
private void createAndShowGUI() {
    frame = new JFrame("Mood-based Music Player");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setSize(680, 420);
    frame.setLayout(new BorderLayout(10, 10));

    // ====== TOP: Mood buttons + Now Playing ======
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

    // ====== CENTER: Playlist list ======
    listModel = new DefaultListModel<>();
    playlistList = new JList<>(listModel);
    playlistList.setVisibleRowCount(12);
    playlistList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    JScrollPane scrollPane = new JScrollPane(playlistList);

    // ====== BOTTOM: Open button ======
    JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
    openButton = new JButton("Open Selected");
    bottom.add(openButton);

    // Wire up the layout
    frame.add(top, BorderLayout.NORTH);
    frame.add(scrollPane, BorderLayout.CENTER);
    frame.add(bottom, BorderLayout.SOUTH);

    // Load data
    seedData();

    // Actions
    happyButton.addActionListener(e -> showPlaylist("happy"));
    chillButton.addActionListener(e -> showPlaylist("chill"));
    energeticButton.addActionListener(e -> showPlaylist("energetic"));
    mixedButton.addActionListener(e -> showPlaylist("mixed"));

    // Double-click open
    playlistList.addMouseListener(new MouseAdapter() {
        @Override public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) openSelected();
        }
    });

    openButton.addActionListener(e -> openSelected());

    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
}
private void showPlaylist(String moodKey) {
    listModel.clear();
    List<LinkItem> items = moodData.getOrDefault(moodKey, Collections.emptyList());
    for (LinkItem item : items) listModel.addElement(item);

    // Soft mood colors
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

private void seedData() {
    // ===== TODO: add your songs here (titles + URLs). =====
    // You can paste YouTube, Spotify, Apple Music, etc.

    moodData.put("happy", Arrays.asList(
        new LinkItem("Happy Single 1", "https://www.youtube.com/watch?v=dQw4w9WgXcQ"),
        new LinkItem("Happy Single 2", "https://music.youtube.com/watch?v=xxxxxxxxxxx"),
        new LinkItem("Happy Album Playlist", "https://open.spotify.com/playlist/xxxxxxxxxxx")
    ));

    moodData.put("chill", Arrays.asList(
        new LinkItem("Chill Single 1", "https://www.youtube.com/watch?v=yyyyyyyyyyy"),
        new LinkItem("Lo-fi Mix", "https://www.youtube.com/watch?v=5qap5aO4i9A"),
        new LinkItem("Chill Album", "https://open.spotify.com/album/xxxxxxxxxxx")
    ));

    moodData.put("energetic", Arrays.asList(
        new LinkItem("Energetic Single 1", "https://www.youtube.com/watch?v=zzzzzzzzzzz"),
        new LinkItem("Workout Mix", "https://open.spotify.com/playlist/xxxxxxxxxxx"),
        new LinkItem("High-Energy Album", "https://music.apple.com/ca/album/ID_HERE")
    ));

    // MIXED: combine a bit of everything (you can customize)
    List<LinkItem> mixed = new ArrayList<>();
    mixed.addAll(moodData.get("happy"));
    mixed.addAll(moodData.get("chill"));
    mixed.addAll(moodData.get("energetic"));
    moodData.put("mixed", mixed);
}
}

