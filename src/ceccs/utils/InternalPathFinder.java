package ceccs.utils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class InternalPathFinder {

    final private static Path baseDirectory;
    final private Path path;

    static {
        baseDirectory = Paths.get(System.getProperty("user.home"), ".ceccs_agario_server");

        baseDirectory.toFile().mkdirs();
    }

    public InternalPathFinder(String fileName) throws IOException {
        this.path = Paths.get(baseDirectory.toString(), fileName);

        if (!this.path.toFile().exists()) {
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

}
