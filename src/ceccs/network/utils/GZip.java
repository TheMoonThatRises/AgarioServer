package ceccs.network.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class GZip {

    public static byte[] compress(byte[] data) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);
        GZIPOutputStream gzipOutputStream = new GZIPOutputStream(outputStream, 65534);

        gzipOutputStream.write(data);
        gzipOutputStream.close();

        return outputStream.toByteArray();
    }

    public static byte[] decompress(byte[] data) throws IOException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
        GZIPInputStream gzipInputStream = new GZIPInputStream(inputStream, 65534);

        return gzipInputStream.readAllBytes();
    }

}
