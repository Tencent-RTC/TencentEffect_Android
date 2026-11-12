package com.tencent.effect.beautykit.download;

/**
 * 下载完成后的结果信息。
 * <p>
 * 相比旧接口只返回文件路径，这里额外携带解析后的文件名与响应的 Content-Type，
 * 供上层据此决定后续处理（例如是否为 zip 需要解压）。
 */
public class TEDownloadResult {

    private final String filePath;
    private final String fileName;
    private final String contentType;

    public TEDownloadResult(String filePath, String fileName, String contentType) {
        this.filePath = filePath;
        this.fileName = fileName;
        this.contentType = contentType;
    }

    /**
     * 最终保存的文件绝对路径。
     */
    public String getFilePath() {
        return filePath;
    }

    /**
     * 解析后的最终文件名（含扩展名），可能为空（旧下载器实现）。
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * 响应的 Content-Type，可能为空。
     */
    public String getContentType() {
        return contentType;
    }
}
