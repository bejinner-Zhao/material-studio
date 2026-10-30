package com.xiaoying.materialstudio.asset;

/**
 * 按文件头识别图片类型。客户端声明的 Content-Type 可以伪造，
 * 服务端以真实字节为准来判断格式与大小。
 */
public final class ImageTypeDetector {

    private ImageTypeDetector() {
    }

    public static String detect(byte[] header) {
        if (header == null) {
            return null;
        }
        if (header.length >= 3
                && (header[0] & 0xFF) == 0xFF
                && (header[1] & 0xFF) == 0xD8
                && (header[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (header.length >= 8
                && (header[0] & 0xFF) == 0x89
                && header[1] == 'P' && header[2] == 'N' && header[3] == 'G'
                && (header[4] & 0xFF) == 0x0D && (header[5] & 0xFF) == 0x0A
                && (header[6] & 0xFF) == 0x1A && (header[7] & 0xFF) == 0x0A) {
            return "image/png";
        }
        if (header.length >= 12
                && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P') {
            return "image/webp";
        }
        return null;
    }
}
