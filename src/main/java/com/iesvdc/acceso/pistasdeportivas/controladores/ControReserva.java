package com.iesvdc.acceso.pistasdeportivas.controladores;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.BindingResult;
import javax.validation.Valid;

import com.iesvdc.acceso.pistasdeportivas.modelos.Reserva;
import com.iesvdc.acceso.pistasdeportivas.modelos.Usuario;
import com.iesvdc.acceso.pistasdeportivas.modelos.Horario;
import com.iesvdc.acceso.pistasdeportivas.repos.RepoReserva;
import com.iesvdc.acceso.pistasdeportivas.repos.RepoUsuario;
import com.iesvdc.acceso.pistasdeportivas.repos.RepoInstalacion;
import com.iesvdc.acceso.pistasdeportivas.repos.RepoHorario;
import com.iesvdc.acceso.pistasdeportivas.modelos.Instalacion;

import java.util.List;
import java.util.Comparator;

@Controller
@RequestMapping("/reservas")
public class ControReserva {

    @Autowired
    RepoReserva repoReserva;

    @Autowired
    RepoUsuario repoUsuario;

    @Autowired
    RepoInstalacion repoInstalacion;

    @Autowired
    RepoHorario repoHorario;

    @GetMapping("")
    public String listarReservas(Model model, @PageableDefault(size = 10) Pageable pageable) {
        Page<Reserva> page = repoReserva.findAll(pageable);
        model.addAttribute("page", page);
        model.addAttribute("reservas", page.getContent());
        return "reservas/reservas";
    }

    @GetMapping("/add")
    public String mostrarFormularioAgregarReserva(Model model) {
        try {
            model.addAttribute("instalaciones", repoInstalacion.findAll());
            List<Horario> horarios = repoHorario.findAll();
            horarios.sort(Comparator.comparing(Horario::getHoraInicio));
            model.addAttribute("horarios", horarios);
            model.addAttribute("reserva", new Reserva());
        } catch (Exception e) {
            model.addAttribute("mensaje", "Error al cargar los datos: " + e.getMessage());
            e.printStackTrace();
            return "error";
        }
        return "reservas/add";
    }

    @PostMapping("/add")
    public String agregarReserva(@Valid @ModelAttribute("reserva") Reserva reserva, BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("mensaje", "Error en los datos de la reserva.");
            model.addAttribute("instalaciones", repoInstalacion.findAll());
            model.addAttribute("horarios", repoHorario.findAll());
            return "reservas/add";
        }

        List<Reserva> reservasExistentes = repoReserva.findByHorario(reserva.getHorario());
        if (!reservasExistentes.isEmpty()) {
            model.addAttribute("mensaje", "La pista ya está reservada en este horario.");
            model.addAttribute("instalaciones", repoInstalacion.findAll());
            model.addAttribute("horarios", repoHorario.findAll());
            return "reservas/add";
        }

        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Usuario usuario = repoUsuario.findByUsername(authentication.getName()).get(0);
            reserva.setUsuario(usuario);
            repoReserva.save(reserva);
            return "redirect:/reservas";
        } catch (Exception e) {
            model.addAttribute("mensaje", "Error al procesar la reserva: " + e.getMessage());
            e.printStackTrace();
            return "error";
        }
    }

    @GetMapping("/del/{id}")
    public String eliminarReserva(@PathVariable Long id, Model model) {
        try {
            repoReserva.deleteById(id);
            return "redirect:/reservas";
        } catch (Exception e) {
            model.addAttribute("mensaje", "Error al eliminar la reserva: " + e.getMessage());
            e.printStackTrace();
            return "error";
        }
    }
}