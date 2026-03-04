package com.esprit.furhope.services;

import com.esprit.utils.DBConnection;

import java.sql.Connection;
import java.sql.SQLException;

public abstract class ConnectToDbService {

    protected final Connection con;

    protected ConnectToDbService() {
        try {
            this.con = DBConnection.getConnection();
            SocialSchemaService.ensureSocialSchema(this.con);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to initialize social database connection: " + e.getMessage(), e);
        }
    }
}
