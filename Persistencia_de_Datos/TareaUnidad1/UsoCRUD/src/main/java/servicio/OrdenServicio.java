package servicio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tb_orden_servicio")
@Getter @Setter @NoArgsConstructor
public class OrdenServicio {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "os_id", nullable = false)
    private Integer id;

    @Column(name = "os_cliente", nullable = false)
    private String cliente;

    @Column(name = "os_equipo", nullable = false)
    private String equipo;

    @Column(name = "os_procesador")
    private String procesador;

    @Column(name = "os_ram")
    private String ram;

    @Column(name = "os_almacenamiento")
    private String almacenamiento;

    @Column(name = "os_falla")
    private String falla;

    // RECIBIDA, EN_REPARACION, FINALIZADA, ENTREGADA
    @Column(name = "os_estado", nullable = false)
    private String estado;

    @Column(name = "os_fecha_ingreso")
    private LocalDate fechaIngreso;

    @Column(name = "os_costo")
    private Double costo;

    public OrdenServicio(String cliente, String equipo, String procesador, String ram,
                         String almacenamiento, String falla, String estado, Double costo) {
        this.cliente = cliente;
        this.equipo = equipo;
        this.procesador = procesador;
        this.ram = ram;
        this.almacenamiento = almacenamiento;
        this.falla = falla;
        this.estado = estado;
        this.costo = costo;
        this.fechaIngreso = LocalDate.now();
    }

    @Override
    public String toString() {
        return "OrdenServicio{id=" + id + ", cliente=" + cliente + ", equipo=" + equipo
                + ", procesador=" + procesador + ", RAM=" + ram + ", almacenamiento=" + almacenamiento
                + ", falla=" + falla + ", estado=" + estado
                + ", fechaIngreso=" + fechaIngreso + ", costo=" + costo + "}";
    }
}