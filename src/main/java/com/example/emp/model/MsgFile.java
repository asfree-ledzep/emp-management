package com.example.emp.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MsgFile {
    private Long   fileId;
    private Long   msgId;
    private String fileName;
    private String s3Url;
    private Long   fileSize;
    private String contentType;
}
