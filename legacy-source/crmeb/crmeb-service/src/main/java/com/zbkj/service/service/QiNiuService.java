package com.zbkj.service.service;

import com.zbkj.common.vo.CloudVo;
import com.qiniu.storage.UploadManager;

import java.io.File;

/**
 * QiNiuService 接口


 */
public interface QiNiuService {
    void upload(UploadManager uploadManager, CloudVo cloudVo, String upToken, String webPth, String localFile, Integer id);

    void uploadFile(UploadManager uploadManager, String upToken, String webPth, String localFile, File file);
}
