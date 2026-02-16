package com.projet.services;

import com.projet.entities.Produit;

import java.sql.SQLException;
import java.util.List;

public interface CrudService <T>{
    void ajouter(T t) throws SQLException;
    void supprimer(int id) throws SQLException;
    List<T> afficher() throws SQLException;
    void modifier(T t) throws SQLException;


}
