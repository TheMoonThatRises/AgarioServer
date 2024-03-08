package ceccs.utils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class Configurations {

    final public static Configurations shared;

    final private InternalPathFinder fileHandler;
    final private Properties properties;

    static {
        try {
            shared = new Configurations();
        } catch (IOException exception) {
            System.err.println("failed to get shared configuration file");

            throw new RuntimeException(exception);
        }
    }

    public Configurations() throws IOException {
        this.fileHandler = new InternalPathFinder("configs.cfg");
        this.properties = new Properties();

        try {
            this.readConfig();
        } catch (FileNotFoundException exception) {
            this.writeConfig();
        }
    }

    protected void readConfig() throws IOException {
        FileInputStream inputStream = fileHandler.getInputStream();

        properties.load(inputStream);

        inputStream.close();
    }

    protected void writeConfig() throws IOException {
        FileOutputStream outputStream = fileHandler.getOutputStream();

        properties.store(outputStream, null);

        outputStream.close();
    }

    public void setProperty(String key, String value) {
        properties.setProperty(key, value);

        try {
            writeConfig();
        } catch (IOException exception) {
            exception.printStackTrace();

            System.err.println("unable to save configuration file");
        }
    }

    public String getProperty(String key) {
        return properties.getProperty(key, "");
    }

}
