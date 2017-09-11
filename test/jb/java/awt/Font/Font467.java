/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.AWTException;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.font.GlyphVector;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/*
 * @test
 * @summary regression test on JRE-467 Wrong rendering of variation sequences
 * @run main/othervm Font467 font467_screenshot1.png font467_screenshot2.png
 */

/*
 * Description: The test draws the letter 'a' on the first step and 'a' with a variation selector. Because variation
 * sequence being used is not standard, it is expected two identical letters 'a' will be rendered in both cases.
 *
 */
public class Font467 extends JFrame implements WindowFocusListener {

    // A font supporting Unicode variation selectors is required
    private static Font FONT;

    private static Robot robot;
    private static String SCREENSHOT_FILE_NAME1, SCREENSHOT_FILE_NAME2;
    private static Font467 frame = new Font467();
    private static Point point1 = new Point(80, 50);
    private static Point point2 = new Point(80, 100);

    private static final Object testCompleted = new Object();

    public static void main(String[] args) throws Exception {

        String fontFileName = Font467.class.getResource("fonts/DejaVuSans.ttf").getFile();
        robot = new Robot();
        if (args.length > 0)
            SCREENSHOT_FILE_NAME1 = args[0];
        if (args.length > 1)
            SCREENSHOT_FILE_NAME2 = args[1];

        try {
            GraphicsEnvironment ge =
                    GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, new File(fontFileName)));
        } catch (IOException | FontFormatException e) {
            e.printStackTrace();
        }
        FONT = new Font("DejaVu Sans", Font.PLAIN, 12);
        synchronized (testCompleted) {

            SwingUtilities.invokeLater(() -> {
                frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                MyComponent myComponent =new MyComponent();
                frame.add(myComponent);
                frame.setSize(200, 200);
                frame.setLocationRelativeTo(null);
                frame.addWindowFocusListener(frame);
                frame.setVisible(true);
            });
            testCompleted.wait();
            frame.setVisible(false);
            frame.dispose();
        }
    }

    @Override
    public void windowGainedFocus(WindowEvent e) {
        robot.delay(100);
        Rectangle rect1 = new Rectangle(frame.getLocation().x + point1.x, frame.getLocation().y + point1.y,
                20, 50);
        Rectangle rect2 = new Rectangle(frame.getLocation().x + point2.x, frame.getLocation().y + point2.y,
                20, 50);
        System.out.println("Taking screen shots");
        try {
            BufferedImage capture1 = new Robot().createScreenCapture(rect1);
            BufferedImage capture2 = new Robot().createScreenCapture(rect2);

            try {
                ImageIO.write(capture1, "png", new File(System.getProperty("test.classes")
                        + File.separator + SCREENSHOT_FILE_NAME1));
                ImageIO.write(capture2, "png", new File(System.getProperty("test.classes")
                        + File.separator + SCREENSHOT_FILE_NAME2));
            } catch (IOException | NullPointerException ex) {
                ex.printStackTrace();
            }

            if (!imagesAreEqual(capture1, capture2)) {
                throw new RuntimeException("Expected: screenshots must be equal");
            }
        } catch (AWTException ex) {
            ex.printStackTrace();
        } finally {

            frame.setVisible(false);
            frame.dispose();

            synchronized (testCompleted) {
                testCompleted.notifyAll();
            }
        }
    }

    @Override
    public void windowLostFocus(WindowEvent e) {
    }

    private static class MyComponent extends JComponent {
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g;
            String text = "a";
            GlyphVector gv = FONT.layoutGlyphVector(g2d.getFontRenderContext(), text.toCharArray(), 0,
                    text.length(), Font.LAYOUT_LEFT_TO_RIGHT);
            g2d.drawGlyphVector(gv, point1.x, point1.y);

            String text2 = "a\ufe00";
            GlyphVector gv2 = FONT.layoutGlyphVector(g2d.getFontRenderContext(), text2.toCharArray(), 0,
                    text2.length(), Font.LAYOUT_LEFT_TO_RIGHT);
            g2d.drawGlyphVector(gv2, point2.x, point2.y);
        }
    }

    private static boolean imagesAreEqual(BufferedImage i1, BufferedImage i2) {
        System.out.println("Comparing screen shots");
        if (i1.getWidth() != i2.getWidth() || i1.getHeight() != i2.getHeight()) return false;
        for (int i = 0; i < i1.getWidth(); i++) {
            for (int j = 0; j < i1.getHeight(); j++) {
                if (i1.getRGB(i, j) != i2.getRGB(i, j)) return false;
            }
        }
        return true;
    }
}