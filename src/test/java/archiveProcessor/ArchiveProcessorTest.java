package archiveProcessor;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;

class ArchiveProcessorTest {
    private static final String TEST_ZIP = "test.zip";
    private static final String TEST_RAR = "test.rar";
    private static final String TEMP_DIR = "tempDir";
    private static final Properties config = new Properties();
    private static ArchiveProcessor ap = null;

    @BeforeAll
    static void setup() throws IOException {
        config.setProperty("ip", "192.168.0.1");
        config.setProperty("computerName", "NewComputerName");
        config.setProperty("userName", "NewUserName");
        ap = new ArchiveProcessor(config);

        // Создание тестового архива ZIP
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(TEST_ZIP))) {
            addToZipFile("test1.txt", "IP: 123.456.789.0\nComputer: OldComputer\nUser: OldUser", zos);
            addToZipFile("test2.txt", "User: AnotherUser\nIP: 192.168.0.2\nComputer: AnotherComputer", zos);
        }

        // Создание тестового архива RAR (используйте junrar или другой инструмент для создания тестового RAR)
        // Для упрощения тут только zip пример
    }

    @AfterAll
    static void cleanup() throws IOException {
        Files.deleteIfExists(Paths.get(TEST_ZIP));
        Files.deleteIfExists(Paths.get(TEST_RAR));
        deleteDirectory(new File(TEMP_DIR));
    }

    @Test
    void testUnzip() throws ZipException, IOException {
        ap.unzip(TEST_ZIP, TEMP_DIR);
        assertTrue(Files.exists(Paths.get(TEMP_DIR, "test1.txt")));
        assertTrue(Files.exists(Paths.get(TEMP_DIR, "test2.txt")));
    }

    @Test
    void testReplaceContent() throws IOException {
        String content = "IP: 123.456.789.0\nComputer: OldComputer\nUser: OldUser";
        String expected = "IP: 192.168.0.1\nComputer: NewComputerName\nUser: NewUserName";
        assertEquals(expected, ap.replaceContent(content));
    }

    @Test
    void testProcessFiles() throws IOException, ZipException {
        ap.unzip(TEST_ZIP, TEMP_DIR);
        ap.processFiles(TEMP_DIR);
        String content1 = new String(Files.readAllBytes(Paths.get(TEMP_DIR, "test1.txt")));
        String content2 = new String(Files.readAllBytes(Paths.get(TEMP_DIR, "test2.txt")));
        assertTrue(content1.contains("192.168.0.1"));
        assertTrue(content1.contains("NewComputerName"));
        assertTrue(content1.contains("NewUserName"));
        assertTrue(content2.contains("192.168.0.1"));
        assertTrue(content2.contains("NewComputerName"));
        assertTrue(content2.contains("NewUserName"));
    }

    private static void addToZipFile(String fileName, String content, ZipOutputStream zos) throws IOException {
        ZipEntry zipEntry = new ZipEntry(fileName);
        zos.putNextEntry(zipEntry);
        zos.write(content.getBytes());
        zos.closeEntry();
    }

    private static void deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        directoryToBeDeleted.delete();
    }
}