package com.xuecheng.media.model.po;

import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class MediaDivFiles implements Serializable {


    //主键id
    private int id;
    //桶
    private String bucket;
    //分开文件路径
    private String filePath;
    //创建人
    private String username;
    //创建时间
    private LocalDateTime createDate;
    //分块文件数量
    private int count;
    //大小
    private Long fileSize;
    //有效时间
    private Long validity;
}
