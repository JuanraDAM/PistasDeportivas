package com.iesvdc.acceso.pistasdeportivas.controladores;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.iesvdc.acceso.pistasdeportivas.modelos.Usuario;
import com.iesvdc.acceso.pistasdeportivas.repos.RepoUsuario;
import com.iesvdc.acceso.pistasdeportivas.modelos.Rol;

@Controller
@RequestMapping("/register")
public class ControRegistro {

    @Autowired
    RepoUsuario repoUsuario;

    @Autowired
    PasswordEncoder passwordEncoder;

    @GetMapping("")
    public String getRegistro(Model modelo) {
        modelo.addAttribute("usuario", new Usuario());
        return "register"; // Asegúrate de que la plantilla se llama register.html
    }

    @PostMapping("")
    public String postRegistro(@ModelAttribute("usuario") Usuario usuario, Model modelo) {
        // Verificar si el usuario ya existe
        if (!repoUsuario.findByUsername(usuario.getUsername()).isEmpty()) {
            modelo.addAttribute("error", "El nombre de usuario ya está en uso.");
            return "register"; // Regresar a la página de registro si hay un error
        }
        
        // Codificar la contraseña antes de guardar
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        usuario.setEnabled(true); // Asegúrate de que el usuario esté habilitado

        // Asignar un rol por defecto (por ejemplo, USUARIO)
        usuario.setTipo(Rol.USUARIO); // Asegúrate de que Rol.USUARIO esté definido en tu enum Rol

        repoUsuario.save(usuario);
        return "redirect:/login"; // Redirige a la página de login después del registro
    }
} 