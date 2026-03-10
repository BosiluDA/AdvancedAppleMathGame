package applegame;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        SwingUtilities.invokeLater(() -> {
            DataBaseManager db = new DataBaseManager();
            GameState state    = new GameState();
            MainWindow window  = new MainWindow(state, db);
            window.setVisible(true);
        });
    }
}
