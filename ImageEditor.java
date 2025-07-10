import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.File;
import java.util.Stack;
import javax.imageio.ImageIO;

public class ImageEditor extends JFrame {
    private BufferedImage originalImage;
    private BufferedImage loadedImage; // copie permanentă a imaginii inițiale
    private JLabel imageLabel;
    private JButton loadButton, undoButton, saveButton;
    private JSlider blurSlider;
    private JComboBox<String> rotationComboBox, filterComboBox;
    private Stack<BufferedImage> undoStack = new Stack<>();

    public ImageEditor() {
        setTitle("Image Editor 2025");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        imageLabel = new JLabel("", SwingConstants.CENTER);
        add(new JScrollPane(imageLabel), BorderLayout.CENTER);

        // Bottom Panel
        JPanel bottomPanel = new JPanel(new BorderLayout());
        blurSlider = new JSlider(0, 10, 0);
        blurSlider.setPreferredSize(new Dimension(150, 40));
        blurSlider.setBackground(Color.BLACK);
        blurSlider.setForeground(Color.YELLOW);
        blurSlider.setOpaque(true);
        blurSlider.setPaintTicks(true);
        blurSlider.setPaintLabels(true);
        blurSlider.setMajorTickSpacing(2);
        blurSlider.setEnabled(false);
        blurSlider.addChangeListener(e -> applyEffects());

        JPanel blurPanel = new JPanel(new BorderLayout());
        blurPanel.setBackground(Color.GRAY);
        blurPanel.add(new JLabel("  Blur:"), BorderLayout.WEST);
        blurPanel.add(blurSlider, BorderLayout.CENTER);
        bottomPanel.add(blurPanel, BorderLayout.CENTER);

        add(bottomPanel, BorderLayout.SOUTH);

        // Top Panel
        JPanel topPanel = new JPanel();

        loadButton = new JButton("Load Image");
        loadButton.setBackground(new Color(255, 182, 193));
        loadButton.setOpaque(true);
        loadButton.setBorderPainted(false);
        loadButton.addActionListener(e -> loadImage());

        undoButton = new JButton("Undo");
        undoButton.setEnabled(false);
        undoButton.addActionListener(e -> undo());

        saveButton = new JButton("Save Image");
        saveButton.setEnabled(false);
        saveButton.addActionListener(e -> saveImage());
        saveButton.setBackground(new Color(255, 182, 193));
        saveButton.setOpaque(true);
        saveButton.setBorderPainted(false);

        String[] angles = {"0°", "90°", "180°", "270°"};
        rotationComboBox = new JComboBox<>(angles);
        rotationComboBox.setEnabled(false);
        rotationComboBox.addActionListener(e -> applyEffects());
        rotationComboBox.setBackground(new Color(255, 182, 193));
        rotationComboBox.setOpaque(true);
        rotationComboBox.setForeground(Color.BLACK);

        String[] filters = {"None", "Grayscale", "Sepia", "Negative", "High Contrast"};
        filterComboBox = new JComboBox<>(filters);
        filterComboBox.setEnabled(false);
        filterComboBox.addActionListener(e -> applyEffects());
        filterComboBox.setBackground(new Color(255, 182, 193));
        filterComboBox.setOpaque(true);
        filterComboBox.setForeground(Color.BLACK);

        topPanel.add(loadButton);
        topPanel.add(undoButton);
        topPanel.add(saveButton);
        topPanel.add(new JLabel("Rotate:"));
        topPanel.add(rotationComboBox);
        topPanel.add(new JLabel("Filter:"));
        topPanel.add(filterComboBox);

        add(topPanel, BorderLayout.NORTH);

        setSize(900, 700);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void loadImage() {
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                originalImage = ImageIO.read(fileChooser.getSelectedFile());
                loadedImage = deepCopy(originalImage); // păstrăm copia de bază
                undoStack.clear();
                showImage(originalImage);
                updateUIState(true);
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Failed to load image.");
            }
        }
    }

    private void updateUIState(boolean enabled) {
        blurSlider.setEnabled(enabled);
        rotationComboBox.setEnabled(enabled);
        filterComboBox.setEnabled(enabled);
        undoButton.setEnabled(enabled);
        saveButton.setEnabled(enabled);
    }

    private void showImage(BufferedImage img) {
        imageLabel.setIcon(new ImageIcon(img));
    }

    private void saveStateForUndo() {
        if (originalImage != null) {
            undoStack.push(deepCopy(originalImage));
        }
    }

    private void undo() {
        if (!undoStack.isEmpty()) {
            originalImage = undoStack.pop();
            loadedImage = deepCopy(originalImage); // refacem imaginea de bază
            showImage(originalImage);
        }
    }

    private void applyEffects() {
        if (loadedImage == null) return;

        saveStateForUndo();

        BufferedImage result = deepCopy(loadedImage);

        int blurValue = blurSlider.getValue();
        int angle = rotationComboBox.getSelectedIndex() * 90;
        String filter = (String) filterComboBox.getSelectedItem();

        if (blurValue > 0) result = applyBlur(result, blurValue);
        if (angle != 0) result = rotateImage(result, angle);

        switch (filter) {
            case "Grayscale" -> result = applyGrayscale(result);
            case "Sepia" -> result = applySepia(result);
            case "Negative" -> result = applyNegative(result);
            case "High Contrast" -> result = applyContrast(result);
        }

        originalImage = result;
        showImage(result);
    }

    private BufferedImage applyBlur(BufferedImage img, int radius) {
        BufferedImage result = new BufferedImage(img.getWidth(), img.getHeight(), img.getType());
        for (int y = radius; y < img.getHeight() - radius; y++) {
            for (int x = radius; x < img.getWidth() - radius; x++) {
                int r = 0, g = 0, b = 0, count = 0;
                for (int dy = -radius; dy <= radius; dy++) {
                    for (int dx = -radius; dx <= radius; dx++) {
                        Color c = new Color(img.getRGB(x + dx, y + dy));
                        r += c.getRed();
                        g += c.getGreen();
                        b += c.getBlue();
                        count++;
                    }
                }
                r /= count;
                g /= count;
                b /= count;
                result.setRGB(x, y, new Color(r, g, b).getRGB());
            }
        }
        return result;
    }

    private BufferedImage rotateImage(BufferedImage img, double degrees) {
        double radians = Math.toRadians(degrees);
        int w = img.getWidth();
        int h = img.getHeight();
        BufferedImage result = new BufferedImage(w, h, img.getType());
        Graphics2D g2d = result.createGraphics();
        g2d.rotate(radians, w / 2.0, h / 2.0);
        g2d.drawImage(img, 0, 0, null);
        g2d.dispose();
        return result;
    }

    private BufferedImage applyGrayscale(BufferedImage img) {
        BufferedImage result = new BufferedImage(img.getWidth(), img.getHeight(), img.getType());
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                Color c = new Color(img.getRGB(x, y));
                int gray = (c.getRed() + c.getGreen() + c.getBlue()) / 3;
                Color newColor = new Color(gray, gray, gray);
                result.setRGB(x, y, newColor.getRGB());
            }
        }
        return result;
    }

    private BufferedImage applySepia(BufferedImage img) {
        BufferedImage result = new BufferedImage(img.getWidth(), img.getHeight(), img.getType());
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                Color c = new Color(img.getRGB(x, y));
                int r = c.getRed();
                int g = c.getGreen();
                int b = c.getBlue();

                int tr = (int)(0.393 * r + 0.769 * g + 0.189 * b);
                int tg = (int)(0.349 * r + 0.686 * g + 0.168 * b);
                int tb = (int)(0.272 * r + 0.534 * g + 0.131 * b);

                Color newColor = new Color(clamp(tr), clamp(tg), clamp(tb));
                result.setRGB(x, y, newColor.getRGB());
            }
        }
        return result;
    }

    private BufferedImage applyNegative(BufferedImage img) {
        BufferedImage result = new BufferedImage(img.getWidth(), img.getHeight(), img.getType());
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                Color c = new Color(img.getRGB(x, y));
                Color newColor = new Color(255 - c.getRed(), 255 - c.getGreen(), 255 - c.getBlue());
                result.setRGB(x, y, newColor.getRGB());
            }
        }
        return result;
    }

    private BufferedImage applyContrast(BufferedImage img) {
        BufferedImage result = new BufferedImage(img.getWidth(), img.getHeight(), img.getType());
        float factor = 1.3f;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                Color c = new Color(img.getRGB(x, y));
                int r = clamp((int)((c.getRed() - 128) * factor + 128));
                int g = clamp((int)((c.getGreen() - 128) * factor + 128));
                int b = clamp((int)((c.getBlue() - 128) * factor + 128));
                result.setRGB(x, y, new Color(r, g, b).getRGB());
            }
        }
        return result;
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private BufferedImage deepCopy(BufferedImage img) {
        ColorModel cm = img.getColorModel();
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        WritableRaster raster = img.copyData(null);
        return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
    }

    private void saveImage() {
        if (originalImage == null) return;

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Image As");
        fileChooser.setSelectedFile(new File("edited_image.png"));

        int userSelection = fileChooser.showSaveDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            String path = fileToSave.getAbsolutePath();
            if (!path.toLowerCase().endsWith(".png")) {
                fileToSave = new File(path + ".png");
            }

            try {
                ImageIO.write(originalImage, "png", fileToSave);
                JOptionPane.showMessageDialog(this, "Image saved successfully!");
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error saving image.");
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ImageEditor::new);
    }
}
