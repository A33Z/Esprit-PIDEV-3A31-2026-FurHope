package services;
import model.Rendezvous;
import utils.MyDatabase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class ServiceRendezvous implements IService<Rendezvous> {
    private final MyDatabase database;

    public ServiceRendezvous() {
        database = MyDatabase.getInstance();
    }

    @Override
    public void add(Rendezvous rendezvous) throws SQLException {
        Connection connection = database.getConnectionOrThrow();
        String sql = "INSERT INTO rendezvous (status, description, client_id, num, vet_id, animal_id, disponibilite_id,slotStart) VALUES (?, ?, ?, ?, ?, ?, ?,?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, rendezvous.getStatus());
            ps.setString(2, rendezvous.getDescription());
            ps.setInt(3, rendezvous.getClient_id());
            ps.setInt(4, rendezvous.getNum()); // ✅
            ps.setInt(5, rendezvous.getVet_id());
            ps.setInt(6, rendezvous.getAnimal_id());
            ps.setInt(7, rendezvous.getDisponibilite_id());
            ps.setString(8, rendezvous.getSlotStart());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) rendezvous.setId_rdv(rs.getInt(1));
            }
        }
    }

    @Override
    public void update(Rendezvous rendezvous) throws SQLException {
        Connection connection = database.getConnectionOrThrow();
        String sql = "UPDATE rendezvous SET status = ?, description = ?, client_id = ?, num = ?, vet_id = ?, animal_id = ?, disponibilite_id = ? , slotStart = ? WHERE id_rdv = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, rendezvous.getStatus());
            ps.setString(2, rendezvous.getDescription());
            ps.setInt(3, rendezvous.getClient_id());
            ps.setInt(4, rendezvous.getNum()); // ✅
            ps.setInt(5, rendezvous.getVet_id());
            ps.setInt(6, rendezvous.getAnimal_id());
            ps.setInt(7, rendezvous.getDisponibilite_id());
            ps.setString(8, rendezvous.getSlotStart());
            ps.setInt(9, rendezvous.getId_rdv());

            ps.executeUpdate();
        }
    }

    @Override
    public void delete(int id_rdv) throws SQLException {
        Connection connection = database.getConnectionOrThrow();
        String sql = "DELETE FROM rendezvous WHERE id_rdv = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id_rdv);
            ps.executeUpdate();
        }
    }

    @Override
    public List<Rendezvous> read() throws SQLException {
        Connection connection = database.getConnectionOrThrow();
        String sql = "SELECT * FROM rendezvous ORDER BY id_rdv DESC";
        List<Rendezvous> rendezvousList = new ArrayList<>();
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) rendezvousList.add(mapRow(rs));
        }
        return rendezvousList;
    }

    public List<Rendezvous> readByVetId(int vetId) throws SQLException {
        Connection connection = database.getConnectionOrThrow();
        String sql = "SELECT * FROM rendezvous WHERE vet_id = ? ORDER BY id_rdv DESC";
        List<Rendezvous> rendezvousList = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, vetId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) rendezvousList.add(mapRow(rs));
            }
        }
        return rendezvousList;
    }

    public String getVetEmail(int vetId) throws SQLException {
        Connection conn = database.getConnectionOrThrow();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT email FROM user WHERE id = ?")) {
            ps.setInt(1, vetId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("email");
        }
        return "";
    }

    public String getVetNom(int vetId) throws SQLException {
        Connection conn = database.getConnectionOrThrow();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT first_name, last_name FROM user WHERE id = ?")) {
            ps.setInt(1, vetId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("first_name") + " " + rs.getString("last_name");
        }
        return "";
    }

    public String getClientNum(int clientId) throws SQLException {
        Connection conn = database.getConnectionOrThrow();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT num FROM rendezvous WHERE client_id = ? ORDER BY id_rdv DESC LIMIT 1"
        )) {
            ps.setInt(1, clientId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return String.valueOf(rs.getInt("num"));
        }
        return "";
    }

    public String getClientEmail(int clientId) throws SQLException {
        Connection conn = database.getConnectionOrThrow();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT email FROM user WHERE id = ?")) {
            ps.setInt(1, clientId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("email");
        }
        return "";
    }

    // ✅ num ajouté dans mapRow
    private Rendezvous mapRow(ResultSet rs) throws SQLException {
        Rendezvous r = new Rendezvous();
        r.setId_rdv(rs.getInt("id_rdv"));
        r.setStatus(rs.getString("status"));
        r.setDescription(rs.getString("description"));
        r.setClient_id(rs.getInt("client_id"));
        r.setNum(rs.getInt("num")); // ✅
        r.setVet_id(rs.getInt("vet_id"));
        r.setAnimal_id(rs.getInt("animal_id"));
        r.setDisponibilite_id(rs.getInt("disponibilite_id"));
        r.setSlotStart(rs.getString("slotStart"));
        return r;
    }
    public boolean slotAlreadyTaken(int vetId, int dispoId, String slotStart) throws SQLException {
        Connection conn = database.getConnectionOrThrow();
        String sql = "SELECT COUNT(*) FROM rendezvous WHERE vet_id = ? AND disponibilite_id = ? " +
                "AND slotStart = ? AND status != 'ANNULE'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, vetId);
            ps.setInt(2, dispoId);
            ps.setString(3, slotStart);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        }
        return false;
    }
    public List<String> getTakenSlots(int vetId) throws SQLException {
        Connection conn = database.getConnectionOrThrow();
        String sql = "SELECT slotStart FROM rendezvous WHERE vet_id = ? AND status != 'ANNULE'";
        List<String> taken = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, vetId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String s = rs.getString("slotStart");
                if (s != null) taken.add(s);
            }
        }
        return taken;
    }
    public List<Rendezvous> readByClientId(int clientId) throws SQLException {
        Connection connection = database.getConnectionOrThrow();
        String sql = "SELECT * FROM rendezvous WHERE client_id = ? ORDER BY id_rdv DESC";
        List<Rendezvous> list = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, clientId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }
    // ✅ RDV par jour pour le mois actuel
    public Map<String, Integer> getRdvParJourCeMois(int vetId) throws SQLException {
        Connection conn = database.getConnectionOrThrow();
        String sql = "SELECT DATE(created_at) as jour, COUNT(*) as total " +
                "FROM rendezvous WHERE vet_id = ? " +
                "AND MONTH(created_at) = MONTH(CURRENT_DATE()) " +
                "AND YEAR(created_at) = YEAR(CURRENT_DATE()) " +
                "GROUP BY DATE(created_at) ORDER BY jour";
        Map<String, Integer> data = new LinkedHashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, vetId);
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                data.put(rs.getString("jour"), rs.getInt("total"));
        }
        return data;
    }

    // ✅ Stats mois actuel
    public Map<String, Integer> getStatsMoisActuel(int vetId) throws SQLException {
        Connection conn = database.getConnectionOrThrow();
        String sql = "SELECT status, COUNT(*) as total FROM rendezvous " +
                "WHERE vet_id = ? " +
                "AND MONTH(created_at) = MONTH(CURRENT_DATE()) " +
                "AND YEAR(created_at) = YEAR(CURRENT_DATE()) " +
                "GROUP BY status";
        Map<String, Integer> stats = new HashMap<>();
        stats.put("CONFIRME", 0);
        stats.put("ANNULE", 0);
        stats.put("EN_ATTENTE", 0);
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, vetId);
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                stats.put(rs.getString("status"), rs.getInt("total"));
        }
        return stats;
    }

    // ✅ Stats mois précédent
    public Map<String, Integer> getStatsMoisPrecedent(int vetId) throws SQLException {
        Connection conn = database.getConnectionOrThrow();
        String sql = "SELECT status, COUNT(*) as total FROM rendezvous " +
                "WHERE vet_id = ? " +
                "AND MONTH(created_at) = MONTH(CURRENT_DATE() - INTERVAL 1 MONTH) " +
                "AND YEAR(created_at) = YEAR(CURRENT_DATE() - INTERVAL 1 MONTH) " +
                "GROUP BY status";
        Map<String, Integer> stats = new HashMap<>();
        stats.put("CONFIRME", 0);
        stats.put("ANNULE", 0);
        stats.put("EN_ATTENTE", 0);
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, vetId);
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                stats.put(rs.getString("status"), rs.getInt("total"));
        }
        return stats;
    }
}