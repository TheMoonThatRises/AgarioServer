package ceccs.utils;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;

public class InternalPathFinder {

    final private static Path baseDirectory;
    final private Path path;

    static {
        baseDirectory = Paths.get(System.getProperty("user.home"), ".ceccs_agario", "server");

        baseDirectory.toFile().mkdirs();
    }

    public InternalPathFinder(String fileName) throws IOException {
        this.path = Paths.get(baseDirectory.toString(), fileName);

        this.path.toFile().getParentFile().mkdirs();

        if (!this.path.toFile().createNewFile()) {
            this.path.toFile().delete();
            this.path.toFile().createNewFile();
        }
    }

    public InternalPathFinder(String... paths) throws IOException {
        this.path = Paths.get(baseDirectory.toString(), paths);

        this.path.toFile().getParentFile().mkdirs();

        if (!this.path.toFile().createNewFile()) {
            this.path.toFile().delete();
            this.path.toFile().createNewFile();
        }
    }

    public Path getPath() {
        return path;
    }

    public FileInputStream getInputStream() throws FileNotFoundException {
        return new FileInputStream(this.path.toFile());
    }

    public FileOutputStream getOutputStream() throws FileNotFoundException {
        return new FileOutputStream(this.path.toFile());
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
