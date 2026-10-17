package com.minimarket.importaciones;

import com.minimarket.importaciones.form.ImportacionExcel.Tipo;
import com.minimarket.importaciones.service.LectorExcelService;
import java.io.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LectorExcelServiceTest {
    private final LectorExcelService lector=new LectorExcelService();
    private byte[] archivo(String cantidad,boolean formula) throws Exception {
        try(var wb=new XSSFWorkbook(new ByteArrayInputStream(lector.plantilla(Tipo.STOCK)));var out=new ByteArrayOutputStream()) {
            var fila=wb.getSheetAt(0).createRow(1);fila.createCell(0).setCellValue("Arroz");
            if(formula)fila.createCell(1).setCellFormula("1+1");else fila.createCell(1).setCellValue(cantidad);
            fila.createCell(2).setCellValue("2.33");fila.createCell(3).setCellValue("Compra de prueba");wb.write(out);return out.toByteArray();
        }
    }
    @Test void leeDecimalesSinFloat() throws Exception {
        var fila=lector.leer(Tipo.STOCK,archivo("1,25",false)).get(0);
        assertTrue(fila.errores().isEmpty());assertEquals("1.25",fila.cantidad().toPlainString());
    }
    @Test void rechazaFormulasSinEvaluarlas() throws Exception {
        assertFalse(lector.leer(Tipo.STOCK,archivo("1",true)).get(0).errores().isEmpty());
    }
    @Test void rechazaPrecisionExcesiva() throws Exception {
        assertFalse(lector.leer(Tipo.STOCK,archivo("1.123",false)).get(0).errores().isEmpty());
    }
    @Test void rechazaArchivoVacioODemasiadoGrande() {
        assertThrows(IllegalArgumentException.class,()->lector.leer(Tipo.STOCK,new byte[0]));
        assertThrows(IllegalArgumentException.class,()->lector.leer(Tipo.STOCK,new byte[LectorExcelService.MAX_BYTES+1]));
    }
    @Test void plantillaEsExcelValidoConCabeceras() throws Exception {
        try(var wb=new XSSFWorkbook(new ByteArrayInputStream(lector.plantilla(Tipo.PRODUCTOS)))) {
            assertEquals("nombre",wb.getSheetAt(0).getRow(0).getCell(0).getStringCellValue());assertEquals(1,wb.getNumberOfSheets());
        }
    }
    @Test void rechazaCabeceraConColumnasAdicionales() throws Exception {
        try(var wb=new XSSFWorkbook(new ByteArrayInputStream(archivo("1",false)));var out=new ByteArrayOutputStream()) {
            wb.getSheetAt(0).getRow(0).createCell(4).setCellValue("extra");wb.write(out);
            assertThrows(IllegalArgumentException.class,()->lector.leer(Tipo.STOCK,out.toByteArray()));
        }
    }
}
