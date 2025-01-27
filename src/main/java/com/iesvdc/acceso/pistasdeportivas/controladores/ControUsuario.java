package com.iesvdc.acceso.pistasdeportivas.controladores;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.iesvdc.acceso.pistasdeportivas.modelos.Usuario;
import com.iesvdc.acceso.pistasdeportivas.repos.RepoUsuario;

import java.util.Optional;

@Controller
@RequestMapping("/usuario")
public class ControUsuario {

    @Autowired
    RepoUsuario repoUsuario;

    @Autowired
    PasswordEncoder passwordEncoder;

    @GetMapping("")
    public String listarUsuarios(Model modelo) {
        modelo.addAttribute("usuarios", repoUsuario.findAll());
        return "usuarios/usuarios";
    }

    @GetMapping("/add")
    public String mostrarFormularioAlta(Model modelo) {
        modelo.addAttribute("usuario", new Usuario());
        return "usuarios/add";
    }

    @PostMapping("/add")
    public String agregarUsuario(@ModelAttribute("usuario") Usuario usuario) {
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        repoUsuario.save(usuario);
        return "redirect:/usuario";
    }

    @GetMapping("/edit/{id}")
    public String mostrarFormularioEdicion(@PathVariable Long id, Model modelo) {
        Optional<Usuario> usuarioOpt = repoUsuario.findById(id);
        if (usuarioOpt.isPresent()) {
            modelo.addAttribute("usuario", usuarioOpt.get());
            return "usuarios/edit";
        } else {
            return "redirect:/usuario";
        }
    }

    @PostMapping("/edit/{id}")
    public String editarUsuario(@PathVariable Long id, @ModelAttribute("usuario") Usuario usuario) {
        Optional<Usuario> usuarioExistenteOpt = repoUsuario.findById(id);
        if (usuarioExistenteOpt.isPresent()) {
            Usuario usuarioExistente = usuarioExistenteOpt.get();
            usuarioExistente.setUsername(usuario.getUsername());
            usuarioExistente.setEmail(usuario.getEmail());
            if (usuario.getPassword() != null && !usuario.getPassword().isEmpty()) {
                usuarioExistente.setPassword(passwordEncoder.encode(usuario.getPassword()));
            }
            usuarioExistente.setTipo(usuario.getTipo());
            repoUsuario.save(usuarioExistente);
        }
        return "redirect:/usuario";
    }

    @GetMapping("/del/{id}")
    public String eliminarUsuario(@PathVariable Long id) {
        repoUsuario.deleteById(id);
        return "redirect:/usuario";
    }
} 