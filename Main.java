package blocked;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import javax.swing.SwingUtilities;

import blocked.ui.GameFrame;

public final class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
    private static final Path ERROR_LOG = Paths.get("logs", "error.log");

    private Main() {
    }

    public static void main(String[] args) {
        configureErrorLogging();
        Thread.setDefaultUncaughtExceptionHandler(
                (thread, exception) -> LOGGER.log(Level.SEVERE,
                        "Uncaught exception on thread " + thread.getName(), exception));

        SwingUtilities.invokeLater(() -> {
            try {
                GameFrame gameFrame = new GameFrame();
                gameFrame.setVisible(true);
            } catch (RuntimeException exception) {
                LOGGER.log(Level.SEVERE, "Application startup failed on the Event Dispatch Thread",
                        exception);
                throw exception;
            }
        });
    }

    private static void configureErrorLogging() {
        try {
            Files.createDirectories(ERROR_LOG.getParent());
            FileHandler fileHandler = new FileHandler(ERROR_LOG.toString(), true);
            fileHandler.setFormatter(new SimpleFormatter());
            LOGGER.addHandler(fileHandler);
            LOGGER.setUseParentHandlers(true);
        } catch (IOException | SecurityException exception) {
            System.err.println("Unable to initialize error logging at " + ERROR_LOG);
            exception.printStackTrace(System.err);
        }
    }
}
