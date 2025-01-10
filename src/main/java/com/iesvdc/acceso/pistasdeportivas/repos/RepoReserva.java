package com.iesvdc.acceso.pistasdeportivas.repos;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.iesvdc.acceso.pistasdeportivas.modelos.Reserva;
import com.iesvdc.acceso.pistasdeportivas.modelos.Usuario;
import com.iesvdc.acceso.pistasdeportivas.modelos.Horario;
import com.iesvdc.acceso.pistasdeportivas.modelos.Instalacion;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RepoReserva extends JpaRepository<Reserva, Long> {
    List<Reserva> findByUsuario(Usuario usuario);
    Page<Reserva> findByUsuario(Usuario usuario, Pageable pageable);
    List<Reserva> findByHorario(Horario horario);
    List<Reserva> findByHorarioIn(List<Horario> horarios);
    List<Reserva> findByInstalacionAndFecha(Instalacion instalacion, LocalDate fecha);
    List<Reserva> findByInstalacionAndFechaAndHorario(Instalacion instalacion, LocalDate fecha, Horario horario);
}