package com.minimarket.compras.repository;

import com.minimarket.compras.form.ProveedorForm;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ProveedorRepository {
    private final JdbcTemplate jdbc;
    public ProveedorRepository(JdbcTemplate jdbc){this.jdbc=jdbc;}
    public record Proveedor(Long id,String nombre,String telefono,boolean activo) {}
    public List<Proveedor> listar(){return jdbc.query("SELECT id,nombre,telefono,activo FROM proveedores ORDER BY nombre",(r,n)->new Proveedor(r.getLong(1),r.getString(2),r.getString(3),r.getBoolean(4)));}
    public Optional<Proveedor> obtener(Long id,boolean bloquear){
        return jdbc.query("SELECT id,nombre,telefono,activo FROM proveedores WHERE id=?"+(bloquear?" FOR UPDATE":""),
                (r,n)->new Proveedor(r.getLong(1),r.getString(2),r.getString(3),r.getBoolean(4)),id).stream().findFirst();
    }
    public void guardar(Long id,ProveedorForm f){
        if(id==null)jdbc.update("INSERT INTO proveedores(nombre,telefono,activo) VALUES(?,?,?)",f.getNombre().strip(),f.getTelefono(),f.isActivo());
        else jdbc.update("UPDATE proveedores SET nombre=?,telefono=?,activo=? WHERE id=?",f.getNombre().strip(),f.getTelefono(),f.isActivo(),id);
    }
}
