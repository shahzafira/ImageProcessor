package com.kcl.osc.imageprocessor;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class ImageProcessorApplicationMT extends Application  {

    /**
     * Change this constant to change the filtering operation. Options are
     * IDENTITY, EDGE, BLUR, SHARPEN, EMBOSS, EDGE, GREY
     */
    private static final String filter = "GREY";

    /**
     * Set this boolean to false if you do NOT wish the new images to be
     * saved after processing.
     */
    private static final boolean saveNewImages = true;


    @Override
    public void start(Stage stage) {

        // gets the images from the 'img' folder.
        ArrayList<ImageInfo> images = findImages();

        System.out.println("Working.");

        ThreadPool threadpool = new ThreadPool(5);

        // for each image creates and runs an ImageProcessor to process the image.
        for (int i = 0; i < images.size(); i++) {
            ImageProcessorMT ip = new ImageProcessorMT(images.get(i).getImage(), filter, saveNewImages, images.get(i).getFilename() + "_filtered.png", 3);
            threadpool.submit(ip);
        }
        threadpool.start();
        threadpool.join();
        threadpool.quit();

        System.out.println("Done.");

        // Kill this application
        Platform.exit();
    }

    /**
     * This method expects all of the images that are to be processed to
     * be in a folder called img that is in the current working directory.
     * In Eclipse, for example, this means the img folder should be in the project
     * folder (alongside src and bin).
     * @return Info about the images found in the folder.
     */
    private ArrayList<ImageInfo> findImages() {
        ArrayList<ImageInfo> images = new ArrayList<ImageInfo>();
        Collection<File> files = listFileTree(new File("img"));
        for (File f: files) {
            if (f.getName().startsWith(".")) {
                continue;
            }
            Image img = new Image("file:" + f.getPath());
            ImageInfo info = new ImageInfo(img, f.getName());
            images.add(info);
        }
        return images;
    }

    private static Collection<File> listFileTree(File dir) {
        Set<File> fileTree = new HashSet<File>();
        if (dir.listFiles() == null)
            return fileTree;
        for (File entry : dir.listFiles()) {
            if (entry.isFile())
                fileTree.add(entry) /* */;
            else
                fileTree.addAll(listFileTree(entry));
        }
        return fileTree;
    }

    public static void main(String[] args) {
        launch(args);
    }

    /**
     * Simply class to hold an Image and its filename.
     * @author iankenny
     *
     */
    private static class ImageInfo {
        private Image image;
        private String filename;

        public ImageInfo(Image image, String filename) {
            this.image = image;
            this.filename = filename;
        }

        public Image getImage() {
            return image;
        }

        public String getFilename() {
            return filename;
        }
    }
}
