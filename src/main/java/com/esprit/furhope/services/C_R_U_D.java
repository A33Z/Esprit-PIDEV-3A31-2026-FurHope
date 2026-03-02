package com.esprit.furhope.services;

import java.sql.SQLException;
import java.util.List;

public interface C_R_U_D<T> {
    void ajouter(T t) throws SQLException;
    void supprimer(long id) throws SQLException;   // BIGINT -> long
    List<T> afficher() throws SQLException;
    void modifier(T t) throws SQLException;
}
