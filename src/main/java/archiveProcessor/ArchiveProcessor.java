package archiveProcessor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.regex.*;
import java.util.zip.*;

import com.github.junrar.exception.RarException;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import com.github.junrar.Archive;
import com.github.junrar.rarfile.FileHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArchiveProcessor extends JFrame {

    private static final Logger logger = LoggerFactory.getLogger(ArchiveProcessor.class);
    private JTextField ipField;
    private JTextField computerNameField;
    private JTextField userNameField;
    private JTextField archivePathField;
    private JProgressBar progressBar;
    private Properties config;

    public ArchiveProcessor() {
        setTitle("Archive Processor");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        initUI();
    }

    private void initUI() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(6, 2));

        JLabel ipLabel = new JLabel("IP Address:");
        ipField = new JTextField();
        panel.add(ipLabel);
        panel.add(ipField);

        JLabel computerNameLabel = new JLabel("Computer Name:");
        computerNameField = new JTextField();
        panel.add(computerNameLabel);
        panel.add(computerNameField);

        JLabel userNameLabel = new JLabel("User Name:");
        userNameField = new JTextField();
        panel.add(userNameLabel);
        panel.add(userNameField);

        JLabel archivePathLabel = new JLabel("Archive Path:");
        archivePathField = new JTextField();
        panel.add(archivePathLabel);
        panel.add(archivePathField);

        JButton browseButton = new JButton("Browse");
        browseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    archivePathField.setText(fileChooser.getSelectedFile().getAbsolutePath());
                }
            }
        });
        panel.add(browseButton);

        JButton processButton = new JButton("Process");
        processButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new Thread(() -> {
                    try {
                        processArchive();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }).start();
            }
        });
        panel.add(processButton);

        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        panel.add(new JLabel("Progress:"));
        panel.add(progressBar);

        add(panel);
    }

    private void processArchive() throws IOException, ZipException, RarException {
        config = new Properties();
        config.setProperty("ip", ipField.getText());
        config.setProperty("computerName", computerNameField.getText());
        config.setProperty("userName", userNameField.getText());

        String archivePath = archivePathField.getText();
        String tempDir = "tempDir";

        logger.info("Starting processing archive: " + archivePath);
        if (archivePath.endsWith(".zip")) {
            unzip(archivePath, tempDir);
        } else if (archivePath.endsWith(".rar")) {
            unrar(archivePath, tempDir);
        }

        processFiles(tempDir);
        zip(tempDir, "new_" + new File(archivePath).getName());

        JOptionPane.showMessageDialog(this, "Processing Complete!");
        logger.info("Processing complete for archive: " + archivePath);
    }

    private void unzip(String archivePath, String destDir) throws ZipException {
        ZipFile zipFile = new ZipFile(archivePath);
        zipFile.extractAll(destDir);
        logger.info("Unzipped archive: " + archivePath + " to directory: " + destDir);
    }

    private void unrar(String archivePath, String destDir) throws IOException, RarException {
        Archive archive = new Archive(new File(archivePath));
        FileHeader fileHeader;
        while ((fileHeader = archive.nextFileHeader()) != null) {
            File file = new File(destDir, fileHeader.getFileNameString().trim());
            if (fileHeader.isDirectory()) {
                file.mkdirs();
            } else {
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    archive.extractFile(fileHeader, fos);
                }
            }
        }
        logger.info("Unrarred archive: " + archivePath + " to directory: " + destDir);
    }

    private void zip(String sourceDirPath, String zipFilePath) throws IOException {
        Path zipFile = Files.createFile(Paths.get(zipFilePath));
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(zipFile))) {
            Path sourceDir = Paths.get(sourceDirPath);
            Files.walk(sourceDir).filter(path -> !Files.isDirectory(path)).forEach(path -> {
                ZipEntry zipEntry = new ZipEntry(sourceDir.relativize(path).toString());
                try {
                    zipOutputStream.putNextEntry(zipEntry);
                    Files.copy(path, zipOutputStream);
                    zipOutputStream.closeEntry();
                } catch (IOException e) {
                    logger.error("Error zipping file: " + path.toString(), e);
                }
            });
        }
        logger.info("Zipped directory: " + sourceDirPath + " to archive: " + zipFilePath);
    }

    private void processFiles(String dir) throws IOException {
        List<Path> fileList = new ArrayList<>();
        Files.walk(Paths.get(dir)).filter(Files::isRegularFile).forEach(fileList::add);

        int totalFiles = fileList.size();
        for (int i = 0; i < totalFiles; i++) {
            Path path = fileList.get(i);
            String content = new String(Files.readAllBytes(path));
            String newContent = replaceContent(content);
            Files.write(path, newContent.getBytes());
            progressBar.setValue((int) (((i + 1) / (double) totalFiles) * 100));

            logger.info("Processed file: " + path.toString() + " replaced content with: " + newContent);
        }
    }

    private String replaceContent(String content) {
        String ipPattern = "\\b(?:[0-9]{1,3}\\.){3}[0-9]{1,3}\\b";
        String computerNamePattern = "\\b[A-Za-z0-9_-]{1,15}\\b";
        String userNamePattern = "\\b[A-Za-z0-9_-]{1,15}\\b";

        String newContent = content.replaceAll(ipPattern, config.getProperty("ip"))
                .replaceAll(computerNamePattern, config.getProperty("computerName"))
                .replaceAll(userNamePattern, config.getProperty("userName"));

        logger.debug("Original content: " + content);
        logger.debug("New content: " + newContent);

        return newContent;
    }

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            ArchiveProcessor ex = new ArchiveProcessor();
            ex.setVisible(true);
        });
    }
}
