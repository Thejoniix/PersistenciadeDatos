package com.mycompany.usocrud;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.util.List;
import servicio.OrdenServicio;

public class PruebaJPA {

    public static void main(String[] args) {

        // la misma unidad de persistencia del proyecto (persistence.xml)
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("compumaibPU");
        EntityManager em = emf.createEntityManager();

        try {
            cicloDeVida(em);
            consultasJPQL(em);
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            em.close();
            emf.close();
        }
    }

    // =====================================================
    // CICLO DE VIDA DE UNA ENTIDAD
    // =====================================================
    static void cicloDeVida(EntityManager em) {
        System.out.println("=== CICLO DE VIDA ===");

        // 1. NEW: el objeto solo existe en memoria
        OrdenServicio orden = new OrdenServicio("PRUEBA JPA", "Laptop Asus ROG G15",
                "AMD Ryzen 7", "16GB", "SSD 500GB", "Ampliar RAM", "RECIBIDA", 170.0);
        System.out.println("1. NEW      -> gestionada? " + em.contains(orden)
                + " | id: " + orden.getId());

        // 2. MANAGED: el EntityManager la controla
        em.getTransaction().begin();
        em.persist(orden);
        System.out.println("2. MANAGED  -> gestionada? " + em.contains(orden)
                + " | id: " + orden.getId());
        em.getTransaction().commit();

        // un cambio en una entidad gestionada se guarda solo, sin merge
        em.getTransaction().begin();
        orden.setEstado("EN_REPARACION");
        em.getTransaction().commit();
        System.out.println("   Estado cambiado a EN_REPARACION sin llamar a merge");

        // 3. DETACHED: ya no esta controlada
        em.detach(orden);
        System.out.println("3. DETACHED -> gestionada? " + em.contains(orden));

        // un cambio en una entidad detached NO se guarda hasta hacer merge
        orden.setCosto(200.0);
        em.getTransaction().begin();
        orden = em.merge(orden);
        em.getTransaction().commit();
        System.out.println("4. MERGE    -> gestionada? " + em.contains(orden)
                + " | costo guardado: " + orden.getCosto());

        // 5. REMOVED: se borra de la base al hacer commit
        em.getTransaction().begin();
        em.remove(orden);
        System.out.println("5. REMOVED  -> gestionada? " + em.contains(orden));
        em.getTransaction().commit();
        System.out.println("   La orden se elimino de la base de datos");
    }

    // =====================================================
    // CONSULTAS JPQL
    // =====================================================
    static void consultasJPQL(EntityManager em) {
        System.out.println("\n=== GUARDANDO DATOS DE PRUEBA ===");

        OrdenServicio o1 = new OrdenServicio("PRUEBA JPQL Ana", "Laptop Asus ROG G15",
                "AMD Ryzen 7", "16GB", "SSD 500GB", "Ampliar RAM y cambiar disco",
                "EN_REPARACION", 170.0);
        OrdenServicio o2 = new OrdenServicio("PRUEBA JPQL Luis", "Laptop HP Pavilion 15",
                "Intel Core i7", "16GB", "SSD 512GB", "La bateria no carga",
                "RECIBIDA", 70.0);
        OrdenServicio o3 = new OrdenServicio("PRUEBA JPQL Marta", "Laptop Lenovo IdeaPad 3",
                "Intel Core i5", "8GB", "SSD 256GB", "Sobrecalentamiento",
                "FINALIZADA", 45.0);

        em.getTransaction().begin();
        em.persist(o1);
        em.persist(o2);
        em.persist(o3);
        em.getTransaction().commit();
        System.out.println("Se guardaron 3 ordenes de prueba");

        System.out.println("\n=== JPQL 1: ordenes de prueba (parametro con LIKE) ===");
        List<OrdenServicio> lista = em.createQuery(
                "SELECT o FROM OrdenServicio o WHERE o.cliente LIKE :texto ORDER BY o.id",
                OrdenServicio.class)
                .setParameter("texto", "PRUEBA JPQL%")
                .getResultList();
        for (OrdenServicio o : lista) {
            System.out.println(o.getId() + " | " + o.getCliente() + " | " + o.getEquipo()
                    + " | " + o.getEstado() + " | $" + o.getCosto());
        }

        System.out.println("\n=== JPQL 2: solo las que estan RECIBIDA ===");
        List<OrdenServicio> recibidas = em.createQuery(
                "SELECT o FROM OrdenServicio o WHERE o.estado = :estado AND o.cliente LIKE :texto",
                OrdenServicio.class)
                .setParameter("estado", "RECIBIDA")
                .setParameter("texto", "PRUEBA JPQL%")
                .getResultList();
        for (OrdenServicio o : recibidas) {
            System.out.println(o.getCliente() + " | " + o.getFalla());
        }

        System.out.println("\n=== JPQL 3: costo mayor a 50 ===");
        List<OrdenServicio> caras = em.createQuery(
                "SELECT o FROM OrdenServicio o WHERE o.costo > :min AND o.cliente LIKE :texto"
                + " ORDER BY o.costo DESC", OrdenServicio.class)
                .setParameter("min", 50.0)
                .setParameter("texto", "PRUEBA JPQL%")
                .getResultList();
        for (OrdenServicio o : caras) {
            System.out.println(o.getCliente() + " | $" + o.getCosto());
        }

        System.out.println("\n=== JPQL 4: cantidad de ordenes por estado ===");
        List<Object[]> conteo = em.createQuery(
                "SELECT o.estado, COUNT(o) FROM OrdenServicio o"
                + " WHERE o.cliente LIKE :texto GROUP BY o.estado", Object[].class)
                .setParameter("texto", "PRUEBA JPQL%")
                .getResultList();
        for (Object[] fila : conteo) {
            System.out.println(fila[0] + ": " + fila[1]);
        }

        System.out.println("\n=== JPQL 5: suma y promedio del costo ===");
        Object[] totales = em.createQuery(
                "SELECT SUM(o.costo), AVG(o.costo) FROM OrdenServicio o"
                + " WHERE o.cliente LIKE :texto", Object[].class)
                .setParameter("texto", "PRUEBA JPQL%")
                .getSingleResult();
        System.out.println("Suma: $" + totales[0] + " | Promedio: $" + totales[1]);

        // se borran solo las ordenes de prueba (JPQL), tus datos reales no se tocan
        System.out.println("\n=== LIMPIEZA (JPQL DELETE) ===");
        em.getTransaction().begin();
        int borradas = em.createQuery("DELETE FROM OrdenServicio o WHERE o.cliente LIKE :texto")
                .setParameter("texto", "PRUEBA JPQL%")
                .executeUpdate();
        em.getTransaction().commit();
        System.out.println("Ordenes de prueba borradas: " + borradas);
    }
}
