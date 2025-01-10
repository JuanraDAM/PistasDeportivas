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

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Comparator;
import java.util.stream.Collectors;

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

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @GetMapping("")
    public String listarReservas(Model model, @PageableDefault(size = 10) Pageable pageable) {
        try {
            Page<Reserva> page = repoReserva.findAll(pageable);
            model.addAttribute("page", page);
            model.addAttribute("reservas", page.getContent());
        } catch (Exception e) {
            model.addAttribute("mensaje", "Error al cargar las reservas: " + e.getMessage());
            e.printStackTrace();
            return "error";
        }
        return "reservas/reservas";
    }

    @GetMapping("/mis-reservas")
    public String listarMisReservas(Model model) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Usuario usuario = repoUsuario.findByUsername(authentication.getName()).get(0);
            List<Reserva> reservas = repoReserva.findByUsuario(usuario);
            model.addAttribute("reservas", reservas);
        } catch (Exception e) {
            model.addAttribute("mensaje", "Error al cargar las reservas: " + e.getMessage());
            e.printStackTrace();
            return "error";
        }
        return "mis-datos/mis-reservas";
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

        List<Reserva> reservasExistentes = repoReserva.findByInstalacionAndFechaAndHorario(reserva.getInstalacion(), reserva.getFecha(), reserva.getHorario());
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

    @GetMapping("/edit/{id}")
    public String mostrarFormularioEditarReserva(@PathVariable Long id, Model model) {
        try {
            Reserva reserva = repoReserva.findById(id).orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada"));
            model.addAttribute("reserva", reserva);
            model.addAttribute("instalaciones", repoInstalacion.findAll());
            List<Horario> horarios = repoHorario.findAll().stream()
                .distinct()
                .sorted(Comparator.comparing(Horario::getHoraInicio))
                .collect(Collectors.toList());
            model.addAttribute("horarios", horarios);
        } catch (Exception e) {
            model.addAttribute("mensaje", "Error al cargar los datos: " + e.getMessage());
            e.printStackTrace();
            return "error";
        }
        return "reservas/edit";
    }

    @PostMapping("/edit/{id}")
    public String editarReserva(@PathVariable Long id, @Valid @ModelAttribute("reserva") Reserva reserva, BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("mensaje", "Error en los datos de la reserva.");
            cargarDatosReserva(model, reserva);
            return "reservas/edit";
        }

        try {
            Reserva reservaExistente = repoReserva.findById(id).orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada"));
            reservaExistente.setFecha(reserva.getFecha());
            reservaExistente.setHorario(reserva.getHorario());
            reservaExistente.setInstalacion(reserva.getInstalacion());
            repoReserva.save(reservaExistente);
            return "redirect:/reservas";
        } catch (Exception e) {
            model.addAttribute("mensaje", "Error al procesar la reserva: " + e.getMessage());
            cargarDatosReserva(model, reserva);
            e.printStackTrace();
            return "error";
        }
    }

    private void cargarDatosReserva(Model model, Reserva reserva) {
        model.addAttribute("reserva", reserva);
        model.addAttribute("instalaciones", repoInstalacion.findAll());
        model.addAttribute("horarios", repoHorario.findAll().stream()
            .sorted(Comparator.comparing(Horario::getHoraInicio))
            .collect(Collectors.toList()));
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