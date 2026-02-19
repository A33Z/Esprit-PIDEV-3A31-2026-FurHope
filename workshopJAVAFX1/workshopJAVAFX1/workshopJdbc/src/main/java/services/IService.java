package services;

import java.sql.SQLException;
import java.util.List;

public interface IService <T>{

    void add(T t )throws SQLException;

    void update(T t ) throws  SQLException;

    void delete(int id_disponibilite) throws SQLException;

    List<T> read() throws  SQLException ;

}
