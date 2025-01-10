package com.iesvdc.acceso.pistasdeportivas.repos;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import com.iesvdc.acceso.pistasdeportivas.modelos.Usuario;

public interface RepoUsuario extends JpaRepository<Usuario, Long> {
    List<Usuario> findByUsername(String username);
}
