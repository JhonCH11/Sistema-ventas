package com.minimarket.compras.service;

import com.minimarket.compras.form.ProveedorForm;
import com.minimarket.compras.repository.ProveedorRepository;
import com.minimarket.compras.repository.ProveedorRepository.Proveedor;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class ProveedorService {
    private final ProveedorRepository repo;
    public ProveedorService(ProveedorRepository repo){this.repo=repo;}
    @Transactional(readOnly=true) public List<Proveedor> listar(){return repo.listar();}
    @Transactional(readOnly=true) public Proveedor obtener(Long id){return repo.obtener(id,false).orElseThrow(()->new IllegalArgumentException("Proveedor no encontrado."));}
    @Transactional public void guardar(Long id,ProveedorForm form){
        if(form.getNombre()==null||form.getNombre().isBlank()||form.getNombre().strip().length()>150
                ||(form.getTelefono()!=null&&form.getTelefono().length()>30))throw new IllegalArgumentException("Revise el nombre y telefono del proveedor.");
        if(id!=null)repo.obtener(id,true).orElseThrow(()->new IllegalArgumentException("Proveedor no encontrado."));
        form.setTelefono(form.getTelefono()==null||form.getTelefono().isBlank()?null:form.getTelefono().strip());
        repo.guardar(id,form);
    }
}
