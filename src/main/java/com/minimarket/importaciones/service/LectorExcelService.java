package com.minimarket.importaciones.service;

import com.minimarket.importaciones.form.ImportacionExcel.*;
import com.minimarket.productos.domain.TipoVenta;
import java.io.*;
import java.math.BigDecimal;
import java.util.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.springframework.stereotype.Service;

@Service
public class LectorExcelService {
    public static final int MAX_BYTES=2*1024*1024;
    public LectorExcelService() {ZipSecureFile.setMaxEntrySize(16L*1024*1024);}
    public List<String> cabeceras(Tipo tipo) {return tipo==Tipo.PRODUCTOS?List.of("nombre","categoria","tipo_venta","precio","stock_minimo"):List.of("producto","cantidad","costo_unitario","observacion");}
    public byte[] plantilla(Tipo tipo) {
        try(var libro=new XSSFWorkbook();var salida=new ByteArrayOutputStream()) {
            var hoja=libro.createSheet(tipo.name());var fila=hoja.createRow(0);var columnas=cabeceras(tipo);
            var estilo=libro.createCellStyle();var fuente=libro.createFont();fuente.setBold(true);estilo.setFont(fuente);
            for(int i=0;i<columnas.size();i++){var c=fila.createCell(i);c.setCellValue(columnas.get(i));c.setCellStyle(estilo);hoja.setColumnWidth(i,28*256);}
            hoja.createFreezePane(0,1);libro.write(salida);return salida.toByteArray();
        }catch(IOException ex){throw new IllegalStateException("No se pudo generar la plantilla.",ex);}
    }
    public List<Fila> leer(Tipo tipo,byte[] bytes) {
        if(bytes.length==0||bytes.length>MAX_BYTES)throw new IllegalArgumentException("Seleccione un archivo .xlsx de hasta 2 MB.");
        try(var libro=new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            if(libro.getNumberOfSheets()!=1)throw new IllegalArgumentException("El archivo debe contener una sola hoja.");
            var hoja=libro.getSheetAt(0);var columnas=cabeceras(tipo);
            if(hoja.getLastRowNum()>500)throw new IllegalArgumentException("Se permiten hasta 500 filas de datos.");
            if(hoja.getRow(0)!=null&&hoja.getRow(0).getLastCellNum()>columnas.size())throw new IllegalArgumentException("Hay columnas adicionales. Use la plantilla.");
            for(int i=0;i<columnas.size();i++)if(!texto(hoja.getRow(0),i).equals(columnas.get(i)))throw new IllegalArgumentException("Las columnas deben coincidir con la plantilla: "+String.join(", ",columnas));
            List<Fila> filas=new ArrayList<>();
            for(int n=1;n<=hoja.getLastRowNum();n++) {
                var row=hoja.getRow(n);if(row==null||row.getPhysicalNumberOfCells()==0)continue;
                List<String> errores=new ArrayList<>();String nombre="",categoria="",observacion="";TipoVenta venta=null;
                BigDecimal precio=null,minimo=null,cantidad=null,costo=null;
                try {
                    boolean vacia=true;for(var c:row)if(c.getCellType()!=CellType.BLANK&&!c.toString().isBlank())vacia=false;
                    if(vacia)continue;
                    if(row.getLastCellNum()>columnas.size())throw new IllegalArgumentException("Hay columnas adicionales. Use la plantilla.");
                    nombre=texto(row,0);if(nombre.isBlank()||nombre.length()>150)throw new IllegalArgumentException("Producto requerido, hasta 150 caracteres.");
                    if(tipo==Tipo.PRODUCTOS) {
                        categoria=texto(row,1);if(categoria.isBlank()||categoria.length()>100)throw new IllegalArgumentException("Categoria requerida, hasta 100 caracteres.");
                        try{venta=TipoVenta.valueOf(texto(row,2).toUpperCase(Locale.ROOT));}catch(Exception ex){throw new IllegalArgumentException("tipo_venta debe ser PESO o UNIDAD.");}
                        precio=decimal(row,3,false);minimo=decimal(row,4,true);
                        if(venta==TipoVenta.UNIDAD&&minimo.stripTrailingZeros().scale()>0)throw new IllegalArgumentException("El minimo por unidad debe ser entero.");
                    } else {
                        cantidad=decimal(row,1,false);costo=decimal(row,2,false);observacion=texto(row,3);
                        if(observacion.length()>180)throw new IllegalArgumentException("La observacion admite hasta 180 caracteres.");
                    }
                }catch(IllegalArgumentException ex){errores.add(ex.getMessage());}
                filas.add(new Fila(n+1,nombre,categoria,venta,precio,minimo,cantidad,costo,observacion,null,null,List.copyOf(errores)));
            }
            if(filas.isEmpty())throw new IllegalArgumentException("El archivo no contiene productos para importar.");
            return List.copyOf(filas);
        }catch(IllegalArgumentException ex){throw ex;}catch(Exception ex){throw new IllegalArgumentException("No se pudo leer el Excel. Use un archivo .xlsx sin contrasena ni formulas.");}
    }
    private static String texto(Row row,int columna) {
        var c=row==null?null:row.getCell(columna);if(c==null||c.getCellType()==CellType.BLANK)return "";
        if(c.getCellType()!=CellType.STRING)throw new IllegalArgumentException("Los nombres, categorias y observaciones deben ser texto, sin formulas.");
        return c.getStringCellValue().strip();
    }
    private static BigDecimal decimal(Row row,int columna,boolean cero) {
        var c=row.getCell(columna);String valor;
        if(c==null)throw new IllegalArgumentException("Falta un importe o cantidad.");
        if(c.getCellType()==CellType.NUMERIC&&!DateUtil.isCellDateFormatted(c))valor=((XSSFCell)c).getRawValue();
        else if(c.getCellType()==CellType.STRING)valor=c.getStringCellValue().strip().replace(',','.');
        else throw new IllegalArgumentException("Use numeros, sin fechas ni formulas.");
        try {
            var numero=new BigDecimal(valor);
            if(numero.signum()<(cero?0:1)||numero.stripTrailingZeros().scale()>2||numero.precision()-numero.scale()>10)throw new NumberFormatException();
            return numero;
        }catch(NumberFormatException ex){throw new IllegalArgumentException("Use valores "+(cero?"no negativos":"positivos")+" con hasta dos decimales, sin separadores de miles.");}
    }
}
