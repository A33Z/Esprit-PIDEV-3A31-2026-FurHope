package com.esprit.furhope.services;

import com.esprit.furhope.utils.thisWhereIconnectToDb;
import java.sql.Connection;

public abstract class ConnectToDbService {

    protected final Connection con;

    protected ConnectToDbService() {
        this.con = thisWhereIconnectToDb.getInstance().getConnection();
        if (this.con == null) {
            throw new IllegalStateException("Database connection is null");
        }
    }
}
