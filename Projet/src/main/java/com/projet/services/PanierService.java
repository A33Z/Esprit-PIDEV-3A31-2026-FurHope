package com.projet.services;

import com.projet.entities.Panier;
import com.projet.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PanierService implements CrudService<Panier> {

    Connection con;

    public PanierService() {
        con = MyDataBase.getInstance().getConnection();
    }

    @Override
    public void ajouter(Panier p) throws SQLException {

        // fetch product info
        String sqlProduit = "SELECT title, price, tva FROM produit WHERE id=" + p.getIdProduit();
        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery(sqlProduit);

        if (rs.next()) {

            double price = rs.getDouble("price");
            double tva = rs.getDouble("tva");

            // calculate totals
            double totalP = price * p.getQty();
            double totalt = tva * p.getQty();

            // insert into panier
            String sql = "INSERT INTO panier(idProduit, title, totalP, totalt, qty) VALUES ("
                    + p.getIdProduit() + ",'"
                    + rs.getString("title") + "',"
                    + totalP + ","
                    + totalt + ","
                    + p.getQty() + ")";

            st.executeUpdate(sql);

            System.out.println("Produit ajouté au panier !");
        }
    }

    @Override
    public void supprimer(int id) throws SQLException {

        String sql = "DELETE FROM panier WHERE id=" + id;

        Statement statement = con.createStatement();
        statement.executeUpdate(sql);

        System.out.println("Produit supprimé du panier !");
    }

    @Override
    public List<Panier> afficher() throws SQLException {

        List<Panier> paniers = new ArrayList<>();

        String sql = "SELECT title, qty, totalP, totalt FROM panier";
        Statement statement = con.createStatement();
        ResultSet rs = statement.executeQuery(sql);

        while (rs.next()) {
            Panier p = new Panier();
            p.setTitle(rs.getString("title"));
            p.setQty(rs.getInt("qty"));
            p.setTotalP(rs.getDouble("totalP"));
            p.setTotalt(rs.getDouble("totalt"));

            paniers.add(p);
        }

        return paniers;
    }

    @Override
    public void modifier(int id) throws SQLException {

        // get panier info
        String getPanier = "SELECT idProduit, qty FROM panier WHERE id=?";
        PreparedStatement ps1 = con.prepareStatement(getPanier);
        ps1.setInt(1, id);
        ResultSet rs1 = ps1.executeQuery();

        if (rs1.next()) {

            int idProduit = rs1.getInt("idProduit");
            int qty = rs1.getInt("qty");

            // fetch product
            String getProduit = "SELECT price, tva FROM produit WHERE id=?";
            PreparedStatement ps2 = con.prepareStatement(getProduit);
            ps2.setInt(1, idProduit);
            ResultSet rs2 = ps2.executeQuery();

            if (rs2.next()) {

                double price = rs2.getDouble("price");
                double tva = rs2.getDouble("tva");

                double totalP = price * qty;
                double totalt = tva * qty;

                // update panier
                String update = "UPDATE panier SET totalP=?, totalt=? WHERE id=?";
                PreparedStatement ps3 = con.prepareStatement(update);

                ps3.setDouble(1, totalP);
                ps3.setDouble(2, totalt);
                ps3.setInt(3, id);

                ps3.executeUpdate();

                System.out.println("Totaux recalculés !");
            }
        }
    }



}
