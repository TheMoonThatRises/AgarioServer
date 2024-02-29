package ceccs.network.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class GZip {

    public static byte[] compress(byte[] data) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);
        GZIPOutputStream gzipOutputStream = new GZIPOutputStream(outputStream, data.length);

        gzipOutputStream.write(data);
        gzipOutputStream.close();

        return outputStream.toByteArray();
    }

    public static byte[] decompress(byte[] data) throws IOException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
        GZIPInputStream gzipInputStream = new GZIPInputStream(inputStream, data.length);

        return gzipInputStream.readAllBytes();
    }

}
