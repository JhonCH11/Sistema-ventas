package com.minimarket.importaciones.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@ControllerAdvice
public class ExcelUploadErrorHandler {
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String archivoGrande(Model model) {
        model.addAttribute("error","El archivo supera el limite de carga. Seleccione un .xlsx de hasta 2 MB.");
        return "importaciones/excel";
    }
}
