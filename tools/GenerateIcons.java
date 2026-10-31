import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

public class GenerateIcons {

    public static BufferedImage renderIcon(int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        double s = size / 512.0;

        // 1. Base Squircle / Rounded Rectangle
        double padding = 20 * s;
        double bgSize = size - (2 * padding);
        double cornerRadius = 110 * s;

        RoundRectangle2D bgShape = new RoundRectangle2D.Double(
            padding, padding, bgSize, bgSize, cornerRadius, cornerRadius
        );

        // Soft Outer Shadow
        for (int i = 8; i >= 1; i--) {
            float alpha = 0.02f * i;
            g2.setColor(new Color(0f, 0f, 0f, alpha));
            double shadowPad = padding - (i * 1.5 * s);
            double shadowSize = bgSize + (i * 3.0 * s);
            double shadowCorner = cornerRadius + (i * 1.5 * s);
            g2.fill(new RoundRectangle2D.Double(shadowPad, shadowPad + 4 * s, shadowSize, shadowSize, shadowCorner, shadowCorner));
        }

        // Background Gradient: Deep obsidian with rich dark vignette
        Point2D startBg = new Point2D.Double(padding, padding);
        Point2D endBg = new Point2D.Double(padding + bgSize, padding + bgSize);
        Paint bgPaint = new GradientPaint(
            (float) startBg.getX(), (float) startBg.getY(), new Color(0x1E, 0x1E, 0x22),
            (float) endBg.getX(), (float) endBg.getY(), new Color(0x0C, 0x0D, 0x0E)
        );
        g2.setPaint(bgPaint);
        g2.fill(bgShape);

        // Subtle ambient neon-green glow in the center
        RadialGradientPaint centerGlow = new RadialGradientPaint(
            new Point2D.Double(size / 2.0, size / 2.0),
            (float)(190 * s),
            new float[]{0.0f, 0.6f, 1.0f},
            new Color[]{new Color(0x1D, 0xB9, 0x54, 45), new Color(0x1D, 0xB9, 0x54, 12), new Color(0x1D, 0xB9, 0x54, 0)}
        );
        g2.setPaint(centerGlow);
        g2.fill(bgShape);

        // Delicate Squircle Border with top highlight
        Paint borderPaint = new GradientPaint(
            (float)(size / 2.0), (float)padding, new Color(0x44, 0x44, 0x4C, 160),
            (float)(size / 2.0), (float)(padding + bgSize), new Color(0x1A, 0x1A, 0x1E, 120)
        );
        g2.setPaint(borderPaint);
        g2.setStroke(new BasicStroke((float)(2.0 * s)));
        g2.draw(bgShape);

        // 2. Vinyl Sound Grooves (Fine acoustic concentric circles)
        double cx = size / 2.0;
        double cy = size / 2.0;
        g2.setStroke(new BasicStroke((float)(1.2 * s)));
        double[] grooveRadii = { 185 * s, 160 * s, 135 * s };
        for (double r : grooveRadii) {
            g2.setColor(new Color(0xFF, 0xFF, 0xFF, 10));
            g2.draw(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
        }

        // 3. Central Vibrant Emerald Badge
        double badgeRadius = 115 * s;
        Ellipse2D badgeCircle = new Ellipse2D.Double(cx - badgeRadius, cy - badgeRadius, badgeRadius * 2, badgeRadius * 2);

        // Glow behind badge
        for (int i = 5; i >= 1; i--) {
            float glowAlpha = 0.05f * i;
            g2.setColor(new Color(0x1E, 0xD7, 0x60, (int)(glowAlpha * 255)));
            double gr = badgeRadius + (i * 6 * s);
            g2.fill(new Ellipse2D.Double(cx - gr, cy - gr + 2 * s, gr * 2, gr * 2));
        }

        // Badge Gradient (Vibrant Spotify neon emerald)
        Paint badgePaint = new GradientPaint(
            (float)cx, (float)(cy - badgeRadius), new Color(0x1E, 0xD7, 0x60), // #1ED760 bright neon
            (float)cx, (float)(cy + badgeRadius), new Color(0x16, 0x9C, 0x46)  // #169C46 deep emerald
        );
        g2.setPaint(badgePaint);
        g2.fill(badgeCircle);

        // Badge Top Rim Lighting (Glass/Gloss reflection)
        Arc2D badgeRim = new Arc2D.Double(cx - badgeRadius, cy - badgeRadius, badgeRadius * 2, badgeRadius * 2, 0, 180, Arc2D.OPEN);
        g2.setPaint(new GradientPaint(
            (float)cx, (float)(cy - badgeRadius), new Color(0xFF, 0xFF, 0xFF, 110),
            (float)cx, (float)(cy), new Color(0xFF, 0xFF, 0xFF, 0)
        ));
        g2.setStroke(new BasicStroke((float)(2.2 * s)));
        g2.draw(badgeRim);

        // 4. Central Graphic: Stylized Vibe Soundwaves (curved acoustic bars)
        // Draw 3 dynamic curved soundwaves in sleek dark obsidian (#121214)
        g2.setColor(new Color(0x0C, 0x0D, 0x0E));

        drawSoundwave(g2, cx, cy - 26 * s, 68 * s, 17.5 * s, 42, s);
        drawSoundwave(g2, cx, cy + 4 * s, 54 * s, 15.5 * s, 45, s);
        drawSoundwave(g2, cx, cy + 32 * s, 40 * s, 13.5 * s, 48, s);

        g2.dispose();
        return image;
    }

    private static void drawSoundwave(Graphics2D g2, double cx, double cy, double rx, double strokeWidth, double arcDeg, double s) {
        Path2D.Double path = new Path2D.Double();
        // Create smooth arched wave
        double angleRad = Math.toRadians(arcDeg);
        double x1 = cx - rx * Math.cos(angleRad * 0.7);
        double y1 = cy + rx * 0.38;
        
        double xControl = cx;
        double yControl = cy - rx * 0.38;

        double x2 = cx + rx * Math.cos(angleRad * 0.7);
        double y2 = cy + rx * 0.38;

        path.moveTo(x1, y1);
        path.quadTo(xControl, yControl, x2, y2);

        g2.setStroke(new BasicStroke((float)strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(path);
    }

    public static byte[] imageToPngBytes(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", baos);
        return baos.toByteArray();
    }

    public static void writeIco(List<BufferedImage> images, File outputFile) throws IOException {
        List<byte[]> pngBytesList = new ArrayList<>();
        for (BufferedImage img : images) {
            pngBytesList.add(imageToPngBytes(img));
        }

        int count = images.size();
        int headerSize = 6;
        int dirEntrySize = 16;
        int offset = headerSize + (count * dirEntrySize);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        // ICO Header
        dos.writeShort(Short.reverseBytes((short)0)); // Reserved
        dos.writeShort(Short.reverseBytes((short)1)); // Type 1 = Icon
        dos.writeShort(Short.reverseBytes((short)count)); // Image count

        for (int i = 0; i < count; i++) {
            BufferedImage img = images.get(i);
            byte[] png = pngBytesList.get(i);
            int w = img.getWidth() >= 256 ? 0 : img.getWidth();
            int h = img.getHeight() >= 256 ? 0 : img.getHeight();

            dos.writeByte(w); // width
            dos.writeByte(h); // height
            dos.writeByte(0); // color palette count
            dos.writeByte(0); // reserved
            dos.writeShort(Short.reverseBytes((short)1)); // color planes
            dos.writeShort(Short.reverseBytes((short)32)); // bits per pixel
            dos.writeInt(Integer.reverseBytes(png.length)); // size of image data
            dos.writeInt(Integer.reverseBytes(offset)); // file offset
            offset += png.length;
        }

        // Image data
        for (byte[] png : pngBytesList) {
            dos.write(png);
        }

        dos.flush();
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            fos.write(baos.toByteArray());
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Generating Vibe Music icons...");

        int[] icoSizes = { 256, 128, 64, 48, 32, 16 };
        List<BufferedImage> icoImages = new ArrayList<>();
        for (int s : icoSizes) {
            icoImages.add(renderIcon(s));
        }

        // 1. Desktop resources: icon.png (256x256) and icon.ico
        File desktopRes = new File("desktopApp/src/main/resources");
        desktopRes.mkdirs();

        BufferedImage icon256 = icoImages.get(0);
        ImageIO.write(icon256, "PNG", new File(desktopRes, "icon.png"));
        writeIco(icoImages, new File(desktopRes, "icon.ico"));
        System.out.println("Generated desktopApp/src/main/resources/icon.png and icon.ico");

        // 2. Android mipmap icons
        int[] androidSizes = { 48, 72, 96, 144, 192 };
        String[] androidFolders = { "mipmap-mdpi", "mipmap-hdpi", "mipmap-xhdpi", "mipmap-xxhdpi", "mipmap-xxxhdpi" };

        File resDir = new File("androidApp/src/main/res");
        for (int i = 0; i < androidSizes.length; i++) {
            int px = androidSizes[i];
            String folderName = androidFolders[i];
            File dir = new File(resDir, folderName);
            dir.mkdirs();
            BufferedImage mipmapImg = renderIcon(px);
            ImageIO.write(mipmapImg, "PNG", new File(dir, "ic_launcher.png"));
            ImageIO.write(mipmapImg, "PNG", new File(dir, "ic_launcher_round.png"));
        }
        System.out.println("Generated Android mipmap launcher icons");
        System.out.println("All icons generated successfully!");
    }
}
