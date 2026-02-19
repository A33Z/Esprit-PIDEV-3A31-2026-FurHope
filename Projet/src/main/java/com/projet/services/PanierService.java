package com.projet.services;

import com.projet.entities.Panier;
import com.projet.entities.Produit;
import com.projet.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PanierService implements CrudService<Panier> {
    private static final int TEMP_CLIENT_ID = 1;

    Connection con;

    public PanierService() {
        con = MyDataBase.getInstance().getConnection();
    }

    @Override
    public void ajouter(Panier p) throws SQLException {

        int TEMP_CLIENT_ID = 1;

        // 1️⃣ Get product info
        String sqlProduit = "SELECT title, price, tva, stock FROM produit WHERE id=?";
        PreparedStatement psProduit = con.prepareStatement(sqlProduit);
        psProduit.setInt(1, p.getIdProduit());

        ResultSet rs = psProduit.executeQuery();

        if (rs.next()) {

            int currentStock = rs.getInt("stock");
            int qtyRequested = p.getQty();

            // 2️⃣ Check stock availability
            if (qtyRequested > currentStock) {
                throw new SQLException("Stock insuffisant !");
            }

            double price = rs.getDouble("price");
            double tva = rs.getDouble("tva");

            double totalP = price * qtyRequested;
            double totalt = tva * qtyRequested;

            // 3️⃣ Insert into panier
            String insert = "INSERT INTO panier(client_id, idProduit, title, totalP, totalt, qty) VALUES (?,?,?,?,?,?)";
            PreparedStatement psInsert = con.prepareStatement(insert);
            psInsert.setInt(1, TEMP_CLIENT_ID);
            psInsert.setInt(2, p.getIdProduit());
            psInsert.setString(3, rs.getString("title"));
            psInsert.setDouble(4, totalP);
            psInsert.setDouble(5, totalt);
            psInsert.setInt(6, qtyRequested);
            psInsert.executeUpdate();

            // 4️⃣ Decrement stock
            String updateStock = "UPDATE produit SET stock = stock - ? WHERE id=?";
            PreparedStatement psUpdate = con.prepareStatement(updateStock);
            psUpdate.setInt(1, qtyRequested);
            psUpdate.setInt(2, p.getIdProduit());
            psUpdate.executeUpdate();

            System.out.println("Produit ajouté + stock mis à jour !");
        }
    }



    @Override
    public void supprimer(int id) throws SQLException {

        // 1️⃣ Get panier row first
        String select = "SELECT idProduit, qty FROM panier WHERE id=?";
        PreparedStatement psSelect = con.prepareStatement(select);
        psSelect.setInt(1, id);

        ResultSet rs = psSelect.executeQuery();

        if (rs.next()) {

            int idProduit = rs.getInt("idProduit");
            int qty = rs.getInt("qty");

            // 2️⃣ Restore stock
            String updateStock = "UPDATE produit SET stock = stock + ? WHERE id=?";
            PreparedStatement psUpdate = con.prepareStatement(updateStock);
            psUpdate.setInt(1, qty);
            psUpdate.setInt(2, idProduit);
            psUpdate.executeUpdate();

            // 3️⃣ Delete from panier
            String delete = "DELETE FROM panier WHERE id=?";
            PreparedStatement psDelete = con.prepareStatement(delete);
            psDelete.setInt(1, id);
            psDelete.executeUpdate();

            System.out.println("Produit supprimé + stock restauré !");
        }
    }


    @Override
    public List<Panier> afficher() throws SQLException {

        List<Panier> paniers = new ArrayList<>();

        String sql = "SELECT id, title, qty, totalP, totalt FROM panier";
        Statement statement = con.createStatement();
        ResultSet rs = statement.executeQuery(sql);

        while (rs.next()) {
            Panier p = new Panier();

            p.setId(rs.getInt("id"));        // 🔥 VERY IMPORTANT
            p.setTitle(rs.getString("title"));
            p.setQty(rs.getInt("qty"));
            p.setTotalP(rs.getDouble("totalP"));
            p.setTotalt(rs.getDouble("totalt"));

            paniers.add(p);
        }

        return paniers;
    }


    @Override
    public void modifier(Panier p) throws SQLException {

        String sql = "UPDATE produit SET title=?, price=?, tva=?, image=?, description=?, stock=? WHERE id=?";

        PreparedStatement ps = con.prepareStatement(sql);

        ps.setString(1, p.getTitle());
        ps.setDouble(2, p.getTotalP());
        ps.setDouble(3, p.getTotalt());
        ps.setString(4, p.getTitle());
        ps.setString(5, p.getTitle());
        ps.setInt(7, p.getId()); // only used for WHERE

        ps.executeUpdate();

        System.out.println("Produit modifié (id inchangé)");
    }
    }

