package com.projet.services;

import com.projet.entities.Panier;
import com.projet.entities.Produit;
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

