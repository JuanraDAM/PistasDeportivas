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
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.time.LocalDate;
import java.util.Map;
import java.util.HashMap;

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
        return "reservas/reservas"; // Asegúrate de que esta plantilla exista
    }

    @GetMapping("/add")
    public String mostrarFormularioAgregarReserva(Model model) {
        try {
            // Cargar instalaciones
            model.addAttribute("instalaciones", repoInstalacion.findAll());

            // Cargar horarios y ordenarlos por hora de inicio
            List<Horario> horarios = repoHorario.findAll();
            horarios.sort(Comparator.comparing(Horario::getHoraInicio)); // Ordenar horarios por hora de inicio
            
            // Añadir los horarios al modelo
            model.addAttribute("horarios", horarios);

            // Añadir un objeto de reserva vacío al modelo
            model.addAttribute("reserva", new Reserva());

        } catch (Exception e) {
            model.addAttribute("mensaje", "Error al cargar los datos: " + e.getMessage());
            e.printStackTrace();
            return "error"; // Redirigir a una página de error si algo falla al cargar
        }
        return "reservas/add"; // Asegúrate de que esta plantilla exista
    }


    @PostMapping("/add")
    public String agregarReserva(@Valid @ModelAttribute("reserva") Reserva reserva, BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("mensaje", "Error en los datos de la reserva.");
            model.addAttribute("instalaciones", repoInstalacion.findAll());
            model.addAttribute("horarios", repoHorario.findAll());
            return "reservas/add"; // Regresar al formulario si hay errores
        }
        
        // Verificar si ya existe una reserva para la misma pista y horario
        List<Reserva> reservasExistentes = repoReserva.findByHorario(reserva.getHorario());
        if (!reservasExistentes.isEmpty()) {
            model.addAttribute("mensaje", "La pista ya está reservada en este horario."); // Mensaje de error
            model.addAttribute("instalaciones", repoInstalacion.findAll());
            model.addAttribute("horarios", repoHorario.findAll());
            return "reservas/add"; // Regresar al formulario si ya hay una reserva
        }

        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Usuario usuario = repoUsuario.findByUsername(authentication.getName()).get(0);
            reserva.setUsuario(usuario); // Establecer el usuario que realiza la reserva
            repoReserva.save(reserva); // Guardar la reserva en la base de datos
            return "redirect:/reservas"; // Redirigir a la lista de reservas
        } catch (Exception e) {
            model.addAttribute("mensaje", "Error al procesar la reserva: " + e.getMessage());
            e.printStackTrace(); // Imprimir el stack trace en los logs
            return "error"; // Redirigir a una página de error
        }
    }

    @PostMapping("/checkHorarios")
    public String checkHorarios(@RequestParam Long instalacionId, Model model) {
        Instalacion instalacion = repoInstalacion.findById(instalacionId).orElse(null);
        if (instalacion == null) {
            model.addAttribute("mensaje", "Instalación no encontrada.");
            return "reservas/add"; // Regresar al formulario si no se encuentra la instalación
        }

        List<Horario> horarios = repoHorario.findByInstalacion(instalacion);
        LocalDate today = LocalDate.now();
        List<Reserva> reservas = repoReserva.findByHorarioIn(horarios);

        Map<LocalDate, List<Reserva>> reservasPorFecha = new HashMap<>();
        for (Reserva reserva : reservas) {
            reservasPorFecha.computeIfAbsent(reserva.getFecha(), k -> new ArrayList<>()).add(reserva);
        }

        model.addAttribute("instalacion", instalacion);
        model.addAttribute("horarios", horarios);
        model.addAttribute("reservasPorFecha", reservasPorFecha);
        model.addAttribute("today", today);
        return "reservas/horarios"; // Asegúrate de que esta plantilla exista
    }

    @GetMapping("/del/{id}")
    public String eliminarReserva(@PathVariable Long id, Model model) {
        try {
            repoReserva.deleteById(id); // Eliminar la reserva por ID
            return "redirect:/reservas"; // Redirigir a la lista de reservas
        } catch (Exception e) {
            model.addAttribute("mensaje", "Error al eliminar la reserva: " + e.getMessage());
            e.printStackTrace(); // Imprimir el stack trace en los logs
            return "error"; // Redirigir a una página de error
        }
    }
}
