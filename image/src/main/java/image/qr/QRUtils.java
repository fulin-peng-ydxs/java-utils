package image.qr;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * 二维码工具类，提供二维码生成功能
 *
 * @author fulin-peng
 * @since 2024-09-20
 */
public class QRUtils {

    private QRUtils() {
        // Utility class should not be instantiated
    }

    /** 默认字符集 */
    private static final String DEFAULT_CHARSET = "UTF-8";

    /** 默认图片格式 */
    private static final String DEFAULT_FORMAT = "PNG";

    /** Logo默认尺寸比例（相对于二维码） */
    private static final int LOGO_RATIO = 5;

    /** Logo默认边框粗细 */
    private static final float LOGO_BORDER_THICKNESS = 2f;

    /** Logo默认圆角大小 */
    private static final int LOGO_CORNER_RADIUS = 15;

    /** 默认边框颜色 */
    private static final Color LOGO_BORDER_COLOR = Color.WHITE;

    /** 错误消息 */
    private static final String GENERATE_ERROR = "生成二维码失败: ";
    private static final String INVALID_SIZE = "二维码尺寸无效";
    private static final String INVALID_CONTENT = "二维码内容不能为空";
    private static final String INVALID_TARGET = "输出目标类型无效";

    /**
     * 生成二维码（根据二维码尺寸和logo有无动态调整容错率）
     * 
     * @param content 二维码内容
     * @param width 二维码宽度
     * @param height 二维码长度
     * @param formatName 二维码图片格式（JPG、PNG等）
     * @param logo 二维码中心图标（Path或InputStream）
     * @param target 二维码输出目标（Path或OutputStream）
     * @throws RuntimeException 如果生成失败
     * @author fulin-peng
     * @since 2024/9/20
     */
    public static void generateQRCode(String content, int width, int height, String formatName,
            Object logo, Object target) {
        generateQRCode(content, width, height, 
            logo == null ? ErrorCorrectionLevel.L : ErrorCorrectionLevel.H,
            formatName, logo, target);
    }

    /**
     * 生成二维码（完整参数）
     * 
     * @param content 二维码内容
     * @param width 二维码宽度
     * @param height 二维码长度
     * @param errorCorrectionLevel 二维码纠错级别
     * @param formatName 二维码图片格式（JPG、PNG等）
     * @param logo 二维码中心图标（Path或InputStream）
     * @param target 二维码输出目标（Path或OutputStream）
     * @throws RuntimeException 如果生成失败
     * @author fulin-peng
     * @since 2024/9/20
     */
    public static void generateQRCode(String content, int width, int height,
            ErrorCorrectionLevel errorCorrectionLevel, String formatName, Object logo, Object target) {
        // 参数验证
        validateParameters(content, width, height, target);
        
        try {
            // 生成二维码矩阵
            BitMatrix bitMatrix = generateQRMatrix(content, width, height, errorCorrectionLevel);
            
            // 处理输出
            if (logo == null) {
                writeQRCode(bitMatrix, formatName, target);
            } else {
                BufferedImage qrImage = createQRImage(bitMatrix, width, height);
                addLogoToQRCode(qrImage, logo);
                writeQRCodeWithLogo(qrImage, formatName, target);
            }
        } catch (Exception e) {
            throw new RuntimeException(GENERATE_ERROR + e.getMessage(), e);
        }
    }

    /**
     * 验证参数
     */
    private static void validateParameters(String content, int width, int height, Object target) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException(INVALID_CONTENT);
        }
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException(INVALID_SIZE);
        }
        if (!(target instanceof Path || target instanceof OutputStream)) {
            throw new IllegalArgumentException(INVALID_TARGET);
        }
    }

    /**
     * 生成二维码矩阵
     */
    private static BitMatrix generateQRMatrix(String content, int width, int height,
            ErrorCorrectionLevel errorCorrectionLevel) throws Exception {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, DEFAULT_CHARSET);
        hints.put(EncodeHintType.ERROR_CORRECTION, errorCorrectionLevel);
        return qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, width, height, hints);
    }

    /**
     * 创建二维码图像
     */
    private static BufferedImage createQRImage(BitMatrix bitMatrix, int width, int height) {
        BufferedImage qrImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                qrImage.setRGB(x, y, bitMatrix.get(x, y) ? Color.BLACK.getRGB() : Color.WHITE.getRGB());
            }
        }
        return qrImage;
    }

    /**
     * 添加Logo到二维码
     */
    private static void addLogoToQRCode(BufferedImage qrImage, Object logo) throws IOException {
        // 加载Logo图片
        BufferedImage logoImage = loadLogoImage(logo);
        
        // 计算Logo尺寸和位置
        int logoWidth = qrImage.getWidth() / LOGO_RATIO;
        int logoHeight = qrImage.getHeight() / LOGO_RATIO;
        int x = (qrImage.getWidth() - logoWidth) / 2;
        int y = (qrImage.getHeight() - logoHeight) / 2;

        // 绘制Logo
        Graphics2D g = qrImage.createGraphics();
        try {
            configureGraphics(g);
            drawLogo(g, logoImage, x, y, logoWidth, logoHeight);
            drawLogoBorder(g, x, y, logoWidth, logoHeight);
        } finally {
            g.dispose();
        }
    }

    /**
     * 加载Logo图片
     */
    private static BufferedImage loadLogoImage(Object logo) throws IOException {
        if (logo instanceof Path) {
            return ImageIO.read(((Path) logo).toFile());
        } else if (logo instanceof InputStream) {
            return ImageIO.read((InputStream) logo);
        } else {
            throw new IllegalArgumentException("不支持的Logo类型");
        }
    }

    /**
     * 配置图形上下文
     */
    private static void configureGraphics(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, 
            RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    }

    /**
     * 绘制Logo
     */
    private static void drawLogo(Graphics2D g, BufferedImage logoImage, int x, int y, 
            int logoWidth, int logoHeight) {
        g.drawImage(logoImage, x, y, logoWidth, logoHeight, null);
    }

    /**
     * 绘制Logo边框
     */
    private static void drawLogoBorder(Graphics2D g, int x, int y, int logoWidth, int logoHeight) {
        g.setStroke(new BasicStroke(LOGO_BORDER_THICKNESS));
        g.setColor(LOGO_BORDER_COLOR);
        g.drawRoundRect(x, y, logoWidth, logoHeight, LOGO_CORNER_RADIUS, LOGO_CORNER_RADIUS);
    }

    /**
     * 写入二维码（无Logo）
     */
    private static void writeQRCode(BitMatrix bitMatrix, String formatName, Object target) 
            throws IOException {
        String format = formatName != null ? formatName : DEFAULT_FORMAT;
        if (target instanceof Path) {
            MatrixToImageWriter.writeToPath(bitMatrix, format, (Path) target);
        } else {
            MatrixToImageWriter.writeToStream(bitMatrix, format, (OutputStream) target);
        }
    }

    /**
     * 写入二维码（带Logo）
     */
    private static void writeQRCodeWithLogo(BufferedImage qrImage, String formatName, Object target) 
            throws IOException {
        String format = formatName != null ? formatName : DEFAULT_FORMAT;
        if (target instanceof Path) {
            ImageIO.write(qrImage, format, ((Path) target).toFile());
        } else {
            ImageIO.write(qrImage, format, (OutputStream) target);
        }
    }
}
