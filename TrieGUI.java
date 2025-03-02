import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.List;

public class TrieGUI extends JFrame {
    private Trie trie;
    private JTextField searchField;
    private JTextArea resultsArea;
    private JButton addWordButton, clearButton, deleteWordButton, saveButton, loadButton, exportHistoryButton, deleteHistoryButton, clearHistoryButton;
    private JList<String> searchHistoryList;
    private DefaultListModel<String> searchHistoryModel;
    private JLabel welcomeLabel;
    private String userName;
    private Connection dbConnection;

    public TrieGUI() {
        super("Hindi Trie GUI");

        // Initialize SQLite connection
        initDatabase();

        // Prompt user for their name and fetch their history from the database
        welcomeUser();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLayout(new BorderLayout());

        // Load words from file and initialize Trie
        List<String> words = loadWordsFromFile("hindi.txt");
        trie = new Trie(words);

        // Create and add components
        searchField = new JTextField();
        searchField.setFont(new Font("Mangal", Font.PLAIN, 16));
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY, 1),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        resultsArea = new JTextArea();
        resultsArea.setEditable(false);
        resultsArea.setFont(new Font("Mangal", Font.PLAIN, 14));
        resultsArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        addWordButton = new JButton("नया शब्द जोड़ें।");
        addWordButton.setToolTipText("ट्राई में एक नया शब्द जोड़ें");


        clearButton = new JButton("साफ करें");
        clearButton.setToolTipText("खोज क्षेत्र साफ करें");

        deleteWordButton = new JButton("शब्द हटाएँ");
        deleteWordButton.setToolTipText("एक शब्द हटाएं");

        saveButton = new JButton("शब्द सेव करें");
        saveButton.setToolTipText("वर्तमान शब्द को एक फ़ाइल में सहेजें");

        loadButton = new JButton("शब्द लोड करें");
        loadButton.setToolTipText("एक फ़ाइल से शब्द लोड करें");

        exportHistoryButton = new JButton("खोज इतिहास निर्यात करें");
        exportHistoryButton.setToolTipText("खोज इतिहास को .txt फ़ाइल में निर्यात करें");

        deleteHistoryButton = new JButton("इतिहास हटाएं");
        deleteHistoryButton.setToolTipText("खोज इतिहास से चयनित आइटम हटाएं");

        clearHistoryButton = new JButton("सभी इतिहास साफ़ करें");
        clearHistoryButton.setToolTipText("सभी खोज इतिहास को साफ़ करें");


        // Search history functionality
        searchHistoryModel = new DefaultListModel<>();
        searchHistoryList = new JList<>(searchHistoryModel);
        searchHistoryList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        searchHistoryList.setVisibleRowCount(5);
        searchHistoryList.setFont(new Font("Mangal", Font.PLAIN, 14));

        JPanel searchHistoryPanel = new JPanel();
        searchHistoryPanel.setLayout(new BorderLayout());
        searchHistoryPanel.add(new JLabel("खोज इतिहास:"), BorderLayout.NORTH);
        searchHistoryPanel.add(new JScrollPane(searchHistoryList), BorderLayout.CENTER);
        searchHistoryPanel.setPreferredSize(new Dimension(150, getHeight()));

        welcomeLabel = new JLabel("स्वागत है, " + userName + "!", JLabel.CENTER);
        welcomeLabel.setFont(new Font("Mangal", Font.BOLD, 16));
        welcomeLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        topPanel.add(welcomeLabel, BorderLayout.NORTH);
        topPanel.add(new JLabel("खोजें:"), BorderLayout.WEST);
        topPanel.add(searchField, BorderLayout.CENTER);
        topPanel.add(addWordButton, BorderLayout.EAST);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        bottomPanel.add(deleteWordButton);
        bottomPanel.add(clearButton);
        bottomPanel.add(saveButton);
        bottomPanel.add(loadButton);
        bottomPanel.add(exportHistoryButton);
        bottomPanel.add(deleteHistoryButton);
        bottomPanel.add(clearHistoryButton);

        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(resultsArea), BorderLayout.CENTER);
        add(searchHistoryPanel, BorderLayout.WEST);
        add(bottomPanel, BorderLayout.SOUTH);

        // Add listeners
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) { updateResults(); }
            public void removeUpdate(DocumentEvent e) { updateResults(); }
            public void insertUpdate(DocumentEvent e) { updateResults(); }
        });
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(new JScrollPane(resultsArea), BorderLayout.CENTER);

        // Add Hindi keyboard
        JPanel keyboardPanel = createHindiKeyboard();
        mainPanel.add(keyboardPanel, BorderLayout.SOUTH);

        addWordButton.addActionListener(e -> addWord());
        deleteWordButton.addActionListener(e -> deleteWord());
        clearButton.addActionListener(e -> clearSearch());
        saveButton.addActionListener(e -> saveTrie());
        loadButton.addActionListener(e -> loadTrie());
        exportHistoryButton.addActionListener(e -> exportSearchHistory());
        deleteHistoryButton.addActionListener(e -> deleteHistoryItem()); // Add listener for delete history button
        clearHistoryButton.addActionListener(e -> clearAllHistory()); // Add listener for clear history button
        searchHistoryList.addListSelectionListener(e -> loadSearchFromHistory());
        searchField.addActionListener(e -> saveSearchToHistory());

        add(mainPanel, BorderLayout.CENTER);
        setVisible(true);
    }




    private void initDatabase() {
        String dbPath = "C:\\Users\\deepa\\IdeaProjects\\Wordhunt_data\\game.db";
        String url = "jdbc:sqlite:" + dbPath;

        try {
            dbConnection = DriverManager.getConnection(url);
            Statement stmt = dbConnection.createStatement();

            // Create table for storing user data and history if it doesn't exist
            String createTableSQL = """
                CREATE TABLE IF NOT EXISTS user_history (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT NOT NULL,
                    search_term TEXT NOT NULL
                );
            """;
            stmt.execute(createTableSQL);
            stmt.close();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "डेटाबेस कनेक्ट करने में त्रुटि: " + e.getMessage());
            System.exit(1);
        }
    }

    private void welcomeUser() {
        userName = JOptionPane.showInputDialog(this, "कृपया अपना नाम दर्ज करें:", "उपयोगकर्ता नाम");
        if (userName == null || userName.trim().isEmpty()) {
            userName = "अतिथि";
        }
    }

    private JPanel createHindiKeyboard() {
        JPanel keyboardPanel = new JPanel(new GridLayout(4, 12, 5, 5));
        String[] hindiChars = {
                "क", "ख", "ग", "घ", "ङ", "च", "छ", "ज", "झ", "ञ", "ट", "ठ",
                "ड", "ढ", "ण", "त", "थ", "द", "ध", "न", "प", "फ", "ब", "भ",
                "म", "य", "र", "ल", "व", "श", "ष", "स", "ह", "़", ".", "ा",
                "ि", "ी", "ु", "ू", "े", "ै", "ो", "ौ", "ं", "ः", "ँ", "्",
                "०", "१", "२", "३", "४", "५", "६", "७", "८", "९", " ", "←"
        };

        for (String ch : hindiChars) {
            JButton button = new JButton(ch);
            button.setFont(new Font("Mangal", Font.PLAIN, 16));
            button.addActionListener(e -> {
                if (ch.equals("←")) {
                    String text = searchField.getText();
                    if (!text.isEmpty()) {
                        searchField.setText(text.substring(0, text.length() - 1));
                    }
                } else {
                    searchField.setText(searchField.getText() + ch);
                }
            });
            keyboardPanel.add(button);
        }

        return keyboardPanel;
    }

    private void loadUserHistory() {
        try {
            String query = "SELECT search_term FROM user_history WHERE username = ?";
            PreparedStatement pstmt = dbConnection.prepareStatement(query);
            pstmt.setString(1, userName);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                searchHistoryModel.addElement(rs.getString("search_term"));
            }

            rs.close();
            pstmt.close();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "खोज इतिहास लोड करने में त्रुटि: " + e.getMessage());
        }
    }

    private void saveSearchToHistory() {
        String searchTerm = searchField.getText();
        if (!searchTerm.isEmpty() && !searchHistoryModel.contains(searchTerm)) {
            searchHistoryModel.addElement(searchTerm);

            try {
                String insertSQL = "INSERT INTO user_history (username, search_term) VALUES (?, ?)";
                PreparedStatement pstmt = dbConnection.prepareStatement(insertSQL);
                pstmt.setString(1, userName);
                pstmt.setString(2, searchTerm);
                pstmt.executeUpdate();
                pstmt.close();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "इतिहास सहेजने में त्रुटि: " + e.getMessage());
            }
        }
    }

    // Close the database connection when the application ends
    private void closeDatabaseConnection() {
        try {
            if (dbConnection != null && !dbConnection.isClosed()) {
                dbConnection.close();  // Close the connection
                System.out.println("Database connection closed.");
            }
        } catch (SQLException e) {
            System.out.println("Error closing database connection: " + e.getMessage());
        }
    }

    private void deleteHistoryItem() {
        String selectedTerm = searchHistoryList.getSelectedValue();
        if (selectedTerm != null) {
            searchHistoryModel.removeElement(selectedTerm);

            try {
                String deleteSQL = "DELETE FROM user_history WHERE username = ? AND search_term = ?";
                PreparedStatement pstmt = dbConnection.prepareStatement(deleteSQL);
                pstmt.setString(1, userName);
                pstmt.setString(2, selectedTerm);
                pstmt.executeUpdate();
                pstmt.close();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "इतिहास हटाने में त्रुटि: " + e.getMessage());
            }
        } else {
            JOptionPane.showMessageDialog(this, "कृपया एक इतिहास आइटम का चयन करें।");
        }
    }

    private void clearAllHistory() {
        searchHistoryModel.clear();

        try {
            String deleteSQL = "DELETE FROM user_history WHERE username = ?";
            PreparedStatement pstmt = dbConnection.prepareStatement(deleteSQL);
            pstmt.setString(1, userName);
            pstmt.executeUpdate();
            pstmt.close();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "सभी इतिहास हटाने में त्रुटि: " + e.getMessage());
        }
    }

    private List<String> loadWordsFromFile(String filePath) {
        List<String> words = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                words.add(line.trim());
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "शब्द लोड करने में त्रुटि: " + e.getMessage());
        }
        return words;
    }

    private void updateResults() {
        String searchTerm = searchField.getText();
        List<String> results = trie.suggest(searchTerm);

        results.sort((a, b) -> Integer.compare(trie.getFrequency(b), trie.getFrequency(a)));

        StringBuilder display = new StringBuilder();
        for (String result : results) {
            display.append(result).append(" (").append(trie.getFrequency(result)).append(")\n");
        }
        resultsArea.setText(display.toString());

        if (trie.search(searchTerm)) {
            trie.incrementFrequency(searchTerm);
            searchField.setBackground(Color.GREEN);
        } else {
            searchField.setBackground(Color.WHITE);
        }
    }

    private void addWord() {
        String newWord = JOptionPane.showInputDialog(this, "नया शब्द दर्ज करें:");
        if (newWord != null && !newWord.isEmpty()) {
            trie.insert(newWord);
            JOptionPane.showMessageDialog(this, "शब्द सफलतापूर्वक जोड़ा गया!");
            updateResults();
        }
    }

    private void deleteWord() {
        String wordToDelete = JOptionPane.showInputDialog(this, "हटाने के लिए शब्द दर्ज करें:");
        if (wordToDelete != null && trie.search(wordToDelete)) {
            trie.delete(wordToDelete);
            JOptionPane.showMessageDialog(this, "शब्द सफलतापूर्वक हटाया गया!");
            updateResults();
        } else {
            JOptionPane.showMessageDialog(this, "शब्द ट्राई में नहीं मिला।");
        }
    }

    private void clearSearch() {
        searchField.setText("");
        searchField.setBackground(Color.WHITE);
        resultsArea.setText("");
    }

    private void saveTrie() {
        try {
            trie.saveToFile("trie_data.txt");
            JOptionPane.showMessageDialog(this, "ट्राई सफलतापूर्वक सहेजा गया!");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "ट्राई सहेजने में त्रुटि: " + e.getMessage());
        }
    }

    private void loadTrie() {
        try {
            trie.loadFromFile("trie_data.txt");
            JOptionPane.showMessageDialog(this, "सफलतापूर्वक लोड किया गया!");
            updateResults();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "लोड करने में त्रुटि: " + e.getMessage());
        }
    }



    private void loadSearchFromHistory() {
        String selectedTerm = searchHistoryList.getSelectedValue();
        if (selectedTerm != null) {
            searchField.setText(selectedTerm);
            updateResults();
        }
    }

    private void exportSearchHistory() {
        try {
            File file = new File("search_history.txt");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                for (int i = 0; i < searchHistoryModel.size(); i++) {
                    writer.write(searchHistoryModel.getElementAt(i));
                    writer.newLine();
                }
            }
            JOptionPane.showMessageDialog(this, "खोज इतिहास सफलतापूर्वक निर्यात किया गया: " + file.getAbsolutePath());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "निर्यात करने में त्रुटि: " + e.getMessage());
        }
    }





    public static void main(String[] args) {
        SwingUtilities.invokeLater(TrieGUI::new);
    }
}

class Trie {
    private TrieNode root;

    public Trie(List<String> words) {
        root = new TrieNode();
        for (String word : words) {
            insert(word);
        }
    }

    public void insert(String word) {
        insertRecursive(root, word, 0);
    }

    private void insertRecursive(TrieNode node, String word, int index) {
        if (index == word.length()) {
            node.isWord = true;
            node.frequency++;
            return;
        }
        char ch = word.charAt(index);
        node.children.putIfAbsent(ch, new TrieNode());
        insertRecursive(node.children.get(ch), word, index + 1);
    }

    public boolean search(String word) {
        TrieNode node = getNodeForPrefix(root, word, 0);
        return node != null && node.isWord;
    }

    public void delete(String word) {
        deleteRecursive(root, word, 0);
    }

    private boolean deleteRecursive(TrieNode node, String word, int index) {
        if (index == word.length()) {
            if (!node.isWord) return false;
            node.isWord = false;
            return node.children.isEmpty();
        }
        char ch = word.charAt(index);
        TrieNode nextNode = node.children.get(ch);
        if (nextNode == null) return false;

        boolean shouldDeleteCurrentNode = deleteRecursive(nextNode, word, index + 1);

        if (shouldDeleteCurrentNode) {
            node.children.remove(ch);
            return node.children.isEmpty();
        }
        return false;
    }

    public List<String> suggest(String prefix) {
        TrieNode node = getNodeForPrefix(root, prefix, 0);
        List<String> results = new ArrayList<>();
        collectWords(node, new StringBuilder(prefix), results);
        return results;
    }

    private TrieNode getNodeForPrefix(TrieNode node, String prefix, int index) {
        if (node == null || index == prefix.length()) {
            return node;
        }
        char ch = prefix.charAt(index);
        return getNodeForPrefix(node.children.get(ch), prefix, index + 1);
    }

    private void collectWords(TrieNode node, StringBuilder prefix, List<String> results) {
        if (node == null) {
            return;
        }
        if (node.isWord) {
            results.add(prefix.toString());
        }
        for (Map.Entry<Character, TrieNode> entry : node.children.entrySet()) {
            char ch = entry.getKey();
            prefix.append(ch);
            collectWords(entry.getValue(), prefix, results);
            prefix.deleteCharAt(prefix.length() - 1);
        }
    }

    public int getFrequency(String word) {
        TrieNode node = getNodeForPrefix(root, word, 0);
        return node != null ? node.frequency : 0;
    }

    public void incrementFrequency(String word) {
        TrieNode node = getNodeForPrefix(root, word, 0);
        if (node != null) {
            node.frequency++;
        }
    }

    public void saveToFile(String filePath) throws IOException {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(filePath))) {
            out.writeObject(root);
        }
    }

    public void loadFromFile(String filePath) throws IOException {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(filePath))) {
            root = (TrieNode) in.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException("Error loading Trie: " + e.getMessage());
        }
    }
}

class TrieNode implements Serializable {
    Map<Character, TrieNode> children;
    boolean isWord;
    int frequency;

    public TrieNode() {
        children = new HashMap<>();
        isWord = false;
        frequency = 0;
    }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(TrieGUI::new);
    }
}



