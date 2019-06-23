
import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.filter.*;
import blablab.*;
import java.io.*;
import java.awt.event.*;
import java.util.Arrays;
// Unmixing the spectral images by phasor approach
// written by Farzad Fereidouni (F.Fereidouni@uu.nl) & Gerhard Blab (G.A.Blab@uu.nl)
// November 10, 2011
public class phasor_unmix implements PlugInFilter, MouseListener, MouseMotionListener {

    ImagePlus imp;
    ImageCanvas canvas;
    // counter for selecting the reference points manually
    int clik = 0;
    //Array for recording the phasor cloud
    int[][] pixels = new int[401][401];
    //reference points array
    int rx[] = new int[4];
    int ry[] = new int[4];
    //Original image dimensions without ROI
    int Dim_x, Dim_y;
    // imp for spectrum plot
    ImagePlus imp_plot = null;
//    ImageProcessor ip3 = null;
    private double minPxlPctl = 0.001;    // signal (percentile); intensity image is
    private double maxPxlPctl = 0.990;    // scaled to display minPxlPctl -> maxPxlPctl
    private double X1_max, X2_max, X3_max;
    private int K;

    public int setup(String arg, ImagePlus imp) {
        this.imp = imp;
        return DOES_RGB;
    }

    public void run(ImageProcessor ip) {
        Rectangle r = ip.getRoi();
        //reading the information from the header of the phasor cloud
        MetaData meta = new MetaData(imp);
        //imp2 (Original image)
        ImagePlus imp2 = WindowManager.getImage(meta.getImageTitle());
        ImageStack ip2 = imp2.getStack();
        K = ip2.getSize();


        //recording array for the phasor cloud
        for (int xx = 0; xx < ip.getWidth(); xx++) {
            for (int yy = 0; yy < ip.getHeight(); yy++) {
                pixels[xx][yy] = ip.getPixel(xx, yy);
            }
        }
        ImageWindow win = imp.getWindow();
//Items to be shown in dialog
        String[] items = {"Default", "Add manually", "Draw Trajectories"};
        GenericDialog gd = new GenericDialog("Spectral phasor Unmixing");
        if (WindowManager.getImage(meta.getImageTitle()) != null) {
            //The maximums of the spectrum
            gd.addNumericField("Max X1:", ip2.getSize() / 3, 0);
            gd.addNumericField("Max X2:", ip2.getSize() / 2, 0);
            gd.addNumericField("Max X3:", ip2.getSize(), 0);
            gd.addMessage("-------------------------------------");
            gd.addChoice("Setting refrence points", items, "Default");
            gd.showDialog();
            if (gd.wasCanceled()) {
                return;
            }
        } else {
            IJ.showMessage("The corresponding image is not found");
        }
//The maximums of the spectrum
        X1_max = gd.getNextNumber();
        X2_max = gd.getNextNumber();
        X3_max = gd.getNextNumber();
        int option = gd.getNextChoiceIndex();

        switch (option) {
            case 1:
                //Link an event to the phasor cloud image for mouse motion and click


                Draw_Trajectories(K, X1_max, X2_max, X3_max);


                canvas = win.getCanvas();
                canvas.addMouseListener(this);
                canvas.addMouseMotionListener(this);


                break;
            case 2:
                Draw_Trajectories(K, X1_max, X2_max, X3_max);
                break;
            default:
                //reading the priviously recorded refrence points file
                try {
                    FileReader inStream = new FileReader("Phasor_refrence.txt");

                    // Filter the Input Stream - buffers characters for efficiency
                    BufferedReader in = new BufferedReader(inStream);

                    // read the M and phase
                    for (int i = 1; i <= 3; i++) {
                        rx[i] = Integer.parseInt(in.readLine());
                        ry[i] = Integer.parseInt(in.readLine());
                    }
                    in.close();


                    ip.setColor(Color.yellow);

                    //Drawing the triangle of refrence points
                    ip.moveTo(rx[1], ry[1]);
                    ip.setColor(Color.white);
                    ip.drawString("1");
                    ip.moveTo(rx[1], ry[1]);
                    ip.setColor(Color.YELLOW);

                    ip.lineTo(rx[2], ry[2]);
                    ip.setColor(Color.white);
                    ip.drawString("2");
                    ip.moveTo(rx[2], ry[2]);
                    ip.setColor(Color.YELLOW);

                    ip.lineTo(rx[3], ry[3]);
                    ip.setColor(Color.white);
                    ip.drawString("3");
                    ip.moveTo(rx[3], ry[3]);
                    ip.setColor(Color.YELLOW);

                    ip.lineTo(rx[1], ry[1]);
                    imp.updateAndDraw();
                    unmix();



                } catch (IOException ioe) {
                }
        }
    }

    public void mouseClicked(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        int offscreenX = canvas.offScreenX(x);
        int offscreenY = canvas.offScreenY(y);
        ImageProcessor ip = imp.getProcessor();
        ip.setColor(Color.yellow);
        if (clik == 0) {
            for (int xx = 0; xx < 401; xx++) {
                for (int yy = 0; yy < 401; yy++) {
                    ip.putPixel(xx, yy, pixels[xx][yy]);
                }
            }
            Draw_Trajectories(K, X1_max, X2_max, X3_max);
            ip.drawDot(offscreenX, offscreenY);
            ip.moveTo(offscreenX, offscreenY);
            ip.setColor(Color.white);
            ip.drawString("1");
            ip.moveTo(offscreenX, offscreenY);
            ip.setColor(Color.YELLOW);
        }
        clik++;
        if (clik == 2) {
            ip.lineTo(offscreenX, offscreenY);
            ip.setColor(Color.white);
            ip.drawString("2");
            ip.moveTo(offscreenX, offscreenY);
            ip.setColor(Color.YELLOW);

            imp.updateAndDraw();
        }

        rx[clik] = offscreenX;
        ry[clik] = offscreenY;


        if (clik == 3) {
            ip.lineTo(offscreenX, offscreenY);
            ip.setColor(Color.white);
            ip.drawString("3");
            ip.moveTo(offscreenX, offscreenY);
            ip.setColor(Color.YELLOW);

            ip.lineTo(rx[1], ry[1]);
            imp.updateAndDraw();
            clik = 0;


            // Create a File

            File myFile = new File("Phasor_refrence.txt");
            try {
// Create an Output Stream
                FileOutputStream outStream = new FileOutputStream(myFile);

// Filter bytes to ASCII
                PrintWriter out = new PrintWriter(outStream);

// Here we actually write to file
                for (int i = 1; i <= 3; i++) {
                    out.println(rx[i]);
                    out.println(ry[i]);
                }
                out.close();
            } catch (IOException ioe) {
            }




            unmix();

        }

    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mouseMoved(MouseEvent e) {
        /*
        MetaData meta = new MetaData(imp);
        ImagePlus imp2 = WindowManager.getImage(meta.getImageTitle());
        ImageStack ip2 = imp2.getStack();
        double phasor_r[] = meta.getPhasorMAP_r();
        double phasor_i[] = meta.getPhasorMAP_i();

        int K = ip2.getSize();
        int xx = e.getX();
        int yy = e.getY();
        int offscreenX = canvas.offScreenX(xx);
        int offscreenY = canvas.offScreenY(yy);
        Dim_x = ip2.getWidth();
        Dim_y = ip2.getHeight();
        double X[] = new double[K];
        double Y[] = new double[K];
        double max_z = 0, min_z = 1e10;
        int xc, yc;

        int i = 0;
        int found = 0;
        for (i = 0; i < Dim_x * Dim_y; i++) {
        if (phasor_r[i] == offscreenX && phasor_i[i] == offscreenY) {
        found = 1;
        break;
        }
        }


        if (found == 1) {

        xc = (int) (i - Math.floor(i / Dim_x) * Dim_x);
        yc = (int) Math.floor(i / Dim_x);

        for (int z = 0; z < K; z++) {
        X[z] = z;

        for (int x_bin = xc - 2; x_bin <= xc + 2; x_bin++) {

        for (int y_bin = yc - 2; y_bin <= yc + 2; y_bin++) {

        Y[z] += ip2.getVoxel(x_bin, y_bin, z);
        }
        }
        if (Y[z] > max_z) {
        max_z = Y[z];
        }
        if (Y[z] < min_z) {
        min_z = Y[z];
        }
        }




        Plot Spectrum = new Plot("Spectrum", "Pixel", "Intensity", X, Y);
        Spectrum.setLimits(0, K, min_z, max_z);

        if (imp_plot == null) {
        imp_plot = Spectrum.getImagePlus();
        imp_plot.show();
        } else {
        imp_plot.setImage(Spectrum.getImagePlus().getImage());
        imp_plot.show();
        }

        }

         */
    }

    public void mouseDragged(MouseEvent e) {
    }

    public void unmix() {


        MetaData meta = new MetaData(imp);

        ImagePlus imp2 = WindowManager.getImage(meta.getImageTitle());
        //ImageProcessor ip2=imp2.getProcessor();
        ImageStack ip2 = imp2.getStack();

        ImageProcessor ip = imp.getProcessor();

        Dim_x = ip2.getWidth();
        Dim_y = ip2.getHeight();
        ImagePlus X1 = NewImage.createRGBImage("X1", Dim_x, Dim_y,
                1, NewImage.FILL_BLACK);
        ImageProcessor X1_ip = X1.getProcessor();
        double X1_I = 0;

        ImagePlus X2 = NewImage.createRGBImage("X2", Dim_x, Dim_y,
                1, NewImage.FILL_BLACK);
        ImageProcessor X2_ip = X2.getProcessor();
        double X2_I = 0;

        ImagePlus X3 = NewImage.createRGBImage("X3", Dim_x, Dim_y,
                1, NewImage.FILL_BLACK);
        ImageProcessor X3_ip = X3.getProcessor();
        double X3_I = 0;

        ImagePlus Overlay = NewImage.createRGBImage("Overlay", Dim_x, Dim_y,
                1, NewImage.FILL_BLACK);
        ImageProcessor Overlay_ip = Overlay.getProcessor();
        double XO_I = 0;
        double phasor_r[] = meta.getPhasorMAP_r();
        double phasor_i[] = meta.getPhasorMAP_i();

        double a, c;
        double alpha[][] = new double[4][Dim_x * Dim_y];
        double alpha1[] = new double[Dim_x * Dim_y];
        double alpha2[] = new double[Dim_x * Dim_y];
        double alpha3[] = new double[Dim_x * Dim_y];
        double alpha0[] = new double[Dim_x * Dim_y];
        double count[] = new double[Dim_x * Dim_y];
        double alpha_max[] = new double[4];
        double alpha_min[] = new double[4];


        Rectangle r = ip.getRoi();
        K = ip2.getSize();

        for (int i = 1; i < Dim_x * Dim_y; i++) {
            if (phasor_r[i] != -2 && phasor_i[i] != -2) {
                if (r.contains(phasor_r[i], phasor_i[i])) {

                    count[i] = 0;
                    for (int z = 1; z < K; z++) {
                        count[i] += ip2.getVoxel((int) (i - Math.floor(i / Dim_x) * Dim_x), (int) Math.floor(i / Dim_x), z);
                    }

                    a = Math.abs(rx[2] * ry[1] - rx[1] * ry[2] + rx[3] * ry[2] - rx[2] * ry[3] + rx[1] * ry[3] - rx[3] * ry[1]);
                    alpha[1][i] = Math.abs(rx[2] * phasor_i[i] - phasor_r[i] * ry[2] + rx[3] * ry[2] - rx[2] * ry[3] + phasor_r[i] * ry[3] - rx[3] * phasor_i[i]) / a;
                    alpha[2][i] = Math.abs(rx[3] * phasor_i[i] - phasor_r[i] * ry[3] + rx[1] * ry[3] - rx[3] * ry[1] + phasor_r[i] * ry[1] - rx[1] * phasor_i[i]) / a;
                    alpha[3][i] = Math.abs(rx[1] * phasor_i[i] - phasor_r[i] * ry[1] + rx[2] * ry[1] - rx[1] * ry[2] + phasor_r[i] * ry[2] - rx[2] * phasor_i[i]) / a;
                    for (int j = 1; j <= 3; j++) {
                        if (alpha[j][i] > 1) {
                            alpha[j][i] = 1;
                        }
                        if (alpha[j][i] < 0) {
                            alpha[j][i] = 0;

                        }
                    }
                    c = alpha[1][i] + alpha[2][i] + alpha[3][i];
                    if (c > 1) {
                        alpha[1][i] = alpha[1][i] / c;
                        alpha[2][i] = alpha[2][i] / c;
                        alpha[3][i] = alpha[3][i] / c;
                    }

                    alpha1[i] = alpha[1][i] * count[i];
                    alpha2[i] = alpha[2][i] * count[i];
                    alpha3[i] = alpha[3][i] * count[i];
                    alpha0[i] = count[i];
                }
            }


        }

        Arrays.sort(alpha0);
        Arrays.sort(alpha1);
        Arrays.sort(alpha2);
        Arrays.sort(alpha3);


        alpha_min[0] = alpha0[(int) (alpha0.length * minPxlPctl)];
        alpha_max[0] = alpha0[(int) (alpha0.length * maxPxlPctl)];


        alpha_min[1] = alpha1[(int) (alpha1.length * minPxlPctl)];
        alpha_max[1] = alpha1[(int) (alpha1.length * maxPxlPctl)];

        alpha_min[2] = alpha2[(int) (alpha2.length * minPxlPctl)];
        alpha_max[2] = alpha2[(int) (alpha2.length * maxPxlPctl)];

        alpha_min[3] = alpha3[(int) (alpha3.length * minPxlPctl)];
        alpha_max[3] = alpha3[(int) (alpha3.length * maxPxlPctl)];


        int xx, yy;
        double alpha_t[] = new double[4];
        int RGBpxl[] = new int[3];
        for (int i = 1; i < Dim_x * Dim_y; i++) {

            if (phasor_r[i] != -2 && phasor_i[i] != -2) {

                if (r.contains(phasor_r[i], phasor_i[i])) {

                    X1_I = (count[i] * alpha[1][i]);

                    X2_I = (count[i] * alpha[2][i]);

                    X3_I = (int) (count[i] * alpha[3][i]);

                    alpha_t[1] += alpha[1][i];
                    alpha_t[2] += alpha[2][i];
                    alpha_t[3] += alpha[3][i];
                    xx = (int) (i - Math.floor(i / Dim_x) * Dim_x);
                    yy = (int) Math.floor(i / Dim_x);

                    RGBpxl[0] = (int) ((X1_I - alpha_min[1]) * 255 / (alpha_max[1] - alpha_min[1]));
                    if (RGBpxl[0] > 255) {
                        RGBpxl[0] = 255;
                    }
                    if (RGBpxl[0] < 0) {
                        RGBpxl[0] = 0;
                    }
                    RGBpxl[1] = 0;
                    RGBpxl[2] = 0;

                    X1_ip.putPixel(xx, yy, RGBpxl);

                    RGBpxl[0] = 0;
                    RGBpxl[1] = (int) ((X2_I - alpha_min[2]) * 255 / (alpha_max[2] - alpha_min[2]));
                    RGBpxl[2] = 0;

                    if (RGBpxl[1] > 255) {
                        RGBpxl[1] = 255;
                    }
                    if (RGBpxl[1] < 0) {
                        RGBpxl[1] = 0;
                    }

                    X2_ip.putPixel(xx, yy, RGBpxl);

                    RGBpxl[0] = 0;
                    RGBpxl[1] = 0;
                    RGBpxl[2] = (int) ((X3_I - alpha_min[3]) * 255 / (alpha_max[3] - alpha_min[3]));

                    if (RGBpxl[2] > 255) {
                        RGBpxl[2] = 255;
                    }
                    if (RGBpxl[2] < 0) {
                        RGBpxl[2] = 0;
                    }

                    X3_ip.putPixel(xx, yy, RGBpxl);

                    RGBpxl[0] = (int) ((X1_I - alpha_min[0]) * 255 / (alpha_max[0] - alpha_min[0]));
                    RGBpxl[1] = (int) ((X2_I - alpha_min[0]) * 255 / (alpha_max[0] - alpha_min[0]));
                    RGBpxl[2] = (int) ((X3_I - alpha_min[0]) * 255 / (alpha_max[0] - alpha_min[0]));
                    if (RGBpxl[0] > 255) {
                        RGBpxl[0] = 255;
                    }
                    if (RGBpxl[0] < 0) {
                        RGBpxl[0] = 0;
                    }

                    if (RGBpxl[1] > 255) {
                        RGBpxl[1] = 255;
                    }
                    if (RGBpxl[1] < 0) {
                        RGBpxl[1] = 0;
                    }


                    if (RGBpxl[2] > 255) {
                        RGBpxl[2] = 255;
                    }
                    if (RGBpxl[2] < 0) {
                        RGBpxl[2] = 0;
                    }

                    Overlay_ip.putPixel(xx, yy, RGBpxl);
                }
            }
        }

        X3.show();
        X3.updateAndDraw();


        X2.show();
        X2.updateAndDraw();



        X1.show();
        X1.updateAndDraw();
        //X1.updateAndRepaintWindow();

        Overlay.show();
        Overlay.updateAndDraw();
        IJ.log(imp.getTitle());
        IJ.log("X1  " + alpha_t[1] * 100 / (alpha_t[1] + alpha_t[2] + alpha_t[3]));
        IJ.log("X2  " + alpha_t[2] * 100 / (alpha_t[1] + alpha_t[2] + alpha_t[3]));
        IJ.log("X3  " + alpha_t[3] * 100 / (alpha_t[1] + alpha_t[2] + alpha_t[3]));


    }

    public void Draw_Trajectories(int K, double X1, double X2, double X3) {
        int phasor_dim = 400;
        double sp[] = new double[K + 1];
        double sr = 0;
        double si = 0;
        double sp_t = 0;
        double omega = 2.0 * Math.PI / K;
        ImageProcessor ip = imp.getProcessor();
        for (int w = 1; w <= 100; w++) {
            sr = 0;
            si = 0;
            sp_t = 0;
            for (int z = 1; z <= K; z++) {
                sp[z] = Math.exp(-Math.pow((z - X1), 2) / (2 * w * w));
                sr += sp[z] * Math.cos(omega * (z - .5));
                si += sp[z] * Math.sin(-omega * (z - .5));
                sp_t += sp[z];
            }
            ip.setColor(Color.red);
            ip.drawPixel((int) (phasor_dim * ((sr / sp_t) + 1) / 2), (int) (phasor_dim * ((si / sp_t) + 1) / 2));
            //X2
            sr = 0;
            si = 0;
            sp_t = 0;
            for (int z = 1; z <= K; z++) {
                sp[z] = Math.exp(-Math.pow((z - X2), 2) / (2 * w * w));
                sr += sp[z] * Math.cos(omega * (z - .5));
                si += sp[z] * Math.sin(-omega * (z - .5));
                sp_t += sp[z];
            }
            ip.setColor(Color.green);
            ip.drawPixel((int) (phasor_dim * ((sr / sp_t) + 1) / 2), (int) (phasor_dim * ((si / sp_t) + 1) / 2));
            //X3
            sr = 0;
            si = 0;
            sp_t = 0;
            for (int z = 1; z <= K; z++) {
                sp[z] = Math.exp(-Math.pow((z - X3), 2) / (2 * w * w));
                sr += sp[z] * Math.cos(omega * (z - .5));
                si += sp[z] * Math.sin(-omega * (z - .5));
                sp_t += sp[z];
            }
            ip.setColor(Color.blue);
            ip.drawPixel((int) (phasor_dim * ((sr / sp_t) + 1) / 2), (int) (phasor_dim * ((si / sp_t) + 1) / 2));
        }
    }
}
