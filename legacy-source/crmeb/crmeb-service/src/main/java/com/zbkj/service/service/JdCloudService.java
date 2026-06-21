package com.zbkj.service.service;


/**
 * 京东云 Service
 */
public interface JdCloudService {

    /**
     * 文件上传
     *
     * @param fileName      文件名称
     * @param localFilePath 本地文件地址
     * @param bucket        存储桶名称
     */
    void uploadFile(String fileName, String localFilePath, String bucket);

    /**
     * 创建新的存储空间
     */
    void createBucket(String bucketName);

    /**
     * 获取文件URL
     *
     * @param bucket   存储桶名称
     * @param fileName 文件名称
     */
    String getUrl(String bucket, String fileName);
}
