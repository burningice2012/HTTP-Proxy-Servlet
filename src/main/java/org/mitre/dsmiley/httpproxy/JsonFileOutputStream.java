package org.mitre.dsmiley.httpproxy;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.zip.GZIPInputStream;

/**
 * @author burningice
 */
public class JsonFileOutputStream  extends ByteArrayOutputStream {
    private static final byte[] GZIP_HEADER = new byte[] {(byte)0x1f, (byte)0x8b};
    private JsonDataFilter filter;
    private final File file;

    public JsonFileOutputStream(File file) {
        this.file = file;
    }

    public JsonFileOutputStream(File file, JsonDataFilter filter) {
        this(file);
        this.filter = filter;
    }

    @Override
    public void close() throws IOException {
        super.close();

        // flush finally
        boolean accept = true;
        if (filter != null) {
            byte[] data = this.toByteArray();
            InputStream in = new ByteArrayInputStream(data);
            if (isGzip(data)) {
                in = new GZIPInputStream(in);
            }

            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                JsonObject json = (JsonObject) JsonParser.parseReader(reader);
                accept = filter.accept(json);
            }
        }

        if (accept) {
            Files.write(file.toPath(), this.toByteArray(), StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
    }

    private boolean isGzip(byte[] data) {
        return data.length >= GZIP_HEADER.length && java.util.Arrays.equals(data, 0, GZIP_HEADER.length, GZIP_HEADER, 0, GZIP_HEADER.length);
    }

    public interface JsonDataFilter {
        boolean accept(JsonObject json);
    }
}
