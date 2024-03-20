package fi.jannetahkola.palikka.game.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class GameFileProcessor {
    private static final String EULA_FILE_NAME = "eula.txt";

    public void downloadFile(URL downloadUri, File toFile) throws IOException {
        try (ReadableByteChannel readableByteChannel = Channels.newChannel(downloadUri.openStream());
             FileOutputStream fileOutputStream = new FileOutputStream(toFile)) {
            FileChannel fileChannel = fileOutputStream.getChannel();
            log.info("Download size = {} bytes", fileChannel.size());
            fileChannel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
        }
    }

    public void acceptEula(File toFile) {
        log.info("Attempting to accept EULA...");
        File eulaFile = Paths.get(toFile.getParentFile().getPath(), EULA_FILE_NAME).toFile();
        try {
            if (eulaFile.exists()) {
                log.info("'{}' already exists", EULA_FILE_NAME);
            } else if (eulaFile.createNewFile()) {
                log.info("'{}' created", EULA_FILE_NAME);
            } else {
                log.error("Failed to accept EULA - file creation failed");
            }
            try (FileWriter fileWriter = new FileWriter(eulaFile.getPath())) {
                fileWriter.write("eula=true");
                log.info("EULA accepted successfully");
            }
        } catch (IOException e) {
            log.error("Failed to write '{}'", EULA_FILE_NAME, e);
        }
    }

    public List<String> readFile(String pathToDir, String fileName) throws IOException {
        File targetFile = Paths.get(pathToDir, fileName).toFile();
        if (!targetFile.exists()) {
            throw new IOException(String.format("File '%s' not found", targetFile));
        }
        List<String> configLines = new ArrayList<>();
        try (var br = new BufferedReader(new InputStreamReader(new FileInputStream(targetFile)))) {
            String line;
            while ((line = br.readLine()) != null) {
                configLines.add(line);
            }
        } catch (IOException e) {
            throw new IOException(
                    String.format("Failed to read file '%s'", targetFile), e);
        }
        return configLines;
    }
}
