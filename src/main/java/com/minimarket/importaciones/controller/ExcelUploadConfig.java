package com.minimarket.importaciones.controller;

import jakarta.servlet.MultipartConfigElement;
import org.springframework.context.annotation.*;

@Configuration
public class ExcelUploadConfig {
    @Bean
    MultipartConfigElement multipartConfigElement() {
        return new MultipartConfigElement("",2L*1024*1024,3L*1024*1024,0);
    }
}
