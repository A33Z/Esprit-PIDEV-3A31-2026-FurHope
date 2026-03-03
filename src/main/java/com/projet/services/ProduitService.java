package com.projet.services;

import com.projet.entities.Produit;
import com.projet.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProduitService implements CrudService<Produit> {

    Connection con;

    public ProduitService() {
        con = MyDataBase.getInstance().getConnection();
    }

    @Override
    public void ajouter(Produit p) throws SQLException {

        String sql = "INSERT INTO produit(title, price, tva, image, description, stock) VALUES (?, ?, ?, ?, ?, ?)";

        PreparedStatement ps = con.prepareStatement(sql);

        ps.setString(1, p.getTitle());
        ps.setDouble(2, p.getPrice());
        ps.setDouble(3, p.getTva());
        ps.setString(4, p.getImage());
        ps.setString(5, p.getDescription());
        ps.setInt(6, p.getStock());

        ps.executeUpdate();

        System.out.println("Produit ajouté avec succès !");
    }


    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM produit WHERE id=" + id;
        Statement statement = con.createStatement();
        statement.executeUpdate(sql);

        System.out.println("Produit supprimé !");
    }

    @Override
    public List<Produit> afficher() throws SQLException {
        List<Produit> produits = new ArrayList<>();

        String sql = "SELECT * FROM produit";
        Statement statement = con.createStatement();
        ResultSet rs = statement.executeQuery(sql);

        while (rs.next()) {
            Produit p = new Produit();
            p.setId(rs.getInt("id"));
            p.setTitle(rs.getString("title"));
            p.setPrice(rs.getDouble("price"));
            p.setTva(rs.getDouble("tva"));
            p.setImage(rs.getString("image"));
            p.setDescription(rs.getString("description"));
            p.setStock(rs.getInt("stock"));

            produits.add(p);
        }

        return produits;
    }
    @Override
    public void modifier(Produit p) throws SQLException {

        String sql = "UPDATE produit SET title=?, price=?, tva=?, image=?, description=?, stock=? WHERE id=?";

        PreparedStatement ps = con.prepareStatement(sql);

        ps.setString(1, p.getTitle());
        ps.setDouble(2, p.getPrice());
        ps.setDouble(3, p.getTva());
        ps.setString(4, p.getImage());
        ps.setString(5, p.getDescription());
        ps.setInt(6, p.getStock());
        ps.setInt(7, p.getId()); // only used for WHERE

        ps.executeUpdate();

        System.out.println("Produit modifié (id inchangé)");
    }


    public Produit findById(int id) throws SQLException {

        String sql = "SELECT * FROM produit WHERE id=?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            Produit p = new Produit();
            p.setId(rs.getInt("id"));
            p.setTitle(rs.getString("title"));
            p.setPrice(rs.getDouble("price"));
            p.setTva(rs.getDouble("tva"));
            p.setImage(rs.getString("image"));
            p.setDescription(rs.getString("description"));
            p.setStock(rs.getInt("stock"));
            return p;
        }

        return null;
    }


}
