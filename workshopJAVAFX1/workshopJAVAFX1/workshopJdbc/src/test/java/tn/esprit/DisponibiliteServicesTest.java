package tn.esprit;
import model.Disponibilite;
import org.junit.jupiter.api.*;
import services.ServiceDisponibilite;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DisponibiliteServicesTest {


    static ServiceDisponibilite service;

    static int idDisponibiliteTest;

    @BeforeAll
    static void setup() {

        service = new ServiceDisponibilite();

    }

    @Test
    @Order(1)
    void testAjouterDisponibilite() throws SQLException {
        Disponibilite d = new Disponibilite(2, "Teststart", "Testend", Disponibilite.Statut.VALABLE);
        service.add(d);
        List<Disponibilite> disponibilites = service.read();
        assertFalse(disponibilites.isEmpty());
        assertTrue(
                disponibilites.stream().anyMatch(pers ->
                        pers.getStarttime().equals("Teststart")
                )

        );

        idDisponibiliteTest = disponibilites.get(disponibilites.size()-1).getVetid();

        System.out.println(idDisponibiliteTest);
    }



    @Test

    @Order(2)
    void testModifierDisponibilite() throws SQLException {
        Disponibilite d = new Disponibilite();
        d.setVetid(13);
        d.setStarttime("StarttimModifie");
        d.setEndtime("EndtimeModifie");
        d.setStatut(Disponibilite.Statut.NONVALABLE);
        service.update(d);
        List<Disponibilite> disponibilites = service.read();
        boolean trouve = disponibilites.stream()
                .anyMatch(per ->
                        per.getStarttime().equals("starttilmeModifie"));
        assertTrue(trouve);
    }


    @Test
    @Order(3)
    void testSupprimerDisponibilite() throws SQLException {
        service.delete(13);
        List<Disponibilite> disponibilites = service.read();
        boolean existe = disponibilites.stream().anyMatch(p -> p.getVetid() == idDisponibiliteTest);
        assertFalse(existe);
    }


    @AfterEach
    void cleanUp() throws SQLException {
        List<Disponibilite> disponibilites = service.read();
        if (!disponibilites.isEmpty()) {
            Disponibilite last = disponibilites.get(disponibilites.size() - 1);
            service.delete(last.getVetid());
        } }

}
