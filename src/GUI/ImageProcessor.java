package GUI;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ImageProcessor {

    private static String prevMask;
    private static Mat theMask;

    public static int smoothAmount = 15; //1-15
    public static int sharpenAmount = 0; //0-6

    private String name;
    private Mat frame;

    /* Number of images written on disk */
    private static int counter = 0;

    /* Face detection parameters */
    private static final float FACE_PERCENT = 0.15f;
    private CascadeClassifier faceClassifier;
    private int absoluteFaceSize = 0;

    /* Eye classifier parameters */
    private static final float EYE_PERCENT = 0.1f;
    private CascadeClassifier eyesClassifier;
    private int absoluteEyeSize = 0;

    /* Mouth classifier parameters */
    private static final float MOUTH_PERCENT = 0.2f;
    private CascadeClassifier mouthClassifier;
    private int absoluteMouthSize = 0;

    private Rect[] facesArray;

    public ImageProcessor(Mat inputImage, String face_xml, String eyes_xml, String mouth_xml){
        this.eyesClassifier = new CascadeClassifier(eyes_xml);
        this.faceClassifier = new CascadeClassifier(face_xml);
        this.mouthClassifier = new CascadeClassifier(mouth_xml);
        this.frame = inputImage;
    }

    public Mat processFrame(ImageProcessor t, int smoothAmount, boolean save){
        if(save){
            Imgcodecs.imwrite("gallery/original"+counter+".jpg", frame);
        }

        ImageProcessor.smoothAmount = smoothAmount;

        frame = t.smoothSkin( new Scalar(0,38,80), new Scalar(20,255,255));
        if(sharpenAmount >0) {
            frame = t.crispSkin(new Scalar(0, 38, 80), new Scalar(20, 255, 255));
        }

        if(!MainForm.activeMask.equals("None")) {
            if (!MainForm.activeMask.equals(prevMask)) {
                theMask = Imgcodecs.imread("masks/" + MainForm.activeMask, 1);
                prevMask = MainForm.activeMask;
            }
            t.detectFeatures();
            frame = t.putMask(theMask);
        }

        if(save) {
            Imgcodecs.imwrite("gallery/still" + counter + ".jpg", frame);
            counter++;
        }
        return frame;
    }


    //========== helper functions ===========================================

    /* Put the mask on the face */
    public Mat putMask(Mat mask) {

        for(Rect r : facesArray) {
            Mat newMask = new Mat();
            Imgproc.resize(mask, newMask, r.size());

            Point center = new Point(r.x + r.width * 0.5, r.y + r.height * 0.5);
            Rect roi = new Rect((int)(center.x - r.size().width/2), (int)(center.y - r.size().height/2),
                    (int)r.size().width, (int)r.size().height);
            Mat src = frame.submat(roi);
            Mat mask2, m, m1;
            m = new Mat();
            mask2 = new Mat();
            Mat mask1 = newMask;
            Imgproc.threshold(mask1, mask2, 235, 255, Imgproc.THRESH_BINARY_INV); //fundal masca negru
            Core.bitwise_not(mask2, m); //spatiu masca negru
            Core.bitwise_and(src, m, src); //fata peste care este suprascris spatiul mastii
            Core.bitwise_and(newMask, mask2, newMask); //doar masca de pus pe fata
            Imgcodecs.imwrite("___C.jpg", src); //
            Core.addWeighted(src, 1, newMask, 0.9, 0, src);
            int left_pad = roi.x;
            int right_pad = frame.width() - roi.x - roi.width;
            int bottom_pad = roi.y;
            int top_pad = frame.height() - roi.y - roi.height;
            Core.copyMakeBorder(src, src, top_pad, bottom_pad, left_pad, right_pad, Core.BORDER_CONSTANT,
                    new Scalar(0, 0 ,0));

        }
        return this.frame;
    }

    public Mat SkinMask(Scalar color_sample1, Scalar color_sample2){

        Mat converted = new Mat();
        Mat skinMask = new Mat();
        Mat kernel = new Mat();

        Imgproc.cvtColor(frame, converted, Imgproc.COLOR_BGR2HSV);

        Core.inRange(converted, color_sample1,color_sample2 ,skinMask);

        kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(11, 11));

        Imgproc.erode(skinMask, skinMask, kernel);
        Imgproc.dilate(skinMask,skinMask, kernel);

        return skinMask;
    }


    public Mat smoothSkin(Scalar color_sample1, Scalar color_sample2) {
        Mat skinMask = SkinMask(color_sample1, color_sample2);
        Mat aux = new Mat();
        Mat aux2 = new Mat();
        Mat skin = new Mat();
        Mat background = new Mat();

        Core.bitwise_and(frame, frame, skin, skinMask);
        Core.bitwise_not(skinMask, aux);
        Core.bitwise_and(frame, frame, background, aux);
        Imgproc.bilateralFilter(skin, aux2, smoothAmount, 80, 100);
        Core.addWeighted(background, 1, aux2, 1, 0, this.frame);
        return this.frame;
    }

    public Mat crispSkin(Scalar color_sample1, Scalar color_sample2) {

        Mat skinMask = SkinMask(color_sample1, color_sample2);
        Mat aux = new Mat();
        Mat aux2 = new Mat();
        Mat skin = new Mat();
        Mat background = new Mat();

        Core.bitwise_and(frame, frame, skin, skinMask);
        Core.bitwise_not(skinMask, aux);
        Core.bitwise_and(frame, frame, aux2, skinMask);
        Core.bitwise_and(frame, frame, background, aux);
        Imgproc.GaussianBlur(skin,skin, new Size(0, 0), sharpenAmount);//5
        Core.addWeighted(aux2, 1.4, skin, -0.4, 0, this.frame);//-0.4
        Core.addWeighted(background, 1.0, this.frame, 1, 0, this.frame);

        return this.frame;
    }

    /* Method for detecting all the facial features in the image */
    public void detectFeatures(){
        int tmp;
        Mat frame_gray = new Mat();
        MatOfRect faces = new MatOfRect();

		/* Make greyscale image */
        Imgproc.cvtColor(frame, frame_gray, Imgproc.COLOR_BGRA2GRAY);
        Imgproc.equalizeHist(frame_gray, frame_gray);

		/* Detect all the faces */
        if (this.absoluteFaceSize == 0){
            tmp = frame_gray.rows();
            if (Math.round(tmp * FACE_PERCENT) > 0){
                this.absoluteFaceSize = Math.round(tmp * FACE_PERCENT);
            }
        }

        faceClassifier.detectMultiScale(frame_gray, faces, 1.15, 3, 0 | Objdetect.CASCADE_SCALE_IMAGE,
                new Size(this.absoluteFaceSize, this.absoluteFaceSize), new Size());
        facesArray = faces.toArray();

		/* Determine facial features */
        for (int i = 0; i < facesArray.length; i++) {

			/* Find features for a face */
            Mat faceROI = frame_gray.submat(facesArray[i]);
            MatOfRect eyes = new MatOfRect();

			/* Detect the eyes */
            tmp = faceROI.rows();
            if (Math.round(tmp * EYE_PERCENT) > 0){
                this.absoluteEyeSize = Math.round(tmp * EYE_PERCENT);
            }

            eyesClassifier.detectMultiScale(faceROI, eyes, 1.1, 7, 0 | Objdetect.CASCADE_SCALE_IMAGE,
                    new Size(30, 30)/*this.absoluteEyeSize, this.absoluteEyeSize)*/, new Size());

            List<Rect> eyesList = eyes.toList();
            ArrayList<Rect> eye_lst = new ArrayList<>(eyesList);
            Iterator<Rect> eye_it = eye_lst.iterator();
            while (eye_it.hasNext()){
                Rect elm = eye_it.next();
                Point center1 = new Point(facesArray[i].x + elm.x + elm.width * 0.5, facesArray[i].y + elm.y + elm.height * 0.5);
                if (center1.y > facesArray[i].y + facesArray[i].height * 0.5)
                    eye_it.remove();
            }

            Rect[] eyesArray = eyes.toArray();

            for (int j = 0; j < eyesArray.length; j++){
                Point center1 = new Point(facesArray[i].x + eyesArray[j].x + eyesArray[j].width * 0.5, facesArray[i].y + eyesArray[j].y + eyesArray[j].height * 0.5);
                int radius = (int) Math.round((eyesArray[j].width + eyesArray[j].height) * 0.25);
                //Imgproc.circle(frame, center1, radius, new Scalar(255, 0, 0), 4, 8, 0);
            }

			/* Detect the mouth */
            MatOfRect mouth = new MatOfRect();

            tmp = faceROI.rows();
            if (Math.round(tmp * MOUTH_PERCENT) > 0){
                this.absoluteMouthSize = Math.round(tmp * MOUTH_PERCENT);
            }

            mouthClassifier.detectMultiScale(faceROI, mouth, 1.1, 7, 0 | Objdetect.CASCADE_SCALE_IMAGE,
                    new Size(this.absoluteMouthSize, this.absoluteMouthSize), new Size());

            List<Rect> mouthList = mouth.toList();
            ArrayList<Rect> mouth_lst = new ArrayList<>(mouthList);
            Iterator<Rect> mouth_it = mouth_lst.iterator();
            while (mouth_it.hasNext()){
                Rect elm = mouth_it.next();
                Point center2 = new Point(facesArray[i].x + elm.x + elm.width * 0.5, facesArray[i].y + elm.y + elm.height * 0.5);
                if (center2.y < facesArray[i].y + facesArray[i].height * 0.5)
                    mouth_it.remove();
            }

            Rect[] mouthArray = mouth.toArray();

            for (int j = 0; j < mouthArray.length; j++){
                Point center2 = new Point(facesArray[i].x + mouthArray[j].x + mouthArray[j].width * 0.5, facesArray[i].y + mouthArray[j].y + mouthArray[j].height * 0.5);
                int radius = (int) Math.round((mouthArray[j].width + mouthArray[j].height) * 0.25);
                //Imgproc.circle(frame, center2, radius, new Scalar(255, 255, 0), 4, 8, 0);
            }

        }
    }

    public void writeImage(String file){
        Imgcodecs.imwrite(file, frame);
    }

}
