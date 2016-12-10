package GUI;

import java.awt.image.BufferedImage;
import org.opencv.core.Core;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;


public class VideoCap {

    private static int smoothAmount = 1;

    public static void setTakePhoto(boolean takePhoto){
        VideoCap.takePhoto = takePhoto;
    }

    public static void setSharpenAmount(int sharpenAmount) {
        VideoCap.sharpenAmount = sharpenAmount;
    }

    private static int sharpenAmount = 0;

    private static boolean takePhoto = false;

    public static void setSmoothAmount(int smoothAmount) {
        VideoCap.smoothAmount = smoothAmount;
    }

    //========================

    public static Mat2Image mat2Img = new Mat2Image();

    ImageProcessor p;

    VideoCapture cap;

    VideoCap(){
        cap = new VideoCapture();
        cap.open(0);
    }

    BufferedImage getOneFrame() {
        cap.read(mat2Img.mat);

        //This is where the magic happens
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        String path1 = "resources/haarcascades/haarcascade_frontalface_default.xml";
        String path2 = "resources/haarcascades/haarcascade_eye.xml";
        String path3 = "resources/haarcascades/haarcascade_mcs_mouth.xml";
        p = new ImageProcessor(mat2Img.mat, path1, path2, path3);

        p.sharpenAmount = sharpenAmount;
        mat2Img.mat = p.processFrame(p, smoothAmount, takePhoto);

        this.takePhoto = false;

        //figure out a way to capture it

        Imgproc.cvtColor(mat2Img.mat,mat2Img.mat,Imgproc.COLOR_RGB2BGR);
        return mat2Img.getImage(mat2Img.mat);
    }
}