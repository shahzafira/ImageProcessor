package com.kcl.osc.imageprocessor;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import javax.imageio.ImageIO;
import java.io.File;

public class ImageProcessorMT implements Runnable {

    private Image image;
    private String opfilename;
    private String filterType;
    private boolean save;
    private int numberOfThreads;

    /**
     * Constructor.
     * @param image The image to process.
     * @param filter The filter to use.
     * @param save Whether to save the new image or not.
     * @param opname The output image filename
     */
    public ImageProcessorMT(Image image, String filter, boolean save, String opname, int numberOfThreads) {

        this.image = image;
        this.opfilename = opname;
        this.filterType = filter;
        this.save = save;
        this.numberOfThreads = numberOfThreads;

    }

    /**
     * Runs this image processor.
     */
    public void run() {
        this.filter();
    }

    /**
     * This method decides whether a filter needs to be applied or not and then
     * calls appropriate methods to create the new, filtered pixel data.
     * @return
     */
    private Color[][] filterImage() {

        if (filterType.equals("GREY")) {
            return applyGreyscale();
        }

        Color[][] pixels = getPixelDataExtended();

        float[][] filter = createFilter(filterType);

        Color[][] filteredImage = applyFilter(pixels, filter);

        return filteredImage;
    }

    /**
     * Applies the greyscale operation.
     * @return the new pixel data.
     */
    private Color[][] applyGreyscale() {

        ThreadPool threadpool = new ThreadPool(5);

        Color[][] inputPixels = getPixelData();
        Color[][] outputPixels = new Color[inputPixels.length][inputPixels.length];

        int rowsAssignedToThread = inputPixels.length/numberOfThreads;

        for (int i = 0; i < (inputPixels.length); i++) {

            // Not shared, final row value to pass to each thread
            final int x = i;

            // Lambda to pass each row of image to individual threads
            Runnable task = () -> {

                for (int j = 0; j < (inputPixels[0].length); j++) {

                    double red = inputPixels[x][j].getRed();
                    double green = inputPixels[x][j].getGreen();
                    double blue = inputPixels[x][j].getBlue();

                    double newRGB = (red + green + blue) / 3;
                    newRGB = clampRGB(newRGB);

                    Color newPixel = new Color(newRGB, newRGB, newRGB, 1.0);
                    outputPixels[x][j] = newPixel;
                }
            };

            threadpool.submit(task);
            System.out.println("new thread");
        }

        threadpool.start();
        threadpool.join();
        threadpool.quit();
        return outputPixels;
    }

    /**
     * Applies the required filter to the input pixel data.
     * @param pixels The input pixel data.
     * @param filter The filter.
     * @return The new, filtered pixel data.
     */
    private Color[][] applyFilter(Color[][] pixels, float[][] filter) {

        ThreadPool threadPool = new ThreadPool(5);

        Color[][] finalImage = new Color[pixels.length - 2][pixels[0].length - 2];

        for (int i = 1; i < pixels.length -1; i++) {

            // Not shared, final row value to pass to each thread
            final int x = i;

            // Lambda to pass each row of image to individual threads
            Runnable task = () -> {
                for (int j = 1; j < pixels[x].length - 1; j++) {

                    double red = 0.0;
                    double green = 0.0;
                    double blue = 0.0;

                    for (int k = -1; k < filter.length - 1; k++) {
                        for (int l = -1; l < filter[0].length - 1; l++) {
                            red += pixels[x + k][j + l].getRed() * filter[1 + k][1 + l];
                            green += pixels[x + k][j + l].getGreen() * filter[1 + k][1 + l];
                            blue += pixels[x + k][j + l].getBlue() * filter[1 + k][1 + l];
                        }
                    }

                    red = clampRGB(red);
                    green = clampRGB(green);
                    blue = clampRGB(blue);
                    finalImage[x - 1][j - 1] = new Color(red, green, blue, 1.0);
                }
            };
            threadPool.submit(task);
        }

        threadPool.start();
        threadPool.join();
        threadPool.quit();
        return finalImage;
    }

    private void filter() {

        Color[][] pixels = filterImage();

        if (save) {
            saveNewImage(pixels, opfilename);
        }
    }

    /**
     * Creates the filter.
     * @param filterType The type of filter required.
     * @return The filter.
     */
    private float[][] createFilter(String filterType) {
        filterType = filterType.toUpperCase();

        if (filterType.equals("IDENTITY")) {
            return (new float[][] {{0,0,0},{0,1,0},{0,0,0}});
        } else if (filterType.equals("BLUR")) {
            return (new float[][] {{0.0625f,0.125f,0.0625f},{0.125f,0.25f,0.125f},{0.0625f,0.125f,0.0625f}});
        } else if (filterType.equals("SHARPEN")) {
            return (new float[][] {{0,-1,0},{-1,5,-1},{0,-1,0}});
        } else if (filterType.equals("EDGE")) {
            return (new float[][] {{-1,-1,-1},{-1,8,-1},{-1,-1,-1}});
        } else if (filterType.equals("EMBOSS")) {
            return (new float[][] {{-2,-1,0},{-1,0,1},{0,1,2}});
        }
        return null;
    }

    /**
     * Saves the pixel data in the parameter as a new image file.
     * @param pixels The pixel data.
     * @param filename The output filename.
     */
    private void saveNewImage(Color[][] pixels, String filename) {
        WritableImage wimg = new WritableImage(image.getPixelReader(), (int) image.getWidth(), (int) image.getHeight());

        PixelWriter pw = wimg.getPixelWriter();
        for (int i = 0; i < wimg.getHeight(); i++) {
            for (int j = 0; j < wimg.getWidth(); j++) {
                pw.setColor(i, j, pixels[i][j]);
            }
        }

        File newFile = new File(filename);

        try {
            ImageIO.write(SwingFXUtils.fromFXImage(wimg, null), "png", newFile);
        } catch (Exception s) {
        }
    }

    /**
     * This method ensures that the computations on color values have not
     * strayed outside of the range [0,1].
     * @param RGBValue the value to clamp.
     * @return The clamped value.
     */
    protected static double clampRGB(double RGBValue) {
        if (RGBValue < 0.0) {
            return 0.0;
        } else if (RGBValue > 1.0) {
            return 1.0;
        } else {
            return RGBValue;
        }
    }

    /**
     * Gets the pixel data from the image but does
     * NOT add a border.
     * @return The pixel data.
     */
    private Color[][] getPixelData() {
        PixelReader pr = image.getPixelReader();
        Color[][] pixels = new Color[(int) image.getWidth()][(int) image.getHeight()];
        for (int i = 0; i < image.getWidth(); i++) {
            for (int j = 0; j < image.getHeight(); j++) {
                pixels[i][j] = pr.getColor(i, j);
            }
        }

        return pixels;
    }

    /**
     * Gets the pixel data from the image but with a one-pixel border added.
     * @return The pixel data.
     */
    private Color[][] getPixelDataExtended() {
        PixelReader pr = image.getPixelReader();
        Color[][] pixels = new Color[(int) image.getWidth() + 2][(int) image.getHeight() + 2];

        for (int i = 0; i < pixels.length; i++) {
            for (int j = 0; j < pixels.length; j++) {
                pixels[i][j] = new Color(0.5, 0.5, 0.5, 1.0);
            }
        }

        for (int i = 0; i < image.getWidth(); i++) {
            for (int j = 0; j < image.getHeight(); j++) {
                pixels[i + 1][j + 1] = pr.getColor(i, j);
            }
        }

        return pixels;
    }
}
