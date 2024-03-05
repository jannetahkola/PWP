package fi.jannetahkola.palikka.game.service;

import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;

@Service
public class FileDownloaderService {
    public void download(URI downloadUri, File toFile) throws IOException {
        try (ReadableByteChannel readableByteChannel = Channels.newChannel(downloadUri.toURL().openStream());
             FileOutputStream fileOutputStream = new FileOutputStream(toFile)) {
            FileChannel fileChannel = fileOutputStream.getChannel();
            fileChannel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
        }
    }
}
