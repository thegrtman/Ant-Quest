package antquest;

import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.Scanner;
import javax.swing.JFrame;

/**
 * FullScreenView.java
 * <p>
 * The a singleton Frame which will be painted to the screen for FSEM when the
 * program is executed.
 * <p>
 * Author: Kevin Groat
 * Language: Java
 *
 * @author Kevin Groat
 * @version 0.1.0
 */
public class FullScreenView extends JFrame {

   /** The singleton instance of FullScreenView. */
   private static FullScreenView instance;
   /** The original DisplayMode of the monitor on which this program is being
    * viewed.  This is necessary to return back from FSEM. */
   private DisplayMode originalDisplayMode;
   /** The GraphicsDevice which is being rendered through. */
   private GraphicsDevice screen;
   /** The width (in pixels) of the display. */
   private int screenWidth;
   /** The height (in pixels) of the display. */
   private int screenHeight;
   private int insetLeft;
   private int insetTop;
   private boolean isFullScreen;
   private static boolean tryFull = false;

   /**
    * Getter method for the singleton instance of FullScreenView.
    * @return The singleton instance of FullScreenView.
    */
   public static FullScreenView instance() {
      return instance;
   }

   /**
    * Main method.  Starts the program, by initializing the singleton
    * instance of FullScreenView.
    * @param args - unused - -
    */
   public static void main(String[] args) throws IOException {
      Properties pr = System.getProperties();
      System.out.println("Path separator: \""+pr.getProperty("path.separator")+"\"");
      if (args != null && args.length > 0 && args[0].toLowerCase().endsWith("proper")) {
         if (instance == null) {
            instance = new FullScreenView(GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice());
         }
      }else{
         Runtime r = Runtime.getRuntime();
         String s = FullScreenView.class.getProtectionDomain().getCodeSource().getLocation().toString();
         
         String nativedir = findLibPath();
         final Process p;
         if(s.endsWith(".jar")){
            s = s.substring(s.lastIndexOf("/")+1);
            System.out.println(s);
            p = r.exec("java -classpath lib"+File.separator+" -Dorg.lwjgl.util.Debug=true -Djava.library.path=lib"+File.separator+"native"+File.separator+nativedir+File.separator+" -jar "+s+" -proper");
         }else{
            s = "build/classes/"+"dwimmer"+File.separator+"FullScreenView";
            System.out.println(s);
            p = r.exec("java -classpath lib"+File.separator+" -Dorg.lwjgl.util.Debug=true -Djava.library.path=lib"+File.separator+"native"+File.separator+nativedir+File.separator+" "+s+" -proper");
         }
         
         if(p!=null){
            Scanner er = new Scanner(p.getErrorStream());
            Scanner ot = new Scanner(p.getInputStream());
            while(er.hasNext() || ot.hasNext()){
               if(er.hasNext())
                  System.out.println(er.nextLine());
               if(ot.hasNext())
                  System.out.println(ot.nextLine());
            }
         }
         
         //File f = new File(class.getProtect);
      }
   }
   
   private static String findLibPath(){
      Properties p = System.getProperties();
      p.list(System.out);
      String t = p.getProperty("os.name").toLowerCase();
      if(t.contains("mac")){
         return "macosx";
      }
      if(t.contains("windows")){
         return "windows";
      }
      if(t.contains("solaris")){
         return "solaris";
      }
      return "linux";
   }
   
   /**
    * The only constructor available for FullScreenView, private because it
    * is a singleton.  It is called only in the main method.
    * @param screenDevice the GraphicsDevice which controls the user's monitor.
    */
   private FullScreenView(GraphicsDevice screenDevice) {
      setIgnoreRepaint(true);
      addKeyListener(new KeyAdapter() {

         @Override
         public void keyPressed(KeyEvent ke) {
            press(ke);
         }

         @Override
         public void keyReleased(KeyEvent ke) {
            release(ke);
         }
      });
      this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
      addWindowListener(new WindowAdapter(){
         @Override
         public void windowClosing(WindowEvent e) {
            escape();
         }
      });

      screen = screenDevice;
      initializeGraphics();

      java.awt.EventQueue.invokeLater(new Runnable() {

         public void run() {
            fullScreenMode();
         }
      });
   }

   /**
    * Called by the KeyListener whenever a keyboard key is pressed.
    * @param e the KeyEvent containing the key information.
    */
   public void press(KeyEvent e) {
      if (e.getKeyCode() == KeyEvent.VK_ESCAPE && e.isShiftDown()) {
         escape();
      } else {
         AQEngine.pressKey(e);
      }
   }

   /**
    * Called by the KeyListener whenever a keyboard key is released.
    * @param e the KeyEvent containing the key information.
    */
   public void release(KeyEvent e) {
      AQEngine.releaseKey(e);
   }

   /**
    * Gets the screen dimension information, and makes an Image to buffer with.
    */
   private void initializeGraphics() {
      screenWidth = screen.getDisplayMode().getWidth();
      screenHeight = screen.getDisplayMode().getHeight();
   }

   /**
    * Sets the graphics of the screen to FSEM, if possible.
    * <p>
    * <FONT COLOR="#FF0000"><b>Do not alter this method!</b></FONT>
    */
   private static void fullScreenMode() {
      if (instance == null) {
         java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
               fullScreenMode();
            }
         });
      } else {
         instance.setTitle("FullScreenView");
         instance.originalDisplayMode = instance.screen.getDisplayMode();
         try {
            if (instance.tryFull && instance.screen.isFullScreenSupported()) {
               instance.setUndecorated(true);
               instance.setResizable(false);
               instance.screen.setFullScreenWindow(instance);
               instance.validate();
               instance.isFullScreen = true;
               instance.insetLeft = instance.insetTop = 0;
            } else {
               instance.setBounds(50, 50, instance.screenWidth-100, instance.screenHeight-100);
               instance.setUndecorated(false);
               instance.setVisible(true);
               instance.setResizable(false);
               Insets i = instance.getInsets();
               instance.screenWidth = instance.getWidth() - (i.right + i.left);
               instance.screenHeight = instance.getHeight() - (i.top + i.bottom);
               instance.isFullScreen = false;
               instance.insetLeft = i.left;
               instance.insetTop = i.top;
            }
         } catch (Exception e) {
            instance.closeProgram();
            System.err.println(e);
         }
         System.out.println("View initialized");
         AQEngine.start();
      }
   }

   /**
    * Getter method that returns the width dimension of the screen, in pixels.
    * @return The horizontal resolution of the screen.
    */
   public int getScreenWidth() {
      return screenWidth;
   }

   /**
    * Getter method that returns the height dimension of the screen, in pixels.
    * @return The vertical resolution of the screen.
    */
   public int getScreenHeight() {
      return screenHeight;
   }

   /**
    * Returns the graphics of the screen to the standard graphics, and
    * terminates the processing of the program.
    * <p>
    * <FONT COLOR="#FF0000"><b>Do not alter this method!</b></FONT>
    */
   void closeProgram() {
      AQEngine.stop();
      if (isFullScreen) {
         screen.setDisplayMode(originalDisplayMode);
      }
      System.exit(0);
   }

   /**
    * Draws a given image to the screen, stretching as necessary (using 
    * nearest neighbor algorithm).
    * @param in the image to be drawn to the screen.
    */
   public void drawImage(BufferedImage in) {
      Graphics g = this.getGraphics();
      g.drawImage(in, insetLeft, insetTop, screenWidth, screenHeight, this);
   }
   
   private void escape(){
      GameMode gm;
      if((gm = AQEngine.getCurrentMode()) instanceof QuittingMode)
         closeProgram();
      else
         AQEngine.setMode(new QuittingMode(gm));
   }
}
