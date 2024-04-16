package ceccs.utils;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;

public class InternalPathFinder {

    final private static Path baseDirectory;

    static {
        baseDirectory = Paths.get(System.getProperty("user.home"), ".ceccs_agario", "server");

        baseDirectory.toFile().mkdirs();
    }

    final private Path path;

    public InternalPathFinder(String fileName, boolean overwrite) throws IOException {
        this.path = Paths.get(baseDirectory.toString(), fileName);

        this.path.toFile().getParentFile().mkdirs();

        if (!this.path.toFile().createNewFile() && overwrite) {
            this.path.toFile().delete();
            this.path.toFile().createNewFile();
        }
    }

    public InternalPathFinder(boolean overwrite, String... paths) throws IOException {
        this.path = Paths.get(baseDirectory.toString(), paths);

        this.path.toFile().getParentFile().mkdirs();

        if (!this.path.toFile().createNewFile() && overwrite) {
            this.path.toFile().delete();
            this.path.toFile().createNewFile();
        }
    }

    public Path getPath() {
        return path;
    }

    public FileInputStream getInputStream() throws FileNotFoundException {
        return new FileInputStream(path.toFile());
    }

    public FileOutputStream getOutputStream() throws FileNotFoundException {
        return new FileOutputStream(path.toFile());
    }

    public void writeToFile(String data, boolean append) throws IOException {
        FileWriter fileWriter = new FileWriter(path.toFile(), append);

        try {
            fileWriter.write(data);

            fileWriter.flush();
        } finally {
            fileWriter.close();
        }
    }

}
