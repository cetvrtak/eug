/*
 * MapPanel.java
 *
 * Created on January 25, 2007, 6:39 PM
 */

package posed;

import eug.shared.GenericObject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import javax.swing.Timer;

/**
 *
 * @author  Michael Myers
 */
public class MapPanel extends javax.swing.JPanel implements Scrollable {
    
    private BufferedImage mapImage;
    private BufferedImage scaledMapImage;
    
    private double scaleFactor = DEFAULT_SCALE_FACTOR;
    private static final double DEFAULT_SCALE_FACTOR = 1.0; //0.8; //0.7;
    
    private static final double MIN_SCALE = 0.05;
    private static final double MAX_SCALE = 5.0;
    private static final double DEFAULT_ZOOM_AMOUNT = 0.2; //0.1;
    
    private static final boolean paintPositions = false; // not working
    
    
    private Map map;
    
    private final MapData mapData;
    
    private final ProvinceData provinceData;
    
    private final GenericObject positions;

    private final GameVersion gameVersion;
    
    /**
     * Mapping which allows this class to override the color of a province.
     * This is used to make provinces blink when selected.
     */
    private transient java.util.Map<Integer, Color> overrides;
    
    /**
     * Creates new form MapPanel.
     * @param mapFileName The name of the file where the map definitions are.
     * The other map files are assumed to be in the same directory.
     * @param positions The contents of the positions.txt file.
     */
    public MapPanel(String mapFileName, GenericObject positions, GameVersion gameVersion, boolean useLocalization) {
        this(new Map(mapFileName, gameVersion, useLocalization), new File(mapFileName).getParentFile(), positions, gameVersion);
    }
    
    public MapPanel(Map map, File mapDir, GenericObject positions, GameVersion gameVersion) {
        this.positions = positions;
        this.map = map;
        this.gameVersion = gameVersion;
        
        createMapImage(mapDir);
        
        provinceData = map.getProvinceData();
        mapData = new MapData(scaledMapImage, Integer.parseInt(map.getString("max_provinces")));
        
        initComponents();
        
        overrides = new HashMap<Integer, Color>();
        
        addMouseMotionListener(new TooltipMouseListener());
    }
    
    private void createMapImage(File mapDir) {
        try {
            String provFileName = map.getString("provinces").replace('\\', '/');
//            if (!provFileName.contains("/"))
//                provFileName = "map/" + provFileName;
            
            String filename = mapDir.getAbsolutePath() + "/" + provFileName;
            System.out.println("Reading map from " + filename);
            mapImage = ImageIO.read(new File(filename));
            rescaleMap();
            
        } catch (IOException ex) {
            javax.swing.JOptionPane.showMessageDialog(null,
                    "Error reading map: " + ex.getMessage(), "Error",
                    javax.swing.JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {

    }// </editor-fold>//GEN-END:initComponents
    
    // <editor-fold defaultstate="collapsed" desc=" Generated Variables ">
    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
    // </editor-fold>
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        final Graphics2D g2D = (Graphics2D) g;
        paintProvinces(g2D);
        
        for (java.util.Map.Entry<Integer, Color> override : overrides.entrySet()) {
            paintProvince(g2D, override.getKey(), override.getValue());
        }
        
        if (paintPositions)
            paintPositions(g2D);
    }
    
    private void paintProvinces(final Graphics2D g2D) {
        g2D.drawImage(scaledMapImage, 0, 0, null);
    }
    
    /**
     * Paint a province the given color.
     */
    public final void paintProvince(final Graphics2D g, int provId, final Color c) {
        paintLines(g, getLinesInProv(provId), c);
    }
    
    /**
     * This is the actual method used to paint a province.
     */
    private void paintLines(final Graphics2D g, final List<Integer[]> lines, final Paint paint) {
        // Quick sanity check
        if (lines == null)
            return;
        
        if (g.getPaint() != paint) {
            g.setPaint(paint);
        }
        for(Integer[] line : lines) {
            paintPixelRect(g, line[1], line[0], line[2], line[0]);
        }
    }
    
    private void paintPixelRect(Graphics2D g, int x1, int y1, int x2, int y2) {
            final double scale = scaleFactor/DEFAULT_SCALE_FACTOR;
            final int maxY = getHeight();

            //int x1,  x2,  y1,  y2;
            double y;
        
            x1 = (int) Math.floor(((double) x1) * scale);
            x2 = (int) (((double) x2) * scale);
            y1 = Math.max((int) Math.floor(((double)y1) * scale - scale), 0);
            y2 = Math.min((int) Math.ceil(((double)y2) * scale + scale), maxY);
//                for (y = y1; y < y2; y++)
//                    g2D.drawLine(x1, y, x2, y);
            g.fillRect(x1, y1, x2-x1, y2-y1);
    }
    
    private void paintPositions(final Graphics2D g) {
        g.setColor(Color.BLACK);
        
        Font oldFont = g.getFont();
        g.setFont(oldFont.deriveFont(Font.BOLD, (float)scaleFactor));
            
        final int numProvs = Integer.parseInt(map.getString("max_provinces"));
        for (int i = 1; i < numProvs; i++) {
            GenericObject positionData = positions.getChild(Integer.toString(i));
            
//            // draw icons
//            for (String name : positionStrings) {
//                drawLocationIfPossible(name, g, positionData);
//            }
            
            drawPort(g, positionData);
            drawText(g, positionData, i);
        }
            
        g.setFont(oldFont);
    }
    
    private List<Integer[]> getLinesInProv(int provId) {
        return mapData.getLinesInProv(provinceData.getProvByID(provId).getColor());
    }
    
//    private void drawLocationIfPossible(final String name, final Graphics2D g, final GenericObject positionData) {
//        if (positionData == null)
//            return;
//        
//        GenericObject temp = positionData.getChild(name);
//        if (temp != null) {
//            g.drawString(name, translateX(temp.getDouble("x"), null), translateY(temp.getDouble("y"), null));
//        }
//    }
    
    private void drawText(final Graphics2D g, final GenericObject positionData, final int provId) {
        GenericObject text = positionData != null ? positionData.getChild("text_position") : null;
        
        float x = -1f;
        float y = -1f;
        
        if (text != null) {
            x = translateX(text.getDouble("x"), null);
            y = translateY(text.getDouble("y"), null);
        } else {
            Rectangle bounds = calculateBounds(getLinesInProv(provId));
            x = (float) (((bounds.getWidth() / 2.0) + bounds.getX()) * scaleFactor); //(float) (image.getImageBounds().getWidth() / 2.0) * (float) ourScale;
            y = (float) ((mapImage.getHeight()-((bounds.getHeight() / 2.0) + bounds.getY())) * scaleFactor); //(float) (image.getImageBounds().getHeight() / 2.0) * (float) ourScale;
        }

        double textScale = positionData != null ? positionData.getDouble("text_scale") : 1.0;
        double textRotation = positionData != null ? positionData.getDouble("text_rotation") : 0.0;

        Font oldFont = null;
        if (textScale > 0.0) {
            oldFont = g.getFont();
            g.setFont(oldFont.deriveFont((float) (scaleFactor * textScale)));
        }
        
        final String provName = provinceData.getProvByID(provId).getName();

        Rectangle2D rect = g.getFontMetrics().getStringBounds(provName, g);
        
        AffineTransform at = null;
        AffineTransform oldTx = null;

        if (textRotation > 0.0) {
            at = AffineTransform.getTranslateInstance(x, y);
            at.rotate(-textRotation);
            oldTx = g.getTransform();
            g.setTransform(at);
        }
        
        if (at != null) {
            g.drawString(provName, (int) (rect.getWidth()/2), (int) (rect.getHeight()/2));
            g.drawString(provName + " not centered", 0, 0);
//            System.out.println("string bounds = " + rect);
        } else {
            g.drawString(provName, x - (int) (rect.getWidth()/2), y - (int) (rect.getHeight()/2));
//            g.drawString(image.getProvName(), x, y);
            g.drawString(provName + " not centered", x, y);
//            System.out.println("string bounds = " + rect);
        }

        if (textRotation > 0.0) {
            g.setTransform(oldTx);
        }

        if (textScale > 0.0) {
            g.setFont(oldFont);
        }
    }
    
    private void drawPort(final Graphics2D g, final GenericObject positionData) {
        if (positionData == null)
            return; // no port unless explicitly specified
        
        GenericObject port = positionData.getChild("port");
        if (port != null) {
            float x = translateX(port.getDouble("x"), null);
            float y = translateY(port.getDouble("y"), null);
            
            AffineTransform at = null;
            AffineTransform oldTx = null;

            double portRotation = positionData.getDouble("port_rotation");

            if (portRotation > 0.0) {
                at = AffineTransform.getTranslateInstance(x, y);
                at.rotate(portRotation);
                oldTx = g.getTransform();
                g.setTransform(at);
            }
            
            if (at != null) {
                g.drawString("port", 0, 0);
            } else {
                g.drawString("port", x, y);
            }
            
            if (portRotation > 0.0) {
                g.setTransform(oldTx);
            }
        }
    }
    
    private float translateX(double coordinate, final Rectangle bounds) {
        // scale and add
        // x_new = (x*scale - xPos)*ourScale + imageOrigin
        coordinate *= scaleFactor;
        if (bounds != null)
            coordinate -= (double)bounds.x;
//        coordinate *= ourScale;
        
//        coordinate += (getWidth() - scaledImage.getWidth())/2;
        return (float) coordinate;
    }
    
    private float translateY(double coordinate, final Rectangle bounds) {
        // flip, scale, and add
        // y_new = (mapHeight - (y*scale + yPos))*ourScale
        coordinate *= scaleFactor;
        if (bounds != null)
            coordinate += (double)bounds.y;
        coordinate = scaledMapImage.getHeight() - coordinate;
//        coordinate *= ourScale;
//        System.out.println("y: " + coordinate);
        return (float) coordinate;
    }
    
    
    @Override
    public Dimension getPreferredSize() {
        if (scaledMapImage == null) {
            System.out.println("scaledMap == null!");
            return super.getPreferredSize();
        }
        return new Dimension(scaledMapImage.getWidth(), scaledMapImage.getHeight());
    }
    
    
    private static final NumberFormat rounder = NumberFormat.getNumberInstance(Locale.US);
    static {
        if (rounder instanceof DecimalFormat) {
            ((DecimalFormat)rounder).setDecimalSeparatorAlwaysShown(true);
            ((DecimalFormat)rounder).setMaximumFractionDigits(2);
        }
    }
    
    /**
     * @deprecated Use {@link zoomIn()}, {@link zoomIn(double)},
     * {@link zoomOut}, or {@link zoomOut(double)} instead.
     */
    @Deprecated
    public void setScaleFactor(double factor) {
        if (factor < 0.05)
            return;
        System.out.println("New scale factor: " + factor);
        scaleFactor = factor;
        rescaleMap();
//        repaint();
    }
    
    public double getScaleFactor() {
        return scaleFactor;
    }
    
    public void zoomIn() {
        zoomIn(DEFAULT_ZOOM_AMOUNT);
    }
    
    public void zoomIn(double amount) {
        if (scaleFactor <= MAX_SCALE - amount) {
            scaleFactor += amount;
            scaleFactor = Double.parseDouble(rounder.format(scaleFactor));
            rescaleMap();
        }
    }
    
    public void zoomOut() {
        zoomOut(DEFAULT_ZOOM_AMOUNT);
    }
    
    public void zoomOut(double amount) {
        if (scaleFactor >= amount + MIN_SCALE) {
            scaleFactor -= amount;
            rescaleMap();
        }
    }
    
    private static final RenderingHints scalingHints = new RenderingHints(null);
    static {
        scalingHints.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        scalingHints.put(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DISABLE);
        scalingHints.put(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
    }

    private boolean isMapRightSideUp() {
        return !gameVersion.isMapInverted();
    }

    private void rescaleMap() {
        if (scaledMapImage != null) {
            scaledMapImage.flush();
            scaledMapImage = null;
        }

        scaledMapImage = new BufferedImage(
                (int) Math.ceil(mapImage.getWidth() * scaleFactor),
                (int) Math.ceil(mapImage.getHeight() * scaleFactor),
                mapImage.getType()
                );

        final BufferedImageOp transform;
        if (isMapRightSideUp()) {
            transform =
                new AffineTransformOp(
                new AffineTransform(scaleFactor, 0.0, 0.0, scaleFactor, 0.0, -scaledMapImage.getHeight()),
                scalingHints
                );
        } else {
            transform =
                new AffineTransformOp(
                new AffineTransform(scaleFactor, 0.0, 0.0, -scaleFactor, 0.0, 0.0),
                scalingHints
                );
        }

        scaledMapImage.createGraphics().drawImage(mapImage, transform, 0, scaledMapImage.getHeight());
    }
    
    
    public ProvinceData.Province getProvinceAt(final Point pt) {
        if (pt == null)
            return null;
        
        final ProvinceData.Province p = provinceData.getProv(scaledMapImage.getRGB(pt.x, pt.y));
        
        if (p == null) {
            java.awt.Color c = new java.awt.Color(scaledMapImage.getRGB(pt.x, pt.y));
            System.err.println("No province registered for " + c.getRed() + "," + c.getGreen() + "," + c.getBlue());
        }
        
        return p;
    }
    
    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }
    
    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 5;
    }
    
    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 50;
    }
    
    @Override
    public boolean getScrollableTracksViewportWidth() {
        return false;
    }
    
    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }
    
    public void goToProv(int provId) {
        Rectangle r =
                calculateBounds(mapData.getLinesInProv(provinceData.getProvByID(provId).getColor()));
        
        // We want the point x + width/2, y + height/2 to be in the
        // center of the viewport.
        int x = r.x + r.width/2;
        int y = r.y + r.height/2;
        
        // Since the bounds were calculated for the default scale factor, we
        // need to adjust them.
        x = (int) (((double) x) * (scaleFactor / DEFAULT_SCALE_FACTOR));
        y = (int) (((double) y) * (scaleFactor / DEFAULT_SCALE_FACTOR));
        
        centerMap(x, y);
    }
    
    
    public void centerMap() {
        centerMap(scaledMapImage.getWidth()/2, scaledMapImage.getHeight()/2);
    }
    
    private void centerMap(int x, int y) {
        java.awt.Dimension size = getParent().getSize();
        x = Math.min(scaledMapImage.getWidth() - size.width, Math.max(0, x - (size.width/2)));
        y = Math.min(scaledMapImage.getHeight() - size.height, Math.max(0, y - (size.height/2)));
        ((JViewport)getParent()).setViewPosition(new Point(x, y));
    }
    
    
    private static java.awt.Rectangle calculateBounds(final List<Integer[]> lines) {
        int xLeft = Integer.MAX_VALUE;
        int xRight = Integer.MIN_VALUE;
        int yTop = Integer.MAX_VALUE;
        int yBot = Integer.MIN_VALUE;
        
        for (Integer[] line : lines) {
            if (line[0] < yTop)
                yTop = line[0];
            
            if (line[0] > yBot)
                yBot = line[0];
            
            if (line[1] < xLeft)
                xLeft = line[1];
            
            if (line[2] > xRight)
                xRight = line[2];
        }
        
        return new java.awt.Rectangle(xLeft, yTop, Math.max(1, xRight-xLeft), Math.max(1, yBot-yTop));
    }
    
    public Color getColor(Point point) {
        if (point == null)
            return null;
        
        return new Color(scaledMapImage.getRGB(point.x, point.y));
    }
    
    public void setToolTipTextForProv(ProvinceData.Province p) {
        if (p == null)
            setToolTipText(null);
        else
            setToolTipText("<html><b>" + p.getName() + "</b><br>ID: " + p.getId() + "</html>");
    }
    
    
    public void colorProvince(final int provId, final Color color) {
        overrides.put(provId, color);
    }
    
    public void uncolorProvince(final int provId) {
        overrides.remove(provId);
    }
    
    public void flashProvince(final int provId) {
        flashProvince(provId, 3);
    }
    
    public void flashProvince(final int provId, final int numFlashes) {
        final ActionListener listener = new ActionListener() {
            private boolean color = true;
            public void actionPerformed(final ActionEvent e) {
                if (color) {
                    colorProvince(provId, Color.WHITE);
                } else {
                    uncolorProvince(provId);
                }
                color = !color;
                repaint();
            }
        };
        
        for (int i = 1; i <= numFlashes*2; i++) {
            Timer t = new Timer(333*i, listener);
            t.setRepeats(false);
            t.start();
        }
    }

    public BufferedImage getMapImage() {
        return mapImage;
    }

    public Map getMap() {
        return map;
    }

    public MapData getMapData() {
        return mapData;
    }
    
    public ProvinceData getProvinceData() {
        return provinceData;
    }

    public boolean isPaintPositions() {
        return paintPositions;
    }

    public void setPaintPositions(boolean paintPositions) {
//        this.paintPositions = paintPositions;
    }
    
    private final class TooltipMouseListener implements MouseMotionListener {
        private ProvinceData.Province lastProv = null;

        @Override
        public void mouseDragged(MouseEvent e) {
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            ProvinceData.Province p = MapPanel.this.getProvinceAt(e.getPoint());
            if (p != lastProv) {
                lastProv = p;
                MapPanel.this.setToolTipTextForProv(p);
            }
        }
    }
    
    public ProvinceImage createImage(int provId) {
        ProvinceData.Province p = provinceData.getProvByID(provId);
        Rectangle provBounds =
                calculateBounds(mapData.getLinesInProv(p.getColor()));
        
        provBounds.x *= scaleFactor;
        provBounds.y *= scaleFactor;
        provBounds.height *= scaleFactor;
        provBounds.width *= scaleFactor;
        
        Rectangle imgBounds = provBounds.getBounds();
            
        
        // expand the box a little
        imgBounds.x = Math.max(0, imgBounds.x-5);
        imgBounds.y = Math.max(0, imgBounds.y-5);
        imgBounds.width = Math.min(scaledMapImage.getWidth()-imgBounds.x, imgBounds.width + 10);
        imgBounds.height = Math.min(scaledMapImage.getHeight()-imgBounds.y, imgBounds.height + 10);

        try {
            BufferedImage img = scaledMapImage.getSubimage(imgBounds.x, imgBounds.y, imgBounds.width, imgBounds.height);
            return new ProvinceImage(img, provBounds, imgBounds, provId, p.getName(), scaleFactor, scaledMapImage.getHeight());
        } catch (java.awt.image.RasterFormatException ex) {
            System.err.println("rectangle is " + imgBounds);
            System.err.println("scaled image's size is " + scaledMapImage.getWidth() + " x " + scaledMapImage.getHeight());
            System.err.println("lines are: ");
            for (Integer[] line : mapData.getLinesInProv(p.getColor())) {
                System.err.println(java.util.Arrays.toString(line));
            }
            throw ex;
        }
    }
    
    public static final class ProvinceImage {
        private BufferedImage image;
        private Rectangle provBounds;
        private Rectangle imgBounds;
        private int provId;
        private String provName;
        private double scale;
        private double mapHeight;

        public ProvinceImage(BufferedImage image, Rectangle provBounds, Rectangle imgBounds, int provId, String provName, double scale, int mapHeight) {
            this.image = image;
            this.provBounds = provBounds;
            this.imgBounds = imgBounds;
            this.provId = provId;
            this.provName = provName;
            this.scale = scale;
            this.mapHeight = mapHeight;
        }

        public BufferedImage getImage() {
            return image;
        }

        public Rectangle getProvBounds() {
            return provBounds;
        }
        
        public Rectangle getImageBounds() {
            return imgBounds;
        }

        public int getProvId() {
            return provId;
        }
        
        public String getProvName() {
            return provName;
        }
        
        public double getScale() {
            return scale;
        }
        
        public double getMapHeight() {
            return mapHeight;
        }
    }
}
