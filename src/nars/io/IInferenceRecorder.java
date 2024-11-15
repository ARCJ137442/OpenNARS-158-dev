package nars.io;

public interface IInferenceRecorder {

    /**
     * Initialize the window and the file
     */
    public abstract void init();

    /**
     * Show the window
     */
    public abstract void show();

    /**
     * Begin the display
     */
    public abstract void play();

    /**
     * Stop the display
     */
    public abstract void stop();

    /**
     * Add new text to display
     *
     * @param s The line to be displayed
     */
    public abstract void append(String s);

    /**
     * Open the log file
     */
    public abstract void openLogFile();

    /**
     * Close the log file
     */
    public abstract void closeLogFile();

    /**
     * Check file logging
     *
     * @return If the file logging is going on
     */
    public abstract boolean isLogging();

    /**
     * 空的「推理记录器」
     * * 📌【2024-06-26 02:28:15】从「推理器」中搬迁过来
     */
    public static final class NullInferenceRecorder implements IInferenceRecorder {

        @Override
        public void init() {
        }

        @Override
        public void show() {
        }

        @Override
        public void play() {
        }

        @Override
        public void stop() {
        }

        @Override
        public void append(String s) {
        }

        @Override
        public void openLogFile() {
        }

        @Override
        public void closeLogFile() {
        }

        @Override
        public boolean isLogging() {
            return false;
        }
    }
}