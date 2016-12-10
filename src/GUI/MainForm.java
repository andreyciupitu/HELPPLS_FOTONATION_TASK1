package GUI;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class MainForm extends JFrame implements ChangeListener,ActionListener{

    private JPanel panel1;
    private JButton btnPhoto;
    private JSlider sliderSmooth;
    private JButton beautifyAnEntireFolderButton;
    private JComboBox comboBoxMask;
    private JTextField textField1;
    private JTextField textField2;
    private JTextField textField3;
    private JTextField textField4;
    private JTextField textField5;
    private JTextField textField6;
    private JSlider sharpenSlider;


    static final int SMOOTH_MIN = 1;
    static final int SMOOTH_MAX = 15;
    static final int SMOOTH_INIT = 1;

    public static String activeMask = "None";

    public MainForm(){

        setContentPane(panel1);

        setTitle("Beautify Settings");
        setSize(400, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        try {
            btnPhoto.setIcon(new ImageIcon(ImageIO.read(new File("UI_Resources/Icon_Photo.png"))));
        }catch (IOException e){
            e.printStackTrace();
        }

        initializeMaskBox();

        sliderSmooth.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                int smoothAmount = sliderSmooth.getValue();
                VideoCap.setSmoothAmount(smoothAmount);
            }
        });
        btnPhoto.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                VideoCap.setTakePhoto(true);

            }
        });
        comboBoxMask.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                activeMask = (String)comboBoxMask.getSelectedItem();

            }
        });
        beautifyAnEntireFolderButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

            }
        });
        beautifyAnEntireFolderButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                File folder = new File("demo/");
                File[] listOfFiles = folder.listFiles();

                ArrayList<String> demoFiles =  new ArrayList<>();

                for (File f:listOfFiles) {
                    if (f.isFile()) {
                        demoFiles.add(f.getName());
                    }
                }

                for ( String s: demoFiles) {

                        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
                        Mat m = Imgcodecs.imread("demo/"+s);
                        String path1 = "resources/haarcascades/haarcascade_frontalface_default.xml";
                        String path2 = "resources/haarcascades/haarcascade_eye.xml";
                        String path3 = "resources/haarcascades/haarcascade_mcs_mouth.xml";
                        ImageProcessor p = new ImageProcessor(m,path1,path2,path3);

                        m = p.processFrame(p, 9, false);

                        Imgcodecs.imwrite("results/"+s, m);


                }


            }
        });
        sharpenSlider.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                super.componentResized(e);
            }
        });
        sharpenSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                int sharpenAmount = sharpenSlider.getValue();
                VideoCap.setSharpenAmount(sharpenAmount);
            }
        });
    }

    private void initializeMaskBox(){

        File folder = new File("masks/");
        File[] listOfFiles = folder.listFiles();

        ArrayList<String> maskOptions =  new ArrayList<>();

        for (File f:listOfFiles) {
            if (f.isFile()) {
                maskOptions.add(f.getName());
            }
        }

        comboBoxMask.removeAllItems();
        comboBoxMask.addItem("None");
        for(String i:maskOptions){
            comboBoxMask.addItem(i);
            System.out.println(i);
        }

        comboBoxMask.setSelectedIndex(0);

    }

    @Override
    public void stateChanged(ChangeEvent e) {

    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
    }

    @Override
    public void actionPerformed(ActionEvent e) {

    }
}