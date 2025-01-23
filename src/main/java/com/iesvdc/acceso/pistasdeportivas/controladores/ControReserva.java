package com.iesvdc.acceso.pistasdeportivas.controladores;

import java.util.Comparator;
import java.util.stream.Collectors;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping
    public String listarReservas(Model model, @PageableDefault(size = 10) Pageable pageable,
                                 @RequestParam(required = false) Long instalacionId) {
        try {
            Page<Reserva> page;
            if (instalacionId != null && instalacionId != 0) {
                Instalacion instalacion = repoInstalacion.findById(instalacionId).orElse(null);
                if (instalacion != null) {
                    page = repoReserva.findByInstalacion(instalacion, pageable);
                } else {
                    page = repoReserva.findAll(pageable);
                }
            } else {
                page = repoReserva.findAll(pageable);
            }
            model.addAttribute("page", page);
            model.addAttribute("reservas", page.getContent());
            model.addAttribute("instalaciones", repoInstalacion.findAll());
            model.addAttribute("horarios", repoHorario.findAll());
            model.addAttribute("instalacionId", instalacionId);
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
            model.addAttribute("mensaje", "Error al cargar tus reservas: " + e.getMessage());
            e.printStackTrace();
            return "error";
        }
        return "mis-datos/mis-reservas";
    }

    @GetMapping("/add")
    public String mostrarFormularioAgregarReserva(Model model) {
        try {
            model.addAttribute("reserva", new Reserva());
            model.addAttribute("instalaciones", repoInstalacion.findAll());
            model.addAttribute("horarios", repoHorario.findAll().stream()
                .sorted(Comparator.comparing(Horario::getHoraInicio))
                .collect(Collectors.toList()));
        } catch (Exception e) {
            model.addAttribute("mensaje", "Error al cargar el formulario: " + e.getMessage());
            e.printStackTrace();
            return "error";
        }
        return "reservas/add";
    }

    @PostMapping("/add")
    public String agregarReserva(@Valid @ModelAttribute("reserva") Reserva reserva,
                                 BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("mensaje", "Por favor corrija los errores en el formulario.");
            model.addAttribute("instalaciones", repoInstalacion.findAll());
            model.addAttribute("horarios", repoHorario.findAll().stream()
                .sorted(Comparator.comparing(Horario::getHoraInicio))
                .collect(Collectors.toList()));
            return "reservas/add";
        }

        try {
            if (reserva.getHorario() == null || reserva.getHorario().getId() == null) {
                result.rejectValue("horario", "error.reserva", "Horario es obligatorio");
                model.addAttribute("mensaje", "El horario es obligatorio.");
                model.addAttribute("instalaciones", repoInstalacion.findAll());
                model.addAttribute("horarios", repoHorario.findAll().stream()
                    .sorted(Comparator.comparing(Horario::getHoraInicio))
                    .collect(Collectors.toList()));
                return "reservas/add";
            }

            Horario horarioSeleccionado = repoHorario.findById(reserva.getHorario().getId())
                .orElse(null);
            if (horarioSeleccionado == null) {
                result.rejectValue("horario", "error.reserva", "Horario inválido");
                model.addAttribute("mensaje", "Horario inválido.");
                model.addAttribute("instalaciones", repoInstalacion.findAll());
                model.addAttribute("horarios", repoHorario.findAll().stream()
                    .sorted(Comparator.comparing(Horario::getHoraInicio))
                    .collect(Collectors.toList()));
                return "reservas/add";
            }

            reserva.setHorario(horarioSeleccionado);

            checkReservaConstraints(reserva, false);

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Usuario usuario = repoUsuario.findByUsername(authentication.getName()).get(0);
            reserva.setUsuario(usuario);

            repoReserva.save(reserva);
        } catch (Exception e) {
            model.addAttribute("mensaje", e.getMessage());
            model.addAttribute("instalaciones", repoInstalacion.findAll());
            model.addAttribute("horarios", repoHorario.findAll().stream()
                .sorted(Comparator.comparing(Horario::getHoraInicio))
                .collect(Collectors.toList()));
            e.printStackTrace();
            return "reservas/add";
        }
        return "redirect:/reservas/mis-reservas";
    }

    @GetMapping("/edit/{id}")
    public String mostrarFormularioEditarReserva(@PathVariable Long id, Model model) {
        try {
            Reserva reserva = repoReserva.findById(id).orElse(null);
            if (reserva != null) {
                cargarDatosReserva(model, reserva);
            } else {
                model.addAttribute("mensaje", "Reserva no encontrada.");
                return "error";
            }
        } catch (Exception e) {
            model.addAttribute("mensaje", "Error al cargar la reserva: " + e.getMessage());
            e.printStackTrace();
            return "error";
        }
        return "reservas/edit";
    }

    @PostMapping("/edit/{id}")
    public String editarReserva(@PathVariable Long id,
                                @Valid @ModelAttribute("reserva") Reserva reserva,
                                BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("mensaje", "Por favor corrija los errores en el formulario.");
            model.addAttribute("instalaciones", repoInstalacion.findAll());
            model.addAttribute("horarios", repoHorario.findAll().stream()
                .sorted(Comparator.comparing(Horario::getHoraInicio))
                .collect(Collectors.toList()));
            return "reservas/edit";
        }

        try {
            if (reserva.getHorario() == null || reserva.getHorario().getId() == null) {
                result.rejectValue("horario", "error.reserva", "Horario es obligatorio");
                model.addAttribute("mensaje", "El horario es obligatorio.");
                model.addAttribute("instalaciones", repoInstalacion.findAll());
                model.addAttribute("horarios", repoHorario.findAll().stream()
                    .sorted(Comparator.comparing(Horario::getHoraInicio))
                    .collect(Collectors.toList()));
                return "reservas/edit";
            }

            Horario horarioSeleccionado = repoHorario.findById(reserva.getHorario().getId())
                .orElse(null);
            if (horarioSeleccionado == null) {
                result.rejectValue("horario", "error.reserva", "Horario inválido");
                model.addAttribute("mensaje", "Horario inválido.");
                model.addAttribute("instalaciones", repoInstalacion.findAll());
                model.addAttribute("horarios", repoHorario.findAll().stream()
                    .sorted(Comparator.comparing(Horario::getHoraInicio))
                    .collect(Collectors.toList()));
                return "reservas/edit";
            }

            reserva.setHorario(horarioSeleccionado);

            checkReservaConstraints(reserva, true);

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Usuario usuario = repoUsuario.findByUsername(authentication.getName()).get(0);
            reserva.setUsuario(usuario);

            repoReserva.save(reserva);
        } catch (Exception e) {
            model.addAttribute("mensaje", e.getMessage());
            model.addAttribute("instalaciones", repoInstalacion.findAll());
            model.addAttribute("horarios", repoHorario.findAll().stream()
                .sorted(Comparator.comparing(Horario::getHoraInicio))
                .collect(Collectors.toList()));
            e.printStackTrace();
            return "reservas/edit";
        }
        return "redirect:/reservas/mis-reservas";
    }

    @GetMapping("/del/{id}")
    public String eliminarReserva(@PathVariable Long id, Model model) {
        try {
            Reserva reserva = repoReserva.findById(id).orElse(null);
            if (reserva != null) {
                repoReserva.delete(reserva);
            } else {
                model.addAttribute("mensaje", "Reserva no encontrada.");
                return "error";
            }
        } catch (Exception e) {
            model.addAttribute("mensaje", "Error al eliminar la reserva: " + e.getMessage());
            e.printStackTrace();
            return "error";
        }
        return "redirect:/reservas/mis-reservas";
    }

    private void cargarDatosReserva(Model model, Reserva reserva) {
        try {
            model.addAttribute("reserva", reserva);
            model.addAttribute("instalaciones", repoInstalacion.findAll());
            model.addAttribute("horarios", repoHorario.findAll().stream()
                .sorted(Comparator.comparing(Horario::getHoraInicio))
                .collect(Collectors.toList()));
        } catch (Exception e) {
            model.addAttribute("mensaje", "Error al cargar datos de la reserva: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void checkReservaConstraints(Reserva reserva, boolean isEdit) throws Exception {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Usuario user = repoUsuario.findByUsername(authentication.getName()).get(0);
    
        LocalDate hoy = LocalDate.now();
        LocalDateTime ahora = LocalDateTime.now();
    
        if (reserva.getFecha().isBefore(hoy)) {
            throw new Exception("La fecha de la reserva no puede ser anterior al día de hoy.");
        }
    
        if (reserva.getFecha().isAfter(hoy.plusWeeks(1))) {
            throw new Exception("No se puede reservar con más de una semana de antelación.");
        }
    
        if (isEdit && !reserva.getFecha().isAfter(hoy)) {
            throw new Exception("No se puede actualizar reservas que ya han pasado o son para el día de hoy.");
        }
    
        List<Reserva> reservasSameDay = repoReserva.findByUsuario(user).stream()
            .filter(r -> r.getFecha().equals(reserva.getFecha()) && (reserva.getId() == null || !r.getId().equals(reserva.getId())))
            .collect(Collectors.toList());
        if (!reservasSameDay.isEmpty()) {
            throw new Exception("Ya tienes una reserva para ese día.");
        }
    
        if (reserva.getFecha().isEqual(hoy)) {
            LocalTime horaReserva = reserva.getHorario().getHoraInicio();
            if (horaReserva.isBefore(ahora.toLocalTime())) {
                throw new Exception("No se puede reservar en horas ya pasadas del día actual.");
            }
        }
    
        if (reserva.getHorario() == null || reserva.getHorario().getHoraInicio() == null || reserva.getHorario().getHoraFin() == null) {
            throw new Exception("El horario de la reserva no puede ser nulo.");
        }
        List<Reserva> overlappingReservas = repoReserva.findByInstalacionAndFecha(reserva.getInstalacion(), reserva.getFecha()).stream()
            .filter(r -> r.getHorario() != null &&
                         r.getHorario().getHoraInicio() != null &&
                         r.getHorario().getHoraFin() != null &&
                         r.getHorario().getHoraInicio().isBefore(reserva.getHorario().getHoraFin()) &&
                         r.getHorario().getHoraFin().isAfter(reserva.getHorario().getHoraInicio()) &&
                         (reserva.getId() == null || !r.getId().equals(reserva.getId())))
            .collect(Collectors.toList());
        if (!overlappingReservas.isEmpty()) {
            throw new Exception("El horario seleccionado ya está reservado para esta instalación.");
        }
    }
}